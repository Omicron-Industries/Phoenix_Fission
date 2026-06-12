package net.phoenix.phoenix_fission.common.data.block;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.phoenix.phoenix_fission.PhoenixAPI;
import net.phoenix.phoenix_fission.PhoenixFission;
import net.phoenix.phoenix_fission.api.block.*;
import net.phoenix.phoenix_fission.configs.PhoenixFissionConfigs;
import net.phoenix.phoenix_fission.datagen.models.PhoenixFissionMachineModels;

import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullBiFunction;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static net.phoenix.phoenix_fission.PhoenixFission.PHOENIX_REGISTRATE;

@SuppressWarnings("unused")
public class PhoenixFissionBlocks {

    // Global toggle helper to check configuration states safely
    private static boolean isEnabled(boolean flag) {
        return flag || GTCEu.isDataGen();
    }

    private static boolean fissionEnabled() {
        return PhoenixFissionConfigs.INSTANCE != null && isEnabled(PhoenixFissionConfigs.INSTANCE.fission.fissionReactorEnabled);
    }

    private static boolean msrEnabled() {
        return PhoenixFissionConfigs.INSTANCE != null && isEnabled(PhoenixFissionConfigs.INSTANCE.fission.msrEnabled);
    }

    private static boolean breederEnabled() {
        return PhoenixFissionConfigs.INSTANCE != null && isEnabled(PhoenixFissionConfigs.INSTANCE.fission.breederReactorEnabled);
    }

    // --- Global Registrations & Caches ---
    public static final Map<IMSRCoreLinerType, BlockEntry<MSRCoreLinerBlock>> MSR_LINERS = new HashMap<>();

    public static BlockEntry<NukeBlock> NUKE_BLOCK = null;

    // --- MSR Core Liners ---
    public static BlockEntry<MSRCoreLinerBlock> LINER_GRAPHITE = null;
    public static BlockEntry<MSRCoreLinerBlock> LINER_HASTELLOY = null;
    public static BlockEntry<MSRCoreLinerBlock> LINER_TITANIUM = null;
    public static BlockEntry<MSRCoreLinerBlock> LINER_NETHERITE = null;

    // --- Coolers ---
    public static BlockEntry<FissionCoolerBlock> COOLER_BASIC = null;
    public static BlockEntry<FissionCoolerBlock> COOLER_EV = null;
    public static BlockEntry<FissionCoolerBlock> COOLER_IV = null;
    public static BlockEntry<FissionCoolerBlock> COOLER_LUV = null;

    // --- Moderators ---
    public static BlockEntry<FissionModeratorBlock> MODERATOR_GRAPHITE = null;
    public static BlockEntry<FissionModeratorBlock> MODERATOR_BERYLLIUM = null;
    public static BlockEntry<FissionModeratorBlock> MODERATOR_HEAVY_WATER = null;
    public static BlockEntry<FissionModeratorBlock> MODERATOR_NIOBIUM_SIC = null;

    // --- Fuel Rods ---
    public static BlockEntry<FissionFuelRodBlock> FUEL_ROD_T1 = null;
    public static BlockEntry<FissionFuelRodBlock> FUEL_ROD_T2 = null;
    public static BlockEntry<FissionFuelRodBlock> FUEL_ROD_T3 = null;
    public static BlockEntry<FissionFuelRodBlock> FUEL_ROD_T4 = null;
    public static BlockEntry<FissionFuelRodBlock> FUEL_ROD_T5 = null;

    // --- Breeder Blankets ---
    public static BlockEntry<FissionBlanketBlock> THORIUM_BLANKET = null;
    public static BlockEntry<FissionBlanketBlock> URANIUM_BLANKET = null;
    public static BlockEntry<FissionBlanketBlock> NEPTUNIUM_BLANKET = null;
    public static BlockEntry<FissionBlanketBlock> PLUTONIUM_BLANKET = null;
    public static BlockEntry<FissionBlanketBlock> AMERICIUM_BLANKET = null;

    // --- Casings ---
    public static BlockEntry<Block> FISSILE_HEAT_SAFE_CASING = null;
    public static BlockEntry<Block> FISSILE_REACTION_SAFE_CASING = null;
    public static BlockEntry<Block> FISSILE_SAFE_GEARBOX_CASING = null;
    public static BlockEntry<Block> EMPTY_REACTOR_COMPONENT = null;

    // All blocks execute dynamically via init() to check features configuration safely
    public static void init() {

        // 1. Optional Weapons Toggle
        if (fissionEnabled() || msrEnabled() || breederEnabled()) {
            NUKE_BLOCK = PHOENIX_REGISTRATE
                    .block("nuke", NukeBlock::new)
                    .initialProperties(() -> Blocks.TNT)
                    .properties(p -> p.isValidSpawn((state, level, pos, ent) -> false))
                    .blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(),
                            prov.models().cubeBottomTop(
                                    ctx.getName(),
                                    PhoenixFission.id("block/nuke_side"),
                                    PhoenixFission.id("block/nuke_bottom"),
                                    PhoenixFission.id("block/nuke_top"))))
                    .item(BlockItem::new)
                    .build()
                    .register();
        }

        // 2. Molten Salt Reactor Blocks Initialization
        if (msrEnabled()) {
            var msrConfigs = PhoenixFissionConfigs.INSTANCE.fissionStats.msrLiners;

            if (isEnabled(msrConfigs.enableGraphiteLiner))
                LINER_GRAPHITE  = createMSRLinerBlock(MSRCoreLinerBlock.MSRLinerTypes.LINER_GRAPHITE);
            if (isEnabled(msrConfigs.enableHastelloyLiner))
                LINER_HASTELLOY = createMSRLinerBlock(MSRCoreLinerBlock.MSRLinerTypes.LINER_HASTELLOY);
            if (isEnabled(msrConfigs.enableTitaniumLiner))
                LINER_TITANIUM  = createMSRLinerBlock(MSRCoreLinerBlock.MSRLinerTypes.LINER_TITANIUM);
            if (isEnabled(msrConfigs.enableNetheriteLiner))
                LINER_NETHERITE = createMSRLinerBlock(MSRCoreLinerBlock.MSRLinerTypes.LINER_NETHERITE);
        }

        // 3. Fission & Breeder Reactor Shared Core Structural Blocks
        if (fissionEnabled() || breederEnabled()) {
            var stats = PhoenixFissionConfigs.INSTANCE.fissionStats;

            // Coolers
            if (isEnabled(stats.coolers.enableBasicCooler))
                COOLER_BASIC = createCoolerBlock(FissionCoolerBlock.FissionCoolerTypes.COOLER_BASIC);
            if (isEnabled(stats.coolers.enableEVCooler))
                COOLER_EV    = createCoolerBlock(FissionCoolerBlock.FissionCoolerTypes.COOLER_EV);
            if (isEnabled(stats.coolers.enableIVCooler))
                COOLER_IV    = createCoolerBlock(FissionCoolerBlock.FissionCoolerTypes.COOLER_IV);
            if (isEnabled(stats.coolers.enableLuVCooler))
                COOLER_LUV   = createCoolerBlock(FissionCoolerBlock.FissionCoolerTypes.COOLER_LUV);

            // Moderators
            if (isEnabled(stats.moderators.enableGraphiteModerator))
                MODERATOR_GRAPHITE    = createModeratorBlock(FissionModeratorBlock.FissionModeratorTypes.GRAPHITE);
            if (isEnabled(stats.moderators.enableBerylliumModerator))
                MODERATOR_BERYLLIUM   = createModeratorBlock(FissionModeratorBlock.FissionModeratorTypes.BERYLLIUM);
            if (isEnabled(stats.moderators.enableHeavyWaterModerator))
                MODERATOR_HEAVY_WATER = createModeratorBlock(FissionModeratorBlock.FissionModeratorTypes.HEAVY_WATER);
            if (isEnabled(stats.moderators.enableNiobiumSicModerator))
                MODERATOR_NIOBIUM_SIC = createModeratorBlock(FissionModeratorBlock.FissionModeratorTypes.NIOBIUM_SIC);

            // Fuel Rods
            if (isEnabled(stats.fuelRods.enableFuelRodT1))
                FUEL_ROD_T1 = createFuelRodBlock(FissionFuelRodBlock.FissionFuelRodTypes.T1_FUEL_ROD);
            if (isEnabled(stats.fuelRods.enableFuelRodT2))
                FUEL_ROD_T2 = createFuelRodBlock(FissionFuelRodBlock.FissionFuelRodTypes.T2_FUEL_ROD);
            if (isEnabled(stats.fuelRods.enableFuelRodT3))
                FUEL_ROD_T3 = createFuelRodBlock(FissionFuelRodBlock.FissionFuelRodTypes.T3_FUEL_ROD);
            if (isEnabled(stats.fuelRods.enableFuelRodT4))
                FUEL_ROD_T4 = createFuelRodBlock(FissionFuelRodBlock.FissionFuelRodTypes.T4_FUEL_ROD);
            if (isEnabled(stats.fuelRods.enableFuelRodT5))
                FUEL_ROD_T5 = createFuelRodBlock(FissionFuelRodBlock.FissionFuelRodTypes.T5_FUEL_ROD);
        }

        // 4. Breeder Blanket Core Structural Blocks Only
        if (breederEnabled()) {
            THORIUM_BLANKET   = createBlanketBlock(FissionBlanketBlock.BreederBlanketTypes.THORIUM_BLANKET);
            URANIUM_BLANKET   = createBlanketBlock(FissionBlanketBlock.BreederBlanketTypes.URANIUM_BLANKET);
            NEPTUNIUM_BLANKET = createBlanketBlock(FissionBlanketBlock.BreederBlanketTypes.NEPTUNIUM_BLANKET);
            PLUTONIUM_BLANKET = createBlanketBlock(FissionBlanketBlock.BreederBlanketTypes.PLUTONIUM_BLANKET);
            AMERICIUM_BLANKET = createBlanketBlock(FissionBlanketBlock.BreederBlanketTypes.AMERICIUM_BLANKET);
        }

        // 5. Global Modular Casings Initialization
        if (fissionEnabled() || msrEnabled() || breederEnabled()) {
            FISSILE_HEAT_SAFE_CASING     = registerSimpleBlock("§bFissile Heat Safe Casing", "fissile_heat_safe_casing", "fissile_heat_safe_casing", BlockItem::new);
            FISSILE_REACTION_SAFE_CASING = registerSimpleBlock("§bFissile Reaction Safe Casing", "fissile_reaction_safe_casing", "fissile_reaction_safe_casing", BlockItem::new);
            FISSILE_SAFE_GEARBOX_CASING   = registerSimpleBlock("§bFissile Safe Gearbox", "fissile_safe_gearbox_casing", "fissile_safe_gearbox", BlockItem::new);

            EMPTY_REACTOR_COMPONENT = PHOENIX_REGISTRATE
                    .block("empty_reactor_component", Block::new)
                    .initialProperties(() -> Blocks.IRON_BLOCK)
                    .blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(),
                            prov.models().cubeAll(ctx.getName(), PhoenixFission.id("block/fission/cooler_base"))))
                    .lang("Empty Reactor Component")
                    .item((block, props) -> new TooltipBlockItem(block, props, "tooltip.phoenix.empty_component"))
                    .build()
                    .register();
        }
    }

    // --- Helper Methods ---

    private static BlockEntry<MSRCoreLinerBlock> createMSRLinerBlock(MSRCoreLinerBlock.MSRLinerTypes type) {
        var liner = PHOENIX_REGISTRATE
                .block("msr_liner_%s".formatted(type.getName()), p -> new MSRCoreLinerBlock(p, type))
                .initialProperties(() -> Blocks.IRON_BLOCK)
                .properties(p -> p.isValidSpawn((state, level, pos, ent) -> false))
                .blockstate((ctx, prov) -> {
                    String name = ctx.getName();
                    var inactive = prov.models().cubeAll(name, type.getTexture());
                    var active = prov.models().cubeAll(name + "_active",
                            PhoenixFission.id("block/fission/msr/liners/" + type.getName() + "_active"));
                    prov.getVariantBuilder(ctx.getEntry())
                            .partialState()
                            .with(com.gregtechceu.gtceu.api.block.property.GTBlockStateProperties.ACTIVE, false)
                            .modelForState().modelFile(inactive).addModel()
                            .partialState()
                            .with(com.gregtechceu.gtceu.api.block.property.GTBlockStateProperties.ACTIVE, true)
                            .modelForState().modelFile(active).addModel();
                })
                .tag(CustomTags.MINEABLE_WITH_CONFIG_VALID_PICKAXE_WRENCH)
                .item(BlockItem::new)
                .build()
                .register();

        MSR_LINERS.put(type, liner);
        PhoenixAPI.MSR_LINERS.put(type, liner);
        return liner;
    }

    private static BlockEntry<FissionModeratorBlock> createModeratorBlock(IFissionModeratorType type) {
        var moderator = PHOENIX_REGISTRATE
                .block("%s".formatted(type.getName()), p -> new FissionModeratorBlock(p, type))
                .initialProperties(() -> Blocks.IRON_BLOCK)
                .properties(p -> p.isValidSpawn((state, level, pos, ent) -> false))
                .blockstate(PhoenixFissionMachineModels.createFissionModeratorModel(type))
                .tag(CustomTags.MINEABLE_WITH_CONFIG_VALID_PICKAXE_WRENCH)
                .item(BlockItem::new)
                .build()
                .register();

        PhoenixAPI.FISSION_MODERATORS.put(type, moderator);
        return moderator;
    }

    private static BlockEntry<FissionFuelRodBlock> createFuelRodBlock(IFissionFuelRodType type) {
        var rod = PHOENIX_REGISTRATE
                .block("%s".formatted(type.getName()), p -> new FissionFuelRodBlock(p, type))
                .initialProperties(() -> Blocks.IRON_BLOCK)
                .properties(p -> p.isValidSpawn((state, level, pos, ent) -> false))
                .blockstate(PhoenixFissionMachineModels.createFuelRodModel(type))
                .tag(CustomTags.MINEABLE_WITH_CONFIG_VALID_PICKAXE_WRENCH)
                .item(BlockItem::new)
                .build()
                .register();

        PhoenixAPI.FISSION_FUEL_RODS.put(type, rod);
        return rod;
    }

    private static BlockEntry<FissionBlanketBlock> createBlanketBlock(IFissionBlanketType type) {
        var blanket = PHOENIX_REGISTRATE
                .block("%s".formatted(type.getName()), p -> new FissionBlanketBlock(p, type))
                .initialProperties(() -> Blocks.IRON_BLOCK)
                .properties(p -> p.isValidSpawn((state, level, pos, ent) -> false))
                .blockstate(PhoenixFissionMachineModels.createBlanketRodModel(type))
                .tag(CustomTags.MINEABLE_WITH_CONFIG_VALID_PICKAXE_WRENCH)
                .item(BlockItem::new)
                .build()
                .register();

        PhoenixAPI.FISSION_BLANKETS.put(type, blanket);
        return blanket;
    }

    private static BlockEntry<FissionCoolerBlock> createCoolerBlock(IFissionCoolerType type) {
        var cooler = PHOENIX_REGISTRATE
                .block("%s".formatted(type.getName()), p -> new FissionCoolerBlock(p, type))
                .initialProperties(() -> Blocks.IRON_BLOCK)
                .properties(p -> p.isValidSpawn((state, level, pos, ent) -> false))
                .blockstate(PhoenixFissionMachineModels.createActiveCoolerModel(type))
                .tag(CustomTags.MINEABLE_WITH_CONFIG_VALID_PICKAXE_WRENCH)
                .item(BlockItem::new)
                .build()
                .register();

        PhoenixAPI.FISSION_COOLERS.put(type, cooler);
        return cooler;
    }

    private static @NotNull BlockEntry<Block> registerSimpleBlock(String name, String id, String texture,
                                                                  NonNullBiFunction<Block, Item.Properties, ? extends BlockItem> func) {
        return PHOENIX_REGISTRATE
                .block(id, Block::new)
                .initialProperties(() -> Blocks.IRON_BLOCK)
                .properties(p -> p.isValidSpawn((state, level, pos, ent) -> false))
                .blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(),
                        prov.models().cubeAll(ctx.getName(), PhoenixFission.id("block/" + texture))))
                .lang(name)
                .item(func)
                .build()
                .register();
    }
}