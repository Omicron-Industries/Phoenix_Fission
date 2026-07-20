package net.phoenix.phoenix_fission.api.block;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;


public interface IFissionModeratorType extends StringRepresentable {

    @NotNull
    String getName();

    default int getTintColor() {
        return 0xFFFFFFFF;
    }

    int getEUBoost();

    int getFuelDiscount();

    default double getHeatMultiplier() {
        return getTier() * 0.5;
    }

    default int getParallelBonus() {
        return getTier();
    }

    Material getMaterial();

    int getTier();

    ResourceLocation getTexture();

    @Override
    default @NotNull String getSerializedName() {
        return getName();
    }



}
