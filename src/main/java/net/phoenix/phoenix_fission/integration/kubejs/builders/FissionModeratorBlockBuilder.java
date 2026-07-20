package net.phoenix.phoenix_fission.integration.kubejs.builders;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.phoenix.phoenix_fission.PhoenixAPI;
import net.phoenix.phoenix_fission.api.block.IFissionModeratorType;
import net.phoenix.phoenix_fission.common.data.block.FissionModeratorBlock;

import dev.latvian.mods.kubejs.block.BlockBuilder;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

@Accessors(chain = true, fluent = true)
public class FissionModeratorBlockBuilder extends BlockBuilder {

    @Setter
    public transient int euBoost = 1;
    @Setter
    public transient int fuelDiscount = 1;
    @Setter
    public transient int tier = 1;
    @Setter
    public transient int tintColor = -1;
    @Setter
    public transient String texture = "phoenix_fission:block/fission/moderator_base";
    @Setter
    public transient String maskTexture = "phoenix_fission:block/fission/moderator_mask";

    public FissionModeratorBlockBuilder(ResourceLocation i) {
        super(i);
        noValidSpawns(true);
    }

    public class KjsModeratorType implements IFissionModeratorType {

        @Override
        public @NotNull String getName() {
            return id.getPath();
        }

        @Override
        public int getEUBoost() {
            return Math.max(0, euBoost);
        }

        @Override
        public int getFuelDiscount() {
            return Math.max(0, fuelDiscount);
        }

        @Override
        public int getTier() {
            return Math.max(0, tier);
        }

        @Override
        public @NotNull ResourceLocation getTexture() {
            ResourceLocation rl = ResourceLocation.tryParse(texture);
            return rl != null ? rl :
                    ResourceLocation.fromNamespaceAndPath("phoenix_fission", "block/fission/moderator_base");
        }

        @Override
        public int getTintColor() {
            if (tintColor != -1) return tintColor;
            return switch (getTier()) {
                case 1 -> 0xFFB07CFF;
                case 2 -> 0xFFE7FF7D;
                case 3 -> 0xFF7DFFB0;
                case 4 -> 0xFFFF7D7D;
                default -> 0xFFFFFFFF;
            };
        }

        @Override
        public @NotNull String getSerializedName() {
            return getName();
        }

        @Override
        public Material getMaterial() {
            return GTMaterials.NULL;
        }

        public @NotNull ResourceLocation getMaskTexture() {
            ResourceLocation rl = ResourceLocation.tryParse(maskTexture);
            return rl != null ? rl :
                    ResourceLocation.fromNamespaceAndPath("phoenix_fission", "block/fission/moderator_mask");
        }
    }

    @Override
    public Block createObject() {
        IFissionModeratorType type = new KjsModeratorType();
        FissionModeratorBlock result = new FissionModeratorBlock(this.createProperties(), type);
        PhoenixAPI.FISSION_MODERATORS.put(type, () -> result);
        return result;
    }
}
