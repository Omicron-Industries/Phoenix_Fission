package net.phoenix.phoenix_fission.api.block;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface IFissionBlanketType extends StringRepresentable {

    @NotNull
    String getName();

    int getTier();

    default int getRequiredFuelTier() {
        return getTier();
    }

    int getDurationTicks();

    int getAmountPerCycle();

    @NotNull
    String getInputKey();

    default int getTintColor() {
        return 0xFFFFFFFF;
    }

    record BlanketOutput(String key, int weight, int instability) {}

    List<BlanketOutput> getOutputs();

    @NotNull
    ResourceLocation getTexture();

    @Override
    default @NotNull String getSerializedName() {
        return getName();
    }
}
