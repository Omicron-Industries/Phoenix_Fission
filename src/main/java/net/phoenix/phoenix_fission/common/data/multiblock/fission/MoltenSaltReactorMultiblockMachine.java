package net.phoenix.phoenix_fission.common.data.multiblock.fission;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fluids.FluidStack;
import net.phoenix.phoenix_fission.api.block.IFissionCoolerType;
import net.phoenix.phoenix_fission.api.block.IMSRCoreLinerType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@MethodsReturnNonnullByDefault
public class MoltenSaltReactorMultiblockMachine extends FissionWorkableElectricMultiblockMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            MoltenSaltReactorMultiblockMachine.class,
            FissionWorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    /**
     * Per-tier EU/mb lookup for the MSR power formula.
     * Calibrated so that at max safe heat + clean core bonus (no catalyst), the output
     * lands near the corresponding GT voltage tier ceiling:
     *   Tier 1 (Graphite)   → ~8 kEU/t (IV)
     *   Tier 2 (Hastelloy)  → ~33 kEU/t (LuV)
     *   Tier 3 (Titanium)   → ~131 kEU/t (ZPM)
     *   Tier 4 (Netherite)  → ~524 kEU/t (UV)
     * Fluorine catalyst triples the output from any of these figures.
     */
    private static final long[] EU_PER_MB_BY_TIER = { 219L, 349L, 699L, 1398L };

    @Persisted
    protected double xenonPoisonLevel = 0.0;

    protected int structuralLinerCount = 0;

    protected @Nullable IMSRCoreLinerType coreLinerSpec = null;

    // ── Cached per-tick state for display (avoids re-checking hatches in addDisplayText
    //    after the tick has already consumed/produced fluids) ──────────────────────────
    @Persisted
    protected boolean lastCatalystActive = false;
    @Persisted
    protected boolean lastXenonPurgeSucceeded = false;

    public MoltenSaltReactorMultiblockMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    // ── Structure ────────────────────────────────────────────────────────────────────

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        this.isFormed = true;

        var context = getMultiblockState().getMatchContext();
        if (context != null) {
            List<IMSRCoreLinerType> liners = new ArrayList<>();
            Object rawList = context.get("LinerTypes");
            if (rawList instanceof List<?> list) {
                for (Object obj : list) {
                    if (obj instanceof IMSRCoreLinerType liner) liners.add(liner);
                }
            }
            this.structuralLinerCount = liners.size();
            this.coreLinerSpec = liners.stream()
                    .min(java.util.Comparator.comparingInt(IMSRCoreLinerType::getTier))
                    .orElse(null);
        }

        // MSR has no solid fuel rods, moderators, or blankets
        this.activeFuelRods   = new ArrayList<>();
        this.activeModerators = new ArrayList<>();
        this.activeBlankets   = new ArrayList<>();
        this.primaryFuelRodType    = null;
        this.primaryModeratorType  = null;

        this.activeCoolers = getListFromContext("CoolerTypes");
        this.persistedCoolerIDs = new ArrayList<>(
                this.activeCoolers.stream().map(IFissionCoolerType::getName).toList());
        this.primaryCoolerType = getPrimaryCooler(this.activeCoolers);

        this.lastHasCoolant = true;
        this.lastParallels  = Math.max(1, this.structuralLinerCount);
        applyParallelsToRecipeLogic(this.lastParallels);

        this.getRecipeLogic().setStatus(RecipeLogic.Status.IDLE);
        this.reactorTickHandler.updateSubscription();
        markDirty();
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        this.structuralLinerCount  = 0;
        this.coreLinerSpec         = null;
        this.lastCatalystActive    = false;
        this.lastXenonPurgeSucceeded = false;
    }

    // ── Run guard ────────────────────────────────────────────────────────────────────

    @Override
    protected boolean shouldRunReactor() {
        if (!isFormed() || coreLinerSpec == null) return false;
        if (isScramActive()) return false;

        int parallels        = Math.max(1, this.structuralLinerCount);
        int totalRequiredMb  = parallels * coreLinerSpec.getFluidFlowRate();
        return canConsumeFluidId(coreLinerSpec.getInputFluidId(), totalRequiredMb);
    }

    // ── Core tick ────────────────────────────────────────────────────────────────────

    @Override
    protected void handleReactorLogic(boolean running) {
        lastHeatGainedPerTick = 0.0;
        lastHeatRemovedPerTick = 0.0;
        lastGeneratedEUt = 0;

        // Reset per-tick display caches
        lastCatalystActive      = false;
        lastXenonPurgeSucceeded = false;

        if (coreLinerSpec == null) return;

        int  parallels     = Math.max(1, this.structuralLinerCount);
        int  saltToProcess = parallels * coreLinerSpec.getFluidFlowRate();
        String inputSalt   = coreLinerSpec.getInputFluidId();
        String outputSalt  = coreLinerSpec.getOutputFluidId();

        // Xenon natural drift
        if (running) {
            continuousBurnTicks++;
            xenonPoisonLevel = Math.min(0.50, xenonPoisonLevel + 0.00004);
        } else {
            continuousBurnTicks = 0;
            xenonPoisonLevel    = Math.max(0.0, xenonPoisonLevel - 0.0002);
        }

        applyParallelsToRecipeLogic(parallels);

        double cleanCoreBonus = 1.0;

        // ── Salt fission loop ────────────────────────────────────────────────────────
        if (running) {
            if (tryConvertFuelSalt(inputSalt, outputSalt, saltToProcess)) {
                double heatProduced = (saltToProcess * coreLinerSpec.getHeatPerMb())
                        * (1.0 - xenonPoisonLevel);
                this.heat += heatProduced;
                this.lastHeatGainedPerTick = heatProduced;

                // ── Off-gassing: xenon gas emitted continuously in small amounts ────
                // Spreading over every tick (at 1/20 the burst volume) avoids the
                // "burst-fill and fail" problem where a 20-tick lump overflows a hatch.
                int xenonPerTick = Math.max(1, (int) Math.ceil(saltToProcess * 0.1 / 20.0));
                lastXenonPurgeSucceeded = tryOutputFluidIdBoolean(
                        "phoenix_fission:radioactive_xenon_gas", xenonPerTick);
                if (lastXenonPurgeSucceeded) {
                    // Continuous stripping
                    xenonPoisonLevel = Math.max(0.0, xenonPoisonLevel - 0.002);
                    if (xenonPoisonLevel < 0.05) cleanCoreBonus = 1.5;
                }

            } else {
                running = false;
            }
        } else {
            if (cfg().passiveCooling) {
                double coolingFactor  = 0.010 / coreLinerSpec.getTier();
                double variableCooling = this.heat * coolingFactor;
                heat -= Math.max(cfg().idleHeatLoss, variableCooling);
            }
        }

        // ── Cooling ──────────────────────────────────────────────────────────────────
        int totalCoolingCapacity = activeCoolers.stream()
                .mapToInt(IFissionCoolerType::getCoolerTemperature).sum();
        lastProvidedCooling = totalCoolingCapacity;
        lastHasCoolant      = true;

        if (cfg().coolingRequiresCoolant && !activeCoolers.isEmpty()) {
            lastHasCoolant = canConsumeCoolantForThisTickMachineDriven()
                    && consumeCoolantForThisTickMachineDriven();
        }

        double removedHeat = 0.0;
        if (!cfg().coolingRequiresCoolant || lastHasCoolant) {
            double aboveMin         = Math.max(0.0, heat - cfg().minHeat);
            double functionalCooling = running ? totalCoolingCapacity : (totalCoolingCapacity * 0.50);
            removedHeat             = Math.min(aboveMin, functionalCooling);
            heat                   -= removedHeat;
        }
        lastHeatRemovedPerTick = removedHeat;
        clampHeat();

        // ── MSR Power Generation ─────────────────────────────────────────────────────
        // Bypasses the solid-fuel tickPowerGeneration() and calls tryAddEnergy directly.
        if (running) {
            double maxSafeHeat      = Math.max(1.0, cfg().maxSafeHeat);
            double heatRatio        = this.heat / maxSafeHeat;
            // Exponential curve: 0.5x at cold start → 2.5x at max safe heat
            double thermalEfficiency = 0.5 + (Math.pow(heatRatio, 2) * 2.0);

            // Volatile Chemical Catalysis ─────────────────────────────────────────────
            // Check availability first (positive mb), then consume atomically.
            double catalystMultiplier = 1.0;
            if (canConsumeFluidId("gtceu:liquid_fluorine", 10)
                    && tryConsumeFluidId("gtceu:liquid_fluorine", 10)) {
                catalystMultiplier = 3.0;
                lastCatalystActive = true;
                xenonPoisonLevel   = Math.min(0.50, xenonPoisonLevel + 0.0004);
            }

            // Tier-scaled base EU using the calibrated lookup table
            int  tierIdx = Math.max(0, Math.min(EU_PER_MB_BY_TIER.length - 1, coreLinerSpec.getTier() - 1));
            long euPerMb = EU_PER_MB_BY_TIER[tierIdx];
            long baseEU  = (long) saltToProcess * euPerMb;

            double structuralDamping = 1.0 - xenonPoisonLevel;

            this.lastGeneratedEUt = (long) (baseEU
                    * thermalEfficiency
                    * structuralDamping
                    * cleanCoreBonus
                    * catalystMultiplier);

            if (this.lastGeneratedEUt > 0) {
                tryAddEnergy(this.lastGeneratedEUt);
            }
        } else {
            this.lastGeneratedEUt = 0;
        }

        tickMeltdown();
    }

    // ── Fuel consumption (MSR has none — salt is handled in handleReactorLogic) ─────

    @Override
    protected void tickFuelConsumptionMachineDriven(int parallels) { /* no-op */ }

    // ── Fluid helpers ────────────────────────────────────────────────────────────────

    protected boolean tryConvertFuelSalt(@NotNull String inFluid, @NotNull String outFluid, int mb) {
        if (mb <= 0) return true;
        if (!canConsumeFluidId(inFluid, mb)) return false;
        if (!tryConsumeFluidId(inFluid, mb)) return false;
        tryOutputFluidId(outFluid, mb);
        return true;
    }

    /**
     * Attempts to push a fluid into an output hatch via IO.OUT and returns whether
     * handleRecipeIO actually accepted it. Used instead of the (void) base
     * tryOutputFluidId so we know whether xenon actually went somewhere.
     */
    protected boolean tryOutputFluidIdBoolean(@NotNull String fluidId, int mb) {
        if (mb <= 0) return true;
        FluidStack fs = resolveFluidStack(fluidId, mb);
        if (fs.isEmpty()) return false;

        GTRecipe dummy = GTRecipeBuilder.ofRaw()
                .outputFluids(fs)
                .buildRawRecipe();

        return RecipeHelper.handleRecipeIO(this, dummy, IO.OUT,
                getRecipeLogic().getChanceCaches()).isSuccess();
    }

    // ── Display ──────────────────────────────────────────────────────────────────────

    @Override
    public void addDisplayText(@NotNull List<Component> textList) {
        if (!isFormed() || coreLinerSpec == null) {
            textList.add(Component.translatable("phoenix_fission.not_formed")
                    .withStyle(s -> s.withColor(0xFF4444)));
            return;
        }

        final var cfg         = cfg();
        final boolean overheat = heat > cfg.maxSafeHeat;

        // 1. Heat
        textList.add(Component.translatable("phoenix_fission.current_heat_display",
                        String.format("%.1f", heat), String.format("%.1f", cfg.maxSafeHeat))
                .withStyle(s -> s.withColor(overheat ? 0xFF3333 : 0x33FF33)));

        // 2. Thermal efficiency
        double maxSafeHeat       = Math.max(1.0, cfg.maxSafeHeat);
        double heatRatio         = this.heat / maxSafeHeat;
        double thermalEfficiency = 0.5 + (Math.pow(heatRatio, 2) * 2.0);
        int    effColor          = thermalEfficiency > 1.8 ? 0x55FF55 : (thermalEfficiency > 1.0 ? 0xFFAA00 : 0xFF5555);
        textList.add(Component.translatable("phoenix_fission.msr.thermal_efficiency",
                        String.format(java.util.Locale.ROOT, "%.1f", thermalEfficiency * 100.0) + "%")
                .withStyle(s -> s.withColor(effColor)));

        // 3. Power
        textList.add(getVoltageFormattedOutputMSR(lastGeneratedEUt));

        // 4. Structure
        textList.add(Component.translatable("phoenix_fission.parallels", lastParallels));
        textList.add(Component.translatable("phoenix_fission.msr.structural_tier",
                        String.format("MK%d (%s)", coreLinerSpec.getTier(), coreLinerSpec.getName().toUpperCase()))
                .withStyle(s -> s.withColor(0x55FF55)));
        textList.add(Component.translatable("phoenix_fission.msr.active_liners", this.structuralLinerCount)
                .withStyle(s -> s.withColor(0x55AAFF)));
        textList.add(Component.translatable("phoenix_fission.msr.processing_rate",
                        (Math.max(1, this.structuralLinerCount) * coreLinerSpec.getFluidFlowRate()) + " mb/t")
                .withStyle(s -> s.withColor(0xFFAA00)));

        // 5. Catalyst — uses cached last-tick boolean, not a live hatch query.
        //    This avoids the "always shows active" false positive caused by querying
        //    the hatch after the tick has already consumed the fluorine.
        if (lastCatalystActive) {
            textList.add(Component.translatable("phoenix_fission.msr.catalyst_fluorine")
                    .withStyle(s -> s.withColor(0x55FFFF)));
        }

        // 6. Xenon poisoning + purge status
        double xenonPct  = xenonPoisonLevel * 100;
        int    xenonColor = xenonPoisonLevel < 0.05 ? 0x55FF55
                : (xenonPoisonLevel > 0.35 ? 0xFF3333 : 0xAA00AA);
        if (xenonPoisonLevel < 0.05 && lastGeneratedEUt > 0) {
            textList.add(Component.translatable("phoenix_fission.msr.xenon_clean")
                    .withStyle(s -> s.withColor(0x55FF55)));
        } else {
            textList.add(Component.translatable("phoenix_fission.msr.xenon_poisoning",
                            String.format(java.util.Locale.ROOT, "%.1f", xenonPct) + "%")
                    .withStyle(s -> s.withColor(xenonColor)));
        }
        // Show whether xenon gas is actually reaching an output hatch.
        // If this says "Purge: Blocked" you need an output hatch for radioactive_xenon_gas.
        textList.add(Component.literal("  Xe Purge: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(lastXenonPurgeSucceeded ? "OK" : "Blocked")
                        .withStyle(s -> s.withColor(lastXenonPurgeSucceeded ? 0x55FF55 : 0xFF5555))));

        // 7. Coolant
        textList.add(Component.translatable("phoenix_fission.cooling_power", lastProvidedCooling)
                .withStyle(s -> s.withColor(0x55FFFF)));
        textList.add(Component.translatable(
                lastHasCoolant ? "phoenix_fission.coolant_status.ok" : "phoenix_fission.coolant_status.empty"));

        if (meltdownTimerTicks > 0) {
            textList.add(Component.translatable("phoenix_fission.status.danger_timer",
                    getMeltdownSecondsRemaining()));
        }
    }

    /**
     * Local copy of the private getVoltageFormattedOutput from the base class.
     * Avoids brittle reflection just to call a private method one class up.
     */
    private Component getVoltageFormattedOutputMSR(long euOut) {
        int tier = 0;
        for (int i = 0; i < GTValues.V.length; i++) {
            if (euOut >= GTValues.V[i]) tier = i; else break;
        }
        return Component.translatable("phoenix_fission.eu_generation", euOut)
                .append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(GTValues.VNF[tier]))
                .append(Component.literal(")").withStyle(ChatFormatting.GRAY));
    }
}