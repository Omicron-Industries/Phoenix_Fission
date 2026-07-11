package net.phoenix.phoenix_fission.api.block;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraftforge.common.util.Lazy;
import net.phoenix.phoenix_fission.PhoenixAPI;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

// Extending StringRepresentable completes the decoupling for the fuel rod builder pipeline
public interface IFissionFuelRodType extends StringRepresentable {

    @NotNull
    String getName();

    default int getTintColor() {
        return 0xFFFFFFFF;
    }

    default int getNeutronBias() {
        return 0;
    }

    int getBaseHeatProduction();

    @NotNull
    String getFuelKey();

    @NotNull
    String getOutputKey();

    int getDurationTicks();

    int getAmountPerCycle();

    int getTier();

    ResourceLocation getTexture();

    // Satisfies StringRepresentable requirements using your existing name method
    @Override
    default @NotNull String getSerializedName() {
        return getName();
    }

    Lazy<IFissionFuelRodType[]> ALL_FUEL_RODS_BY_HEAT = Lazy.of(() -> PhoenixAPI.FISSION_FUEL_RODS.keySet().stream()
            .sorted(Comparator.comparingInt(IFissionFuelRodType::getBaseHeatProduction))
            .toArray(IFissionFuelRodType[]::new));
}
