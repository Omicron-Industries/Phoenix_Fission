package net.phoenix.phoenix_fission.integration.kubejs.builders;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.phoenix.phoenix_fission.PhoenixAPI;
import net.phoenix.phoenix_fission.api.block.IFissionBlanketType;
import net.phoenix.phoenix_fission.common.data.block.FissionBlanketBlock;

import dev.latvian.mods.kubejs.block.BlockBuilder;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Accessors(chain = true, fluent = true)
public class FissionBlanketRodBlockBuilder extends BlockBuilder {

    @Setter
    public transient int tier = 1;
    @Setter
    public transient int durationTicks = 1200;
    @Setter
    public transient int amountPerCycle = 1;
    @Setter
    @NotNull
    public transient String inputKey = "";
    @Setter
    public transient int tintColor = -1;

    public transient List<IFissionBlanketType.BlanketOutput> outputs = new ArrayList<>();

    @Setter
    public transient String texture = "phoenix_fission:block/fission/blanket_base";
    @Setter
    public transient String maskTexture = "phoenix_fission:block/fission/blanket_mask";

    public FissionBlanketRodBlockBuilder(ResourceLocation i) {
        super(i);
        noValidSpawns(true);
    }

    public FissionBlanketRodBlockBuilder addOutput(String registryKey, int weight, int instability) {
        this.outputs.add(new IFissionBlanketType.BlanketOutput(registryKey, weight, instability));
        return this;
    }

    public class KjsBlanketType implements IFissionBlanketType {

        @Override
        public int getRequiredFuelTier() {
            return Math.max(0, tier);
        }

        @Override
        public int getDurationTicks() {
            return Math.max(1, durationTicks);
        }

        @Override
        public int getAmountPerCycle() {
            return Math.max(1, amountPerCycle);
        }

        @Override
        public @NotNull String getInputKey() {
            return inputKey.isEmpty() ? "minecraft:air" : inputKey;
        }

        @Override
        public List<BlanketOutput> getOutputs() {
            return outputs;
        }

        @Override
        public int getTier() {
            return Math.max(0, tier);
        }

        @Override
        public int getTintColor() {
            if (tintColor != -1) return tintColor;
            return switch (getTier()) {
                case 1 -> 0xFF7DE7FF;
                case 2 -> 0xFFB07CFF;
                case 3 -> 0xFFFFD27D;
                case 4 -> 0xFFFF7DAA;
                default -> 0xFFFFFFFF;
            };
        }

        @Override
        public @NotNull ResourceLocation getTexture() {
            ResourceLocation rl = ResourceLocation.tryParse(texture);
            return rl != null ? rl :
                    ResourceLocation.fromNamespaceAndPath("phoenix_fission", "block/fission/blanket_base");
        }

        @Override
        public @NotNull String getSerializedName() {
            return getName();
        }

        @Override
        public @NotNull String getName() {
            return id.getPath();
        }

        public @NotNull ResourceLocation getMaskTexture() {
            ResourceLocation rl = ResourceLocation.tryParse(maskTexture);
            return rl != null ? rl :
                    ResourceLocation.fromNamespaceAndPath("phoenix_fission", "block/fission/blanket_mask");
        }
    }

    @Override
    public Block createObject() {
        IFissionBlanketType type = new KjsBlanketType();
        FissionBlanketBlock result = new FissionBlanketBlock(this.createProperties(), type);
        PhoenixAPI.FISSION_BLANKETS.put(type, () -> result);
        return result;
    }
}
