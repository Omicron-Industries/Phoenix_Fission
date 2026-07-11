package net.phoenix.phoenix_fission.integration.emi;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.phoenix.phoenix_fission.PhoenixAPI;
import net.phoenix.phoenix_fission.PhoenixFission;
import net.phoenix.phoenix_fission.api.block.IFissionModeratorType;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/** Info card describing a fission moderator's passive reactor bonuses. */
public class FissionModeratorEmiRecipe implements EmiRecipe {

    private static final int W = 200;
    private static final int H = 76;

    private final IFissionModeratorType type;
    private final ResourceLocation id;

    public FissionModeratorEmiRecipe(IFissionModeratorType type) {
        this.type = type;
        this.id = PhoenixFission.id("fission_moderator/" + type.getName().toLowerCase(Locale.ROOT).replace(' ', '_'));
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return PhoenixFissionEmiPlugin.MODERATOR;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return id;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return List.of();
    }

    @Override
    public List<EmiStack> getOutputs() {
        return List.of();
    }

    @Override
    public int getDisplayWidth() {
        return W;
    }

    @Override
    public int getDisplayHeight() {
        return H;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        Font font = Minecraft.getInstance().font;

        FissionEmiUtils.drawPanel(widgets, W, H);

        var block = PhoenixAPI.FISSION_MODERATORS.get(type);
        EmiStack stack = block != null && block.get() != null ? EmiStack.of(new ItemStack(block.get().asItem())) :
                EmiStack.EMPTY;
        widgets.addSlot(stack, 8, 8);

        String name = type.getName();
        widgets.addText(Component.literal(name), 30, 8, FissionEmiUtils.tierColor(type.getTier()), false);
        widgets.addText(Component.literal("Tier " + type.getTier()), 30, 18, 0xFF_AAAAAA, false);

        String eu = "+" + type.getEUBoost() + "% EU output";
        String fuel = "-" + type.getFuelDiscount() + "% fuel usage";
        widgets.addText(Component.literal(eu), 8, 40, 0xFF_FFDD55, false);
        widgets.addText(Component.literal(fuel), 8, 51, 0xFF_88FF88, false);

        String heat = String.format(Locale.ROOT, "x%.1f heat multiplier", type.getHeatMultiplier());
        String par = "+" + type.getParallelBonus() + " parallel";
        widgets.addText(Component.literal(heat), W - font.width(heat) - 8, 40, 0xFF_FF9944, false);
        widgets.addText(Component.literal(par), W - font.width(par) - 8, 51, 0xFF_AADDFF, false);

        String note = "Passive reactor bonus while installed";
        widgets.addText(Component.literal(note), W / 2 - font.width(note) / 2, 64, 0xFF_888888, false);
    }
}
