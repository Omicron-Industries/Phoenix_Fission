package net.phoenix.phoenix_fission.api.examples.managers;

import net.phoenix.phoenix_fission.common.data.multiblock.fission.FissionWorkableElectricMultiblockMachine;
import net.phoenix.phoenix_fission.common.data.multiblock.fission.managers.FissionFuelManager;

/**
 * EXAMPLE — LEVEL 1: Flat Physics (Super Easy)
 *
 * Removes all thermal runaway from the reactor. Heat production and fuel
 * consumption stay constant no matter how hot the core gets, making
 * overheating impossible through physics alone (the meltdown timer can
 * still trigger if cooling is absent).
 *
 * Great for: beginner-friendly reactors, decorative/creative machines,
 * or any reactor type where "heat = danger" isn't the design goal.
 *
 * Usage — override createFuelManager() in your machine:
 * 
 * <pre>{@code
 * 
 * @Override
 * protected FissionFuelManager createFuelManager() {
 *     return new FlatPhysicsFuelManager(this);
 * }
 * }</pre>
 */
public class FlatPhysicsFuelManager extends FissionFuelManager {

    public FlatPhysicsFuelManager(FissionWorkableElectricMultiblockMachine machine) {
        super(machine);
    }

    /** Heat production is always 1× regardless of core temperature. */
    @Override
    protected double computeHeatThermalScalar(double heat, double maxSafeHeat) {
        return 1.0;
    }

    /** Fuel consumption is always 1× regardless of core temperature. */
    @Override
    protected double computeFuelConsumptionThermalScalar(double heat, double maxSafeHeat) {
        return 1.0;
    }
}
