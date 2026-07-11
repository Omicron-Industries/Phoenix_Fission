package net.phoenix.phoenix_fission.integration.emi;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.phoenix.phoenix_fission.PhoenixFission;
import net.phoenix.phoenix_fission.api.block.IFissionCoolerType;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public class FissionCoolantCycleEmiRecipe implements EmiRecipe {

    private static final int W = 200;

    private final IFissionCoolerType type;
    private final ResourceLocation id;

    public FissionCoolantCycleEmiRecipe(IFissionCoolerType type) {
        this.type = type;
        this.id = PhoenixFission.id("fission_coolant/" + type.getName().toLowerCase(Locale.ROOT).replace(' ', '_'));
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return PhoenixFissionEmiPlugin.COOLANT_CYCLE;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return id;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        if (type.isPassive()) return List.of();
        EmiStack s = FissionEmiUtils.resolveFluid(type.getInputCoolantFluidId(), type.getCoolantPerTick());
        return s.isEmpty() ? List.of() : List.of(s);
    }

    @Override
    public List<EmiStack> getOutputs() {
        if (type.isPassive()) return List.of();
        String outId = type.getOutputCoolantFluidId();
        if (outId.equals(type.getInputCoolantFluidId())) return List.of();
        EmiStack s = FissionEmiUtils.resolveFluid(outId, type.getCoolantPerTick());
        return s.isEmpty() ? List.of() : List.of(s);
    }

    @Override
    public int getDisplayWidth() {
        return W;
    }

    @Override
    public int getDisplayHeight() {
        return type.isPassive() ? 58 : 74;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        Font font = Minecraft.getInstance().font;
        int h = getDisplayHeight();

        FissionEmiUtils.drawPanel(widgets, W, h);

        // Name header, color-coded by tier
        String name = type.getName();
        widgets.addText(Component.literal(name), W / 2 - font.width(name) / 2, 8,
                FissionEmiUtils.tierColor(type.getTier()), false);

        if (type.isPassive()) {
            String label = "Passive Cooler — no coolant needed";
            widgets.addText(Component.literal(label), W / 2 - font.width(label) / 2, 22, 0xFF_88FF88, false);

            String flat = String.format(Locale.ROOT, "%.0f HU/t (always active)", type.getFlatCoolingHUt());
            widgets.addText(Component.literal(flat), W / 2 - font.width(flat) / 2, 34, 0xFF_55FFFF, false);

            String note = "Active even during coolant outage";
            widgets.addText(Component.literal(note), W / 2 - font.width(note) / 2, 46, 0xFF_888888, false);
        } else {
            EmiStack inFluid = FissionEmiUtils.resolveFluid(type.getInputCoolantFluidId(), type.getCoolantPerTick());
            String outFluidId = type.getOutputCoolantFluidId();
            boolean sameFluid = outFluidId.equals(type.getInputCoolantFluidId());

            int slotY = 22;
            EmiTexture arrow = EmiTexture.FULL_ARROW;

            // Center the input/arrow/(output or label) group in the widened panel
            int outWidth = sameFluid ? font.width("(same fluid)") : 18;
            int groupWidth = 18 + 6 + arrow.width + 6 + outWidth;
            int groupX = W / 2 - groupWidth / 2;

            widgets.addSlot(inFluid, groupX, slotY);
            widgets.addTexture(arrow, groupX + 18 + 6, slotY + 1);

            int outX = groupX + 18 + 6 + arrow.width + 6;
            if (!sameFluid) {
                EmiStack outFluid = FissionEmiUtils.resolveFluid(outFluidId, type.getCoolantPerTick());
                widgets.addSlot(outFluid, outX, slotY).recipeContext(this);
            } else {
                widgets.addText(Component.literal("(same fluid)"), outX, slotY + 6, 0xFF_777777, false);
            }

            String thresh = type.getCoolerTemperature() + " K threshold";
            String mbt = type.getCoolantPerTick() + " mB/t";
            widgets.addText(Component.literal(thresh), 8, 46, 0xFF_55FFFF, false);
            widgets.addText(Component.literal(mbt), W - font.width(mbt) - 8, 46, 0xFF_AADDFF, false);

            String cond = "Cools when reactor heat > threshold";
            widgets.addText(Component.literal(cond), W / 2 - font.width(cond) / 2, 60, 0xFF_888888, false);
        }
    }
}
