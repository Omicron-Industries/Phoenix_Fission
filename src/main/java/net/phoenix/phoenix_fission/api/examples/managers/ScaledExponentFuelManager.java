package net.phoenix.phoenix_fission.api.examples.managers;

import net.phoenix.phoenix_fission.common.data.multiblock.fission.FissionWorkableElectricMultiblockMachine;
import net.phoenix.phoenix_fission.common.data.multiblock.fission.managers.FissionFuelManager;

/**
 * EXAMPLE — LEVEL 2: Configurable Exponents (Easy)
 *
 * Same physics model as the default reactor but lets you dial the heat
 * and fuel runaway curves at construction time instead of hardcoding
 * the default ^2 / ^4 exponents.
 *
 * Raising the heat exponent makes the reactor harder to control at high
 * temperature. Raising the fuel exponent starves the core faster when
 * it overheats, which can act as a natural safety ceiling.
 *
 * Default values reproduce vanilla Phoenix Fission behaviour exactly.
 *
 * Usage:
 * 
 * <pre>{@code
 * 
 * @Override
 * protected FissionFuelManager createFuelManager() {
 *     // More aggressive heat runaway, same fuel curve
 *     return new ScaledExponentFuelManager(this, 3.0, 4.0);
 * }
 * }</pre>
 */
public class ScaledExponentFuelManager extends FissionFuelManager {

    private final double heatExponent;
    private final double fuelExponent;

    /**
     * @param heatExponent exponent for heat production thermal scalar (default 2.0)
     * @param fuelExponent exponent for fuel consumption thermal scalar (default 4.0)
     */
    public ScaledExponentFuelManager(FissionWorkableElectricMultiblockMachine machine,
                                     double heatExponent, double fuelExponent) {
        super(machine);
        this.heatExponent = heatExponent;
        this.fuelExponent = fuelExponent;
    }

    @Override
    protected double computeHeatThermalScalar(double heat, double maxSafeHeat) {
        return Math.pow(1.0 + (heat / Math.max(1.0, maxSafeHeat)), heatExponent);
    }

    @Override
    protected double computeFuelConsumptionThermalScalar(double heat, double maxSafeHeat) {
        return Math.pow(1.0 + (heat / Math.max(1.0, maxSafeHeat)), fuelExponent);
    }
}
