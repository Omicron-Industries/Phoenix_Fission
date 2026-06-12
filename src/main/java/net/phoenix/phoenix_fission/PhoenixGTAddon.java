package net.phoenix.phoenix_fission;

import com.gregtechceu.gtceu.api.addon.GTAddon;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.api.addon.events.KJSRecipeKeyEvent;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;

import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

@SuppressWarnings("unused")
@GTAddon
public class PhoenixGTAddon implements IGTAddon {

    @Override
    public GTRegistrate getRegistrate() {
        return PhoenixFission.PHOENIX_REGISTRATE;
    }

    @Override
    public void initializeAddon() {}

    @Override
    public String addonModId() {
        return PhoenixFission.MOD_ID;
    }

    @Override
    public void registerTagPrefixes() {}

    @Override
    public void addRecipes(Consumer<FinishedRecipe> provider) {
        // PhoenixFissionMachineRecipes.init(provider);
    }

    @Override
    public void registerElements() {
        IGTAddon.super.registerElements();
        // PhoenixFissionElements.init();
    }

    @Override
    public void registerRecipeCapabilities() {}

    @Override
    public void registerRecipeKeys(KJSRecipeKeyEvent event) {}
}
