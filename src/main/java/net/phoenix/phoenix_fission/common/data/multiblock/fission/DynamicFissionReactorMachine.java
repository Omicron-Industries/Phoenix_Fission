package net.phoenix.phoenix_fission.common.data.multiblock.fission;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;

import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.phoenix.phoenix_fission.common.data.multiblock.fission.managers.FissionFuelManager;

@MethodsReturnNonnullByDefault
public class DynamicFissionReactorMachine extends FissionWorkableElectricMultiblockMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            DynamicFissionReactorMachine.class,
            FissionWorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    @Persisted
    private double currentHeatMirror = 0.0;

    public DynamicFissionReactorMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public boolean onWorking() {
        // 1. Capture heat before managers run their calculations
        this.currentHeatMirror = this.getHeat();

        // 2. Allow WorkableElectricMultiblockMachine and your managers to tick
        boolean result = super.onWorking();

        // 3. Capture the final calculated heat for this tick
        this.currentHeatMirror = this.getHeat();

        return result;
    }

    /**
     * GTRecipe modifier hooks directly into your modern FissionFuelManager
     * compilation logic to accurately map parallels and production boosts.
     */
    public static ModifierFunction recipeModifier(MetaMachine machine, GTRecipe recipe) {
        if (!(machine instanceof FissionWorkableElectricMultiblockMachine m)) {
            return com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier
                    .nullWrongType(FissionWorkableElectricMultiblockMachine.class, machine);
        }
        if (!m.isFormed()) return ModifierFunction.IDENTITY;

        return FissionFuelManager.buildRecipeModifier(m);
    }
}
