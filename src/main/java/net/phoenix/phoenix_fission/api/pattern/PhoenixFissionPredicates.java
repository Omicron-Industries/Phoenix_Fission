package net.phoenix.phoenix_fission.api.pattern;

import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks; // Added for fallback
import net.phoenix.phoenix_fission.PhoenixAPI;
import net.phoenix.phoenix_fission.api.block.*;
import net.phoenix.phoenix_fission.common.data.block.*;

import java.util.*;
import java.util.function.Supplier;

public class PhoenixFissionPredicates {

    public static TraceabilityPredicate fissionCoolers() {
        return new TraceabilityPredicate(blockWorldState -> {
            var blockState = blockWorldState.getBlockState();
            for (Map.Entry<IFissionCoolerType, Supplier<FissionCoolerBlock>> entry : PhoenixAPI.FISSION_COOLERS.entrySet()) {
                if (blockState.is(entry.getValue().get())) {
                    var type = entry.getKey();
                    List<IFissionCoolerType> componentList = blockWorldState.getMatchContext().getOrPut("CoolerTypes", new ArrayList<>());
                    componentList.add(type);
                    return true;
                }
            }
            return false;
        },
                () -> {
                    if (PhoenixAPI.FISSION_COOLERS.isEmpty()) {
                        return new BlockInfo[]{BlockInfo.fromBlockState(Blocks.IRON_BLOCK.defaultBlockState())};
                    }
                    return PhoenixAPI.FISSION_COOLERS.entrySet().stream()
                            .sorted(Comparator.comparingInt(e -> e.getKey().getTier()))
                            .map(e -> BlockInfo.fromBlockState(e.getValue().get().defaultBlockState()))
                            .toArray(BlockInfo[]::new);
                })
                .addTooltips(Component.translatable("phoenix.multiblock.pattern.info.multiple_coolers"));
    }

    public static TraceabilityPredicate fissionBlankets() {
        return new TraceabilityPredicate(blockWorldState -> {
            var blockState = blockWorldState.getBlockState();
            for (Map.Entry<IFissionBlanketType, Supplier<FissionBlanketBlock>> entry : PhoenixAPI.FISSION_BLANKETS.entrySet()) {
                if (blockState.is(entry.getValue().get())) {
                    var type = entry.getKey();
                    List<IFissionBlanketType> componentList = blockWorldState.getMatchContext().getOrPut("BlanketTypes", new ArrayList<>());
                    componentList.add(type);
                    return true;
                }
            }
            return false;
        },
                () -> {
                    if (PhoenixAPI.FISSION_BLANKETS.isEmpty()) {
                        return new BlockInfo[]{BlockInfo.fromBlockState(Blocks.IRON_BLOCK.defaultBlockState())};
                    }
                    return PhoenixAPI.FISSION_BLANKETS.entrySet().stream()
                            .sorted(Comparator.comparingInt(e -> e.getKey().getTier()))
                            .map(e -> BlockInfo.fromBlockState(e.getValue().get().defaultBlockState()))
                            .toArray(BlockInfo[]::new);
                })
                .addTooltips(Component.translatable("phoenix.multiblock.pattern.info.multiple_blankets"));
    }

    public static TraceabilityPredicate fissionFuelRods() {
        return new TraceabilityPredicate(blockWorldState -> {
            var blockState = blockWorldState.getBlockState();
            for (Map.Entry<IFissionFuelRodType, Supplier<FissionFuelRodBlock>> entry : PhoenixAPI.FISSION_FUEL_RODS.entrySet()) {
                if (blockState.is(entry.getValue().get())) {
                    var type = entry.getKey();
                    List<IFissionFuelRodType> componentList = blockWorldState.getMatchContext().getOrPut("FuelRodTypes", new ArrayList<>());
                    componentList.add(type);
                    return true;
                }
            }
            return false;
        },
                () -> {
                    if (PhoenixAPI.FISSION_FUEL_RODS.isEmpty()) {
                        return new BlockInfo[]{BlockInfo.fromBlockState(Blocks.DIRT.defaultBlockState())};
                    }
                    return PhoenixAPI.FISSION_FUEL_RODS.entrySet().stream()
                            .sorted(Comparator.comparingInt(e -> e.getKey().getTier()))
                            .map(e -> BlockInfo.fromBlockState(e.getValue().get().defaultBlockState()))
                            .toArray(BlockInfo[]::new);
                })
                .addTooltips(Component.translatable("phoenix.multiblock.pattern.info.multiple_fuel_rods"));
    }

    public static TraceabilityPredicate fissionModerators() {
        return new TraceabilityPredicate(blockWorldState -> {
            var blockState = blockWorldState.getBlockState();
            for (Map.Entry<IFissionModeratorType, Supplier<FissionModeratorBlock>> entry : PhoenixAPI.FISSION_MODERATORS.entrySet()) {
                if (blockState.is(entry.getValue().get())) {
                    var type = entry.getKey();
                    List<IFissionModeratorType> componentList = blockWorldState.getMatchContext().getOrPut("ModeratorTypes", new ArrayList<>());
                    componentList.add(type);
                    return true;
                }
            }
            return false;
        },
                () -> {
                    if (PhoenixAPI.FISSION_MODERATORS.isEmpty()) {
                        return new BlockInfo[]{BlockInfo.fromBlockState(Blocks.LAPIS_BLOCK.defaultBlockState())};
                    }
                    return PhoenixAPI.FISSION_MODERATORS.entrySet().stream()
                            .sorted(Comparator.comparingInt(e -> e.getKey().getTier()))
                            .map(e -> BlockInfo.fromBlockState(e.getValue().get().defaultBlockState()))
                            .toArray(BlockInfo[]::new);
                })
                .addTooltips(Component.translatable("phoenix.multiblock.pattern.info.multiple_moderators"));
    }

    public static TraceabilityPredicate msrCoreLiner() {
        return new TraceabilityPredicate(blockWorldState -> {
            var blockState = blockWorldState.getBlockState();
            for (Map.Entry<IMSRCoreLinerType, Supplier<MSRCoreLinerBlock>> entry : PhoenixAPI.MSR_LINERS.entrySet()) {
                if (blockState.is(entry.getValue().get())) {
                    var type = entry.getKey();
                    blockWorldState.getMatchContext().increment("MSRLinerCount", 1);
                    int currentMinTier = blockWorldState.getMatchContext().getOrDefault("MSRMinTier", Integer.MAX_VALUE);
                    blockWorldState.getMatchContext().set("MSRMinTier", Math.min(currentMinTier, type.getTier()));
                    List<IMSRCoreLinerType> componentList = blockWorldState.getMatchContext().getOrPut("LinerTypes", new ArrayList<>());
                    componentList.add(type);
                    return true;
                }
            }
            return false;
        },
                () -> {
                    // SAFE-GUARD: If the registration loop hasn't filled the map yet,
                    // feed a fallback block to GregTech so the EMI UI engine doesn't blow up.
                    if (PhoenixAPI.MSR_LINERS.isEmpty()) {
                        return new BlockInfo[]{BlockInfo.fromBlockState(net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState())};
                    }
                    return PhoenixAPI.MSR_LINERS.entrySet().stream()
                            .sorted(Comparator.comparingInt(e -> e.getKey().getTier()))
                            .map(e -> BlockInfo.fromBlockState(e.getValue().get().defaultBlockState()))
                            .toArray(BlockInfo[]::new);
                })
                .addTooltips(Component.translatable("phoenix.multiblock.pattern.info.multiple_liners"));
    }
}