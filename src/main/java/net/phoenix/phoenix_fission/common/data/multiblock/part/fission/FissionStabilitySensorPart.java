package net.phoenix.phoenix_fission.common.data.multiblock.part.fission;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget;
import com.gregtechceu.gtceu.api.gui.widget.ToggleButtonWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;

import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.phoenix.phoenix_fission.common.data.multiblock.fission.FissionWorkableElectricMultiblockMachine;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

public class FissionStabilitySensorPart extends SensorHatchPartMachine {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            FissionStabilitySensorPart.class, SensorHatchPartMachine.MANAGED_FIELD_HOLDER);

    @Persisted
    @Getter
    @Setter
    private int minPercent = 0;
    @Persisted
    @Getter
    @Setter
    private int maxPercent = 95;
    @Persisted
    @Getter
    @Setter
    private boolean inverted = false;

    public FissionStabilitySensorPart(IMachineBlockEntity holder, int tier) {
        super(holder, tier);
    }



    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    @Override
    public boolean shouldOpenUI(Player player, InteractionHand hand, BlockHitResult hit) {
        return true;
    }

    @Override
    public Widget createUIWidget() {
        WidgetGroup group = new WidgetGroup(0, 0, 200, 160);

        group.addWidget(new LabelWidget(10, 8, "§6Fission Stability Sensor"));

        group.addWidget(new LabelWidget(10, 24, "§7Heat: --   §7Signal: --") {
            @Override
            public void updateScreen() {
                super.updateScreen();
                var controller = getController();
                String text;
                if (controller instanceof FissionWorkableElectricMultiblockMachine fission) {
                    double pct = (fission.getHeat() / FissionWorkableElectricMultiblockMachine.cfg().maxSafeHeat) * 100.0;
                    int sig = getOutputSignal(null);
                    String sigColor = sig > 0 ? "§a" : "§c";

                    // Format cleanly inside standard java sandbox
                    String formattedPct = String.format(java.util.Locale.ROOT, "%.1f", pct);
                    text = "§7Heat: §f" + formattedPct + "%   §7Signal: " + sigColor + sig;
                } else {
                    text = "§7Heat: §8N/A   §7Signal: §80";
                }

                // This completely bypasses LocalizationUtils.format() on the client render layer!
                this.setComponent(net.minecraft.network.chat.Component.literal(text));
            }
        });

        group.addWidget(new Widget(10, 43, 180, 1).setBackground(GuiTextures.BLANK));

        group.addWidget(new LabelWidget(10, 54, "§fMin Heat Percent:"));
        group.addWidget(new IntInputWidget(115, 49, 75, 20,
                () -> minPercent,
                val -> minPercent = Mth.clamp(val, 0, 200)));

        group.addWidget(new LabelWidget(10, 78, "§fMax Heat Percent:"));
        group.addWidget(new IntInputWidget(115, 73, 75, 20,
                () -> maxPercent,
                val -> maxPercent = Mth.clamp(val, 0, 200)));

        group.addWidget(new LabelWidget(10, 102, "§fInvert output:"));
        group.addWidget(new ToggleButtonWidget(
                115, 98, 18, 18,
                GuiTextures.INVERT_REDSTONE_BUTTON,
                this::isInverted,
                this::setInverted)
                .setTooltipText("Invert Signal Output"));

        group.addWidget(new Widget(10, 127, 180, 1).setBackground(GuiTextures.BLANK));

        group.addWidget(new LabelWidget(10, 138, "Emits signal on all block faces."));

        return group;
    }

    // ── Signal logic ──────────────────────────────────────────────────────────

    @Override
    public int getOutputSignal(@Nullable Direction direction) {
        // REMOVED: Face direction check to allow global output on any side
        var controller = getController();
        if (!(controller instanceof FissionWorkableElectricMultiblockMachine fission)) return 0;

        double maxSafe = FissionWorkableElectricMultiblockMachine.cfg().maxSafeHeat;
        if (maxSafe <= 0) return 0;

        double heatPct = (fission.getHeat() / maxSafe) * 100.0;

        int strength = (int) Math.round((heatPct / 200.0) * 15.0);
        strength = Mth.clamp(strength, 0, 15);

        boolean inRange = heatPct >= minPercent && heatPct <= maxPercent;
        boolean emit = inverted != inRange;

        return emit ? strength : 0;
    }
}
