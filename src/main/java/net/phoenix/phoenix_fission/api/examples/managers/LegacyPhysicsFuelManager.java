package net.phoenix.phoenix_fission.api.examples.managers;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.phoenix_fission.api.block.IFissionFuelRodType;
import net.phoenix.phoenix_fission.api.block.IFissionModeratorType;
import net.phoenix.phoenix_fission.common.data.multiblock.fission.FissionWorkableElectricMultiblockMachine;
import net.phoenix.phoenix_fission.common.data.multiblock.fission.managers.FissionFuelManager;
import net.phoenix.phoenix_fission.configs.PhoenixFissionConfigs;

import java.util.List;

/**
 * EXAMPLE -- LEGACY PHYSICS: Pre-refactor Heat and Parallel Model
 *
 * Ports the original Phoenix Core fission math into the new manager API.
 * Uses three formula differences that make it feel quite distinct from the
 * current default:
 *
 * 1. ADDITIVE MODERATOR HEAT (not multiplicative EU boost)
 * Old: heatMult = 0.1 + sum(mod.getHeatMultiplier()) / 10.0
 * Uses getHeatMultiplier() (= tier * 0.5) rather than getEUBoost().
 * This rewards stacking high-tier moderators differently.
 *
 * 2. NO THERMAL RUNAWAY
 * Heat and fuel consumption do not scale with current reactor temperature.
 * Output is flat until burnup bonus kicks in.
 *
 * 3. BURN BONUS (long-run reward)
 * Running continuously ramps a multiplier from 1.0 to 1+maxBonus over
 * burnBonusRampSeconds. Shutting down resets continuousBurnTicks to 0.
 * The new config already has burnBonusMaxPercent / burnBonusRampSeconds.
 *
 * 4. HEAT-BASED AND MODERATOR-BASED PARALLELS
 * Parallels scale with rod count, moderator parallel bonus, AND
 * optionally with current heat (more heat = more parallels up to the cap).
 * heatPerParallel = 0 disables heat-based parallel scaling.
 *
 * Optional constructor parameters expose the old config fields that do not
 * exist in PhoenixFissionConfigs. Pass them from your machine's
 * createFuelManager() factory, or use the defaults() convenience constructor.
 *
 * Usage (in your machine subclass):
 * 
 * <pre>{@code
 * 
 * @Override
 * protected FissionFuelManager createFuelManager() {
 *     return new LegacyPhysicsFuelManager(this,
 *             0.0,    // baseHeatPerTick: flat heat added regardless of rods
 *             1000.0, // heatPerParallel: every 1000 HU adds one extra parallel
 *             true,   // fuelScalesWithRodCount
 *             false   // fuelScalesWithParallels
 *     );
 * }
 * }</pre>
 *
 * Usage with all defaults:
 * 
 * <pre>{@code
 * 
 * @Override
 * protected FissionFuelManager createFuelManager() {
 *     return new LegacyPhysicsFuelManager(this);
 * }
 * }</pre>
 */
public class LegacyPhysicsFuelManager extends FissionFuelManager {

    /** Flat heat contribution per tick independent of rods (from old cfg.baseHeatPerTick). */
    private final double baseHeatPerTick;

    /**
     * Every this-many HU of current heat grants +1 parallel.
     * Set to 0 to disable heat-based parallel scaling.
     */
    private final double heatPerParallel;

    /** Whether fuel consumption scales with the number of installed fuel rods. */
    private final boolean fuelScalesWithRodCount;

    /** Whether fuel consumption scales with computed parallels. */
    private final boolean fuelScalesWithParallels;


    /** All-defaults constructor. Mirrors the old PhoenixConfigs defaults. */
    public LegacyPhysicsFuelManager(FissionWorkableElectricMultiblockMachine machine) {
        this(machine, 0.0, 0.0, true, false);
    }

    public LegacyPhysicsFuelManager(FissionWorkableElectricMultiblockMachine machine,
                                    double baseHeatPerTick, double heatPerParallel,
                                    boolean fuelScalesWithRodCount, boolean fuelScalesWithParallels) {
        super(machine);
        this.baseHeatPerTick = Math.max(0.0, baseHeatPerTick);
        this.heatPerParallel = Math.max(0.0, heatPerParallel);
        this.fuelScalesWithRodCount = fuelScalesWithRodCount;
        this.fuelScalesWithParallels = fuelScalesWithParallels;
    }


    /**
     * Old formula:
     * base = parallelsPerFuelRod * rodCount
     * modBonus = sum(mod.getParallelBonus())
     * heatBonus = floor(heat / heatPerParallel) [if heatPerParallel > 0]
     * result = clamp(base + modBonus + heatBonus, 1, maxParallels)
     */
    @Override
    public void recalcRuntimeParallels() {
        var cfg = PhoenixFissionConfigs.INSTANCE.fission;
        int rodCount = machine.getComponentManager().getActiveFuelRods().size();

        int base = cfg.parallelsPerFuelRod * Math.max(0, rodCount);

        int modBonus = machine.getComponentManager().getActiveModerators().stream()
                .mapToInt(IFissionModeratorType::getParallelBonus)
                .sum();

        int heatBonus = 0;
        if (heatPerParallel > 0.0) {
            heatBonus = (int) Math.floor(Math.max(0.0, machine.getHeat()) / heatPerParallel);
        }

        int total = base + modBonus + heatBonus;
        machine.lastParallels = Math.max(1, Math.min(total, cfg.maxParallels));
    }



    /**
     * Old formula replaces the whole tick-heat method because the structure is
     * fundamentally different (additive moderator heat, no per-group thermal
     * scalar, parallels multiplied in directly).
     *
     * modMult = 0.1 + sum(mod.getHeatMultiplier()) / 10.0
     * rodHeat = sum(rod.getBaseHeatProduction())
     * burnMult = from continuousBurnTicks ramp
     * result = (baseHeatPerTick + rodHeat * modMult) * parallels * burnMult
     */
    @Override
    public double calculateTickHeat(int machineParallels) {
        List<IFissionFuelRodType> rods = machine.getComponentManager().getActiveFuelRods();
        if (rods.isEmpty() || machine.getReactivityFactor() <= 0.0) return 0.0;

        double totalRodHeat = rods.stream()
                .mapToDouble(IFissionFuelRodType::getBaseHeatProduction)
                .sum();

        double modAdd = machine.getComponentManager().getActiveModerators().stream()
                .mapToDouble(IFissionModeratorType::getHeatMultiplier)
                .sum();
        double moderatorMult = 0.1 + (modAdd / 10.0);

        double parallels = Math.max(1.0, machine.lastParallels);
        double burnMult = computeBurnMultiplier();
        double reactivity = machine.getReactivityFactor();

        return (baseHeatPerTick + totalRodHeat * moderatorMult) * parallels * burnMult * reactivity;
    }



    /**
     * Old fuel consumption uses the primary fuel rod type only, with optional
     * rod-count and parallel scaling instead of the new per-group approach.
     *
     * The hooks computeHeatThermalScalar / computeFuelConsumptionThermalScalar
     * are left at 1.0 (no thermal runaway) by the overrides below.
     */
    @Override
    public void consumeFuelTick(int machineParallels) {
        var comp = machine.getComponentManager();
        if (comp.getActiveFuelRods().isEmpty() || machine.getReactivityFactor() <= 0.0) return;

        IFissionFuelRodType fuelType = comp.getPrimaryFuelRodType();
        if (fuelType == null) fuelType = comp.getActiveFuelRods().get(0);

        int amountPerCycle = Math.max(0, fuelType.getAmountPerCycle());
        if (amountPerCycle <= 0) return;

        double totalPerCycle = amountPerCycle;
        if (fuelScalesWithRodCount) totalPerCycle *= Math.max(1, comp.getActiveFuelRods().size());
        if (fuelScalesWithParallels) totalPerCycle *= Math.max(1, machine.lastParallels);

        double discountMult = computeModeratorFuelDiscountMultiplier();
        totalPerCycle *= discountMult;

        double perTick = totalPerCycle / Math.max(1, fuelType.getDurationTicks());

        String key = fuelType.getName();
        double remainder = machine.fuelRemainderPerType.getOrDefault(key, 0.0) + perTick;
        int toConsume = (int) Math.floor(remainder);

        if (toConsume <= 0) {
            machine.fuelRemainderPerType.put(key, remainder);
            return;
        }
        machine.fuelRemainderPerType.put(key, remainder - toConsume);

        String itemIn = fuelType.getFuelKey();
        if (itemIn.isEmpty()) return;

       ResourceLocation inRl = ResourceLocation.tryParse(itemIn);
        if (inRl == null || !ForgeRegistries.ITEMS.containsKey(inRl)) return;

        ItemStack inStack = new ItemStack(
                ForgeRegistries.ITEMS.getValue(inRl), toConsume);

        if (!machine.executeItemIO(inStack, IO.IN, false)) {
            machine.setHeat(Math.max(machine.getHeat(),
                    machine.getMaxSafeHeatHU() + 1.0));
            return;
        }

        String itemOut = fuelType.getOutputKey();
        if (!itemOut.isEmpty() && !"none".equalsIgnoreCase(itemOut) && !itemOut.equalsIgnoreCase(itemIn)) {
            ResourceLocation outRl = ResourceLocation.tryParse(itemOut);
            if (outRl != null && ForgeRegistries.ITEMS.containsKey(outRl)) {
                machine.executeItemIO(
                        new ItemStack(
                                ForgeRegistries.ITEMS.getValue(outRl), toConsume),
                        IO.OUT);
            }
        }
    }




    /**
     * Adds burn bonus on top of the standard power curve, mirroring old behavior
     * where EU output also scaled with continuousBurnTicks.
     */
    @Override
    protected double computePowerOutput(double activity, double currentHeat, double minHeat,
                                        double maxSafeHeat, double euPerHeatUnit, double powerStart,
                                        double curveExponent) {
        double base = super.computePowerOutput(activity, currentHeat, minHeat, maxSafeHeat,
                euPerHeatUnit, powerStart, curveExponent);
        return base * computeBurnMultiplier();
    }



    @Override
    protected double computeHeatThermalScalar(double heat, double maxSafeHeat) {
        return 1.0;
    }

    @Override
    protected double computeFuelConsumptionThermalScalar(double heat, double maxSafeHeat) {
        return 1.0;
    }



    /**
     * Ramps from 1.0 to 1 + (burnBonusMaxPercent/100) over burnBonusRampSeconds
     * of continuous running. Resets when the reactor shuts down.
     */
    protected double computeBurnMultiplier() {
        var cfg = PhoenixFissionConfigs.INSTANCE.fission;
        double maxPct = Math.max(0.0, cfg.burnBonusMaxPercent);
        double rampTicks = Math.max(1.0, cfg.burnBonusRampSeconds * 20.0);
        double t = Math.min(1.0, machine.continuousBurnTicks / rampTicks);
        return 1.0 + (maxPct / 100.0) * t;
    }
}
