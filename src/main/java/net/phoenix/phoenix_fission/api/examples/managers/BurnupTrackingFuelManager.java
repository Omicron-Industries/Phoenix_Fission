package net.phoenix.phoenix_fission.api.examples.managers;

import net.phoenix.phoenix_fission.api.block.IFissionFuelRodType;
import net.phoenix.phoenix_fission.common.data.multiblock.fission.FissionWorkableElectricMultiblockMachine;
import net.phoenix.phoenix_fission.common.data.multiblock.fission.managers.FissionFuelManager;

import java.util.HashMap;
import java.util.Map;

/**
 * EXAMPLE — LEVEL 4: Burnup Tracking (Medium-Hard)
 *
 * Models fuel rod burnup: as a rod type consumes fuel over time, its
 * fissile inventory depletes and heat production drops. Fresh rods run
 * hot; spent rods run cool. This creates a natural lifecycle where the
 * operator must eventually swap rods even if items are still available.
 *
 * Burnup is tracked as a fraction [0, 1] per fuel rod type, where
 * 0.0 = fresh rod (full output)
 * 1.0 = spent rod (output at minOutputFraction)
 *
 * Burnup accumulates proportionally to fuel consumption each tick and
 * is reset when the structure reforms (simulating fresh rods being
 * inserted).
 *
 * IMPORTANT — persistence note:
 * Burnup state is held in this manager and is NOT automatically saved
 * to disk. For a production implementation, store burnup values in your
 * machine subclass using {@code @Persisted} fields and restore them in
 * {@code onStructureFormed()}. This example keeps it simple to focus
 * on the math pattern.
 *
 * Usage:
 * 
 * <pre>{@code
 * 
 * @Override
 * protected FissionFuelManager createFuelManager() {
 *     // Heat falls to 30% at full burnup; rods are "spent" after consuming
 *     // 1000× their per-cycle amount.
 *     return new BurnupTrackingFuelManager(this, 1000, 0.30);
 * }
 * }</pre>
 */
public class BurnupTrackingFuelManager extends FissionFuelManager {

    /**
     * Total amount of fuel (in items) a single rod type must consume before
     * reaching full burnup. Higher = longer lifetime.
     */
    private final double burnupCapacityPerRod;

    /**
     * Heat output fraction at full burnup [0, 1].
     * 0.0 means spent rods produce no heat; 0.3 means 30% of fresh output.
     */
    private final double minOutputFraction;

    /** Accumulated fuel consumed per rod type since last structure form. */
    private final Map<String, Double> burnupAccumulator = new HashMap<>();

    public BurnupTrackingFuelManager(FissionWorkableElectricMultiblockMachine machine,
                                     double burnupCapacityPerRod, double minOutputFraction) {
        super(machine);
        this.burnupCapacityPerRod = Math.max(1.0, burnupCapacityPerRod);
        this.minOutputFraction = Math.max(0.0, Math.min(1.0, minOutputFraction));
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        burnupAccumulator.clear();
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        burnupAccumulator.clear();
    }


    /**
     * Reduces heat output by the burnup fraction for this rod type.
     * As burnup approaches 1, heat approaches minOutputFraction × base.
     */
    @Override
    protected double computeHeatDeltaForRodGroup(IFissionFuelRodType rod, long count,
                                                 double rodInteraction, double thermalScalar,
                                                 double moderatorMultiplier,
                                                 double reactivity, double fuelConductivity) {
        double base = super.computeHeatDeltaForRodGroup(rod, count, rodInteraction,
                thermalScalar, moderatorMultiplier, reactivity, fuelConductivity);
        return base * getOutputFraction(rod, count);
    }

    /**
     * Records how much fuel was consumed this tick so burnup can accumulate.
     * Fuel consumption itself is NOT reduced — the rod still eats fuel at the
     * full rate (it's just less productive when spent).
     */
    @Override
    protected double computeFuelConsumptionForRodGroup(IFissionFuelRodType rod, long count,
                                                       double rodInteraction, double thermalScalar, double discountMult,
                                                       double reactivity) {
        double consumed = super.computeFuelConsumptionForRodGroup(
                rod, count, rodInteraction, thermalScalar, discountMult, reactivity);

        // Accumulate burnup proportional to per-rod consumption this tick.
        double perRodConsumed = consumed / Math.max(1, count);
        burnupAccumulator.merge(rod.getName(), perRodConsumed, Double::sum);

        return consumed;
    }


    /**
     * Returns the output multiplier for a rod type based on its burnup.
     * Interpolates linearly from 1.0 (fresh) to minOutputFraction (spent).
     */
    private double getOutputFraction(IFissionFuelRodType rod, long count) {
        double totalCapacity = burnupCapacityPerRod * count;
        double consumed = burnupAccumulator.getOrDefault(rod.getName(), 0.0);
        double burnupFraction = Math.min(1.0, consumed / Math.max(1.0, totalCapacity));

        // Linear interpolation: 1.0 at burnup=0, minOutputFraction at burnup=1
        return 1.0 - burnupFraction * (1.0 - minOutputFraction);
    }

    /** Exposes burnup fraction [0, 1] for a rod type — useful for GUI display. */
    public double getBurnupFraction(String rodName) {
        return burnupAccumulator.getOrDefault(rodName, 0.0) / burnupCapacityPerRod;
    }
}
