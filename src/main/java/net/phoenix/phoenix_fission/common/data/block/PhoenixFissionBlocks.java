package net.phoenix.phoenix_fission.common.data.block;

import com.gregtechceu.gtceu.GTCEu;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.phoenix.phoenix_fission.PhoenixFission;
import net.phoenix.phoenix_fission.api.block.*;
import net.phoenix.phoenix_fission.configs.PhoenixFissionConfigs;

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
        return PhoenixFissionConfigs.INSTANCE != null &&
                isEnabled(PhoenixFissionConfigs.INSTANCE.fission.fissionReactorEnabled);
    }

    private static boolean msrEnabled() {
        return PhoenixFissionConfigs.INSTANCE != null && isEnabled(PhoenixFissionConfigs.INSTANCE.fission.msrEnabled);
    }

    private static boolean breederEnabled() {
        return PhoenixFissionConfigs.INSTANCE != null &&
                isEnabled(PhoenixFissionConfigs.INSTANCE.fission.breederReactorEnabled);
    }

    // --- Global Registrations & Caches ---
    public static final Map<IMSRCoreLinerType, BlockEntry<MSRCoreLinerBlock>> MSR_LINERS = new HashMap<>();

    public static final BlockEntry<NukeBlock> NUKE_BLOCK = PHOENIX_REGISTRATE
            .block("nuke_block", NukeBlock::new)
            .initialProperties(() -> Blocks.TNT) // TNT-ish
            .properties(p -> p.isValidSpawn((state, level, pos, ent) -> false))
            .blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(),
                    prov.models().cubeBottomTop(
                            ctx.getName(),
                            PhoenixFission.id("block/nuke_side"),   // Side texture
                            PhoenixFission.id("block/nuke_bottom"), // Bottom texture
                            PhoenixFission.id("block/nuke_top")     // Top texture
                    )))
            .item(BlockItem::new)
            .build()
            .register();

    // --- MSR Core Liners ---
    public static BlockEntry<MSRCoreLinerBlock> LINER_GRAPHITE = null;
    public static BlockEntry<MSRCoreLinerBlock> LINER_HASTELLOY = null;
    public static BlockEntry<MSRCoreLinerBlock> LINER_TITANIUM = null;
    public static BlockEntry<MSRCoreLinerBlock> LINER_NETHERITE = null;

    // --- Coolers (active) ---
    public static BlockEntry<FissionCoolerBlock> COOLER_BASIC = null;
    public static BlockEntry<FissionCoolerBlock> COOLER_EV = null;
    public static BlockEntry<FissionCoolerBlock> COOLER_IV = null;
    public static BlockEntry<FissionCoolerBlock> COOLER_LUV = null;

    // --- Coolers (passive — flat HU/t, no fluid) ---
    public static BlockEntry<FissionCoolerBlock> COOLER_PASSIVE_GRAPHITE = null;
    public static BlockEntry<FissionCoolerBlock> COOLER_PASSIVE_LEAD = null;
    public static BlockEntry<FissionCoolerBlock> COOLER_PASSIVE_BORON = null;

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
        // 5. Global Modular Casings Initialization
        if (fissionEnabled() || msrEnabled() || breederEnabled()) {
            FISSILE_HEAT_SAFE_CASING = registerSimpleBlock("§bFissile Heat Safe Casing", "fissile_heat_safe_casing",
                    "fissile_heat_safe_casing", BlockItem::new);
            FISSILE_REACTION_SAFE_CASING = registerSimpleBlock("§bFissile Reaction Safe Casing",
                    "fissile_reaction_safe_casing", "fissile_reaction_safe_casing", BlockItem::new);
            FISSILE_SAFE_GEARBOX_CASING = registerSimpleBlock("§bFissile Safe Gearbox", "fissile_safe_gearbox_casing",
                    "fissile_safe_gearbox", BlockItem::new);

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
