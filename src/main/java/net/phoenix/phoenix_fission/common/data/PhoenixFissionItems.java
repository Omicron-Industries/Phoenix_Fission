package net.phoenix.phoenix_fission.common.data;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

import com.tterrag.registrate.util.entry.ItemEntry;

import static net.phoenix.phoenix_fission.PhoenixFission.PHOENIX_REGISTRATE;

public class PhoenixFissionItems {

    public static ItemEntry<TooltipItem> THORIUM_FUEL_PELLET = PHOENIX_REGISTRATE
            .item("thorium_fuel_pellet", p -> new TooltipItem(p,
                    () -> Component.literal("§6A compacted pellet of fertile thorium."),
                    () -> Component.literal(
                            "§6Designed for efficient neutron capture and U-233 breeding in reactor blankets.")))
            .lang("Thorium Fuel Pellet")
            .register();

    public static ItemEntry<TooltipItem> U33_FUEL_PELLET = PHOENIX_REGISTRATE
            .item("u233_fuel_pellet", p -> new TooltipItem(p,
                    () -> Component.literal("§aA highly concentrated pellet of bred Uranium-233."),
                    () -> Component.literal("§aDelivers exceptional energy output as primary fissile fuel.")))
            .lang("Uranium-233 Fuel Pellet")
            .register();

    public static ItemEntry<TooltipItem> PLUTONIUM_241_FUEL_PELLET = PHOENIX_REGISTRATE
            .item("plutonium_241_fuel_pellet", p -> new TooltipItem(p,
                    () -> Component.literal("§6A compacted pellet of Plutonium-241."),
                    () -> Component.literal("§aDelivers exceptional energy output as primary fissile fuel.")))
            .lang("Plutonium-241 Fuel Pellet")
            .register();

    public static ItemEntry<TooltipItem> U236_FUEL_PELLET = PHOENIX_REGISTRATE
            .item("u236_fuel_pellet", p -> new TooltipItem(p,
                    () -> Component.literal("§aA highly concentrated pellet of bred Uranium-236."),
                    () -> Component.literal(
                            "§6Designed for efficient neutron capture and Pu-241 breeding reactions as a blanket.")))
            .lang("Uranium-236 Fuel Pellet")
            .register();

    public static ItemEntry<TooltipItem> U235_FUEL_PELLET = PHOENIX_REGISTRATE
            .item("u235_fuel_pellet", p -> new TooltipItem(p,
                    () -> Component.literal("§2A compacted pellet of Uranium-235."),
                    () -> Component.literal("§2Serves as the primary driver fuel for fission reactors.")))
            .lang("Uranium-235 Fuel Pellet")
            .register();

    public static ItemEntry<Item> HONEY_TREAT = PHOENIX_REGISTRATE
            .item("honey_treat", Item::new)
            .lang("Honey Treat")
            .register();

    public static ItemEntry<Item> BASIC_FUEL_ROD = PHOENIX_REGISTRATE
            .item("basic_fuel_rod", Item::new)
            .lang("Basic Fuel Rod")
            .register();

    public static ItemEntry<Item> ZIRCONIUM__ROD = PHOENIX_REGISTRATE
            .item("zirconium_rod", Item::new)
            .lang("Zirconium Rod")
            .register();

    public static void init() {}
}
