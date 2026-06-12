package net.phoenix.phoenix_fission.datagen.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;

public class PhoenixMaterialLangHandler {

    public static void init(RegistrateLangProvider provider) {
        // Tag Prefixes
        provider.add("tagprefix.nanites", "%s Nanites");
        provider.add("tagprefix.crystal_rose", "%s Crystal Rose");
        provider.add("tagprefix.tier_one_bee", "%s Lively Bee");
        provider.add("tagprefix.tier_two_bee", "%s Energetic Bee");
        provider.add("tagprefix.tier_three_bee", "%s Stronk Bee");
        provider.add("tagprefix.honeycomb_block", "%s Rich Honey Comb (Block)");
        provider.add("tagprefix.honeycomb", "%s Rich Honey Comb");

        String[] beeMaterials = {
                "pitchblende", "steel", "apatite", "cobalt", "salt", "sponge", "ghostly", "copper", "rune", "magma",
                "rock_salt", "steamy", "slime", "brown_shroom", "sculk", "nether_quartz",
                "scheelite", "certus_quartz", "silky", "frosty", "withered", "arcane_crystal", "sticky_resin", "zombie",
                "blaze", "ice",
                "red_shroom", "infinity", "bone", "lepidolite", "source_gem", "cinnabar", "topaz", "amethyst",
                "prismarine",
                "realgar", "pyrope", "zinc", "tin", "diamond", "iron", "fluorite", "ruby", "sapphire", "stibnite",
                "opal",
                "cheese", "lapis", "electrotine", "redstone", "saltpeter", "coal", "ilmenite", "silicon",
                "galena",
                "experience", "sodalite", "gold", "obsidian", "cobaltite", "bauxite", "silver", "tungstate", "emerald",
                "tricalcium_phosphate", "nickel", "fluix", "malachite", "lead", "invar", "thorium", "graphite",
                "sphalerite", "netherite", "resonant_ender", "acidic", "chromite", "pyrolusite", "platinum", "bismuth",
                "glowing",
                "bastnasite", "tetrahedrite", "sulfur", "oilsands", "tantalite", "barite", "vanadium_magnetite",
                "draconic",
                "pyrochlore", "voidglass_shard", "crystalized_fluxstone", "ignisium", "sky_steel", "glowstone"
        };

        for (String mat : beeMaterials) {
            String formatted = formatName(mat);
            provider.add("material.phoenix_fission.honeyed_" + mat, "Honeyed " + formatted);
            provider.add("material.phoenix_fission.raw_" + mat + "_wax", "Raw " + formatted + " Wax");
        }
    }

    private static String formatName(String name) {
        String[] parts = name.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    private static void addMaterialLang(RegistrateLangProvider provider, String id, String name) {
        provider.add("material.phoenix_fission." + id, name);
    }
}
