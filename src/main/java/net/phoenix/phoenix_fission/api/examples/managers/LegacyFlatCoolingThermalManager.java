package net.phoenix.phoenix_fission.api.examples.managers;

import net.phoenix.phoenix_fission.api.block.IFissionCoolerType;
import net.phoenix.phoenix_fission.common.data.multiblock.fission.FissionWorkableElectricMultiblockMachine;
import net.phoenix.phoenix_fission.common.data.multiblock.fission.managers.FissionThermalManager;
import net.phoenix.phoenix_fission.configs.PhoenixFissionConfigs;

import java.util.Map;

/**
 * EXAMPLE -- LEGACY PHYSICS: Pre-refactor Flat Cooling Thermal Model
 *
 * Ports the original Phoenix Core thermal logic into the new manager API.
 * This is a full replacement of tickThermalLogic, because the old model's
 * structure differs enough from the current one that individual hook overrides
 * would not cleanly express it.
 *
 * Key differences from the current default:
 *
 * 1. FLAT COOLING (not conductance-based)
 * Old: removed = min(heat - minHeat, sum(cooler.getCoolerTemperature()))
 * Coolers contribute a flat "temperature rating" that is subtracted
 * directly from heat each tick instead of using a conductivity constant.
 *
 * 2. IDLE COOLING (heat decay when reactor is off)
 * When not running, active coolers are applied at 25% efficiency, AND
 * a separate passive idle drain of max(idleHeatLoss, heat * 0.5%)
 * pulls heat toward zero.
 * The new model instead drives heat toward 293K via conductivity.
 *
 * 3. NO AMBIENT TEMPERATURE CONVERGENCE when running
 * Passive cooling only applies when the reactor is off; when running,
 * all heat removal comes from the installed cooler blocks.
 *
 * 4. MELTDOWN TIMER -- same algorithm as the current default, preserved.
 *
 * Usage (pair with LegacyPhysicsFuelManager for full old-model behaviour):
 * 
 * <pre>{@code
 * 
 * @Override
 * protected FissionThermalManager createThermalManager() {
 *     return new LegacyFlatCoolingThermalManager(this, 2.0);
 * }
 * }</pre>
 *
 * @param idleHeatLoss minimum HU drained per tick when reactor is off
 *                     (old cfg.idleHeatLoss default = 2.0)
 */
public class LegacyFlatCoolingThermalManager extends FissionThermalManager {

    /** Minimum heat drained per tick while the reactor is idle. */
    private final double idleHeatLoss;

    public LegacyFlatCoolingThermalManager(FissionWorkableElectricMultiblockMachine machine,
                                           double idleHeatLoss) {
        super(machine);
        this.idleHeatLoss = Math.max(0.0, idleHeatLoss);
    }

    /** Convenience constructor with old-default idle loss of 2.0 HU/t. */
    public LegacyFlatCoolingThermalManager(FissionWorkableElectricMultiblockMachine machine) {
        this(machine, 2.0);
    }

    // =========================================================================
    // Full thermal loop replacement
    // =========================================================================

    /**
     * Replaces the entire thermal tick with the old flat-cooling model.
     * Structure mirrors old handleReactorLogic's heat/cooling section.
     */
    @Override
    public void tickThermalLogic(boolean running) {
        machine.lastHeatGainedPerTick = 0.0;
        machine.lastHeatRemovedPerTick = 0.0;
        machine.lastGeneratedEUt = 0;

        var hm = PhoenixFissionConfigs.INSTANCE.fission.heatModel;
        var cfg = PhoenixFissionConfigs.INSTANCE.fission;
        var comp = machine.getComponentManager();

        // -- Reactivity ramp (same as default) --------------------------------
        if (running && !comp.getActiveFuelRods().isEmpty() && !machine.isScramActive()) {
            machine.setReactivityFactor(Math.min(1.0,
                    machine.getReactivityFactor() + hm.reactivityRampRatePerTick));
            machine.continuousBurnTicks++;
        } else {
            machine.setReactivityFactor(Math.max(0.0,
                    machine.getReactivityFactor() - hm.reactivityRampRatePerTick));
            machine.continuousBurnTicks = 0;
        }

        // -- Heat production (delegated to fuel manager) ----------------------
        if (running && !comp.getActiveFuelRods().isEmpty()) {
            double heatProduced = machine.getFuelManager().calculateTickHeat(machine.lastParallels);
            machine.setHeat(machine.getHeat() + heatProduced);
            machine.lastHeatGainedPerTick = heatProduced;

            machine.getFuelManager().consumeFuelTick(machine.lastParallels);
        }

        // -- Cooling ----------------------------------------------------------
        int totalCooling = comp.getActiveCoolers().stream()
                .mapToInt(IFissionCoolerType::getCoolerTemperature)
                .sum();
        machine.lastProvidedCooling = totalCooling;

        double removed = 0.0;

        if (running) {
            // Active cooling at full power.
            machine.lastHasCoolant = true;
            if (cfg.coolingRequiresCoolant && !comp.getActiveCoolers().isEmpty()) {
                machine.lastHasCoolant = legacyCanConsumeCoolant() && legacyConsumeCoolant();
            }

            if (!cfg.coolingRequiresCoolant || machine.lastHasCoolant) {
                double aboveMin = Math.max(0.0, machine.getHeat() - hm.minHeat);
                removed = Math.min(aboveMin, totalCooling);
                machine.setHeat(machine.getHeat() - removed);
            }
        } else {
            // Idle: active cooling at 25% + passive idle drain.
            machine.lastHasCoolant = true;

            double aboveMin = Math.max(0.0, machine.getHeat() - hm.minHeat);
            double idleCooling = Math.min(aboveMin, totalCooling * 0.25);
            machine.setHeat(machine.getHeat() - idleCooling);
            removed += idleCooling;

            // Passive drain: at least idleHeatLoss, proportional to current heat.
            double variableDrain = machine.getHeat() * 0.005;
            double passiveDrain = Math.min(
                    Math.max(0.0, machine.getHeat() - hm.minHeat),
                    Math.max(idleHeatLoss, variableDrain));
            machine.setHeat(machine.getHeat() - passiveDrain);
            removed += passiveDrain;
        }

        machine.lastHeatRemovedPerTick = removed;

        clampHeat();
        machine.getFuelManager().processPowerGeneration(running);
        processMeltdownTimer();
    }

    // =========================================================================
    // Coolant helpers (mirrors base class logic, kept here for self-containment)
    // =========================================================================

    private boolean legacyCanConsumeCoolant() {
        return processCoolantAction(false);
    }

    private boolean legacyConsumeCoolant() {
        return processCoolantAction(true);
    }

    private boolean processCoolantAction(boolean execute) {
        var cfg = PhoenixFissionConfigs.INSTANCE.fission;
        var comp = machine.getComponentManager();
        if (comp.getActiveCoolers().isEmpty()) return true;

        if (!cfg.coolantUsageAdditive) {
            IFissionCoolerType primary = comp.getPrimaryCoolerType();
            if (primary == null) return true;
            return handleFluidConversion(
                    primary.getInputCoolantFluidId(), primary.getOutputCoolantFluidId(),
                    primary.getCoolantPerTick(), execute);
        }

        // Additive mode: aggregate per coolant type.
        java.util.Map<String, Integer> requirements = new java.util.HashMap<>();
        java.util.Map<String, String> outputs = new java.util.HashMap<>();

        for (IFissionCoolerType c : comp.getActiveCoolers()) {
            if (c.getCoolantPerTick() <= 0 || c.getInputCoolantFluidId().isEmpty() ||
                    "none".equalsIgnoreCase(c.getInputCoolantFluidId()))
                continue;
            requirements.merge(c.getInputCoolantFluidId(), c.getCoolantPerTick(), Integer::sum);
            outputs.put(c.getInputCoolantFluidId(), c.getOutputCoolantFluidId());
        }

        for (var entry : requirements.entrySet()) {
            if (!handleFluidConversion(entry.getKey(), outputs.get(entry.getKey()),
                    entry.getValue(), execute))
                return false;
        }
        return true;
    }

    private boolean handleFluidConversion(String inId, String outId, int amount, boolean execute) {
        if (amount <= 0 || inId.isEmpty() || "none".equalsIgnoreCase(inId)) return true;

        net.minecraft.resources.ResourceLocation inRl = net.minecraft.resources.ResourceLocation.tryParse(inId);
        if (inRl == null || !net.minecraftforge.registries.ForgeRegistries.FLUIDS.containsKey(inRl))
            return false;

        net.minecraftforge.fluids.FluidStack inStack = new net.minecraftforge.fluids.FluidStack(
                net.minecraftforge.registries.ForgeRegistries.FLUIDS.getValue(inRl), amount);

        if (!machine.executeFluidIO(inStack, com.gregtechceu.gtceu.api.capability.recipe.IO.IN, !execute))
            return false;

        if (execute && !outId.isEmpty() && !"none".equalsIgnoreCase(outId) && !outId.equalsIgnoreCase(inId)) {
            net.minecraft.resources.ResourceLocation outRl = net.minecraft.resources.ResourceLocation.tryParse(outId);
            if (outRl != null && net.minecraftforge.registries.ForgeRegistries.FLUIDS.containsKey(outRl)) {
                machine.executeFluidIO(
                        new net.minecraftforge.fluids.FluidStack(
                                net.minecraftforge.registries.ForgeRegistries.FLUIDS.getValue(outRl), amount),
                        com.gregtechceu.gtceu.api.capability.recipe.IO.OUT, false);
            }
        }
        return true;
    }

    // =========================================================================
    // Meltdown timer (same algorithm as default, exposed so subclasses can still
    // override computeMeltdownGracePeriodTicks without duplicating the loop)
    // =========================================================================

    private void processMeltdownTimer() {
        var md = PhoenixFissionConfigs.INSTANCE.fission.meltdown;

        double maxSafeHU = machine.getMaxSafeHeatHU();
        double maxClampHU = machine.getMaxHeatClampHU();

        if (machine.getHeat() >= maxClampHU) {
            machine.setHeat(maxClampHU);
            machine.meltdownTimerMax = (int) Math.max(1, Math.floor(md.minGraceSeconds * 20.0));
            if (machine.meltdownTimerTicks < 0) machine.meltdownTimerTicks = machine.meltdownTimerMax;
        } else if (machine.meltdownTimerTicks > 0) {
            if (!machine.isScramActive()) machine.meltdownTimerTicks--;
            if (machine.meltdownTimerTicks == 0) triggerDetonation();
            return;
        }

        if (machine.getHeat() <= maxSafeHU) {
            if (md.clearTimerWhenSafe) {
                machine.meltdownTimerTicks = -1;
                machine.meltdownTimerMax = 0;
            }
            return;
        }

        machine.meltdownTimerMax = computeMeltdownGracePeriodTicks(
                machine.getHeat(), maxSafeHU,
                md.minGraceSeconds, md.baseGraceSeconds, md.excessHeatSeverity);

        if (machine.meltdownTimerTicks < 0) {
            machine.meltdownTimerTicks = machine.meltdownTimerMax;
        } else if (machine.meltdownTimerTicks > 0) {
            machine.meltdownTimerTicks--;
        }

        if (machine.meltdownTimerTicks == 0) triggerDetonation();
    }

    private void triggerDetonation() {
        if (!(machine.getLevel() instanceof net.minecraft.server.level.ServerLevel world)) return;

        var exp = PhoenixFissionConfigs.INSTANCE.fission.explosion;
        float power = computeExplosionPower(
                exp.baseExplosionPower, exp.explosionPowerPerFuelRod, exp.explosionPowerPerHeatUnit);

        var pos = machine.getPos();

        // Snapshot cache BEFORE onStructureInvalid clears it.
        java.util.List<net.minecraft.core.BlockPos> cached = new java.util.ArrayList<>(
                machine.getMultiblockState().getCache() != null ? machine.getMultiblockState().getCache() :
                        java.util.List.of());

        machine.onStructureInvalid();

        if (exp.destructiveExplosion) {
            for (var targetPos : cached) {
                if (targetPos.equals(pos)) continue;
                var state = world.getBlockState(targetPos);
                boolean vaporize = state
                        .is(net.phoenix.phoenix_fission.common.data.block.PhoenixFissionBlocks.FISSILE_HEAT_SAFE_CASING
                                .get()) ||
                        state.is(
                                net.phoenix.phoenix_fission.common.data.block.PhoenixFissionBlocks.FISSILE_REACTION_SAFE_CASING
                                        .get()) ||
                        state.is(
                                net.phoenix.phoenix_fission.common.data.block.PhoenixFissionBlocks.FISSILE_SAFE_GEARBOX_CASING
                                        .get()) ||
                        state.is(net.minecraft.world.level.block.Blocks.TINTED_GLASS) ||
                        state.getBlock() instanceof net.phoenix.phoenix_fission.common.data.block.FissionFuelRodBlock ||
                        state.getBlock() instanceof net.phoenix.phoenix_fission.common.data.block.FissionModeratorBlock;

                if (vaporize) {
                    world.removeBlock(targetPos, false);
                    world.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                            targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5,
                            3, 0.1, 0.1, 0.1, 0.05);
                }
            }
        }

        world.explode(null,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                power, false, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
        world.removeBlock(pos, false);

        machine.meltdownTimerTicks = -1;
        machine.setHeat(PhoenixFissionConfigs.INSTANCE.fission.heatModel.minHeat);
    }
}
