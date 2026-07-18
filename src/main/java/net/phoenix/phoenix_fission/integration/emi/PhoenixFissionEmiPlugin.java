package net.phoenix.phoenix_fission.integration.emi;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.phoenix.phoenix_fission.PhoenixAPI;
import net.phoenix.phoenix_fission.PhoenixFission;
import net.phoenix.phoenix_fission.common.PhoenixFissionMachines;
import net.phoenix.phoenix_fission.common.data.block.PhoenixFissionBlocks;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;

@EmiEntrypoint
public class PhoenixFissionEmiPlugin implements EmiPlugin {

    // Lambda is the main icon (2nd arg). By render time blocks are registered.
    public static final EmiRecipeCategory FUEL_BURN = new EmiRecipeCategory(
            PhoenixFission.id("fission_fuel_burn"),
            (draw, x, y, delta) -> {
                // Using Vanilla Blaze Powder as the icon
                EmiStack.of(new ItemStack(Items.BLAZE_POWDER)).render(draw, x, y, delta, EmiStack.RENDER_AMOUNT);
            });

    public static final EmiRecipeCategory COOLANT_CYCLE = new EmiRecipeCategory(
            PhoenixFission.id("fission_coolant_cycle"),
            (draw, x, y, delta) -> {
                // Using Vanilla Packed Ice as the icon
                EmiStack.of(new ItemStack(Items.PACKED_ICE)).render(draw, x, y, delta, EmiStack.RENDER_AMOUNT);
            });

    public static final EmiRecipeCategory MODERATOR = new EmiRecipeCategory(
            PhoenixFission.id("fission_moderator"),
            (draw, x, y, delta) -> {
                // Using Vanilla Glowstone Block as the icon
                EmiStack.of(new ItemStack(Items.GLOWSTONE)).render(draw, x, y, delta, EmiStack.RENDER_AMOUNT);
            });

    public static final EmiRecipeCategory BLANKET = new EmiRecipeCategory(
            PhoenixFission.id("fission_blanket"),
            (draw, x, y, delta) -> {
                // Using Vanilla Iron Bars as the icon
                EmiStack.of(new ItemStack(Items.IRON_BARS)).render(draw, x, y, delta, EmiStack.RENDER_AMOUNT);
            });

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(FUEL_BURN);
        registry.addCategory(COOLANT_CYCLE);
        registry.addCategory(MODERATOR);
        registry.addCategory(BLANKET);

        // Machine controllers as workstations
        if (PhoenixFissionMachines.PRESSURIZED_FISSION_REACTOR != null) {
            EmiStack reactor = safeStack(PhoenixFissionMachines.PRESSURIZED_FISSION_REACTOR.asStack());
            if (!reactor.isEmpty()) {
                registry.addWorkstation(FUEL_BURN, reactor);
                registry.addWorkstation(COOLANT_CYCLE, reactor);
            }
        }
        if (PhoenixFissionMachines.HIGH_PERFORMANCE_BREEDER_REACTOR != null) {
            EmiStack breeder = safeStack(PhoenixFissionMachines.HIGH_PERFORMANCE_BREEDER_REACTOR.asStack());
            if (!breeder.isEmpty()) {
                registry.addWorkstation(FUEL_BURN, breeder);
            }
        }

        // Fuel rod blocks as workstations for FUEL_BURN so clicking them opens category
        for (var entry : PhoenixAPI.FISSION_FUEL_RODS.entrySet()) {
            var block = entry.getValue().get();
            if (block != null) {
                EmiStack s = safeStack(new ItemStack(block.asItem()));
                if (!s.isEmpty()) registry.addWorkstation(FUEL_BURN, s);
            }
        }

        // Cooler blocks as workstations for COOLANT_CYCLE
        for (var entry : PhoenixAPI.FISSION_COOLERS.entrySet()) {
            var block = entry.getValue().get();
            if (block != null) {
                EmiStack s = safeStack(new ItemStack(block.asItem()));
                if (!s.isEmpty()) registry.addWorkstation(COOLANT_CYCLE, s);
            }
        }

        // Moderator blocks as workstations for MODERATOR
        for (var entry : PhoenixAPI.FISSION_MODERATORS.entrySet()) {
            var block = entry.getValue().get();
            if (block != null) {
                EmiStack s = safeStack(new ItemStack(block.asItem()));
                if (!s.isEmpty()) registry.addWorkstation(MODERATOR, s);
            }
        }

        // Blanket blocks as workstations for BLANKET
        for (var entry : PhoenixAPI.FISSION_BLANKETS.entrySet()) {
            var block = entry.getValue().get();
            if (block != null) {
                EmiStack s = safeStack(new ItemStack(block.asItem()));
                if (!s.isEmpty()) registry.addWorkstation(BLANKET, s);
            }
        }

        if (PhoenixFissionMachines.HIGH_PERFORMANCE_BREEDER_REACTOR != null) {
            EmiStack breeder = safeStack(PhoenixFissionMachines.HIGH_PERFORMANCE_BREEDER_REACTOR.asStack());
            if (!breeder.isEmpty()) {
                registry.addWorkstation(MODERATOR, breeder);
                registry.addWorkstation(BLANKET, breeder);
            }
        }
        if (PhoenixFissionMachines.PRESSURIZED_FISSION_REACTOR != null) {
            EmiStack reactor = safeStack(PhoenixFissionMachines.PRESSURIZED_FISSION_REACTOR.asStack());
            if (!reactor.isEmpty()) {
                registry.addWorkstation(MODERATOR, reactor);
            }
        }

        // Register recipes
        for (var fuel : PhoenixAPI.FISSION_FUEL_RODS.keySet()) {
            try {
                registry.addRecipe(new FissionFuelBurnEmiRecipe(fuel));
            } catch (Exception ignored) {}
        }
        for (var cooler : PhoenixAPI.FISSION_COOLERS.keySet()) {
            try {
                registry.addRecipe(new FissionCoolantCycleEmiRecipe(cooler));
            } catch (Exception ignored) {}
        }
        for (var moderator : PhoenixAPI.FISSION_MODERATORS.keySet()) {
            try {
                registry.addRecipe(new FissionModeratorEmiRecipe(moderator));
            } catch (Exception ignored) {}
        }
        for (var blanket : PhoenixAPI.FISSION_BLANKETS.keySet()) {
            try {
                registry.addRecipe(new FissionBlanketEmiRecipe(blanket));
            } catch (Exception ignored) {}
        }
    }

    private static EmiStack safeStack(ItemStack stack) {
        return (stack == null || stack.isEmpty()) ? EmiStack.EMPTY : EmiStack.of(stack);
    }
}
