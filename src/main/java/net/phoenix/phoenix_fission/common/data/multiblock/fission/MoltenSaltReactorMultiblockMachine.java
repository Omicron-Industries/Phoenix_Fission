package net.phoenix.phoenix_fission.common.data.multiblock.fission;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fluids.FluidStack;
import net.phoenix.phoenix_fission.api.block.IMSRCoreLinerType;
import net.phoenix.phoenix_fission.configs.PhoenixFissionConfigs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@MethodsReturnNonnullByDefault
public class MoltenSaltReactorMultiblockMachine extends FissionWorkableElectricMultiblockMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            MoltenSaltReactorMultiblockMachine.class,
            FissionWorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    /**
     * Per-tier EU/mb lookup for the MSR power formula.
     */
    private static final long[] EU_PER_MB_BY_TIER = { 219L, 349L, 699L, 1398L };

    @Persisted
    @DescSynced
    public double xenonPoisonLevel = 0.0;

    @Persisted
    @DescSynced
    public int structuralLinerCount = 0;

    protected @Nullable IMSRCoreLinerType coreLinerSpec = null;

    // Synced snapshots of coreLinerSpec so the client HUD and graphs can read liner info.
    @Persisted
    @DescSynced
    public String linerName = "";
    @Persisted
    @DescSynced
    public int linerTier = 0;
    @Persisted
    @DescSynced
    public int linerFlowRate = 0;
    @Persisted
    @DescSynced
    public double linerHeatPerMb = 0.0;

    @Persisted
    @DescSynced
    public boolean lastCatalystActive = false;
    @Persisted
    @DescSynced
    public boolean lastXenonPurgeSucceeded = false;

    public MoltenSaltReactorMultiblockMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    // ── Structure ────────────────────────────────────────────────────────────────────

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();

        var context = getMultiblockState().getMatchContext();
        if (context != null) {
            List<IMSRCoreLinerType> liners = new ArrayList<>();
            Object rawList = context.get("LinerTypes");
            if (rawList instanceof List<?> list) {
                for (Object obj : list) {
                    if (obj instanceof IMSRCoreLinerType liner) liners.add(liner);
                }
            }
            this.structuralLinerCount = liners.size();
            this.coreLinerSpec = liners.stream()
                    .min(java.util.Comparator.comparingInt(IMSRCoreLinerType::getTier))
                    .orElse(null);
        }

        // Snapshot liner info for client sync.
        if (this.coreLinerSpec != null) {
            this.linerName = this.coreLinerSpec.getName();
            this.linerTier = this.coreLinerSpec.getTier();
            this.linerFlowRate = this.coreLinerSpec.getFluidFlowRate();
            this.linerHeatPerMb = this.coreLinerSpec.getHeatPerMb();
        } else {
            this.linerName = "";
            this.linerTier = 0;
            this.linerFlowRate = 0;
            this.linerHeatPerMb = 0.0;
        }

        // Sync modern tracking variables
        this.lastHasCoolant = true;
        this.lastParallels = Math.max(1, this.structuralLinerCount);

        this.getRecipeLogic().setStatus(RecipeLogic.Status.IDLE);
        markDirty();
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        this.structuralLinerCount = 0;
        this.coreLinerSpec = null;
        this.linerName = "";
        this.linerTier = 0;
        this.linerFlowRate = 0;
        this.linerHeatPerMb = 0.0;
        this.lastCatalystActive = false;
        this.lastXenonPurgeSucceeded = false;
    }

    // ── Run guard ────────────────────────────────────────────────────────────────────

    @Override
    protected boolean shouldRunReactor() {
        if (!isFormed() || coreLinerSpec == null) return false;
        if (isScramActive()) return false;

        int totalRequiredMb = Math.max(1, this.structuralLinerCount) * coreLinerSpec.getFluidFlowRate();

        // Emulates GregTech fluid consumption capability simulation directly
        FluidStack targetSalt = resolveFluidStack(coreLinerSpec.getInputFluidId(), totalRequiredMb);
        return executeFluidIO(targetSalt, IO.IN, true);
    }

    // ── Core tick hook ───────────────────────────────────────────────────────────────
    @Override
    public boolean onWorking() {
        // Run standard background tasks
        boolean wasWorking = super.onWorking();

        // Enforce guard check before running reactor processing
        boolean running = shouldRunReactor();

        lastHeatGainedPerTick = 0.0;
        lastHeatRemovedPerTick = 0.0;
        lastGeneratedEUt = 0;

        lastCatalystActive = false;
        lastXenonPurgeSucceeded = false;

        if (coreLinerSpec == null) return wasWorking;

        int parallels = Math.max(1, this.structuralLinerCount);
        int saltToProcess = parallels * coreLinerSpec.getFluidFlowRate();
        String inputSalt = coreLinerSpec.getInputFluidId();
        String outputSalt = coreLinerSpec.getOutputFluidId();

        // Xenon accumulation curve
        if (running) {
            continuousBurnTicks++;
            xenonPoisonLevel = Math.min(0.50, xenonPoisonLevel + 0.00004);
        } else {
            continuousBurnTicks = 0;
            xenonPoisonLevel = Math.max(0.0, xenonPoisonLevel - 0.0002);
        }

        this.lastParallels = parallels;
        double cleanCoreBonus = 1.0;

        // ── Salt fission loop ────────────────────────────────────────────────────────
        if (running) {
            if (tryConvertFuelSalt(inputSalt, outputSalt, saltToProcess)) {
                double heatProduced = (saltToProcess * coreLinerSpec.getHeatPerMb()) * (1.0 - xenonPoisonLevel);
                this.setHeat(this.getHeat() + heatProduced);
                this.lastHeatGainedPerTick = heatProduced;

                // Off-gassing extraction
                int xenonPerTick = Math.max(1, (int) Math.ceil(saltToProcess * 0.1 / 20.0));
                lastXenonPurgeSucceeded = tryOutputFluidIdBoolean("phoenix_fission:radioactive_xenon_gas",
                        xenonPerTick);
                if (lastXenonPurgeSucceeded) {
                    xenonPoisonLevel = Math.max(0.0, xenonPoisonLevel - 0.002);
                    if (xenonPoisonLevel < 0.05) cleanCoreBonus = 1.5;
                }
            } else {
                running = false;
            }
        } else {
            // Passive heat bleeding
            var hm = PhoenixFissionConfigs.INSTANCE.fission.heatModel;
            double tierFactor = 1.0 / coreLinerSpec.getTier();
            double passiveDelta = (hm.ambientTemperatureHU - this.getHeat()) * hm.passiveCoolingConductivity * 2.0 *
                    tierFactor;
            if (passiveDelta < 0) this.setHeat(this.getHeat() + passiveDelta);
        }

        // ── MSR Power Generation ─────────────────────────────────────────────────────
        if (running) {
            double maxSafeHeat = Math.max(1.0, this.getMaxSafeHeatHU());
            double heatRatio = this.getHeat() / maxSafeHeat;
            double thermalEfficiency = 0.5 + (Math.pow(heatRatio, 2) * 2.0);

            // Volatile Chemical Catalysis
            double catalystMultiplier = 1.0;
            FluidStack catalystIn = resolveFluidStack("gtceu:liquid_fluorine", 10);
            if (executeFluidIO(catalystIn, IO.IN, true) && executeFluidIO(catalystIn, IO.IN, false)) {
                catalystMultiplier = 3.0;
                lastCatalystActive = true;
                xenonPoisonLevel = Math.min(0.50, xenonPoisonLevel + 0.0004);
            }

            int tierIdx = Math.max(0, Math.min(EU_PER_MB_BY_TIER.length - 1, coreLinerSpec.getTier() - 1));
            long euPerMb = EU_PER_MB_BY_TIER[tierIdx];
            long baseEU = (long) saltToProcess * euPerMb;

            double structuralDamping = 1.0 - xenonPoisonLevel;

            this.lastGeneratedEUt = (long) (baseEU * thermalEfficiency * structuralDamping * cleanCoreBonus *
                    catalystMultiplier);

            if (this.lastGeneratedEUt > 0) {
                tryAddEnergy(this.lastGeneratedEUt);
            }
        } else {
            this.lastGeneratedEUt = 0;
        }

        return running || wasWorking;
    }

    // ── Fluid helpers ────────────────────────────────────────────────────────────────

    protected boolean tryConvertFuelSalt(@NotNull String inFluid, @NotNull String outFluid, int mb) {
        if (mb <= 0) return true;

        FluidStack inStack = resolveFluidStack(inFluid, mb);
        FluidStack outStack = resolveFluidStack(outFluid, mb);

        if (!executeFluidIO(inStack, IO.IN, true)) return false;
        if (!executeFluidIO(inStack, IO.IN, false)) return false;

        executeFluidIO(outStack, IO.OUT, false);
        return true;
    }

    protected boolean tryOutputFluidIdBoolean(@NotNull String fluidId, int mb) {
        if (mb <= 0) return true;
        FluidStack fs = resolveFluidStack(fluidId, mb);
        if (fs.isEmpty()) return false;

        return executeFluidIO(fs, IO.OUT, false);
    }

    @Nullable
    private FluidStack resolveFluidStack(String fluidId, int amount) {
        ResourceLocation rl = ResourceLocation.tryParse(fluidId);
        if (rl == null) return null;
        var fluid = net.minecraftforge.registries.ForgeRegistries.FLUIDS.getValue(rl);
        if (fluid == null || fluid == net.minecraft.world.level.material.Fluids.EMPTY) return null;
        return new FluidStack(fluid, amount);
    }

    private void tryAddEnergy(long joules) {
        getEnergyContainer().addEnergy(joules);
    }

    // ── Display ──────────────────────────────────────────────────────────────────────

    @Override
    public void addDisplayText(@NotNull List<Component> textList) {
        if (!isFormed() || coreLinerSpec == null) {
            textList.add(Component.translatable("phoenix_fission.not_formed")
                    .withStyle(s -> s.withColor(0xFF4444)));
            return;
        }

        final var config = PhoenixFissionConfigs.INSTANCE.fission;
        double maxSafeHU = this.getMaxSafeHeatHU();
        final boolean overheat = this.getHeat() > maxSafeHU;

        // 1. Heat
        textList.add(Component.translatable("phoenix_fission.current_heat_display",
                String.format("%.1f", this.getHeat()), String.format("%.1f", maxSafeHU))
                .withStyle(s -> s.withColor(overheat ? 0xFF3333 : 0x33FF33)));

        // 2. Thermal efficiency
        double maxSafeHeat = Math.max(1.0, maxSafeHU);
        double heatRatio = this.getHeat() / maxSafeHeat;
        double thermalEfficiency = 0.5 + (Math.pow(heatRatio, 2) * 2.0);
        int effColor = thermalEfficiency > 1.8 ? 0x55FF55 : (thermalEfficiency > 1.0 ? 0xFFAA00 : 0xFF5555);
        textList.add(Component.translatable("phoenix_fission.msr.thermal_efficiency",
                String.format(java.util.Locale.ROOT, "%.1f", thermalEfficiency * 100.0) + "%")
                .withStyle(s -> s.withColor(effColor)));

        // 3. Power
        textList.add(getVoltageFormattedOutputMSR(lastGeneratedEUt));

        // 4. Structure
        textList.add(Component.translatable("phoenix_fission.parallels", lastParallels));
        textList.add(Component.translatable("phoenix_fission.msr.structural_tier",
                String.format("MK%d (%s)", coreLinerSpec.getTier(), coreLinerSpec.getName().toUpperCase()))
                .withStyle(s -> s.withColor(0x55FF55)));
        textList.add(Component.translatable("phoenix_fission.msr.active_liners", this.structuralLinerCount)
                .withStyle(s -> s.withColor(0x55AAFF)));
        textList.add(Component.translatable("phoenix_fission.msr.processing_rate",
                (Math.max(1, this.structuralLinerCount) * coreLinerSpec.getFluidFlowRate()) + " mb/t")
                .withStyle(s -> s.withColor(0xFFAA00)));

        // 5. Catalyst
        if (lastCatalystActive) {
            textList.add(Component.translatable("phoenix_fission.msr.catalyst_fluorine")
                    .withStyle(s -> s.withColor(0x55FFFF)));
        }

        // 6. Xenon poisoning + purge status
        double xenonPct = xenonPoisonLevel * 100;
        int xenonColor = xenonPoisonLevel < 0.05 ? 0x55FF55 : (xenonPoisonLevel > 0.35 ? 0xFF3333 : 0xAA00AA);
        if (xenonPoisonLevel < 0.05 && lastGeneratedEUt > 0) {
            textList.add(Component.translatable("phoenix_fission.msr.xenon_clean")
                    .withStyle(s -> s.withColor(0x55FF55)));
        } else {
            textList.add(Component.translatable("phoenix_fission.msr.xenon_poisoning",
                    String.format(java.util.Locale.ROOT, "%.1f", xenonPct) + "%")
                    .withStyle(s -> s.withColor(xenonColor)));
        }

        textList.add(Component.literal("  Xe Purge: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(lastXenonPurgeSucceeded ? "OK" : "Blocked")
                        .withStyle(s -> s.withColor(lastXenonPurgeSucceeded ? 0x55FF55 : 0xFF5555))));

        // 7. Coolant text indicators managed by core Thermal Manager outputs
        textList.add(Component.translatable("phoenix_fission.cooling_power", lastProvidedCooling)
                .withStyle(s -> s.withColor(0x55FFFF)));
        textList.add(Component.translatable(
                lastHasCoolant ? "phoenix_fission.coolant_status.ok" : "phoenix_fission.coolant_status.empty"));

        if (meltdownTimerTicks > 0) {
            textList.add(Component.translatable("phoenix_fission.status.danger_timer",
                    meltdownTimerTicks / 20));
        }
    }

    @Override
    public @NotNull ModularUI createUI(Player player) {
        if (getLevel() != null && !getLevel().isClientSide) {
            getLevel().sendBlockUpdated(getPos(), getBlockState(), getBlockState(), 3);
        }
        return new ModularUI(264, 256, this, player)
                .widget(new net.phoenix.phoenix_fission.client.gui.MSRFancyUIWidget(this, 264, 256));
    }

    private Component getVoltageFormattedOutputMSR(long euOut) {
        int tier = 0;
        for (int i = 0; i < GTValues.V.length; i++) {
            if (euOut >= GTValues.V[i]) tier = i;
            else break;
        }
        return Component.translatable("phoenix_fission.eu_generation", euOut)
                .append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(GTValues.VNF[tier]))
                .append(Component.literal(")").withStyle(ChatFormatting.GRAY));
    }
}
