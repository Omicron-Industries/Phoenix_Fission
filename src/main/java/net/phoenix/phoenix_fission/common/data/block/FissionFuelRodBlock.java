package net.phoenix.phoenix_fission.common.data.block;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.block.ActiveBlock;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;
import net.phoenix.phoenix_fission.PhoenixFission;
import net.phoenix.phoenix_fission.api.block.IFissionFuelRodType;
import net.phoenix.phoenix_fission.configs.PhoenixFissionConfigs;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@ParametersAreNonnullByDefault
public class FissionFuelRodBlock extends ActiveBlock {

    private final IFissionFuelRodType fuelRodType;

    public FissionFuelRodBlock(Properties props, IFissionFuelRodType type) {
        super(props);
        this.fuelRodType = type;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level,
                                List<Component> tooltip, TooltipFlag flag) {
        if (!GTUtil.isShiftDown()) {
            tooltip.add(Component.translatable("block.phoenix_fission.fission_moderator.shift")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        tooltip.add(Component.translatable("block.phoenix_fission.fission_fuel_rod.info_header")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));

        // Fuel and Output Info
        tooltip.add(
                Component
                        .translatable("phoenix_fission.fuel_required", getRegistryDisplayName(fuelRodType.getFuelKey()))
                        .withStyle(ChatFormatting.WHITE));

        tooltip.add(
                Component
                        .translatable("phoenix_fission.depleted_fuel",
                                getRegistryDisplayName(fuelRodType.getOutputKey()))
                        .withStyle(ChatFormatting.DARK_GRAY));

        // Stats from Config
        tooltip.add(Component.translatable("phoenix_fission.heat_production",
                Component.literal(String.valueOf(fuelRodType.getBaseHeatProduction()))
                        .withStyle(ChatFormatting.RED))
                .append(Component.literal(" HU/t").withStyle(ChatFormatting.GRAY)));

        double seconds = fuelRodType.getDurationTicks() / 20.0;
        tooltip.add(Component.translatable("phoenix_fission.fuel_cycle",
                Component.literal(String.valueOf(fuelRodType.getAmountPerCycle()))
                        .withStyle(ChatFormatting.WHITE),
                Component.literal(String.format("%.2f", seconds))
                        .withStyle(ChatFormatting.GOLD))
                .withStyle(ChatFormatting.GRAY));

        int bias = fuelRodType.getNeutronBias();
        tooltip.add(Component.translatable("phoenix_fission.neutron_bias",
                Component.literal((bias >= 0 ? "+" : "") + bias + "%")
                        .withStyle(bias >= 0 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.BLUE))
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.translatable("gtceu.tooltip.tier",
                Component.literal(GTValues.VNF[fuelRodType.getTier()])
                        .withStyle(ChatFormatting.DARK_PURPLE)));
    }

    public static Component getRegistryDisplayName(String key) {
        ResourceLocation rl = ResourceLocation.tryParse(key);
        if (rl == null) return Component.literal(key).withStyle(ChatFormatting.YELLOW);

        Item item = ForgeRegistries.ITEMS.getValue(rl);
        if (item != null && item != Items.AIR) {
            return item.getName(new ItemStack(item));
        }

        Fluid fluid = ForgeRegistries.FLUIDS.getValue(rl);
        if (fluid != null && fluid != Fluids.EMPTY) {
            return Component.translatable(fluid.getFluidType().getDescriptionId());
        }

        return Component.literal(key).withStyle(ChatFormatting.YELLOW);
    }

    public enum FissionFuelRodTypes implements StringRepresentable, IFissionFuelRodType {

        T1_FUEL_ROD("t1_fuel_rod", PhoenixFissionConfigs.INSTANCE.fissionStats.fuelRods.heatProductionT1, 1,
                PhoenixFissionConfigs.INSTANCE.fissionStats.fuelRods.cycleDurationT1,
                PhoenixFissionConfigs.INSTANCE.fissionStats.fuelRods.cycleAmountT1,
                PhoenixFissionConfigs.INSTANCE.fissionStats.fuelRods.fuelUsedT1,
                PhoenixFissionConfigs.INSTANCE.fissionStats.fuelRods.depletedGivenT1, 0xFF62FF57,
                PhoenixFissionConfigs.INSTANCE.fissionStats.fuelRods.neutronBiasT1),
        T2_FUEL_ROD("t2_fuel_rod", PhoenixFissionConfigs.INSTANCE.fissionStats.fuelRods.heatProductionT2, 2,
                PhoenixFissionConfigs.INSTANCE.fissionStats.fuelRods.cycleDurationT2,
                PhoenixFissionConfigs.INSTANCE.fissionStats.fuelRods.cycleAmountT2,
                PhoenixFissionConfigs.INSTANCE.fissionStats.fuelRods.fuelUsedT2,
                PhoenixFissionConfigs.INSTANCE.fissionStats.fuelRods.depletedGivenT2, 0xFF8AFF57,
                PhoenixFissionConfigs.INSTANCE.fissionStats.fuelRods.neutronBiasT2),
        T3_FUEL_ROD("t3_fuel_rod", PhoenixFissionConfigs.INSTANCE.fissionStats.fuelRods.heatProductionT3, 3,
                PhoenixFissionConfigs.INSTANCE.fissionStats.fuelRods.cycleDurationT3,
                PhoenixFissionConfigs.INSTANCE.fissionStats.fuelRods.cycleAmountT3,
                PhoenixFissionConfigs.INSTANCE.fissionStats.fuelRods.fuelUsedT3,
                PhoenixFissionConfigs.INSTANCE.fissionStats.fuelRods.depletedGivenT3, 0xFF57FFD2,
                PhoenixFissionConfigs.INSTANCE.fissionStats.fuelRods.neutronBiasT3),
        T4_FUEL_ROD("t4_fuel_rod", PhoenixFissionConfigs.INSTANCE.fissionStats.fuelRods.heatProductionT4, 4,
                PhoenixFissionConfigs.INSTANCE.fissionStats.fuelRods.cycleDurationT4,
                PhoenixFissionConfigs.INSTANCE.fissionStats.fuelRods.cycleAmountT4,
                PhoenixFissionConfigs.INSTANCE.fissionStats.fuelRods.fuelUsedT4,
                PhoenixFissionConfigs.INSTANCE.fissionStats.fuelRods.depletedGivenT4, 0xFF57A8FF,
                PhoenixFissionConfigs.INSTANCE.fissionStats.fuelRods.neutronBiasT4),
        T5_FUEL_ROD("t5_fuel_rod", PhoenixFissionConfigs.INSTANCE.fissionStats.fuelRods.heatProductionT5, 5,
                PhoenixFissionConfigs.INSTANCE.fissionStats.fuelRods.cycleDurationT5,
                PhoenixFissionConfigs.INSTANCE.fissionStats.fuelRods.cycleAmountT5,
                PhoenixFissionConfigs.INSTANCE.fissionStats.fuelRods.fuelUsedT5,
                PhoenixFissionConfigs.INSTANCE.fissionStats.fuelRods.depletedGivenT5, 0xFFFF5757,
                PhoenixFissionConfigs.INSTANCE.fissionStats.fuelRods.neutronBiasT5);

        @Getter
        @NotNull
        private final String name;
        private final int defaultHeat;
        @Getter
        private final int tier;
        private final int defaultDuration;
        private final int defaultAmount; // New internal default
        @NotNull
        private final String defaultFuelKey; // New internal default
        @NotNull
        private final String defaultOutputKey; // New internal default
        @Getter
        private final int tintColor;

        FissionFuelRodTypes(String name, int heat, int tier, int duration, int amount, String fuelKey, String outputKey,
                            int tintColor, int neutronBias) {
            this.name = name;
            this.defaultHeat = heat;
            this.tier = tier;
            this.defaultDuration = duration;
            this.defaultAmount = amount;
            this.defaultFuelKey = fuelKey;
            this.defaultOutputKey = outputKey;
            this.tintColor = tintColor;
        }

        @Override
        public int getAmountPerCycle() {
            return defaultAmount;
        }

        @Override
        public @NotNull String getOutputKey() {
            return defaultOutputKey;
        }

        @Override
        public int getDurationTicks() {
            return defaultDuration;
        }

        @Override
        public int getBaseHeatProduction() {
            return defaultHeat;
        }

        @Override
        public @NotNull String getFuelKey() {
            return defaultFuelKey;
        }

        @Override
        public @NotNull ResourceLocation getTexture() {
            return PhoenixFission.id("block/fission/fuel_rod_base");
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}
