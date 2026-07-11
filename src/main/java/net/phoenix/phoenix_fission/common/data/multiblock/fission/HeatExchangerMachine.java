package net.phoenix.phoenix_fission.common.data.multiblock.fission;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.ITieredMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.machine.multiblock.part.EnergyHatchPartMachine;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.phoenix.phoenix_fission.client.gui.HeatExchangerFancyUIWidget;
import net.phoenix.phoenix_fission.common.data.block.PhoenixFissionBlocks;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HeatExchangerMachine extends WorkableElectricMultiblockMachine implements ITieredMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            HeatExchangerMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    @Getter
    @DescSynced
    @Persisted
    private int length = 0;

    @Getter
    @Persisted
    @DescSynced
    private int dynamoTier = GTValues.HV;
    @Getter
    @Persisted
    @DescSynced
    private int heat = 0;
    @Getter
    @Persisted
    @DescSynced
    private int cooldownTicks = 0;
    @Getter
    @Persisted
    @DescSynced
    private boolean heliumActive = false;
    @Getter
    @Persisted
    @DescSynced
    private long maxHatchOutput = 0;

    protected final ConditionalSubscriptionHandler updateHandler;

    public HeatExchangerMachine(IMachineBlockEntity holder) {
        super(holder);
        this.updateHandler = new ConditionalSubscriptionHandler(this, this::updateLogic, this::isFormed);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        calculateExchangerLength();
        detectDynamoTier(); // Your logic already finds the correct tier here!

        this.updateHandler.updateSubscription();

        if (getRecipeLogic() != null) {
            getRecipeLogic().updateTickSubscription();
        }

        // FORCE THE CLIENT PACKET UPDATE IMMEDIATELY
        if (!getLevel().isClientSide) {
            this.markDirty();
            // This forces Minecraft/Forge to resend the description packet
            // containing all @DescSynced fields to nearby players right now.
            getLevel().sendBlockUpdated(getPos(), getBlockState(), getBlockState(), 3);
        }
    }

    // Checks the multiblock cache for gearbox casings.
    // The shapeInfo of the registration sets gearbox casings as one per layer so we can assume
    // 1 gearbox = 1 layer.
    private void calculateExchangerLength() {
        if (getLevel() == null) return;

        int validLayers = 0;
        Direction back = getFrontFacing().getOpposite();

        for (int depth = 1; depth <= 30; depth++) {
            BlockPos scanPos = getPos().relative(back, depth);
            Block blockAtCenter = getLevel().getBlockState(scanPos).getBlock();

            if (isExchangerLayerBlock(blockAtCenter)) {
                validLayers++;
            } else if (depth > 5) {
                break;
            }
        }
        this.length = Math.max(1, validLayers);
    }

    /** Returns true if the given block counts as one exchanger layer during length calculation. */
    protected boolean isExchangerLayerBlock(Block block) {
        return block == PhoenixFissionBlocks.FISSILE_SAFE_GEARBOX_CASING.get();
    }

    // Searches each multiblock part for an EnergyHatchPartMachine configured as
    // an output (dynamo) hatch and reads its public energyContainer field
    // directly. The part itself does NOT implement IEnergyContainer - it HOLDS
    // one as a field (NotifiableEnergyContainer, which does implement
    // IEnergyContainer) - so checking "part instanceof IEnergyContainer" never
    // matches, and neither does the recipe-handler + EURecipeCapability lookup
    // tried earlier (outHandlersSeen was always 0 in testing).
    private void detectDynamoTier() {
        int detectedTier = GTValues.ULV;
        long totalPower = 0;

        var parts = getParts();
        if (parts != null) {
            for (IMultiPart part : parts) {
                if (!(part instanceof EnergyHatchPartMachine hatch)) continue;

                IEnergyContainer container = hatch.energyContainer;
                if (container == null) continue;

                long voltage = container.getOutputVoltage();
                long amperage = container.getOutputAmperage();

                // If it can't output voltage or amperage, it's an energy input hatch!
                if (voltage <= 0 || amperage <= 0) continue;

                detectedTier = Math.max(detectedTier, GTUtil.getFloorTierByVoltage(voltage));
                totalPower += (voltage * amperage);
            }
        }

        if (this.dynamoTier != detectedTier || this.maxHatchOutput != totalPower) {
            this.dynamoTier = detectedTier;
            this.maxHatchOutput = totalPower;

            if (this.isFormed() && getLevel() != null && !getLevel().isClientSide) {
                this.markDirty();
                getLevel().sendBlockUpdated(getPos(), getBlockState(), getBlockState(), 3);
            }
        }
    }

    // Some gtm internals use this, we just provide current tier unless it's not formed (in which case it assumes ulv)
    @Override
    public int getTier() {
        return isFormed() ? this.dynamoTier : GTValues.ULV;
    }

    // When there is liquid helium present the eu produced from the machine is boosted.
    public static ModifierFunction recipeModifier(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof HeatExchangerMachine exchanger))
            return ModifierFunction.IDENTITY;

        double multiplier = 1.5 + (Math.max(0, exchanger.getLength() - 1) * 0.25);

        if (exchanger.heliumActive) multiplier *= 2.0;

        return ModifierFunction.builder()
                .eutMultiplier(multiplier * multiplier)
                .build();
    }

    // Gates/mutates recipe running based on recipe and cold coolants.
    protected void updateLogic() {
        if (getLevel() == null || getLevel().isClientSide) return;

        if (getOffsetTimer() % 20 == 0) {
            detectDynamoTier();
        }

        if (cooldownTicks > 0) {
            cooldownTicks--;
            if (getRecipeLogic().isWorking()) {
                getRecipeLogic().interruptRecipe();
            }
            return;
        }

        if (getRecipeLogic().isWorking()) {
            if (getOffsetTimer() % 20 == 0) {
                if (consumeColdCoolant()) {
                    heat = Math.max(0, heat - 5);
                } else {
                    heat += 10;

                    if (heat >= 100) {
                        heat = 100;
                        cooldownTicks = 600;
                        getRecipeLogic().interruptRecipe();
                    }
                }
            }
        } else {
            if (getOffsetTimer() % 20 == 0 && heat > 0) {
                heat--;
            }
        }
    }

    // Handles consumption of the cold coolant necessary to run the machine.
    private boolean consumeColdCoolant() {
        int effectiveLength = Math.max(1, length);
        int waterAmount = (int) (100 * (1.0 + (effectiveLength - 1) * 0.2));
        int heliumAmount = (int) (25 * (1.0 + (effectiveLength - 1) * 0.2));

        var heliumInput = GTMaterials.Helium.getFluid(FluidStorageKeys.LIQUID, heliumAmount);
        var heliumOutput = GTMaterials.Helium.getFluid(heliumAmount);

        var heliumRecipe = GTRecipeBuilder.ofRaw()
                .inputFluids(heliumInput)
                .outputFluids(heliumOutput)
                .buildRawRecipe();

        if (RecipeHelper.matchRecipe(this, heliumRecipe).isSuccess()) {
            if (RecipeHelper.handleRecipeIO(this, heliumRecipe, IO.IN, getRecipeLogic().getChanceCaches())
                    .isSuccess()) {
                this.heliumActive = true;
                return RecipeHelper.handleRecipeIO(this, heliumRecipe, IO.OUT, getRecipeLogic().getChanceCaches())
                        .isSuccess();
            }
        }

        this.heliumActive = false;
        var distilled = GTRecipeBuilder.ofRaw()
                .inputFluids(GTMaterials.DistilledWater.getFluid(waterAmount))
                .outputFluids(GTMaterials.Steam.getFluid(waterAmount))
                .buildRawRecipe();

        if (RecipeHelper.matchRecipe(this, distilled).isSuccess()) {
            if (RecipeHelper.handleRecipeIO(this, distilled, IO.IN, getRecipeLogic().getChanceCaches()).isSuccess()) {
                return RecipeHelper.handleRecipeIO(this, distilled, IO.OUT, getRecipeLogic().getChanceCaches())
                        .isSuccess();
            }
        }

        var water = GTRecipeBuilder.ofRaw()
                .inputFluids(GTMaterials.Water.getFluid(waterAmount))
                .outputFluids(GTMaterials.Steam.getFluid(waterAmount))
                .buildRawRecipe();

        if (RecipeHelper.matchRecipe(this, water).isSuccess()) {
            if (RecipeHelper.handleRecipeIO(this, water, IO.IN, getRecipeLogic().getChanceCaches()).isSuccess()) {
                return RecipeHelper.handleRecipeIO(this, water, IO.OUT, getRecipeLogic().getChanceCaches()).isSuccess();
            }
        }

        return false;
    }

    @Override
    public @NotNull ModularUI createUI(Player entityPlayer) {
        return new ModularUI(198, 208, this, entityPlayer).widget(new HeatExchangerFancyUIWidget(this, 198, 208));
    }

    @Override
    public void addDisplayText(@NotNull List<Component> textList) {
        // Purposeful noop
    }
}
