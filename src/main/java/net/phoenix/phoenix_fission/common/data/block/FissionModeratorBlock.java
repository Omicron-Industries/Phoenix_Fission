package net.phoenix.phoenix_fission.common.data.block;

import com.gregtechceu.gtceu.api.block.ActiveBlock;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.phoenix.phoenix_fission.PhoenixFission;
import net.phoenix.phoenix_fission.api.block.IFissionModeratorType;
import net.phoenix.phoenix_fission.configs.PhoenixFissionConfigs;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@ParametersAreNonnullByDefault
public class FissionModeratorBlock extends ActiveBlock {

    private final IFissionModeratorType moderatorType;

    public FissionModeratorBlock(Properties properties, IFissionModeratorType moderatorType) {
        super(properties);
        this.moderatorType = moderatorType;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level,
                                List<Component> tooltip, TooltipFlag flag) {
        if (!GTUtil.isShiftDown()) {
            tooltip.add(Component.translatable("block.phoenix_fission.fission_moderator.shift")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        tooltip.add(Component.translatable("block.phoenix_fission.fission_moderator.info_header")
                .withStyle(ChatFormatting.AQUA));

        tooltip.add(Component.translatable("block.phoenix_fission.fission_moderator.boost",
                moderatorType.getEUBoost()).withStyle(ChatFormatting.GREEN));

        tooltip.add(Component.translatable("block.phoenix_fission.fission_moderator.fuel_discount",
                moderatorType.getFuelDiscount()).withStyle(ChatFormatting.YELLOW));
    }

    public enum FissionModeratorTypes implements StringRepresentable, IFissionModeratorType {

        GRAPHITE("graphite_moderator", PhoenixFissionConfigs.INSTANCE.fissionStats.moderators.euBoostGraphiteModerator,
                PhoenixFissionConfigs.INSTANCE.fissionStats.moderators.fuelDiscountGraphiteModerator,
                PhoenixFissionConfigs.INSTANCE.fissionStats.moderators.tierGraphiteModerator, 0xFFB07CFF),
        BERYLLIUM("beryllium_moderator",
                PhoenixFissionConfigs.INSTANCE.fissionStats.moderators.euBoostBerylliumModerator,
                PhoenixFissionConfigs.INSTANCE.fissionStats.moderators.fuelDiscountBerylliumModerator,
                PhoenixFissionConfigs.INSTANCE.fissionStats.moderators.tierBerylliumModerator, 0xFFE7FF7D),
        HEAVY_WATER("heavy_water_moderator",
                PhoenixFissionConfigs.INSTANCE.fissionStats.moderators.euBoostHeavyWaterModerator,
                PhoenixFissionConfigs.INSTANCE.fissionStats.moderators.fuelDiscountHeavyWaterModerator,
                PhoenixFissionConfigs.INSTANCE.fissionStats.moderators.tierHeavyWaterModerator, 0xFF7DFFB0),
        NIOBIUM_SIC("niobium_sic_moderator",
                PhoenixFissionConfigs.INSTANCE.fissionStats.moderators.euBoostNiobiumSicModerator,
                PhoenixFissionConfigs.INSTANCE.fissionStats.moderators.fuelDiscountNiobiumSicModerator,
                PhoenixFissionConfigs.INSTANCE.fissionStats.moderators.tierNiobiumSicModerator, 0xFFFF7D7D);

        @Getter
        @NotNull
        private final String name;
        private final int defaultEUBoost;
        private final int defaultFuelDiscount;
        @Getter
        private final int tier;
        @Getter
        private final int tintColor;

        FissionModeratorTypes(String name, int EUBoost, int fuelDiscount, int tier, int tintColor) {
            this.name = name;
            this.defaultEUBoost = EUBoost;
            this.defaultFuelDiscount = fuelDiscount;
            this.tier = tier;
            this.tintColor = tintColor;
        }

        @Override
        public int getEUBoost() {
            return defaultEUBoost;
        }

        @Override
        public int getFuelDiscount() {
            return defaultFuelDiscount;
        }

        @Override
        public @NotNull ResourceLocation getTexture() {
            // Uses the consolidated base texture
            return PhoenixFission.id("block/fission/moderator_base");
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }

        @Override
        public Material getMaterial() {
            return GTMaterials.NULL;
        }
    }
}
