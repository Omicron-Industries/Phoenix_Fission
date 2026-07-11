package net.phoenix.phoenix_fission.api.block;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraftforge.common.util.Lazy;
import net.phoenix.phoenix_fission.PhoenixAPI;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;

// Extending StringRepresentable makes getSerializedName() a valid override!
public interface IFissionCoolerType extends StringRepresentable {

    @NotNull
    String getName();

    int getTier();

    int getCoolerTemperature();

    @NotNull
    String getRequiredCoolantMaterialId();

    @NotNull
    default String getOutputCoolantFluidId() {
        return getRequiredCoolantMaterialId();
    }

    @NotNull
    default String getInputCoolantFluidId() {
        return getRequiredCoolantMaterialId();
    }

    /**
     * Defines how much coolant is consumed per tick (mB/t).
     */
    int getCoolantUsagePerTick();

    default int getCoolantPerTick() {
        return getCoolantUsagePerTick();
    }

    /**
     * Flat HU/t of cooling this block provides regardless of temperature differential
     * or coolant availability. Active (fluid-using) coolers return 0.
     * Passive cooler blocks override this with their flat cooling value.
     */
    default double getFlatCoolingHUt() {
        return 0.0;
    }

    /** Returns true if this cooler needs no coolant fluid (flat cooling only). */
    default boolean isPassive() {
        return getFlatCoolingHUt() > 0.0;
    }

    default int getTintColor() {
        return 0xFFFFFFFF;
    }

    Material getMaterial();

    ResourceLocation getTexture();

    // Satisfies StringRepresentable requirement cleanly for everything using this interface
    @Override
    default @NotNull String getSerializedName() {
        return getName();
    }

    Lazy<IFissionCoolerType[]> ALL_COOLER_TEMPERATURES_SORTED = Lazy
            .of(() -> PhoenixAPI.FISSION_COOLERS.keySet().stream()
                    .sorted(Comparator.comparingInt(IFissionCoolerType::getCoolerTemperature))
                    .toArray(IFissionCoolerType[]::new));

    @Nullable
    static IFissionCoolerType getMinRequiredType(int requiredTemperature) {
        return Arrays.stream(ALL_COOLER_TEMPERATURES_SORTED.get())
                .filter(cooler -> cooler.getCoolerTemperature() >= requiredTemperature)
                .findFirst()
                .orElse(null);
    }
}
