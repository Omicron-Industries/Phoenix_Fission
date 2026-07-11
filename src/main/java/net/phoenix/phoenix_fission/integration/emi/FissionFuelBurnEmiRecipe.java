package net.phoenix.phoenix_fission.integration.emi;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.phoenix_fission.PhoenixFission;
import net.phoenix.phoenix_fission.api.block.IFissionFuelRodType;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public class FissionFuelBurnEmiRecipe implements EmiRecipe {

    private static final int W = 200;
    private static final int H = 74;
    private static final int SLOT_Y = 8;
    private static final int IN_X = 12;
    private static final int OUT_X = W - 12 - 18;

    private final IFissionFuelRodType type;
    private final ResourceLocation id;

    public FissionFuelBurnEmiRecipe(IFissionFuelRodType type) {
        this.type = type;
        this.id = PhoenixFission.id("fission_fuel/" + type.getName().toLowerCase(Locale.ROOT).replace(' ', '_'));
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return PhoenixFissionEmiPlugin.FUEL_BURN;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return id;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        EmiStack s = FissionEmiUtils.resolveItem(type.getFuelKey(), type.getAmountPerCycle());
        return s.isEmpty() ? List.of() : List.of(s);
    }

    @Override
    public List<EmiStack> getOutputs() {
        EmiStack s = FissionEmiUtils.resolveItem(type.getOutputKey(), 1);
        return s.isEmpty() ? List.of() : List.of(s);
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

        // Item slots + arrow icon between them
        widgets.addSlot(FissionEmiUtils.resolveItem(type.getFuelKey(), type.getAmountPerCycle()), IN_X, SLOT_Y);
        widgets.addSlot(FissionEmiUtils.resolveItem(type.getOutputKey(), 1), OUT_X, SLOT_Y).recipeContext(this);

        EmiTexture arrow = EmiTexture.FULL_ARROW;
        int arrowX = IN_X + 18 + (OUT_X - (IN_X + 18) - arrow.width) / 2;
        widgets.addTexture(arrow, arrowX, SLOT_Y + 1);

        // Name row, color-coded by tier
        String name = type.getName();
        int nameX = W / 2 - font.width(name) / 2;
        widgets.addText(Component.literal(name), nameX, 34, FissionEmiUtils.tierColor(type.getTier()), false);

        // Stat rows
        String hut = type.getBaseHeatProduction() + " HU/t";
        double sec = type.getDurationTicks() / 20.0;
        String dur = String.format(Locale.ROOT, "%.0fs cycle", sec);
        widgets.addText(Component.literal(hut), 8, 46, 0xFF_FF9944, false);
        widgets.addText(Component.literal(dur), W - font.width(dur) - 8, 46, 0xFF_AADDFF, false);

        String amt = "x" + type.getAmountPerCycle() + " consumed per cycle";
        widgets.addText(Component.literal(amt), W / 2 - font.width(amt) / 2, 58, 0xFF_AAAAAA, false);
    }
}
