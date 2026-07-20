package net.phoenix.phoenix_fission.api.examples.managers;

import net.phoenix.phoenix_fission.api.block.IFissionFuelRodType;
import net.phoenix.phoenix_fission.common.data.multiblock.fission.FissionWorkableElectricMultiblockMachine;
import net.phoenix.phoenix_fission.common.data.multiblock.fission.managers.FissionFuelManager;

/**
 * EXAMPLE — LEVEL 3: Neutron Moderation Model (Medium)
 *
 * Replaces the simple rod-count interaction formula with one that also
 * accounts for moderator count. More moderators slow more neutrons,
 * which increases the probability of fission — so the rod-interaction
 * factor grows with the moderator-to-rod ratio.
 *
 * Additionally, per-rod heat output is scaled by fuel tier: higher-
 * tier rods produce proportionally more heat, modelling the richer
 * fissile content of advanced fuels (T1 = 1×, T4 = 1.75×).
 *
 * Physics summary:
 * rodInteraction = sqrt((rods + 1) / 2) × (1 + clamp(mods/rods, 0, maxRatio) / maxRatio)
 *
 * Compared to the default this makes a sparsely-moderated core feel
 * sluggish, while a well-moderated one punches above the default curve.
 *
 * Usage:
 * 
 * <pre>{@code
 * 
 * @Override
 * protected FissionFuelManager createFuelManager() {
 *     return new NeutronModeratedFuelManager(this, 4.0);
 * }
 * }</pre>
 */
public class NeutronModeratedFuelManager extends FissionFuelManager {

    /**
     * The moderator-to-rod ratio at which the neutron bonus is maximised.
     * Above this value extra moderators give no additional benefit.
     */
    private final double maxModeratorRatio;

    /**
     * @param maxModeratorRatio moderator-to-rod ratio for full bonus (e.g. 4.0 means
     *                          4 moderators per rod is optimal)
     */
    public NeutronModeratedFuelManager(FissionWorkableElectricMultiblockMachine machine,
                                       double maxModeratorRatio) {
        super(machine);
        this.maxModeratorRatio = Math.max(1.0, maxModeratorRatio);
    }

    /**
     * Square-root scaling (sublinear growth with rod count) multiplied by a
     * moderator fraction bonus that rewards good moderator-to-rod ratios.
     */
    @Override
    protected double computeRodInteractionFactor(int totalRods) {
        if (totalRods <= 0) return 1.0;

        int moderatorCount = machine.getComponentManager().getActiveModerators().size();
        double moderatorRatio = Math.min((double) moderatorCount / totalRods, maxModeratorRatio);
        double moderatorBonus = moderatorRatio / maxModeratorRatio;

        return Math.sqrt((totalRods + 1.0) / 2.0) * (1.0 + moderatorBonus);
    }

    /**
     * Higher-tier rods produce more heat per rod because they have a greater
     * fissile isotope fraction (T1 = 1×, T2 = 1.25×, T3 = 1.5×, T4 = 1.75×).
     */
    @Override
    protected double computeHeatDeltaForRodGroup(IFissionFuelRodType rod, long count,
                                                 double rodInteraction, double thermalScalar,
                                                 double moderatorMultiplier,
                                                 double reactivity, double fuelConductivity) {
        double tierFactor = 1.0 + (rod.getTier() - 1) * 0.25;
        return super.computeHeatDeltaForRodGroup(rod, count, rodInteraction, thermalScalar,
                moderatorMultiplier, reactivity, fuelConductivity) * tierFactor;
    }
}
