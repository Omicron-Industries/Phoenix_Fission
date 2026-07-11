package net.phoenix.phoenix_fission.common.data.multiblock.fission.managers;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.phoenix_fission.api.block.IFissionFuelRodType;
import net.phoenix.phoenix_fission.api.block.IFissionModeratorType;
import net.phoenix.phoenix_fission.common.data.multiblock.fission.FissionWorkableElectricMultiblockMachine;
import net.phoenix.phoenix_fission.configs.PhoenixFissionConfigs;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FissionFuelManager {

    protected final FissionWorkableElectricMultiblockMachine machine;

    public FissionFuelManager(FissionWorkableElectricMultiblockMachine machine) {
        this.machine = machine;
    }

    public void onStructureFormed() {
        recalcRuntimeParallels();
    }

    public void onStructureInvalid() {
        machine.lastParallels = 1;
        machine.lastGeneratedEUt = 0;
    }

    public void recalcRuntimeParallels() {
        int totalRods = machine.getComponentManager().getActiveFuelRods().size();
        machine.lastParallels = Math.max(1, totalRods);
    }

    // -------------------------------------------------------------------------
    // Math hooks — override these in a subclass to customize reactor physics.
    // All parameters are provided so overrides don't need to reach into machine
    // state themselves, keeping custom implementations self-contained.
    // -------------------------------------------------------------------------

    /**
     * Scaling factor applied to heat/fuel based on the number of fuel rods present.
     * Default: {@code (totalRods + 1) / 2} — linear growth so a single rod is 1×,
     * two rods are 1.5×, etc.
     */
    protected double computeRodInteractionFactor(int totalRods) {
        return (totalRods + 1.0) / 2.0;
    }

    /**
     * Thermal multiplier used in <em>heat production</em>.
     * Default: {@code (1 + heat/maxSafeHeat)^2} — quadratic runaway above safe threshold.
     */
    protected double computeHeatThermalScalar(double heat, double maxSafeHeat) {
        return Math.pow(1.0 + (heat / Math.max(1.0, maxSafeHeat)), 2.0);
    }

    /**
     * Thermal multiplier used in <em>fuel consumption</em>.
     * Default: {@code (1 + heat/maxSafeHeat)^4} — steeper curve than heat production
     * to model fuel burning faster as the core overheats.
     */
    protected double computeFuelConsumptionThermalScalar(double heat, double maxSafeHeat) {
        return Math.pow(1.0 + (heat / Math.max(1.0, maxSafeHeat)), 4.0);
    }

    /**
     * Multiplicative bonus to heat production from moderators.
     * Default: {@code 1 + moderatorEUBoost/100}.
     */
    protected double computeModeratorHeatMultiplier() {
        return 1.0 + (getModeratorEUBoostClamped() / 100.0);
    }

    /**
     * Fuel consumption discount multiplier from moderators.
     * Default: {@code max(0, 1 - moderatorFuelDiscount/100)}.
     */
    protected double computeModeratorFuelDiscountMultiplier() {
        return Math.max(0.0, 1.0 - (getModeratorFuelDiscountClamped() / 100.0));
    }

    /**
     * Heat produced this tick by a single rod-type group.
     *
     * @param rod                 the fuel rod type
     * @param count               number of rods of this type in the reactor
     * @param rodInteraction      value from {@link #computeRodInteractionFactor}
     * @param thermalScalar       value from {@link #computeHeatThermalScalar}
     * @param moderatorMultiplier value from {@link #computeModeratorHeatMultiplier}
     * @param reactivity          current reactivity factor [0, 1]
     * @param fuelConductivity    config conductivity multiplier
     */
    protected double computeHeatDeltaForRodGroup(IFissionFuelRodType rod, long count,
                                                 double rodInteraction, double thermalScalar,
                                                 double moderatorMultiplier,
                                                 double reactivity, double fuelConductivity) {
        return rod.getBaseHeatProduction() * rodInteraction * thermalScalar * count * moderatorMultiplier * reactivity *
                fuelConductivity;
    }

    /**
     * Fuel consumed per tick by a single rod-type group (as a fractional amount;
     * the caller accumulates remainders and converts to whole items).
     *
     * @param rod            the fuel rod type
     * @param count          number of rods of this type
     * @param rodInteraction value from {@link #computeRodInteractionFactor}
     * @param thermalScalar  value from {@link #computeFuelConsumptionThermalScalar}
     * @param discountMult   value from {@link #computeModeratorFuelDiscountMultiplier}
     * @param reactivity     current reactivity factor [0, 1]
     */
    protected double computeFuelConsumptionForRodGroup(IFissionFuelRodType rod, long count,
                                                       double rodInteraction, double thermalScalar, double discountMult,
                                                       double reactivity) {
        double baseRate = (double) rod.getAmountPerCycle() / Math.max(1, rod.getDurationTicks());
        return baseRate * rodInteraction * thermalScalar * count * discountMult * reactivity;
    }

    /**
     * Final EU/t output given the heat activity and current heat level.
     * Called once per tick by {@link #processPowerGeneration}.
     *
     * @param activity      heat gained this tick (or current heat if idle)
     * @param currentHeat   current reactor heat
     * @param maxSafeHeat   configured maximum safe heat
     * @param euPerHeatUnit config: EU multiplier per heat unit
     * @param powerStart    config: heat fraction at which power bonus begins
     * @param curveExponent config: exponent of the power bonus curve
     * @return raw EU/t before clamping; return 0 to suppress power output
     */
    protected double computePowerOutput(double activity, double currentHeat, double minHeat,
                                        double maxSafeHeat, double euPerHeatUnit, double powerStart,
                                        double curveExponent) {
        double baseEU = activity * euPerHeatUnit;
        // Normalize heat relative to ambient so the 1x floor sits at ambient temp,
        // not at absolute zero — cold reactors near ambient get minimum EU.
        double range = Math.max(1.0, maxSafeHeat - minHeat);
        double fraction = Math.max(0.0, Math.min(1.5, (currentHeat - minHeat) / range));
        double start = Math.max(0.0, Math.min(0.99, powerStart));
        double targetCurve = Math.max(0.0, Math.min(1.0, (fraction - start) / Math.max(1e-9, 1.0 - start)));
        double bonus = 1.0 + Math.pow(targetCurve, Math.max(0.01, curveExponent)) * 1.5;
        return baseEU * bonus;
    }

    // -------------------------------------------------------------------------

    public double calculateTickHeat(int ignoredMachineParallels) {
        var comp = machine.getComponentManager();
        if (comp.getActiveFuelRods().isEmpty() || machine.getReactivityFactor() <= 0.0) return 0.0;
        var hm = PhoenixFissionConfigs.INSTANCE.fission.heatModel;

        int totalRods = comp.getActiveFuelRods().size();
        double rodInteractionFactor = computeRodInteractionFactor(totalRods);
        double thermalFactor = computeHeatThermalScalar(machine.getHeat(), machine.getMaxSafeHeatHU());
        double reactivity = machine.getReactivityFactor();
        double modBonus = computeModeratorHeatMultiplier();

        Map<IFissionFuelRodType, Long> fuelRodThreads = comp.getActiveFuelRods().stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        double aggregateReactorHeatDelta = 0.0;

        for (var threadGroup : fuelRodThreads.entrySet()) {
            aggregateReactorHeatDelta += computeHeatDeltaForRodGroup(
                    threadGroup.getKey(), threadGroup.getValue(),
                    rodInteractionFactor, thermalFactor, modBonus,
                    reactivity, hm.fuelConductivity);
        }

        return aggregateReactorHeatDelta;
    }

    public void consumeFuelTick(int ignoredMachineParallels) {
        var comp = machine.getComponentManager();
        if (comp.getActiveFuelRods().isEmpty() || machine.getReactivityFactor() <= 0.0) return;
        var hm = PhoenixFissionConfigs.INSTANCE.fission.heatModel;

        int totalRods = comp.getActiveFuelRods().size();
        double rodInteractionFactor = computeRodInteractionFactor(totalRods);
        double heatScalar = computeFuelConsumptionThermalScalar(machine.getHeat(), machine.getMaxSafeHeatHU());
        double reactivity = machine.getReactivityFactor();
        double discountMult = computeModeratorFuelDiscountMultiplier();

        Map<IFissionFuelRodType, Long> fuelRodThreads = comp.getActiveFuelRods().stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        for (var threadGroup : fuelRodThreads.entrySet()) {
            IFissionFuelRodType fuelType = threadGroup.getKey();
            long countOfThisType = threadGroup.getValue();

            if (fuelType.getAmountPerCycle() <= 0) continue;

            double perTickConsumption = computeFuelConsumptionForRodGroup(
                    fuelType, countOfThisType,
                    rodInteractionFactor, heatScalar,
                    discountMult, reactivity);

            String key = fuelType.getName();
            double remainder = machine.fuelRemainderPerType.getOrDefault(key, 0.0) + perTickConsumption;
            int directConsume = (int) Math.floor(remainder);

            if (directConsume <= 0) {
                machine.fuelRemainderPerType.put(key, remainder);
                continue;
            }
            machine.fuelRemainderPerType.put(key, remainder - directConsume);

            String itemIn = fuelType.getFuelKey();
            if (itemIn.isEmpty() || !processItemConsumption(itemIn, directConsume)) {
                machine.setHeat(Math.max(machine.getHeat(), machine.getMaxSafeHeatHU() + 1.0));
                continue;
            }

            String itemOut = fuelType.getOutputKey();
            if (!itemOut.isEmpty() && !"none".equalsIgnoreCase(itemOut) && !itemOut.equalsIgnoreCase(itemIn)) {
                ResourceLocation outRl = ResourceLocation.tryParse(itemOut);
                if (outRl != null && ForgeRegistries.ITEMS.containsKey(outRl)) {
                    machine.executeItemIO(new ItemStack(ForgeRegistries.ITEMS.getValue(outRl), directConsume), IO.OUT);
                }
            }
        }
    }

    public boolean hasFuelAvailableForNextTick() {
        Set<String> verifiedTypes = new HashSet<>();
        for (IFissionFuelRodType rod : machine.getComponentManager().getActiveFuelRods()) {
            if (rod.getFuelKey().isEmpty() || verifiedTypes.contains(rod.getFuelKey())) continue;
            verifiedTypes.add(rod.getFuelKey());
            ResourceLocation rl = ResourceLocation.tryParse(rod.getFuelKey());
            if (rl == null || !ForgeRegistries.ITEMS.containsKey(rl) ||
                    !machine.executeItemIO(new ItemStack(ForgeRegistries.ITEMS.getValue(rl), 1), IO.IN, true))
                return false;
        }
        return true;
    }

    private boolean processItemConsumption(String id, int count) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null || !ForgeRegistries.ITEMS.containsKey(rl)) return false;
        return machine.executeItemIO(new ItemStack(ForgeRegistries.ITEMS.getValue(rl), count), IO.IN, false);
    }

    public void processPowerGeneration(boolean running) {
        var eu = PhoenixFissionConfigs.INSTANCE.fission.euOutput;
        if (!PhoenixFissionConfigs.INSTANCE.fission.enableDirectEUOutput) return;
        if (machine.isOverCooled) return;

        double activity = (running && machine.lastHeatGainedPerTick > 0.0) ? machine.lastHeatGainedPerTick :
                Math.max(0.0, machine.getHeat());
        if (activity <= 0.0001) return;

        double finalEUt = computePowerOutput(activity, machine.getHeat(),
                PhoenixFissionConfigs.INSTANCE.fission.heatModel.ambientTemperatureHU,
                machine.getMaxSafeHeatHU(),
                eu.euPerHeatUnit, eu.powerStartFraction, eu.powerCurveExponent);

        if (eu.maxGeneratedEUt > 0) finalEUt = Math.min(finalEUt, eu.maxGeneratedEUt);
        long outputEU = (long) Math.floor(finalEUt);
        if (running && eu.minGeneratedEUt > 0) outputEU = Math.max(outputEU, eu.minGeneratedEUt);

        if (outputEU > 0) {
            machine.lastGeneratedEUt = outputEU;
            if (machine.getEnergyContainer() != null) {
                machine.getEnergyContainer().addEnergy(outputEU);
            }
        }
    }

    public static ModifierFunction buildRecipeModifier(FissionWorkableElectricMultiblockMachine m) {
        int parallels = Math.max(1, m.getFuelManager().machine.lastParallels);
        double eutMult = 1.0 + (m.getFuelManager().getModeratorEUBoostClamped() / 100.0);
        double durMult = Math.max(0.01, 1.0 - (m.getFuelManager().getModeratorFuelDiscountClamped() / 100.0));

        var builder = ModifierFunction.builder().eutMultiplier(eutMult).durationMultiplier(durMult);
        if (parallels > 1) {
            var contentMultiplier = com.gregtechceu.gtceu.api.recipe.content.ContentModifier.multiplier(parallels);
            builder.inputModifier(contentMultiplier).outputModifier(contentMultiplier).parallels(parallels);
        }
        return builder.build();
    }

    public int getModeratorEUBoostClamped() {
        return Math.min(machine.getComponentManager().getActiveModerators().stream()
                .mapToInt(IFissionModeratorType::getEUBoost).sum(),
                PhoenixFissionConfigs.INSTANCE.fission.maxEUBoostPercent);
    }

    public int getModeratorFuelDiscountClamped() {
        return Math.min(
                machine.getComponentManager().getActiveModerators().stream()
                        .mapToInt(IFissionModeratorType::getFuelDiscount).sum(),
                PhoenixFissionConfigs.INSTANCE.fission.maxFuelDiscountPercent);
    }

    public void addDisplayText(List<Component> textList) {
        textList.add(Component.translatable("gtceu.multiblock.max_energy_per_tick",
                FormattingUtil.formatNumbers(machine.lastGeneratedEUt)));
        textList.add(Component.translatable("phoenix_fission.parallels", machine.lastParallels));
        textList.add(Component.translatable("block.phoenix_fission.fission_moderator.boost",
                getModeratorEUBoostClamped() + "%"));
        textList.add(Component.translatable("block.phoenix_fission.fission_moderator.fuel_discount",
                getModeratorFuelDiscountClamped() + "%"));
    }
}
