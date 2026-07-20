package net.phoenix.phoenix_fission.common.data.block;

import com.gregtechceu.gtceu.api.block.ActiveBlock;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.phoenix.phoenix_fission.PhoenixFission;
import net.phoenix.phoenix_fission.api.block.IFissionModeratorType;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

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

    public static class BindableModeratorType implements IFissionModeratorType {

        @Getter
        @NotNull
        private final String name;
        @Getter
        private final int tier;
        @Getter
        private final int tintColor;

        private final Supplier<Integer> euBoostSupplier;
        private final Supplier<Integer> fuelDiscountSupplier;

        public BindableModeratorType(String name, int tier, int tintColor,
                                     Supplier<Integer> euBoostSupplier,
                                     Supplier<Integer> fuelDiscountSupplier) {
            this.name = name;
            this.tier = tier;
            this.tintColor = tintColor;
            this.euBoostSupplier = euBoostSupplier != null ? euBoostSupplier : () -> 0;
            this.fuelDiscountSupplier = fuelDiscountSupplier != null ? fuelDiscountSupplier : () -> 0;
        }

        @Override
        public int getEUBoost() {
            return euBoostSupplier.get();
        }

        @Override
        public int getFuelDiscount() {
            return fuelDiscountSupplier.get();
        }

        @Override
        public @NotNull ResourceLocation getTexture() {
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
