package net.phoenix.phoenix_fission.client.gui;

import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyUIProvider;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.phoenix.phoenix_fission.PhoenixAPI;
import net.phoenix.phoenix_fission.api.block.IFissionBlanketType;
import net.phoenix.phoenix_fission.api.block.IFissionCoolerType;
import net.phoenix.phoenix_fission.api.block.IFissionFuelRodType;
import net.phoenix.phoenix_fission.api.block.IFissionModeratorType;
import net.phoenix.phoenix_fission.api.block.IFissionSensorHatchType;
import net.phoenix.phoenix_fission.api.block.IFissionStabilityHatchType;
import net.phoenix.phoenix_fission.common.data.multiblock.fission.BreederWorkableElectricMultiblockMachine;
import net.phoenix.phoenix_fission.common.data.multiblock.fission.FissionWorkableElectricMultiblockMachine;
import net.phoenix.phoenix_fission.configs.PhoenixFissionConfigs;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

public class FissionReactorFancyUIWidget extends FancyMachineUIWidget {

    private final FissionWorkableElectricMultiblockMachine reactor;
    private final boolean isBreeder;

    // =========================================================================
    // Palette
    // =========================================================================
    private static final int BG_TOP = 0xFF04090F;
    private static final int BG_BOT = 0xFF010508;
    private static final int GRID_COL = 0x07_00E5CC;
    private static final int MIST_HUE = 0x0080B8D0;

    private static final int C_CYAN = 0xFF_00E5CC;
    private static final int C_TEAL = 0xFF_00AAA0;
    private static final int C_DIM = 0xFF_3A5E6A;
    private static final int C_MID = 0xFF_6A9BAA;
    private static final int C_WHITE = 0xFF_DCF0F4;
    private static final int C_GOLD = 0xFF_FFC44D;
    private static final int C_ORANGE = 0xFF_FF8833;
    private static final int C_RED = 0xFF_FF3333;
    private static final int C_GREEN = 0xFF_33FF88;
    private static final int C_BLUE = 0xFF_44AAFF;
    private static final int C_PURPLE = 0xFF_CC66FF;

    private static final int A_FUEL = 0xFF_FF7744;
    private static final int A_MODS = 0xFF_44FFAA;
    private static final int A_COOL = 0xFF_44AAFF;
    private static final int A_BLNK = 0xFF_CC66FF;

    // =========================================================================
    // Panel routing
    // 0 = main HUD, 1 = fuel, 2 = moderators, 3 = coolers, 4 = blankets,
    // 5 = heat & cooling detail (opened by clicking the trend graph)
    // =========================================================================
    private int activePanel = 0;

    // Scroll offset for the main HUD panel (0)
    private int hudScroll = 0;

    // Scroll offset per panel (panels 1-5 map to indices 0-4)
    private final int[] panelScroll = new int[5];

    // Hit regions [x,y,w,h] - rebuilt each draw
    private final int[][] tabBtnBounds = new int[4][4]; // up to 4 tabs
    private final int[] backBtnBounds = new int[4];
    private final int[] trendGraphBounds = new int[4]; // click target on the main HUD that opens panel 5
    private int tabCount = 0;

    // =========================================================================
    // Net heat / coolant history (client-side ring buffer for the trend graph)
    // =========================================================================
    private static final int HISTORY_LEN = 64;
    private static final int HISTORY_SAMPLE_INTERVAL_MS = 250;

    private final double[] netHeatHistory = new double[HISTORY_LEN];
    private final double[] coolingHistory = new double[HISTORY_LEN];
    private int historyHead = 0;
    private int historyFilled = 0;
    private long lastSampleMs = 0L;
    private double netHeatPeak = 1.0;
    private double coolingPeak = 1.0;

    public FissionReactorFancyUIWidget(IFancyUIProvider provider, int width, int height) {
        super(provider, width, height);
        this.reactor = (FissionWorkableElectricMultiblockMachine) provider;
        this.isBreeder = provider instanceof BreederWorkableElectricMultiblockMachine;
        setBackground((IGuiTexture) null);
    }

    @Override
    public void initWidget() {
        super.initWidget();
        applyTheme();
        applySlotTheme(this);
    }

    /** Recursively finds every SlotWidget in the tree and gives it a dark themed background. */
    static void applySlotTheme(WidgetGroup group) {
        for (Widget w : group.widgets) {
            if (w instanceof SlotWidget slot) {
                slot.setBackground(new ColorBorderTexture(1, 0xBB_00E5CC).setColor(0xFF_051520));
            } else if (w instanceof WidgetGroup wg) {
                applySlotTheme(wg);
            }
        }
    }

    /** Replace GT's default white tab textures with our dark-cyan theme. */
    static void applyTheme(com.gregtechceu.gtceu.api.gui.fancy.TabsWidget tabs) {
        // Normal tab: fully opaque dark fill, solid cyan border
        tabs.setTabTexture(new ColorBorderTexture(1, 0xBB_00E5CC)
                .setColor(0xFF_010810));
        // Hover: slightly lighter fill, brighter border
        tabs.setTabHoverTexture(new ColorBorderTexture(1, 0xDD_00E5CC)
                .setColor(0xFF_061825));
        // Pressed/selected: teal fill, solid
        tabs.setTabPressedTexture(new ColorBorderTexture(1, 0xFF_00AAA0)
                .setColor(0xFF_002830));
        // Scroll arrows (only visible when there are many tabs)
        tabs.setLeftButtonTexture(new ColorBorderTexture(1, 0x88_00E5CC)
                .setColor(0xFF_010810));
        tabs.setLeftButtonHoverTexture(new ColorBorderTexture(1, 0xBB_00E5CC)
                .setColor(0xFF_061825));
        tabs.setRightButtonTexture(new ColorBorderTexture(1, 0x88_00E5CC)
                .setColor(0xFF_010810));
        tabs.setRightButtonHoverTexture(new ColorBorderTexture(1, 0xBB_00E5CC)
                .setColor(0xFF_061825));
    }

    private void applyTheme() {
        applyTheme(sideTabsWidget);
        if (sideTabsWidget != null) {
            sideTabsWidget.addSelfPosition(-3, 0);
        }
    }

    @Override
    public boolean mouseWheelMove(double mouseX, double mouseY, double amount) {
        if (currentPage == currentHomePage) {
            if (activePanel > 0) {
                panelScroll[activePanel - 1] = Math.max(0,
                        panelScroll[activePanel - 1] - (int) (amount * 10));
                return true;
            } else if (activePanel == 0) {
                // Caps the scrolling at 80px downwards; increase this value if more space is needed
                hudScroll = Math.max(0, Math.min(80, hudScroll - (int) (amount * 12)));
                return true;
            }
        }
        return super.mouseWheelMove(mouseX, mouseY, amount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && currentPage == currentHomePage) {
            // Back button (in sub-screens)
            if (activePanel > 0 && inBounds(backBtnBounds, mouseX, mouseY)) {
                activePanel = 0;
                return true;
            }
            // Main HUD: trend graph opens the heat & cooling detail panel
            if (activePanel == 0 && inBounds(trendGraphBounds, mouseX, mouseY)) {
                activePanel = 5;
                panelScroll[4] = 0;
                return true;
            }
            // Tab buttons (on main screen)
            if (activePanel == 0) {
                for (int i = 0; i < tabCount; i++) {
                    if (inBounds(tabBtnBounds[i], mouseX, mouseY)) {
                        activePanel = i + 1;
                        panelScroll[i] = 0;
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean inBounds(int[] b, double mx, double my) {
        return b[2] > 0 && mx >= b[0] && mx <= b[0] + b[2] && my >= b[1] && my <= b[1] + b[3];
    }

    // =========================================================================
    // Root draw
    // =========================================================================
    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@Nonnull GuiGraphics g, int mouseX, int mouseY, float dt) {
        int x = getPosition().x, y = getPosition().y;
        int w = getSize().width, h = getSize().height;

        DrawerHelper.drawGradientRect(g, x, y, w, h, BG_TOP, BG_BOT, false);
        drawGrid(g, x, y, w, h);
        drawScanlines(g, x, y, w, h);
        drawMist(g, x, y, w, h);

        super.drawInBackground(g, mouseX, mouseY, dt);

        // Cover GT's default white title bar with our dark themed one.
        // We measure the exact height by reading where pageContainer actually starts.
        if (pageContainer != null) {
            int contentY = pageContainer.getPosition().y;
            int titleH = contentY - y;
            if (titleH > 4) {
                g.fill(x, y, x + w, contentY, BG_TOP);
                // Left accent bar
                g.fill(x + 3, y + 3, x + 6, contentY - 3, C_TEAL);
                // Machine name
                Font titleFont = Minecraft.getInstance().font;
                String uiTitle = isBreeder ? "BREEDER FISSION REACTOR" : "FISSION REACTOR";
                int titleY = y + (titleH - titleFont.lineHeight) / 2;
                g.drawString(titleFont, uiTitle, x + 10, titleY, C_CYAN, false);
                // Separator line under title
                g.fill(x, contentY - 1, x + w, contentY, 0x44_00E5CC);
            }
        }

        sampleHistoryIfDue();
        drawHudIfHome(g);

        DrawerHelper.drawBorder(g, x, y, w, h, borderColor(), 1);
        DrawerHelper.drawBorder(g, x + 2, y + 2, w - 4, h - 4, 0x22_00E5CC, 1);
    }

    @OnlyIn(Dist.CLIENT)
    private void drawHudIfHome(GuiGraphics g) {
        if (currentPage != currentHomePage) return;
        if (pageContainer == null || pageContainer.widgets.isEmpty()) return;

        var page = pageContainer.widgets.get(0);
        int px = page.getPosition().x, py = page.getPosition().y;
        int pw = page.getSize().width, ph = page.getSize().height;

        g.enableScissor(px - 2, py - 2, px + pw + 2, py + ph + 2);
        int cx = px + 6, cy = py + 5, cw = pw - 12, ch = ph - 8;

        // Meltdown takes over the entire page regardless of which panel is open.
        if (reactor.meltdownTimerTicks > 0 || (reactor.meltdownTimerMax > 0 && reactor.isScramActive())) {
            drawMeltdownScreen(g, Minecraft.getInstance().font, px, py, pw, ph, reactor);
        } else {
            switch (activePanel) {
                case 1 -> drawFuelPanel(g, cx, cy, cw, ch);
                case 2 -> drawModsPanel(g, cx, cy, cw, ch);
                case 3 -> drawCoolPanel(g, cx, cy, cw, ch);
                case 4 -> drawBlnkPanel(g, cx, cy, cw, ch);
                case 5 -> drawHeatDetailPanel(g, cx, cy, cw, ch);
                default -> drawReactorHUD(g, cx, cy, cw, ch);
            }
        }
        g.disableScissor();
    }

    // =========================================================================
    // History sampling
    // =========================================================================
    @OnlyIn(Dist.CLIENT)
    private void sampleHistoryIfDue() {
        if (!reactor.isFormed()) return;

        long now = System.currentTimeMillis();
        if (now - lastSampleMs < HISTORY_SAMPLE_INTERVAL_MS) return;
        lastSampleMs = now;

        double netHeat = reactor.lastHeatGainedPerTick - reactor.lastHeatRemovedPerTick;
        double cooling = reactor.lastProvidedCooling;

        netHeatHistory[historyHead] = netHeat;
        coolingHistory[historyHead] = cooling;

        historyHead = (historyHead + 1) % HISTORY_LEN;
        historyFilled = Math.min(HISTORY_LEN, historyFilled + 1);

        // Track rolling peaks (decay slowly so the graph rescales instead of
        // permanently flattening after a single spike)
        double sampleMag = Math.max(Math.abs(netHeat), 1.0);
        netHeatPeak = Math.max(sampleMag, netHeatPeak * 0.995);
        coolingPeak = Math.max(Math.max(cooling, 1.0), coolingPeak * 0.995);
    }

    // =========================================================================
    // Main HUD
    // =========================================================================
    @OnlyIn(Dist.CLIENT)
    private void drawReactorHUD(GuiGraphics g, int x, int y, int W, int H) {
        // Apply the scrolling offset to the entire HUD layout
        y -= hudScroll;

        Font font = Minecraft.getInstance().font;
        boolean formed = reactor.isFormed();
        boolean working = formed && reactor.getRecipeLogic().isWorking();

        y = drawHeader(g, font, x, y, W, formed, working);
        if (!formed) {
            g.drawString(font, "Structure not complete", x + 4, y + 6, C_DIM, false);
            return;
        }
        y = drawHeatSection(g, font, x, y, W);
        y = drawTrendGraph(g, font, x, y, W);
        if (PhoenixFissionConfigs.INSTANCE.fission.enableDirectEUOutput) {
            y = drawStatsSection(g, font, x, y, W);
        } else {
            y = drawStatsNoEU(g, font, x, y, W);
        }
        y = drawTabBar(g, font, x, y, W);
        y = drawStabilityBar(g, font, x, y, W);
        drawStatusFooter(g, font, x, y + 2, W);
    }

    // =========================================================================
    // Header
    // =========================================================================
    @OnlyIn(Dist.CLIENT)
    private int drawHeader(GuiGraphics g, Font font, int x, int y, int W,
                           boolean formed, boolean working) {
        String title = isBreeder ? "BREEDER REACTOR" : "FISSION REACTOR";
        boolean scrammed = formed && reactor.isScramActive();
        boolean isCurrentlyRunning = formed && !scrammed && reactor.isRunningForHud();

        if (System.currentTimeMillis() % 1000 < 50) { // throttle so it's not spamming every frame
            System.out.println("[FISSION-UI][" + reactor.getPos() + "] formed=" + formed + " scrammed=" + scrammed +
                    " runningForHud(client)=" + reactor.isRunningForHud() + " isCurrentlyRunning=" +
                    isCurrentlyRunning);
        }

        String badge;
        int badgeColor;
        if (!formed) {
            badge = "OFFLINE";
            badgeColor = C_DIM;
        } else if (scrammed) {
            badge = "SCRAMMED";
            badgeColor = C_RED;
        } else if (isCurrentlyRunning) {
            badge = "RUNNING";
            badgeColor = C_GREEN;
        } // Changed "ACTIVE" to "RUNNING"
        else {
            badge = "STANDBY";
            badgeColor = C_GOLD;
        }

        // Draw badge right-aligned first so we know how much title space is left
        int bw = font.width(badge) + 8, bh = 10;
        int bx = x + W - bw, by = y - 1;
        g.fill(bx, by, bx + bw, by + bh, 0x44_000000);
        DrawerHelper.drawBorder(g, bx, by, bw, bh, (0x88 << 24) | (badgeColor & 0xFFFFFF), 1);
        if (scrammed) {
            drawHazardStripe(g, bx, by + bh - 2, bw, 2);
        }
        g.drawString(font, badge, bx + 4, by + 1, badgeColor, false);

        // Title with dot, truncated to not collide with badge
        int dotColor = !formed ? C_DIM :
                scrammed ? C_RED : isCurrentlyRunning ? pulsingColor(C_GREEN, 0xFF_AAFFCC, 0.006) : C_GOLD;
        g.fill(x, y + 2, x + 5, y + 7, dotColor);
        int titleMaxW = bx - x - 12;
        g.drawString(font, truncate(font, title, titleMaxW), x + 9, y, C_CYAN, false);

        y += 12;
        g.fill(x, y, x + W, y + 1, C_TEAL);
        g.fill(x, y + 1, x + W, y + 2, 0x33_00E5CC);
        return y + 5;
    }

    /** Small red/black diagonal-ish hazard underline used under the SCRAMMED badge. */
    @OnlyIn(Dist.CLIENT)
    private static void drawHazardStripe(GuiGraphics g, int x, int y, int w, int h) {
        int stripeW = 3;
        boolean red = true;
        for (int sx = 0; sx < w; sx += stripeW) {
            int segW = Math.min(stripeW, w - sx);
            g.fill(x + sx, y, x + sx + segW, y + h, red ? 0xFF_FF3333 : 0xFF_1A1A1A);
            red = !red;
        }
    }

    // =========================================================================
    // Heat section (bar + scale on separate lines, no text inside bar)
    // =========================================================================
    @OnlyIn(Dist.CLIENT)
    private int drawHeatSection(GuiGraphics g, Font font, int x, int y, int W) {
        double maxHeat = reactor.getMaxSafeHeatHU();
        double heat = reactor.getHeat();
        double pct = Math.min(1.0, heat / maxHeat);
        double netDelta = reactor.lastHeatGainedPerTick - reactor.lastHeatRemovedPerTick;

        // Section label + delta rate, right-aligned
        String deltaStr;
        int deltaColor;
        if (Math.abs(netDelta) < 0.05) {
            deltaStr = "~ stable";
            deltaColor = C_MID;
        } else if (netDelta > 0) {
            deltaStr = String.format("+%.1f/t", netDelta);
            deltaColor = netDelta > 5 ? C_RED : C_ORANGE;
        } else {
            deltaStr = String.format("%.1f/t", netDelta);
            deltaColor = C_GREEN;
        }
        g.drawString(font, "HEAT", x, y, C_MID, false);
        g.drawString(font, deltaStr, x + W - font.width(deltaStr), y, deltaColor, false);
        y += 10;

        // Bar - no text drawn inside it
        int barH = 8, fillW = (int) (pct * W), half = W / 2;
        g.fill(x, y, x + W, y + barH, 0x22_FFFFFF);
        g.fill(x, y, x + 1, y + barH, 0x44_FFFFFF);
        if (fillW > 0) {
            DrawerHelper.drawGradientRect(g, x, y, Math.min(fillW, half), barH, C_GREEN, C_GOLD, true);
            if (fillW > half)
                DrawerHelper.drawGradientRect(g, x + half, y, fillW - half, barH, C_GOLD, C_RED, true);
        }
        // Shimmer when hot
        if (pct > 0.70 && fillW > 0) {
            long t = System.currentTimeMillis();
            int sw = 18;
            int sOff = (int) ((t / 18) % (fillW + sw));
            int sx1 = Math.max(x + sOff - sw, x), sx2 = Math.min(x + sOff, x + fillW);
            if (sx2 > sx1) {
                int mid = (sx1 + sx2) / 2;
                DrawerHelper.drawGradientRect(g, sx1, y, mid - sx1, barH, 0x00_FFFFFF, 0x33_FFFFFF, true);
                DrawerHelper.drawGradientRect(g, mid, y, sx2 - mid, barH, 0x33_FFFFFF, 0x00_FFFFFF, true);
            }
        }
        // 85% danger marker
        int safeX = x + (int) (0.85 * W);
        g.fill(safeX, y - 1, safeX + 1, y + barH + 1, 0xAA_FFFFFF);
        y += barH + 2;

        // Scale row: just the "safe" danger marker label — no max-HU number,
        // the absolute value is already shown in the percentage row below.
        g.drawString(font, "safe", safeX - font.width("safe") / 2, y, 0x55_FFFFFF, false);
        y += 10;

        // Percentage (left) + absolute heat value (right), anchored to opposite
        // edges instead of one free-floating centered string. A centered string
        // built from heat's raw, unclamped magnitude could grow wide enough
        // during a runaway to visually fold back on itself; anchoring each
        // half to its own edge guarantees they can never collide regardless
        // of how large heat gets.
        String pctStr = String.format("%.1f%%", pct * 100);
        String absStr = String.format("%.0f HU", Math.min(heat, 9_999_999));
        int pctColor = pct > 0.85 ? C_RED : pct > 0.5 ? C_ORANGE : C_GREEN;
        g.drawString(font, pctStr, x, y, pctColor, false);
        g.drawString(font, absStr, x + W - font.width(absStr), y, pctColor, false);

        y += 10;
        rule(g, x, W, y);
        return y + 4;
    }

    // =========================================================================
    // Net heat / coolant trend graph
    // Two overlaid sparklines sharing one plot area:
    // - net heat (gain - removal), colored by sign (red=heating, green=cooling)
    // - coolant throughput (HU/t actually being removed), in blue
    // Both series are independently auto-scaled against a slow-decaying peak
    // so the graph stays readable whether the reactor is idling or surging.
    // The whole strip is clickable - it opens the heat & cooling detail
    // panel, which has the full-size graph plus the per-tick breakdown that
    // used to live cramped into the stats box below this.
    // =========================================================================
    @OnlyIn(Dist.CLIENT)
    private int drawTrendGraph(GuiGraphics g, Font font, int x, int y, int W) {
        int regionStart = y;

        g.drawString(font, "NET HEAT / COOLANT", x, y, C_MID, false);

        // "tap for detail" hint, right-aligned
        String hint = "details >";
        g.drawString(font, hint, x + W - font.width(hint), y, C_TEAL, false);
        y += 10;

        int plotH = 22;
        int plotY = y;
        g.fill(x, plotY, x + W, plotY + plotH, 0x18_FFFFFF);
        DrawerHelper.drawBorder(g, x, plotY, W, plotH, 0x33_00E5CC, 1);

        int midY = plotY + plotH / 2;
        g.fill(x, midY, x + W, midY + 1, 0x33_FFFFFF);

        drawTrendLines(g, x, plotY, W, plotH);

        y = plotY + plotH + 3;
        rule(g, x, W, y);

        trendGraphBounds[0] = x;
        trendGraphBounds[1] = regionStart;
        trendGraphBounds[2] = W;
        trendGraphBounds[3] = y - regionStart;

        return y + 4;
    }

    /** Renders just the sparkline content into the given plot rect; shared by the mini and detail views. */
    @OnlyIn(Dist.CLIENT)
    private void drawTrendLines(GuiGraphics g, int x, int plotY, int W, int plotH) {
        if (historyFilled < 2) {
            Font font = Minecraft.getInstance().font;
            g.drawString(font, "Collecting data...", x + 4, plotY + plotH / 2 - 4, C_DIM, false);
            return;
        }

        int n = historyFilled;
        int oldest = (historyHead - n + HISTORY_LEN) % HISTORY_LEN;
        int midY = plotY + plotH / 2;

        float stepX = W / (float) (HISTORY_LEN - 1);

        Integer prevHx = null, prevHy = null;
        Integer prevCx = null, prevCy = null;

        for (int i = 0; i < n; i++) {
            int idx = (oldest + i) % HISTORY_LEN;
            // Align so the most recent sample sits at the right edge
            int slot = HISTORY_LEN - n + i;
            int px = x + (int) (slot * stepX);

            double netHeat = netHeatHistory[idx];
            double cooling = coolingHistory[idx];

            // net heat: signed, plotted around the midline
            double heatNorm = clamp(netHeat / netHeatPeak, -1.0, 1.0);
            int hy = midY - (int) (heatNorm * (plotH / 2f - 2));

            // cooling: unsigned, plotted from the bottom upward
            double coolNorm = clamp(cooling / coolingPeak, 0.0, 1.0);
            int cy = plotY + plotH - 2 - (int) (coolNorm * (plotH - 4));

            if (prevHx != null) {
                drawLine(g, prevHx, prevHy, px, hy, netHeat >= 0 ? C_ORANGE : C_GREEN);
            }
            if (prevCx != null) {
                drawLine(g, prevCx, prevCy, px, cy, C_BLUE);
            }
            prevHx = px;
            prevHy = hy;
            prevCx = px;
            prevCy = cy;
        }

        // Highlight the latest sample with small dots
        if (prevHx != null) {
            g.fill(prevHx - 1, prevHy - 1, prevHx + 2, prevHy + 2, 0xFF_FFFFFF);
            g.fill(prevCx - 1, prevCy - 1, prevCx + 2, prevCy + 2, 0xFF_FFFFFF);
        }
    }

    /** Simple Bresenham-ish line draw using 1px-tall fills, fine for short sparkline segments. */
    @OnlyIn(Dist.CLIENT)
    private static void drawLine(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        int dx = x2 - x1, dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) {
            g.fill(x1, y1, x1 + 1, y1 + 1, color);
            return;
        }
        for (int s = 0; s <= steps; s++) {
            int px = x1 + Math.round(dx * (s / (float) steps));
            int py = y1 + Math.round(dy * (s / (float) steps));
            g.fill(px, py, px + 1, py + 1, color);
        }
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // =========================================================================
    // Per-tick breakdown: heat generated | heat removed | cooling power
    // Stacked rows (label left, value right) - this used to be crammed into
    // one 3-column line under the EU box, which ran out of room as soon as
    // any value got a couple digits longer. Lives in the heat detail panel now.
    // =========================================================================
    @OnlyIn(Dist.CLIENT)
    private int drawPerTickBreakdown(GuiGraphics g, Font font, int x, int y, int W) {
        double gained = reactor.lastHeatGainedPerTick;
        double removed = reactor.lastHeatRemovedPerTick;
        int cooling = reactor.lastProvidedCooling;

        g.drawString(font, "PER TICK", x, y, C_MID, false);
        y += 11;

        y = drawStatRow(g, font, x, y, W, "Heat generated", String.format("+%.1f HU/t", gained),
                gained > 0 ? A_FUEL : C_DIM);
        y = drawStatRow(g, font, x, y, W, "Heat removed", String.format("-%.1f HU/t", removed),
                removed > 0 ? C_GREEN : C_DIM);
        y = drawStatRow(g, font, x, y, W, "Max cool @ cap", cooling + " HU/t",
                cooling > 0 ? C_BLUE : C_DIM);

        return y;
    }

    /** One label-left / value-right row, the standard layout for the detail panel. */
    @OnlyIn(Dist.CLIENT)
    private static int drawStatRow(GuiGraphics g, Font font, int x, int y, int W,
                                   String label, String value, int valueColor) {
        g.drawString(font, label, x, y, C_MID, false);
        g.drawString(font, value, x + W - font.width(value), y, valueColor, false);
        return y + 10;
    }

    // =========================================================================
    // EU output section (dedicated focused area)
    // =========================================================================
    @OnlyIn(Dist.CLIENT)
    private int drawStatsSection(GuiGraphics g, Font font, int x, int y, int W) {
        boolean hasCoolant = reactor.lastHasCoolant;

        // Fixed: Routed properly through the fuel manager instance
        int boostPct = reactor.getFuelManager().getModeratorEUBoostClamped();
        int discPct = reactor.getFuelManager().getModeratorFuelDiscountClamped();

        // Bordered EU box
        int boxH = 28;
        g.fill(x, y, x + W, y + boxH, 0x22_00AABB);
        DrawerHelper.drawBorder(g, x, y, W, boxH, 0x55_00E5CC, 1);

        // Row 1 inside box: EU/t (large, left) | parallels (right)
        // Fixed: Replaced getVoltageFormattedOutput with GregTech's standard formatting utility
        String euStr = FormattingUtil.formatNumbers(reactor.lastGeneratedEUt) + " EU/t";
        String parStr = "x" + reactor.lastParallels + " par";
        int parW = font.width(parStr);
        int euMaxW = W - parW - 4 /* left pad */ - 6 /* gap before parStr */ - 4 /* right pad */;
        g.drawString(font, truncate(font, euStr, euMaxW), x + 4, y + 3, C_WHITE, false);
        g.drawString(font, parStr, x + W - parW - 4, y + 3, C_MID, false);

        // Row 2 inside box: EU boost | fuel discount
        String boostStr = "+" + boostPct + "% EU";
        String discStr = "-" + discPct + "% fuel";
        g.drawString(font, boostStr, x + 4, y + 15, boostPct > 0 ? 0xFF_AAFFCC : C_DIM, false);
        g.drawString(font, discStr, x + W - font.width(discStr) - 4, y + 15, discPct > 0 ? C_GOLD : C_DIM, false);
        y += boxH + 3;

        // Coolant status line (outside box, full width)
        if (reactor.isOverCooled) {
            g.fill(x, y + 1, x + 5, y + 6, pulsingColor(0xFF_44AAFF, 0xFF_AADDFF, 0.014));
            g.drawString(font, "  OVER-COOLED  (90% coolant refund, no EU)", x, y, 0xFF_44AAFF, false);
        } else {
            String coolantStr = hasCoolant ? "  Coolant OK" : "  NO COOLANT";
            g.fill(x, y + 1, x + 5, y + 6, hasCoolant ? C_GREEN : pulsingColor(C_RED, 0xFF_FF9966, 0.012));
            g.drawString(font, coolantStr, x, y, hasCoolant ? 0xFF_AAFFCC : C_RED, false);
        }
        y += 11;
        rule(g, x, W, y);
        return y + 4;
    }

    // =========================================================================
    // Stats without EU (shown when enableDirectEUOutput = false)
    // =========================================================================
    @OnlyIn(Dist.CLIENT)
    private int drawStatsNoEU(GuiGraphics g, Font font, int x, int y, int W) {
        boolean hasCoolant = reactor.lastHasCoolant;
        // Routed through the fuel manager instance exactly like your stats section
        int boostPct = reactor.getFuelManager().getModeratorEUBoostClamped();
        int discPct = reactor.getFuelManager().getModeratorFuelDiscountClamped();

        String parStr = "x" + reactor.lastParallels + " parallels";
        String boostStr = "+" + boostPct + "% heat";
        String discStr = "-" + discPct + "% fuel";

        g.drawString(font, parStr, x, y, C_MID, false);
        y += 10;

        g.drawString(font, boostStr, x + 4, y, boostPct > 0 ? 0xFF_AAFFCC : C_DIM, false);
        g.drawString(font, discStr, x + W / 2 - font.width(discStr) / 2, y, discPct > 0 ? C_GOLD : C_DIM, false);
        y += 10;

        if (reactor.isOverCooled) {
            g.fill(x, y + 1, x + 5, y + 6, pulsingColor(0xFF_44AAFF, 0xFF_AADDFF, 0.014));
            g.drawString(font, "  OVER-COOLED  (90% coolant refund, no EU)", x, y, 0xFF_44AAFF, false);
        } else {
            String coolantStr = hasCoolant ? "  Coolant OK" : "  NO COOLANT";
            g.fill(x, y + 1, x + 5, y + 6, hasCoolant ? C_GREEN : pulsingColor(C_RED, 0xFF_FF9966, 0.012));
            g.drawString(font, coolantStr, x, y, hasCoolant ? 0xFF_AAFFCC : C_RED, false);
        }
        y += 11;
        rule(g, x, W, y);
        return y + 4;
    }

    // =========================================================================
    // Component tab bar (replaces old manifest section)
    // =========================================================================
    @OnlyIn(Dist.CLIENT)
    private int drawTabBar(GuiGraphics g, Font font, int x, int y, int W) {
        List<IFissionFuelRodType> fuels = resolveTypes(reactor.getPersistedFuelRodIDs(),
                PhoenixAPI.FISSION_FUEL_RODS.keySet(), IFissionFuelRodType::getName);
        List<IFissionModeratorType> mods = resolveTypes(reactor.getPersistedModeratorIDs(),
                PhoenixAPI.FISSION_MODERATORS.keySet(), IFissionModeratorType::getName);
        List<IFissionCoolerType> coolers = resolveTypes(reactor.getPersistedCoolerIDs(),
                PhoenixAPI.FISSION_COOLERS.keySet(), IFissionCoolerType::getName);
        List<IFissionBlanketType> blankets = resolveTypes(reactor.getPersistedBlanketIDs(),
                PhoenixAPI.FISSION_BLANKETS.keySet(), IFissionBlanketType::getName);

        record Tab(String label, int accentColor, int count, boolean show) {}
        List<Tab> tabs = new ArrayList<>();
        tabs.add(new Tab("FUEL", A_FUEL, fuels.size(), true));
        tabs.add(new Tab("MODS", A_MODS, mods.size(), true));
        tabs.add(new Tab("COOL", A_COOL, coolers.size(), true));
        tabs.add(new Tab("BLNK", A_BLNK, blankets.size(), isBreeder));

        // Filter to shown tabs and compute widths
        List<Tab> shown = tabs.stream().filter(Tab::show).toList();
        tabCount = shown.size();

        int gap = 4;
        int btnH = 14;
        int totalGap = gap * (shown.size() - 1);
        int btnW = shown.isEmpty() ? W : (W - totalGap) / shown.size();

        g.drawString(font, "COMPONENTS", x, y, C_MID, false);
        y += 11;

        for (int i = 0; i < shown.size(); i++) {
            Tab tab = shown.get(i);
            int bx = x + i * (btnW + gap);

            boolean empty = tab.count() == 0;
            int accent = empty ? C_DIM : tab.accentColor();
            int bgCol = empty ? 0x22_000000 : 0x33_000000;

            g.fill(bx, y, bx + btnW, y + btnH, bgCol);
            // color stripe on top
            g.fill(bx, y, bx + btnW, y + 2, empty ? 0x33_FFFFFF : (0xBB_000000 | (accent & 0xFFFFFF)));
            DrawerHelper.drawBorder(g, bx, y, btnW, btnH, (0x66 << 24) | (accent & 0xFFFFFF), 1);

            String btnText = tab.label() + (empty ? "" : " " + tab.count());
            int tw = font.width(btnText);
            g.drawString(font, btnText, bx + (btnW - tw) / 2, y + 3, empty ? C_DIM : accent, false);

            tabBtnBounds[i][0] = bx;
            tabBtnBounds[i][1] = y;
            tabBtnBounds[i][2] = btnW;
            tabBtnBounds[i][3] = btnH;
        }
        // zero out unused slots
        for (int i = shown.size(); i < 4; i++) Arrays.fill(tabBtnBounds[i], 0);

        y += btnH + 5;
        rule(g, x, W, y);
        return y + 4;
    }

    // =========================================================================
    // Stability / sensor hatch sub-bar
    // Compact two-row indicator: stability hatches on top, sensor hatches below.
    // Each row shows installed count + total stability score as a small bar.
    // =========================================================================
    @OnlyIn(Dist.CLIENT)
    private int drawStabilityBar(GuiGraphics g, Font font, int x, int y, int W) {
        List<IFissionStabilityHatchType> stabHatches = resolveTypes(
                reactor.getPersistedStabilityIDs(), PhoenixAPI.FISSION_STABILITY_HATCHES.keySet(),
                IFissionStabilityHatchType::getName);
        List<IFissionSensorHatchType> sensorHatches = resolveTypes(
                reactor.getPersistedSensorIDs(), PhoenixAPI.FISSION_SENSOR_HATCHES.keySet(),
                IFissionSensorHatchType::getName);

        int totalStability = stabHatches.stream().mapToInt(IFissionStabilityHatchType::getStability).sum();
        int maxStability = 100; // treat 100 as "full"

        // Row label + bar for stability hatches
        String stabLabel = "STAB";
        int stabCount = stabHatches.size();
        g.drawString(font, stabLabel, x, y, stabCount > 0 ? C_GREEN : C_DIM, false);

        int barX = x + font.width(stabLabel) + 4;
        int barW = W - font.width(stabLabel) - 4 - font.width("x00 +000") - 2;
        int barH = 5;
        int barY = y + 1;
        g.fill(barX, barY, barX + barW, barY + barH, 0x22_FFFFFF);
        if (stabCount > 0) {
            double stabFrac = Math.min(1.0, totalStability / (double) maxStability);
            int fillW = (int) (stabFrac * barW);
            if (fillW > 0) {
                int stabCol = stabFrac >= 1.0 ? C_GREEN : stabFrac > 0.5 ? 0xFF_AAFFCC : C_GOLD;
                g.fill(barX, barY, barX + fillW, barY + barH, stabCol);
            }
        }
        DrawerHelper.drawBorder(g, barX, barY, barW, barH, 0x44_00E5CC, 1);

        String stabRight = "x" + stabCount + " +" + totalStability;
        g.drawString(font, stabRight, x + W - font.width(stabRight), y, stabCount > 0 ? C_MID : C_DIM, false);

        y += 8;

        // Row for sensor hatches (just count + tier range, no score bar)
        int sensorCount = sensorHatches.size();
        String sensLabel = "SENS";
        g.drawString(font, sensLabel, x, y, sensorCount > 0 ? C_BLUE : C_DIM, false);

        int sensBarX = x + font.width(sensLabel) + 4;
        int sensBarW = barW;
        g.fill(sensBarX, y + 1, sensBarX + sensBarW, y + 1 + barH, 0x22_FFFFFF);
        if (sensorCount > 0) {
            int maxSens = 4; // assume up to 4 installed is "full"
            double sensFrac = Math.min(1.0, sensorCount / (double) maxSens);
            int sfillW = (int) (sensFrac * sensBarW);
            if (sfillW > 0) g.fill(sensBarX, y + 1, sensBarX + sfillW, y + 1 + barH, C_BLUE);
        }
        DrawerHelper.drawBorder(g, sensBarX, y + 1, sensBarW, barH, 0x44_00E5CC, 1);

        String sensRight = "x" + sensorCount;
        g.drawString(font, sensRight, x + W - font.width(sensRight), y, sensorCount > 0 ? C_MID : C_DIM, false);

        y += 8;
        rule(g, x, W, y);
        return y + 4;
    }

    // =========================================================================
    // Status footer — meltdown progress bar + condition line
    // =========================================================================
    @OnlyIn(Dist.CLIENT)
    private void drawStatusFooter(GuiGraphics g, Font font, int x, int y, int W) {
        double maxHeat = reactor.getMaxSafeHeatHU();
        double heat = reactor.getHeat();
        int timerTicks = reactor.meltdownTimerTicks;
        int timerMax = reactor.meltdownTimerMax;
        boolean scrammed = reactor.isScramActive();

        // Meltdown countdown bar — shown whenever the timer has ever been started
        // (timerMax > 0) even if currently frozen by SCRAM so the player can see
        // the paused state clearly.
        if (timerMax > 0 && timerTicks >= 0) {
            double frac = timerMax > 0 ? timerTicks / (double) timerMax : 1.0;
            int barH = 6;
            int fillW = (int) (frac * W);

            // Bar background
            g.fill(x, y, x + W, y + barH, 0x44_000000);

            // Fill color: pulsing red, dims to orange when SCRAM has paused it
            int fillCol = scrammed ? 0xFF_AA4400   // muted amber — frozen/paused
                    : pulsingColor(C_RED, 0xFF_FF8844, 0.012);
            if (fillW > 0) g.fill(x, y, x + fillW, y + barH, fillCol);
            DrawerHelper.drawBorder(g, x, y, W, barH, scrammed ? 0x88_AA6600 : 0x88_FF3300, 1);

            // Label inside the bar: "MELTDOWN Xs" on the left,
            // "PAUSED" or remaining-% on the right
            String leftStr = scrammed ? "MELTDOWN" : "MELTDOWN  " + (int) Math.ceil(timerTicks / 20.0) + "s";
            String rightStr = scrammed ? "PAUSED - SCRAM" : String.format("%.0f%%", frac * 100);
            int rightColor = scrammed ? 0xFF_FFAA44 : C_RED;

            // Render text above the bar (bar is only 6px — too short for font)
            g.drawString(font, leftStr, x, y - 9, scrammed ? 0xFF_FFAA44 : pulsingColor(C_RED, 0xFF_FF9966, 0.010),
                    false);
            g.drawString(font, rightStr, x + W - font.width(rightStr), y - 9, rightColor, false);

            y += barH + 3;
        }

        // Condition line
        if (timerTicks > 0 && !scrammed) {
            // Already showed the bar above — just a short pulsing reinforcement
            drawWrappedFooterLine(g, font, x, y, W,
                    "!! EVACUATE -- meltdown imminent", pulsingColor(C_RED, 0xFF_FF9966, 0.010));
        } else if (scrammed) {
            g.fill(x, y + 2, x + 5, y + 7, C_RED);
            drawWrappedFooterLine(g, font, x, y, W,
                    "  SCRAMMED -- reset scram hatch to resume", C_RED);
        } else if (heat > maxHeat) {
            drawWrappedFooterLine(g, font, x, y, W,
                    "!! OVERHEAT -- reduce parallels or improve cooling", C_ORANGE);
        } else if (reactor.isOverCooled) {
            drawWrappedFooterLine(g, font, x, y, W,
                    "OVER-COOLED -- reduce cooling or add fuel rods", pulsingColor(0xFF_44AAFF, 0xFF_AADDFF, 0.012));
        } else if (!reactor.lastHasCoolant) {
            drawWrappedFooterLine(g, font, x, y, W,
                    "!! COOLANT DEPLETED -- supply coolant fluid", C_GOLD);
        } else if (isBreeder) {
            List<IFissionBlanketType> blankets = resolveTypes(
                    reactor.getPersistedBlanketIDs(), PhoenixAPI.FISSION_BLANKETS.keySet(),
                    IFissionBlanketType::getName);
            IFissionBlanketType pb = blankets.stream()
                    .max(Comparator.comparingInt(IFissionBlanketType::getTier)).orElse(null);
            if (pb == null) {
                g.drawString(font, "  No blanket installed", x, y, C_DIM, false);
            } else {
                g.fill(x, y + 2, x + 5, y + 7, C_PURPLE);
                drawWrappedFooterLine(g, font, x, y, W, "  Breeding: " + pb.getName(), C_PURPLE);
            }
        } else {
            g.fill(x, y + 2, x + 5, y + 7, C_GREEN);
            g.drawString(font, "  Nominal", x, y, C_GREEN, false);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void drawWrappedFooterLine(GuiGraphics g, Font font, int x, int y, int W,
                                              String text, int color) {
        List<String> lines = wrapText(font, text, W);
        for (String line : lines) {
            g.drawString(font, line, x, y, color, false);
            y += 10;
        }
    }

    // =========================================================================
    // Sub-screen: FUEL RODS (variable-height card per unique type)
    // =========================================================================
    @OnlyIn(Dist.CLIENT)
    private void drawFuelPanel(GuiGraphics g, int x, int y, int W, int H) {
        Font font = Minecraft.getInstance().font;
        List<IFissionFuelRodType> fuels = resolveTypes(
                reactor.getPersistedFuelRodIDs(), PhoenixAPI.FISSION_FUEL_RODS.keySet(), IFissionFuelRodType::getName);

        y = drawSubHeader(g, font, x, y, W, "FUEL RODS", A_FUEL);

        if (fuels.isEmpty()) {
            g.drawString(font, "No fuel rods installed.", x, y, C_DIM, false);
            return;
        }

        LinkedHashMap<IFissionFuelRodType, Long> counts = groupBy(fuels);
        int textW = W - 10;

        record FuelCard(IFissionFuelRodType type, long count, List<String> cycleLines) {}
        List<FuelCard> cards = new ArrayList<>();
        int totalH = 0;
        for (Map.Entry<IFissionFuelRodType, Long> e : counts.entrySet()) {
            String cycle = prettyName(e.getKey().getFuelKey()) + " ->" + prettyName(e.getKey().getOutputKey());
            List<String> lines = wrapText(font, cycle, textW);
            cards.add(new FuelCard(e.getKey(), e.getValue(), lines));
            // name + heat row + dur/amt row + cycle lines + separator
            totalH += 33 + lines.size() * 9;
        }

        int contentStart = y;
        int maxScroll = Math.max(0, totalH - (H - 4));
        panelScroll[0] = Math.min(panelScroll[0], maxScroll);
        int scroll = panelScroll[0];

        g.enableScissor(x, y, x + W - 5, y + H - 4);
        int iy = y - scroll;
        for (FuelCard card : cards) {
            IFissionFuelRodType f = card.type();
            int cardH = 33 + card.cycleLines().size() * 9;
            if (iy + cardH > contentStart && iy < contentStart + H - 4) {
                g.fill(x, iy, x + 2, iy + cardH - 2, f.getTintColor() | 0xFF000000);

                // Line 1: name | T{tier} x{count}
                String right1 = "T" + f.getTier() + "  x" + card.count();
                int r1w = font.width(right1);
                g.drawString(font, truncate(font, f.getName(), W - r1w - 14), x + 5, iy + 2, C_WHITE, false);
                g.drawString(font, right1, x + W - r1w - 6, iy + 2, C_GOLD, false);

                // Line 2: heat/rod (left) | total HU/t for this type (right)
                long cnt = card.count();
                String heatStr = f.getBaseHeatProduction() + " HU/rod";
                String totalStr = "= " + (f.getBaseHeatProduction() * cnt) + " HU/t";
                g.drawString(font, heatStr, x + 5, iy + 12, A_FUEL, false);
                g.drawString(font, totalStr, x + W - font.width(totalStr) - 6, iy + 12, 0xFF_FFBB66, false);

                // Line 3: cycle duration (left) | amount per cycle (right)
                double durSec = f.getDurationTicks() / 20.0;
                String durStr = String.format(java.util.Locale.ROOT, "%.0fs cycle", durSec);
                String amtStr = "x" + f.getAmountPerCycle() + " per cycle";
                g.drawString(font, durStr, x + 5, iy + 21, C_MID, false);
                g.drawString(font, amtStr, x + W - font.width(amtStr) - 6, iy + 21, C_DIM, false);

                // Wrapped cycle lines (→ input to output)
                int ly = iy + 30;
                for (String line : card.cycleLines()) {
                    g.drawString(font, line, x + 5, ly, C_DIM, false);
                    ly += 9;
                }
                g.fill(x, iy + cardH - 2, x + W - 6, iy + cardH - 1, 0x22_FFFFFF);
            }
            iy += cardH;
        }
        g.disableScissor();
        drawScrollbar(g, x + W - 4, contentStart, 3, H - 4, scroll, totalH);
    }

    // =========================================================================
    // Sub-screen: MODERATORS (3-line card per unique type)
    // =========================================================================
    @OnlyIn(Dist.CLIENT)
    private void drawModsPanel(GuiGraphics g, int x, int y, int W, int H) {
        Font font = Minecraft.getInstance().font;
        List<IFissionModeratorType> mods = resolveTypes(
                reactor.getPersistedModeratorIDs(), PhoenixAPI.FISSION_MODERATORS.keySet(),
                IFissionModeratorType::getName);

        y = drawSubHeader(g, font, x, y, W, "MODERATORS", A_MODS);

        if (mods.isEmpty()) {
            g.drawString(font, "No moderators installed.", x, y, C_DIM, false);
            return;
        }

        LinkedHashMap<IFissionModeratorType, Long> counts = groupBy(mods);
        int CARD = 32, contentStart = y;
        int contentH = counts.size() * CARD;
        int maxScroll = Math.max(0, contentH - (H - 4));
        panelScroll[1] = Math.min(panelScroll[1], maxScroll);
        int scroll = panelScroll[1];

        g.enableScissor(x, y, x + W - 5, y + H - 4);
        int iy = y - scroll;
        for (Map.Entry<IFissionModeratorType, Long> e : counts.entrySet()) {
            IFissionModeratorType m = e.getKey();
            if (iy + CARD > contentStart && iy < contentStart + H - 4) {
                g.fill(x, iy, x + 2, iy + CARD - 2, m.getTintColor() | 0xFF000000);

                // Line 1: name | T{tier} x{count}
                String right1 = "T" + m.getTier() + "  x" + e.getValue();
                int right1W = font.width(right1);
                g.drawString(font, truncate(font, m.getName(), W - right1W - 14), x + 5, iy + 2, C_WHITE, false);
                g.drawString(font, right1, x + W - right1W - 6, iy + 2, C_GOLD, false);

                // Line 2: EU boost (left) | fuel discount (right)
                int eu = m.getEUBoost();
                int disc = m.getFuelDiscount();
                String euStr = "+" + eu + "% EU";
                String discStr = "-" + disc + "% fuel";
                g.drawString(font, euStr, x + 5, iy + 12, eu > 0 ? 0xFF_AAFFCC : C_DIM, false);
                g.drawString(font, discStr, x + W - font.width(discStr) - 6, iy + 12, disc > 0 ? C_GOLD : C_DIM, false);

                // Line 3: parallel bonus (left) | heat multiplier (right)
                int parB = m.getParallelBonus();
                double heatMult = m.getHeatMultiplier();
                String parStr = parB > 0 ? "+" + parB + " parallel" : "no par bonus";
                String heatStr = String.format(java.util.Locale.ROOT, "x%.1f heat", heatMult);
                g.drawString(font, parStr, x + 5, iy + 21, parB > 0 ? A_MODS : C_DIM, false);
                g.drawString(font, heatStr, x + W - font.width(heatStr) - 6, iy + 21, heatMult > 1.0 ? C_ORANGE : C_MID,
                        false);

                g.fill(x, iy + CARD - 2, x + W - 6, iy + CARD - 1, 0x22_FFFFFF);
            }
            iy += CARD;
        }
        g.disableScissor();
        drawScrollbar(g, x + W - 4, contentStart, 3, H - 4, scroll, contentH);
    }

    // =========================================================================
    // Sub-screen: COOLERS (variable-height card, passive vs active layout)
    // =========================================================================
    @OnlyIn(Dist.CLIENT)
    private void drawCoolPanel(GuiGraphics g, int x, int y, int W, int H) {
        Font font = Minecraft.getInstance().font;
        List<IFissionCoolerType> coolers = resolveTypes(
                reactor.getPersistedCoolerIDs(), PhoenixAPI.FISSION_COOLERS.keySet(), IFissionCoolerType::getName);

        y = drawSubHeader(g, font, x, y, W, "COOLERS", A_COOL);

        if (coolers.isEmpty()) {
            g.drawString(font, "No cooler blocks installed.", x, y, C_DIM, false);
            g.drawString(font, "Coolers reduce reactor temperature.", x, y + 11, C_MID, false);
            return;
        }

        LinkedHashMap<IFissionCoolerType, Long> counts = groupBy(coolers);
        int textW = W - 10;
        double currentHeat = reactor.getHeat();

        record CoolCard(IFissionCoolerType type, long count, List<String> extraLines, boolean passive) {}
        List<CoolCard> cards = new ArrayList<>();
        int totalH = 0;
        for (Map.Entry<IFissionCoolerType, Long> e : counts.entrySet()) {
            IFissionCoolerType c = e.getKey();
            boolean passive = c.isPassive();
            List<String> extra;
            if (passive) {
                extra = List.of(); // no fluid lines for passive
            } else {
                String fluid = prettyName(c.getInputCoolantFluidId()) + " ->" + prettyName(c.getOutputCoolantFluidId());
                extra = wrapText(font, fluid, textW);
            }
            cards.add(new CoolCard(c, e.getValue(), extra, passive));
            // 3 content lines (name+2, stats+12, status+21) = base 33; fluid from +30
            totalH += 33 + extra.size() * 9;
        }

        int contentStart = y;
        int maxScroll = Math.max(0, totalH - (H - 4));
        panelScroll[2] = Math.min(panelScroll[2], maxScroll);
        int scroll = panelScroll[2];

        g.enableScissor(x, y, x + W - 5, y + H - 4);
        int iy = y - scroll;
        for (CoolCard card : cards) {
            IFissionCoolerType c = card.type();
            int cardH = 33 + card.extraLines().size() * 9;
            if (iy + cardH > contentStart && iy < contentStart + H - 4) {
                g.fill(x, iy, x + 2, iy + cardH - 2, c.getTintColor() | 0xFF000000);

                if (card.passive()) {
                    // Line 1: name | PASSIVE x{count}
                    String right1 = "PASSIVE  x" + card.count();
                    int r1w = font.width(right1);
                    g.drawString(font, truncate(font, c.getName(), W - r1w - 14), x + 5, iy + 2, C_WHITE, false);
                    g.drawString(font, right1, x + W - r1w - 6, iy + 2, 0xFF_AAFFCC, false);

                    // Line 2: flat HU/t (left) | total HU/t for all of this type (right)
                    double flat = c.getFlatCoolingHUt();
                    String flatStr = String.format(java.util.Locale.ROOT, "%.0f HU/t flat", flat);
                    String totalStr = String.format(java.util.Locale.ROOT, "= %.0f total", flat * card.count());
                    g.drawString(font, flatStr, x + 5, iy + 12, A_COOL, false);
                    g.drawString(font, totalStr, x + W - font.width(totalStr) - 6, iy + 12, C_MID, false);

                    // Line 3: always active note
                    g.drawString(font, "Always active - no coolant needed", x + 5, iy + 21, C_DIM, false);
                } else {
                    // Line 1: name | T{tier} x{count}
                    String right1 = "T" + c.getTier() + "  x" + card.count();
                    int r1w = font.width(right1);
                    g.drawString(font, truncate(font, c.getName(), W - r1w - 14), x + 5, iy + 2, C_WHITE, false);
                    g.drawString(font, right1, x + W - r1w - 6, iy + 2, C_GOLD, false);

                    // Line 2: temp threshold (left) | mB/t (right)
                    String tempStr = c.getCoolerTemperature() + " K threshold";
                    String mbStr = c.getCoolantUsagePerTick() + " mB/t";
                    g.drawString(font, tempStr, x + 5, iy + 12, A_COOL, false);
                    g.drawString(font, mbStr, x + W - font.width(mbStr) - 6, iy + 12, C_MID, false);

                    // Active when reactor heat > threshold indicator
                    boolean activeNow = currentHeat > c.getCoolerTemperature();
                    String activeStr = activeNow ? "Active now" : "Idle (heat < threshold)";
                    g.drawString(font, activeStr, x + 5, iy + 21,
                            activeNow ? C_GREEN : C_DIM, false);

                    // Wrapped fluid lines
                    int ly = iy + 30;
                    for (String line : card.extraLines()) {
                        g.drawString(font, line, x + 5, ly, C_DIM, false);
                        ly += 9;
                    }
                }
                g.fill(x, iy + cardH - 2, x + W - 6, iy + cardH - 1, 0x22_FFFFFF);
            }
            iy += cardH;
        }
        g.disableScissor();
        drawScrollbar(g, x + W - 4, contentStart, 3, H - 4, scroll, totalH);
    }

    // =========================================================================
    // Sub-screen: BLANKETS / BREEDING
    // =========================================================================
    @OnlyIn(Dist.CLIENT)
    private void drawBlnkPanel(GuiGraphics g, int x, int y, int W, int H) {
        Font font = Minecraft.getInstance().font;
        y = drawSubHeader(g, font, x, y, W, "BREEDING DETAILS", C_PURPLE);

        List<IFissionBlanketType> blankets = resolveTypes(
                reactor.getPersistedBlanketIDs(), PhoenixAPI.FISSION_BLANKETS.keySet(), IFissionBlanketType::getName);

        if (blankets.isEmpty()) {
            g.drawString(font, "No blanket blocks detected.", x, y, C_DIM, false);
            y += 11;
            g.drawString(font, "Place blanket blocks inside the reactor", x, y, C_MID, false);
            y += 10;
            g.drawString(font, "casing to begin breeding materials.", x, y, C_MID, false);
            return;
        }

        LinkedHashMap<IFissionBlanketType, Long> blanketCounts = groupBy(blankets);

        // Build full content into a flat list of draw calls via a virtual-scroll approach:
        // Estimate total content height then scissor + offset
        // Simpler: just draw everything scrolled, let scissor clip it
        int contentStart = y;
        // Estimate height per blanket entry
        int estimatedH = blanketCounts.entrySet().stream().mapToInt(e -> {
            int h = 11 + 10 + 10; // name + input + reqTier
            List<IFissionBlanketType.BlanketOutput> outs = e.getKey().getOutputs();
            h += 10 + (outs == null ? 10 : outs.size() * 9);
            h += 8; // divider gap
            return h;
        }).sum();

        int maxScroll = Math.max(0, estimatedH - (H - 10));
        panelScroll[3] = Math.min(panelScroll[3], maxScroll);
        int scroll = panelScroll[3];

        g.enableScissor(x, contentStart, x + W, contentStart + H - 10);
        int iy = contentStart - scroll;

        for (Map.Entry<IFissionBlanketType, Long> entry : blanketCounts.entrySet()) {
            IFissionBlanketType b = entry.getKey();
            long count = entry.getValue();

            int swCol = b.getTintColor() | 0xFF000000;
            g.fill(x + 2, iy, x + 9, iy + 7, swCol);
            g.fill(x + 2, iy, x + 9, iy + 1, 0x55_FFFFFF);

            String bName = prettyName(b.getName());
            String tierStr = "T" + b.getTier() + "  x" + count;
            g.drawString(font, bName, x + 12, iy, C_PURPLE, false);
            g.drawString(font, tierStr, x + W - font.width(tierStr), iy, C_MID, false);
            iy += 11;

            String inputName = prettyName(b.getInputKey());
            int amt = b.getAmountPerCycle(), dur = Math.max(1, b.getDurationTicks());
            String rateStr = dur <= 20 ? "x" + amt + "/tick" :
                    "x" + amt + " / " + String.format(java.util.Locale.ROOT, "%.1fs", dur / 20.0);
            g.drawString(font, "  Input: " + inputName, x, iy, C_MID, false);
            g.drawString(font, rateStr, x + W - font.width(rateStr), iy, C_GOLD, false);
            iy += 10;

            int reqTier = b.getRequiredFuelTier();
            boolean tierOk = hasSufficientFuelTier(reqTier);
            String reqStr = "  Req. fuel: T" + reqTier + "+  " + (tierOk ? "OK" : "MISSING");
            g.drawString(font, reqStr, x, iy, tierOk ? C_GREEN : C_RED, false);
            iy += 10;

            List<IFissionBlanketType.BlanketOutput> outputs = b.getOutputs();
            if (outputs == null || outputs.isEmpty()) {
                g.drawString(font, "  No outputs configured", x, iy, C_DIM, false);
                iy += 10;
            } else {
                int totalW = outputs.stream().mapToInt(o -> o.weight()).sum();
                g.drawString(font, "  Outputs:", x, iy, C_MID, false);
                iy += 10;
                for (IFissionBlanketType.BlanketOutput out : outputs) {
                    String outName = prettyName(out.key());
                    double pct = totalW > 0 ? (out.weight() * 100.0 / totalW) : 0;
                    String instab = instabilityLabel(out.instability());
                    // right-align the % and instability tag so names have room
                    String right = String.format(java.util.Locale.ROOT, "%.0f%%  %s", pct, instab);
                    int rw = font.width(right);
                    g.drawString(font, truncate(font, "    " + outName, W - rw - 8), x, iy,
                            instabilityColor(out.instability()), false);
                    g.drawString(font, right, x + W - rw - 6, iy, instabilityColor(out.instability()), false);
                    iy += 9;
                }
            }

            iy += 2;
            g.fill(x, iy, x + W, iy + 1, 0x33_CC66FF);
            iy += 6;
        }
        g.disableScissor();

        drawScrollbar(g, x + W - 3, contentStart, 3, H - 10, scroll, estimatedH);
    }

    // =========================================================================
    // Sub-screen: HEAT & COOLING DETAIL
    // Opened by clicking the trend graph on the main HUD. Full-size graph
    // plus the per-tick breakdown that used to be crammed under the EU box.
    // =========================================================================
    @OnlyIn(Dist.CLIENT)
    private void drawHeatDetailPanel(GuiGraphics g, int x, int y, int W, int H) {
        Font font = Minecraft.getInstance().font;
        y = drawSubHeader(g, font, x, y, W, "HEAT & COOLING", C_CYAN);

        int contentStart = y;

        // Legend for the graph below
        g.fill(x, y + 1, x + 5, y + 1 + 5, C_ORANGE);
        g.drawString(font, "heat (net)", x + 7, y, C_DIM, false);
        int coolLabelX = x + 7 + font.width("heat (net)") + 10;
        g.fill(coolLabelX, y + 1, coolLabelX + 5, y + 1 + 5, C_BLUE);
        g.drawString(font, "cool (HU/t)", coolLabelX + 7, y, C_DIM, false);
        y += 11;

        int plotH = 48;
        int plotY = y;
        g.fill(x, plotY, x + W, plotY + plotH, 0x18_FFFFFF);
        DrawerHelper.drawBorder(g, x, plotY, W, plotH, 0x33_00E5CC, 1);
        g.fill(x, plotY + plotH / 2, x + W, plotY + plotH / 2 + 1, 0x33_FFFFFF);
        drawTrendLines(g, x, plotY, W, plotH);
        y = plotY + plotH + 5;
        rule(g, x, W, y);
        y += 6;

        // Fixed: Swapped to the correct dynamic configuration object instead of old static macro call
        double maxHeat = reactor.getMaxSafeHeatHU();
        double heat = reactor.getHeat();
        double pct = Math.min(1.0, heat / maxHeat);
        int pctColor = pct > 0.85 ? C_RED : pct > 0.5 ? C_ORANGE : C_GREEN;

        y = drawStatRow(g, font, x, y, W, "Current heat",
                String.format("%.0f / %.0f HU", Math.min(heat, 9_999_999), maxHeat), pctColor);
        y = drawStatRow(g, font, x, y, W, "Heat percentage",
                String.format("%.1f%%", pct * 100), pctColor);
        y += 2;
        rule(g, x, W, y);
        y += 6;

        y = drawPerTickBreakdown(g, font, x, y, W);

        int contentH = y - contentStart;
        int maxScroll = Math.max(0, contentH - H);
        panelScroll[4] = Math.min(panelScroll[4], maxScroll);
        // Content currently fits in the panel in all observed layouts, but the
        // scroll plumbing is here defensively in case a future stat row pushes
        // it past H on a shorter screen - drawScrollbar no-ops when it fits.
        drawScrollbar(g, x + W - 3, contentStart, 3, H, panelScroll[4], contentH);
    }

    // =========================================================================
    // Shared sub-screen header (title + back button + rule)
    // =========================================================================
    @OnlyIn(Dist.CLIENT)
    private int drawSubHeader(GuiGraphics g, Font font, int x, int y, int W,
                              String title, int accentColor) {
        g.fill(x, y + 2, x + 5, y + 7, pulsingColor(accentColor, lerpRGB(accentColor, 0xFFFFFFFF, 0.4), 0.005));
        g.drawString(font, title, x + 9, y, accentColor, false);

        String backLabel = "[< BACK]";
        int bw = font.width(backLabel) + 6, bh = 10;
        int bx = x + W - bw, by = y - 1;
        g.fill(bx, by, bx + bw, by + bh, 0x44_000000);
        DrawerHelper.drawBorder(g, bx, by, bw, bh, 0x88_6A9BAA, 1);
        g.drawString(font, backLabel, bx + 3, by + 1, C_MID, false);
        backBtnBounds[0] = bx;
        backBtnBounds[1] = by;
        backBtnBounds[2] = bw;
        backBtnBounds[3] = bh;

        y += 12;
        g.fill(x, y, x + W, y + 1, (0x55 << 24) | (accentColor & 0xFFFFFF));
        g.fill(x, y + 1, x + W, y + 2, (0x22 << 24) | (accentColor & 0xFFFFFF));
        return y + 5;
    }

    // =========================================================================
    // Scrollbar helper
    // =========================================================================
    private static void drawScrollbar(GuiGraphics g, int trackX, int trackY,
                                      int trackW, int trackH, int scroll, int contentH) {
        if (contentH <= trackH) return;
        g.fill(trackX, trackY, trackX + trackW, trackY + trackH, 0x22_FFFFFF);
        int thumbH = Math.max(6, (int) ((trackH / (float) contentH) * trackH));
        int maxScroll = contentH - trackH;
        int thumbY = trackY + (maxScroll > 0 ? (int) ((scroll / (float) maxScroll) * (trackH - thumbH)) : 0);
        g.fill(trackX, thumbY, trackX + trackW, thumbY + thumbH, 0x99_00E5CC);
    }

    // =========================================================================
    // Helpers
    // =========================================================================
    private static <T> LinkedHashMap<T, Long> groupBy(List<T> list) {
        return list.stream().collect(
                Collectors.groupingBy(t -> t, LinkedHashMap::new, Collectors.counting()));
    }

    private static String prettyName(String key) {
        if (key == null || key.isEmpty()) return "Unknown";
        String path = key.contains(":") ? key.substring(key.indexOf(':') + 1) : key;
        String[] parts = path.split("[_\\s]+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) {
                sb.append(Character.toUpperCase(p.charAt(0)));
                if (p.length() > 1) sb.append(p.substring(1));
                sb.append(' ');
            }
        }
        return sb.toString().trim();
    }

    private static String instabilityLabel(int instability) {
        if (instability <= 0) return "(stable)";
        if (instability < 25) return "(low)";
        if (instability < 50) return "(moderate)";
        return "(high)";
    }

    private static int instabilityColor(int instability) {
        if (instability <= 0) return C_GREEN;
        if (instability < 25) return 0xFF_AAFFCC;
        if (instability < 50) return C_GOLD;
        return C_RED;
    }

    private boolean hasSufficientFuelTier(int requiredTier) {
        return resolveTypes(reactor.getPersistedFuelRodIDs(),
                PhoenixAPI.FISSION_FUEL_RODS.keySet(), IFissionFuelRodType::getName)
                .stream().anyMatch(f -> f.getTier() >= requiredTier);
    }

    private static <T> List<T> resolveTypes(List<String> ids, Set<T> registry, Function<T, String> nameOf) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        Map<String, T> byName = new LinkedHashMap<>();
        for (T t : registry) byName.put(nameOf.apply(t), t);
        List<T> result = new ArrayList<>();
        for (String id : ids) {
            T t = byName.get(id);
            if (t != null) result.add(t);
        }
        return result;
    }

    private static void rule(GuiGraphics g, int x, int W, int y) {
        g.fill(x, y, x + W, y + 1, 0x33_00E5CC);
    }

    private static String truncate(Font font, String text, int maxW) {
        if (maxW <= 0) return "";
        if (font.width(text) <= maxW) return text;
        while (text.length() > 1 && font.width(text + "...") > maxW)
            text = text.substring(0, text.length() - 1);
        return text + "...";
    }

    private static List<String> wrapText(Font font, String text, int maxW) {
        if (font.width(text) <= maxW) return List.of(text);
        List<String> result = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (font.width(candidate) <= maxW) {
                line = new StringBuilder(candidate);
            } else {
                if (!line.isEmpty()) result.add(line.toString());
                line = new StringBuilder(word);
            }
        }
        if (!line.isEmpty()) result.add(line.toString());
        return result.isEmpty() ? List.of(text) : result;
    }

    // =========================================================================
    // Atmosphere
    // =========================================================================
    @OnlyIn(Dist.CLIENT)
    private void drawGrid(GuiGraphics g, int x, int y, int w, int h) {
        for (int gx = x + 18; gx < x + w; gx += 18) g.fill(gx, y, gx + 1, y + h, GRID_COL);
        for (int gy = y + 18; gy < y + h; gy += 18) g.fill(x, gy, x + w, gy + 1, GRID_COL);
    }

    @OnlyIn(Dist.CLIENT)
    private void drawScanlines(GuiGraphics g, int x, int y, int w, int h) {
        DrawerHelper.drawGradientRect(g, x, y, w, h / 2, 0x10_000000, 0x04_000000, false);
        DrawerHelper.drawGradientRect(g, x, y + h / 2, w, h / 2, 0x04_000000, 0x10_000000, false);
    }

    @OnlyIn(Dist.CLIENT)
    private void drawMist(GuiGraphics g, int x, int y, int w, int h) {
        long t = System.currentTimeMillis();
        int blobH = h / 3;
        for (int i = 0; i < 6; i++) {
            double phase = t * 0.00035 + i * (Math.PI / 3.0);
            int alpha = (int) ((Math.sin(phase) + 1.0) * 7);
            int col = (alpha << 24) | (MIST_HUE & 0xFFFFFF);
            DrawerHelper.drawGradientRect(g, x, y + (i * h / 6), w, blobH, col, col & 0x00FFFFFF, false);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private int borderColor() {
        if (!reactor.isFormed()) return 0x55_203040;

        // Fixed: Pulling directly from the machine's synced field, converted to seconds
        int sec = reactor.meltdownTimerTicks / 20;
        if (sec > 0) return pulsingColor(0xBB_FF2222, 0xBB_FF8844, 0.012);

        if (reactor.isScramActive()) return 0xAA_FF3333; // steady red, not pulsing - distinct from meltdown

        switch (activePanel) {
            case 1:
                return 0xAA_FF7744;
            case 2:
                return 0xAA_44FFAA;
            case 3:
                return 0xAA_44AAFF;
            case 4:
                return 0xAA_CC66FF;
            case 5:
                return 0xAA_00E5CC;
        }

        // Fixed: Using the corrected config mapping
        double pct = reactor.getHeat() / Math.max(1, reactor.getMaxSafeHeatHU());
        return pct > 0.85 ? 0xAA_FF6622 : 0xAA_00E5CC;
    }

    @OnlyIn(Dist.CLIENT)
    private static int pulsingColor(int a, int b, double freq) {
        return lerpRGB(a, b, (Math.sin(System.currentTimeMillis() * freq) + 1.0) * 0.5);
    }

    private static int lerpRGB(int a, int b, double f) {
        int aa = (a >>> 24), ar = (a >> 16 & 0xFF), ag = (a >> 8 & 0xFF), ab = (a & 0xFF);
        int ba = (b >>> 24), br = (b >> 16 & 0xFF), bg = (b >> 8 & 0xFF), bb = (b & 0xFF);
        return ((aa + (int) ((ba - aa) * f)) << 24) | ((ar + (int) ((br - ar) * f)) << 16) |
                ((ag + (int) ((bg - ag) * f)) << 8) | (ab + (int) ((bb - ab) * f));
    }

    // =========================================================================
    // Shared meltdown screen — call instead of normal HUD when timer is active
    // =========================================================================
    @OnlyIn(Dist.CLIENT)
    static void drawMeltdownScreen(GuiGraphics g, Font font,
                                   int x, int y, int W, int H,
                                   FissionWorkableElectricMultiblockMachine machine) {
        int timerTicks = machine.meltdownTimerTicks;
        int timerMax = machine.meltdownTimerMax;
        boolean scrammed = machine.isScramActive();

        double heat = machine.getHeat();
        double maxSafe = Math.max(1.0, machine.getMaxSafeHeatHU());
        double heatPct = heat / maxSafe;           // >1.0 means over threshold
        double netRate = machine.lastHeatGainedPerTick - machine.lastHeatRemovedPerTick;
        boolean hasCoolant = machine.lastHasCoolant;

        long t = System.currentTimeMillis();

        // ── Background ────────────────────────────────────────────────────────
        g.fill(x, y, x + W, y + H, 0xDD_1A0000);
        DrawerHelper.drawGradientRect(g, x, y, W, H / 2, 0x22_FF0000, 0x00_FF0000, false);
        DrawerHelper.drawGradientRect(g, x, y + H / 2, W, H / 2, 0x00_FF0000, 0x22_FF0000, false);
        for (int sy = y + 4; sy < y + H - 4; sy += 8)
            g.fill(x, sy, x + W, sy + 1, 0x08_FF2222);

        int borderCol = scrammed ? 0xBB_AA4400 : lerpRGB(0xBB_FF2222, 0xBB_FF8844, (Math.sin(t * 0.012) + 1.0) * 0.5);
        DrawerHelper.drawBorder(g, x, y, W, H, borderCol, 1);
        DrawerHelper.drawBorder(g, x + 2, y + 2, W - 4, H - 4, (borderCol & 0x00FFFFFF) | 0x44_000000, 1);

        int cx = x + 5, cw = W - 10; // content indent
        int cy = y + 10;

        // ── Header ────────────────────────────────────────────────────────────
        String header = scrammed ? "MELTDOWN  -  SCRAM ENGAGED" : "!! MELTDOWN IMMINENT !!";
        int headerCol = scrammed ? 0xFF_FFAA44 : lerpRGB(0xFF_FF2222, 0xFF_FF9966, (Math.sin(t * 0.010) + 1.0) * 0.5);
        int hw = font.width(header);
        int hx = x + (W - hw) / 2;
        g.drawString(font, header, hx + 1, cy + 1, 0x44_000000, false);
        g.drawString(font, header, hx, cy, headerCol, false);
        cy += font.lineHeight + 4;
        g.fill(cx, cy, cx + cw, cy + 1, (borderCol & 0x00FFFFFF) | 0x88_000000);
        cy += 6;

        // ── Core heat ─────────────────────────────────────────────────────────
        // Bar covers 0→150% of safe threshold. Marker at 100% = safe limit.
        String heatLabel = "CORE HEAT";
        String heatVal = String.format(java.util.Locale.ROOT, "%.0f%% of safe", heatPct * 100);
        int heatValCol = heatPct > 1.5 ? lerpRGB(0xFF_FF2222, 0xFF_FF9966, (Math.sin(t * 0.010) + 1.0) * 0.5) :
                heatPct > 1.0 ? 0xFF_FF6633 : 0xFF_FFAA44;
        g.drawString(font, heatLabel, cx, cy, 0xFF_886655, false);
        g.drawString(font, heatVal, cx + cw - font.width(heatVal), cy, heatValCol, false);
        cy += font.lineHeight + 2;

        int hbarH = 6, hbarW = cw;
        double heatBarScale = 1.5; // bar represents 0→150% of safe
        int hfillW = (int) (Math.min(heatPct / heatBarScale, 1.0) * (hbarW - 2));
        g.fill(cx, cy, cx + hbarW, cy + hbarH, 0x44_000000);
        DrawerHelper.drawBorder(g, cx, cy, hbarW, hbarH, 0x55_FF3300, 1);
        if (hfillW > 0) {
            // Green→orange up to safe, orange→red above safe
            int safeX = (int) ((1.0 / heatBarScale) * (hbarW - 2));
            int f1 = Math.min(hfillW, safeX);
            if (f1 > 0) DrawerHelper.drawGradientRect(g, cx + 1, cy + 1, f1, hbarH - 2, 0xFF_336633, 0xFF_886600, true);
            if (hfillW > safeX)
                DrawerHelper.drawGradientRect(g, cx + 1 + safeX, cy + 1, hfillW - safeX, hbarH - 2, 0xFF_CC4400,
                        0xFF_FF1100, true);
        }
        // Safe-limit tick mark
        int safeTick = cx + (int) ((1.0 / heatBarScale) * hbarW);
        g.fill(safeTick, cy - 1, safeTick + 1, cy + hbarH + 1, 0xCC_FFFFFF);
        cy += hbarH + 2;
        g.drawString(font, "safe", safeTick - font.width("safe") / 2, cy, 0x55_FFFFFF, false);
        cy += font.lineHeight + 4;
        g.fill(cx, cy, cx + cw, cy + 1, 0x33_FF2222);
        cy += 5;

        // ── Countdown bar ─────────────────────────────────────────────────────
        g.drawString(font, "COUNTDOWN", cx, cy, 0xFF_886655, false);
        String timeStr = scrammed ? String.format(java.util.Locale.ROOT, "%.1fs  PAUSED", timerTicks / 20.0) :
                String.format(java.util.Locale.ROOT, "%.1fs remaining", timerTicks / 20.0);
        g.drawString(font, timeStr, cx + cw - font.width(timeStr), cy,
                scrammed ? 0xFF_FFAA44 : headerCol, false);
        cy += font.lineHeight + 2;

        double frac = timerMax > 0 ? timerTicks / (double) timerMax : 1.0;
        int barH = 10, barW = cw, bx = cx;
        g.fill(bx, cy, bx + barW, cy + barH, 0x44_000000);
        DrawerHelper.drawBorder(g, bx, cy, barW, barH, 0x66_FF2222, 1);
        int fillW = (int) (frac * (barW - 2));
        if (fillW > 0) {
            int fillCol1 = scrammed ? 0xFF_AA4400 :
                    lerpRGB(0xFF_FF2222, 0xFF_FF6600, (Math.sin(t * 0.015) + 1.0) * 0.5);
            int fillCol2 = scrammed ? 0xFF_661100 : 0xFF_AA0000;
            DrawerHelper.drawGradientRect(g, bx + 1, cy + 1, fillW, barH - 2, fillCol1, fillCol2, true);
            if (!scrammed && fillW > 10) {
                int sw = 12, sOff = (int) ((t / 25) % (fillW + sw));
                int sx1 = Math.max(bx + 1 + sOff - sw, bx + 1), sx2 = Math.min(bx + 1 + sOff, bx + 1 + fillW);
                if (sx2 > sx1) {
                    int mid = (sx1 + sx2) / 2;
                    DrawerHelper.drawGradientRect(g, sx1, cy + 1, mid - sx1, barH - 2, 0x00_FFFFFF, 0x33_FFFFFF, true);
                    DrawerHelper.drawGradientRect(g, mid, cy + 1, sx2 - mid, barH - 2, 0x33_FFFFFF, 0x00_FFFFFF, true);
                }
            }
        }
        cy += barH + 6;
        g.fill(cx, cy, cx + cw, cy + 1, 0x33_FF2222);
        cy += 5;

        // ── Live status ───────────────────────────────────────────────────────
        // Net heat rate — is the situation getting better or worse?
        String netLabel = netRate > 0 ? String.format(java.util.Locale.ROOT, "+%.0f HU/t  (gaining heat)", netRate) :
                String.format(java.util.Locale.ROOT, "%.0f HU/t  (cooling down)", netRate);
        int netCol = netRate > 0 ? lerpRGB(0xFF_FF4444, 0xFF_FF8844, (Math.sin(t * 0.008) + 1.0) * 0.5) : 0xFF_44DD88;
        g.drawString(font, "Net rate", cx, cy, 0xFF_886655, false);
        g.drawString(font, netLabel, cx + cw - font.width(netLabel), cy, netCol, false);
        cy += font.lineHeight + 3;

        // Cooling status
        g.drawString(font, "Coolant", cx, cy, 0xFF_886655, false);
        String coolStr = hasCoolant ? "OK" : "ABSENT";
        int coolCol = hasCoolant ? 0xFF_44CC88 : lerpRGB(0xFF_FF2222, 0xFF_FF9966, (Math.sin(t * 0.014) + 1.0) * 0.5);
        g.drawString(font, coolStr, cx + cw - font.width(coolStr), cy, coolCol, false);
        cy += font.lineHeight + 5;
        g.fill(cx, cy, cx + cw, cy + 1, 0x33_FF2222);
        cy += 5;

        // ── Bottom guidance ───────────────────────────────────────────────────
        if (scrammed) {
            String s1 = "SCRAM has paused the countdown.";
            String s2 = "Cool the core below critical threshold.";
            g.drawString(font, s1, x + (W - font.width(s1)) / 2, cy, 0xFF_FFAA44, false);
            g.drawString(font, s2, x + (W - font.width(s2)) / 2, cy + 11, 0xFF_886644, false);
        } else {
            int urgency = lerpRGB(0xFF_FF4444, 0xFF_FF9966, (Math.sin(t * 0.008) + 1.0) * 0.5);
            String s1 = "Engage SCRAM to pause the countdown.";
            String s2 = "Structural failure imminent.";
            g.drawString(font, s1, x + (W - font.width(s1)) / 2, cy, 0xFF_FF8844, false);
            g.drawString(font, s2, x + (W - font.width(s2)) / 2, cy + 11, urgency, false);
        }
    }
}
