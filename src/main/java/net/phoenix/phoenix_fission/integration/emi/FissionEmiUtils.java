package net.phoenix.phoenix_fission.integration.emi;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;

import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

/** Shared rendering/resolution helpers for the fission component EMI pages. */
final class FissionEmiUtils {

    private FissionEmiUtils() {}

    static final int BG_FILL = 0x30_FFFFFF;
    static final int BG_BORDER = 0xFF_555555;

    /** Draws a subtle panel background + border behind a recipe widget. */
    static void drawPanel(WidgetHolder widgets, int w, int h) {
        widgets.addDrawable(0, 0, w, h, (gg, mouseX, mouseY, delta) -> {
            gg.fill(1, 1, w - 1, h - 1, BG_FILL);
            gg.fill(0, 0, w, 1, BG_BORDER);
            gg.fill(0, h - 1, w, h, BG_BORDER);
            gg.fill(0, 0, 1, h, BG_BORDER);
            gg.fill(w - 1, 0, w, h, BG_BORDER);
        });
    }

    /** Loose tier -> color ramp used to color-code component names by tier. */
    static int tierColor(int tier) {
        return switch (Math.max(1, Math.min(tier, 6))) {
            case 1 -> 0xFF_AAAAAA;
            case 2 -> 0xFF_55FF55;
            case 3 -> 0xFF_55FFFF;
            case 4 -> 0xFF_FF55FF;
            case 5 -> 0xFF_FFAA00;
            default -> 0xFF_FF5555;
        };
    }

    static EmiStack resolveItem(String id, int count) {
        if (id == null || id.isEmpty() || "none".equalsIgnoreCase(id)) return EmiStack.EMPTY;
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return EmiStack.EMPTY;
        Item item = ForgeRegistries.ITEMS.getValue(rl);
        if (item == null || item == net.minecraft.world.item.Items.AIR) return EmiStack.EMPTY;
        return EmiStack.of(new ItemStack(item, Math.max(1, count)));
    }

    static EmiStack resolveFluid(String id, int mb) {
        if (id == null || id.isEmpty() || "none".equalsIgnoreCase(id)) return EmiStack.EMPTY;
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return EmiStack.EMPTY;
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(rl);
        if (fluid == null || fluid == Fluids.EMPTY) return EmiStack.EMPTY;
        return EmiStack.of(fluid, Math.max(1, mb));
    }
}
