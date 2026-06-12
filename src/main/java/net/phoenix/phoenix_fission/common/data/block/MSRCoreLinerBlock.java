package net.phoenix.phoenix_fission.common.data.block;

import com.gregtechceu.gtceu.api.block.ActiveBlock;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.phoenix.phoenix_fission.PhoenixFission;
import net.phoenix.phoenix_fission.api.block.IMSRCoreLinerType;

import lombok.Getter;
import net.phoenix.phoenix_fission.configs.PhoenixFissionConfigs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@ParametersAreNonnullByDefault
public class MSRCoreLinerBlock extends ActiveBlock {

    private final IMSRCoreLinerType linerType;

    public MSRCoreLinerBlock(Properties props, IMSRCoreLinerType type) {
        super(props);
        this.linerType = type;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level,
                                List<Component> tooltip, TooltipFlag flag) {
        if (!GTUtil.isShiftDown()) {
            tooltip.add(Component.translatable("block.phoenix_fission.fission_moderator.shift")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        tooltip.add(Component.translatable("block.phoenix_fission.msr_liner.info_header")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));

        tooltip.add(Component.translatable("phoenix_fission.msr_liner.tier", linerType.getTier())
                .withStyle(ChatFormatting.WHITE));

        tooltip.add(Component.translatable("phoenix_fission.msr_liner.flow_rate", linerType.getFluidFlowRate())
                .withStyle(ChatFormatting.YELLOW));

        tooltip.add(Component.translatable("phoenix_fission.msr_liner.thermal_dissipation", String.format("%.1f", linerType.getHeatPerMb()))
                .withStyle(ChatFormatting.RED));

        // --- NEW: Fluid Loop Definitions ---
        // Cleans up the string (e.g., converts "phoenix_fission:u235_molten_salt" into a display name readable format)
        String inputFluidName = cleanFluidName(linerType.getInputFluidId());
        String outputFluidName = cleanFluidName(linerType.getOutputFluidId());

        tooltip.add(Component.translatable("phoenix_fission.msr_liner.input_salt", inputFluidName)
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("phoenix_fission.msr_liner.output_salt", outputFluidName)
                .withStyle(ChatFormatting.DARK_AQUA));
    }

    /**
     * Helper to clean up raw resource paths for clean text viewing if a language key doesn't catch them.
     */
    private String cleanFluidName(String fluidId) {
        if (fluidId == null || fluidId.isEmpty()) return "None";
        String path = fluidId.contains(":") ? fluidId.split(":")[1] : fluidId;
        return java.util.Arrays.stream(path.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(java.util.stream.Collectors.joining(" "));
    }

    public enum MSRLinerTypes implements IMSRCoreLinerType {

        LINER_GRAPHITE("liner_graphite", 1, 10, 10.0,
                "phoenix_fission:u235_molten_salt", "phoenix_fission:depleted_u235_molten_salt"),

        LINER_HASTELLOY("liner_hastelloy", 2, 25, 15.0,
                "phoenix_fission:thorium_u233_molten_salt", "phoenix_fission:depleted_thorium_molten_salt"),

        LINER_TITANIUM("liner_titanium", 3, 50, 25.0,
                "phoenix_fission:plutonium_molten_salt", "phoenix_fission:irradiated_actinide_waste"),

        LINER_NETHERITE("liner_netherite", 4, 100, 40.0,
                "phoenix_fission:californium_molten_salt", "phoenix_fission:transuranic_sludge_waste");

        @Getter
        @NotNull
        private final String name;

        // Remove @Getter from these so we can write custom ones
        private final int defaultTier;
        private final int defaultFluidFlowRate;
        private final double defaultHeatPerMb;
        private final String defaultInputFluidId;
        private final String defaultOutputFluidId;

        MSRLinerTypes(String name, int tier, int flow, double heat, String inputFluid, String outputFluid) {
            this.name = name;
            this.defaultTier = tier;
            this.defaultFluidFlowRate = flow;
            this.defaultHeatPerMb = heat;
            this.defaultInputFluidId = inputFluid;
            this.defaultOutputFluidId = outputFluid;
        }

        // --- Dynamic Getters ---

        // Assuming your MSRConfigs is accessible via PhoenixFissionConfigs.INSTANCE.fissionStats.msr
        // Adjust the config path below to match wherever you instanced MSRConfigs!

        @Override
        public int getTier() {
            if (PhoenixFissionConfigs.INSTANCE == null) return defaultTier;
            var config = PhoenixFissionConfigs.INSTANCE.fissionStats.msrLiners;
            return switch (this) {
                case LINER_GRAPHITE -> config.tierGraphiteLiner;
                case LINER_HASTELLOY -> config.tierHastelloyLiner;
                case LINER_TITANIUM -> config.tierTitaniumLiner;
                case LINER_NETHERITE -> config.tierNetheriteLiner;
            };
        }

        @Override
        public int getFluidFlowRate() {
            if (PhoenixFissionConfigs.INSTANCE == null) return defaultFluidFlowRate;
            var config = PhoenixFissionConfigs.INSTANCE.fissionStats.msrLiners;
            return switch (this) {
                case LINER_GRAPHITE -> config.flowRateGraphiteLiner;
                case LINER_HASTELLOY -> config.flowRateHastelloyLiner;
                case LINER_TITANIUM -> config.flowRateTitaniumLiner;
                case LINER_NETHERITE -> config.flowRateNetheriteLiner;
            };
        }

        @Override
        public double getHeatPerMb() {
            if (PhoenixFissionConfigs.INSTANCE == null) return defaultHeatPerMb;
            var config = PhoenixFissionConfigs.INSTANCE.fissionStats.msrLiners;
            return switch (this) {
                case LINER_GRAPHITE -> config.heatGraphiteLiner;
                case LINER_HASTELLOY -> config.heatHastelloyLiner;
                case LINER_TITANIUM -> config.heatTitaniumLiner;
                case LINER_NETHERITE -> config.heatNetheriteLiner;
            };
        }

        @Override
        public String getInputFluidId() {
            if (PhoenixFissionConfigs.INSTANCE == null) return defaultInputFluidId;
            var config = PhoenixFissionConfigs.INSTANCE.fissionStats.msrLiners;
            return switch (this) {
                case LINER_GRAPHITE -> config.inputFluidGraphiteLiner;
                case LINER_HASTELLOY -> config.inputFluidHastelloyLiner;
                case LINER_TITANIUM -> config.inputFluidTitaniumLiner;
                case LINER_NETHERITE -> config.inputFluidNetheriteLiner;
            };
        }

        @Override
        public String getOutputFluidId() {
            if (PhoenixFissionConfigs.INSTANCE == null) return defaultOutputFluidId;
            var config = PhoenixFissionConfigs.INSTANCE.fissionStats.msrLiners;
            return switch (this) {
                case LINER_GRAPHITE -> config.outputFluidGraphiteLiner;
                case LINER_HASTELLOY -> config.outputFluidHastelloyLiner;
                case LINER_TITANIUM -> config.outputFluidTitaniumLiner;
                case LINER_NETHERITE -> config.outputFluidNetheriteLiner;
            };
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }

        @Override
        public ResourceLocation getTexture() {
            return PhoenixFission.id("block/fission/msr/liners/" + name);
        }
    }
}
