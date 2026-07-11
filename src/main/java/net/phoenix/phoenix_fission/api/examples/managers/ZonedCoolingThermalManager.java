package net.phoenix.phoenix_fission.api.examples.managers;

import net.phoenix.phoenix_fission.api.block.IFissionCoolerType;
import net.phoenix.phoenix_fission.common.data.multiblock.fission.FissionWorkableElectricMultiblockMachine;
import net.phoenix.phoenix_fission.common.data.multiblock.fission.managers.FissionThermalManager;

/**
 * EXAMPLE -- LEVEL 5: Three-Zone Thermal Model (Complex)
 *
 * Divides the reactor's heat range into three operating zones and applies
 * different cooling and passive-heat physics in each one. This lets you
 * design reactors where operating temperature meaningfully matters --
 * not just "hot = meltdown" but "cold = inefficient, sweet spot = ideal,
 * too hot = runaway".
 *
 * Zone | Definition
 * ---------|--------------------------------------------------
 * COLD | heat &lt; coldThreshold (fraction of maxSafeHeat)
 * NOMINAL | coldThreshold &lt;= heat &lt;= hotThreshold
 * HOT | heat &gt; hotThreshold
 *
 * Passive cooling differences per zone:
 * COLD -- passive cooling is weaker; the core struggles to heat up
 * in cold environments (models insulation being needed).
 * NOMINAL -- standard conductivity (same as base game).
 * HOT -- passive cooling is stronger; convection kicks in and
 * dumps heat faster (but likely not fast enough alone).
 *
 * Active cooler differences per zone:
 * COLD -- coolers are throttled; fluid flowing past a cold core
 * does not exchange much heat.
 * NOMINAL -- standard efficiency.
 * HOT -- coolers become less effective as the temperature
 * differential becomes hard for the coolant to handle;
 * models film-boiling / coolant breakdown at extremes.
 *
 * Meltdown grace period:
 * Scales more aggressively with overheat -- the grace period halves
 * every time heat doubles past the safe ceiling.
 *
 * Usage:
 * 
 * <pre>{@code
 * 
 * @Override
 * protected FissionThermalManager createThermalManager() {
 *     return new ZonedCoolingThermalManager(this,
 *             0.35,   // COLD zone ends at 35% of maxSafeHeat
 *             0.80,   // HOT zone starts at 80% of maxSafeHeat
 *             0.40,   // passive conductivity multiplier in COLD zone
 *             1.80,   // passive conductivity multiplier in HOT zone (convection)
 *             0.50,   // active cooler efficiency in COLD zone
 *             0.65    // active cooler efficiency in HOT zone
 *     );
 * }
 * }</pre>
 */
public class ZonedCoolingThermalManager extends FissionThermalManager {

    // Zone boundary fractions of maxSafeHeat
    private final double coldThreshold;  // e.g. 0.35
    private final double hotThreshold;   // e.g. 0.80

    // Passive cooling conductivity multipliers per zone (NOMINAL = 1.0 always)
    private final double coldPassiveMult;
    private final double hotPassiveMult;

    // Active cooler efficiency multipliers per zone (NOMINAL = 1.0 always)
    private final double coldActiveMult;
    private final double hotActiveMult;

    /**
     * @param coldThreshold   fraction of maxSafeHeat below which the core is "cold"
     * @param hotThreshold    fraction of maxSafeHeat above which the core is "hot"
     * @param coldPassiveMult passive cooling conductivity multiplier in the cold zone
     * @param hotPassiveMult  passive cooling conductivity multiplier in the hot zone
     * @param coldActiveMult  active cooler efficiency multiplier in the cold zone
     * @param hotActiveMult   active cooler efficiency multiplier in the hot zone
     */
    public ZonedCoolingThermalManager(FissionWorkableElectricMultiblockMachine machine,
                                      double coldThreshold, double hotThreshold,
                                      double coldPassiveMult, double hotPassiveMult,
                                      double coldActiveMult, double hotActiveMult) {
        super(machine);
        this.coldThreshold = Math.max(0.0, Math.min(0.99, coldThreshold));
        this.hotThreshold = Math.max(coldThreshold, Math.min(1.0, hotThreshold));
        this.coldPassiveMult = Math.max(0.0, coldPassiveMult);
        this.hotPassiveMult = Math.max(0.0, hotPassiveMult);
        this.coldActiveMult = Math.max(0.0, coldActiveMult);
        this.hotActiveMult = Math.max(0.0, hotActiveMult);
    }

    // -------------------------------------------------------------------------

    @Override
    protected double computePassiveCoolingDelta(double heat, double ambientTemp, double conductivity) {
        double adjusted = conductivity * getPassiveZoneMultiplier(heat);
        return (ambientTemp - heat) * adjusted;
    }

    @Override
    protected double computeActiveCoolerHeatRemoval(IFissionCoolerType cooler, double heat,
                                                    long count, double conductivity) {
        double adjusted = conductivity * getActiveZoneMultiplier(heat);
        double delta = (cooler.getCoolerTemperature() - heat) * count * adjusted;
        return delta < 0 ? -delta : 0.0;
    }

    /**
     * Aggressive meltdown timer: grace period halves every time heat
     * doubles past the safe ceiling, giving runaway reactors very little
     * time to recover the further they overshoot.
     */
    @Override
    protected int computeMeltdownGracePeriodTicks(double heat, double maxSafeHeat,
                                                  double minGrace, double baseGrace, double severity) {
        if (heat <= maxSafeHeat) return (int) (baseGrace * 20.0);

        // doublings = how many times heat has doubled past maxSafeHeat
        double overheatRatio = heat / Math.max(1.0, maxSafeHeat);
        double doublings = Math.log(overheatRatio) / Math.log(2.0);

        double grace = Math.max(minGrace, baseGrace / Math.pow(2.0, doublings * severity));
        return (int) Math.max(1, Math.floor(grace * 20.0));
    }

    // -------------------------------------------------------------------------

    private double getPassiveZoneMultiplier(double heat) {
        return switch (getZone(heat)) {
            case COLD -> coldPassiveMult;
            case NOMINAL -> 1.0;
            case HOT -> hotPassiveMult;
        };
    }

    private double getActiveZoneMultiplier(double heat) {
        return switch (getZone(heat)) {
            case COLD -> coldActiveMult;
            case NOMINAL -> 1.0;
            case HOT -> hotActiveMult;
        };
    }

    private Zone getZone(double heat) {
        double fraction = heat / Math.max(1.0, machine.getMaxSafeHeatHU());
        if (fraction < coldThreshold) return Zone.COLD;
        if (fraction > hotThreshold) return Zone.HOT;
        return Zone.NOMINAL;
    }

    private enum Zone {
        COLD,
        NOMINAL,
        HOT
    }
}
