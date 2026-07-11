package net.phoenix.phoenix_fission.common.data;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.BlastProperty;
import com.gregtechceu.gtceu.common.data.GTElements;

import net.phoenix.phoenix_fission.PhoenixFission;

import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.*;
import static com.gregtechceu.gtceu.api.data.chemical.material.properties.BlastProperty.GasTier.MID;

public class PhoenixFissionMaterials {

    public static Material URANIUM_233, URANIUM_236, AMERICIUM_241, AMERICIUM_HEXAFLUORIDE, URANIUM_OXIDE;
    public static Material INERT_GAS_WASTE, IRRADIATED_THORIUM, IRRADIATED_URANIUM_236, SPENT_URANIUM_233,
            SPENT_URANIUM_235, DEPLETED_URANIUM, DEPLETED_THORIUM, PLUTONIUM_FISSION_ASH, DEPLETED_PLUTONIUM_241,
            LOW_LEVEL_RADIOACTIVE_WASTE, EXTREMELY_MODIFIED_SPACE_GRADE_STEEL;

    public static Material BORON_CARBIDE;
    public static Material NIOBIUM_MODIFIED_SILICON_CARBIDE;
    public static Material CRYO_GRAPHITE_BINDING_SOLUTION, CRYO_ZIRCONIUM_BINDING_SOLUTION;
    public static Material HAFNIUM_CHLORIDE;
    public static Material ZIRCALLOY;
    public static Material ZIRCON, SPACE_GRADE_STEEL;

    public static Material MEDIUM_LEVEL_RADIOACTIVE_WASTE;
    public static Material HOT_SODIUM_POTASSIUM;
    public static Material MEDIUM_PRESSURE_FISSILE_STEAM;
    public static Material CRITICAL_STEAM;

    public static Material FISSION_PRODUCTS_FLUID;
    public static Material RADIOACTIVE_SLUDGE;
    public static Material RADIOACTIVE_GAS_MIXTURE;
    public static Material IMPURE_ZIRCONIUM;
    public static Material IMPURE_HAFNIUM;

    public static Material RHODIUM_PALLADIUM_SOLUTION;
    public static Material TECHNETIUM_STRONTIUM_SOLUTION;
    public static Material GASEOUS_FISSION_BYPRODUCTS;
    public static Material ACIDIC_WASTE;
    public static Material TRACE_FISSION_GASES;
    public static Material PURIFIED_RADIOACTIVE_WASTE_FLUID;

    public static Material EXOTIC_FISSION_CONCENTRATE;
    public static Material EXOTIC_FISSILE_MATERIALS_CLUMP;
    public static Material FISSILE_ASH;
    public static Material TRACE_ACTINIDES, FROST;

    public static Material U235_MOLTEN_SALT, DEPLETED_U235_MOLTEN_SALT;
    public static Material THORIUM_U233_MOLTEN_SALT, DEPLETED_THORIUM_MOLTEN_SALT;
    public static Material PLUTONIUM_MOLTEN_SALT, IRRADIATED_ACTINIDE_WASTE;
    public static Material CALIFORNIUM_MOLTEN_SALT, TRANSURANIC_SLUDGE_WASTE;

    public static void register() {
        ZIRCON = new Material.Builder(PhoenixFission.id("zircon"))
                .dust()
                .color(0xC2B280)
                .secondaryColor(0x8B7D6B)
                .iconSet(MaterialIconSet.DULL)
                .formula("ZrSiO4")
                .buildAndRegister();

        FROST = new Material.Builder(PhoenixFission.id("frost"))
                .langValue("Frost")
                .fluid()
                .color(0xA7D1EB)
                .secondaryColor(0x778899)
                .iconSet(MaterialIconSet.SHINY)
                .buildAndRegister();

        U235_MOLTEN_SALT = new Material.Builder(PhoenixFission.id("u235_molten_salt"))
                .fluid()
                .color(0x39E639) // Bright neon radioactive green
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        DEPLETED_U235_MOLTEN_SALT = new Material.Builder(PhoenixFission.id("depleted_u235_molten_salt"))
                .fluid()
                .color(0x225522) // Muddy, dark waste green
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        // --- MK2 Molten Salts ---
        THORIUM_U233_MOLTEN_SALT = new Material.Builder(PhoenixFission.id("thorium_u233_molten_salt"))
                .fluid()
                .color(0x44CCFF) // Cyan-tinted fuel glow
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        DEPLETED_THORIUM_MOLTEN_SALT = new Material.Builder(PhoenixFission.id("depleted_thorium_molten_salt"))
                .fluid()
                .color(0x2A4450) // Slate waste blue
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        // --- MK3 Molten Salts ---
        PLUTONIUM_MOLTEN_SALT = new Material.Builder(PhoenixFission.id("plutonium_molten_salt"))
                .fluid()
                .color(0x9400D3) // Dark Violet fuel
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        IRRADIATED_ACTINIDE_WASTE = new Material.Builder(PhoenixFission.id("irradiated_actinide_waste"))
                .fluid()
                .color(0x4B0082) // Deep Indigo chemical muck
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        // --- MK4 Molten Salts ---
        CALIFORNIUM_MOLTEN_SALT = new Material.Builder(PhoenixFission.id("californium_molten_salt"))
                .fluid()
                .color(0xFF3366) // Hot radioactive magenta pink
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        TRANSURANIC_SLUDGE_WASTE = new Material.Builder(PhoenixFission.id("transuranic_sludge_waste"))
                .fluid()
                .color(0x331A00) // Highly concentrated toxic brown sludge
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        BORON_CARBIDE = new Material.Builder(PhoenixFission.id("boron_carbide"))
                .ingot()
                .color(0x353630)
                .iconSet(MaterialIconSet.DULL)
                .blastTemp(3600, BlastProperty.GasTier.LOW,
                        500, 1500)
                .flags(MaterialFlags.GENERATE_PLATE, MaterialFlags.GENERATE_ROD, MaterialFlags.GENERATE_DENSE)
                .formula("B4C")
                .buildAndRegister();

        LOW_LEVEL_RADIOACTIVE_WASTE = new Material.Builder(PhoenixFission.id("low_level_radioactive_waste"))
                .ingot()
                .color(0x262a23)
                .secondaryColor(0x365320)
                .iconSet(MaterialIconSet.RADIOACTIVE)
                .buildAndRegister();

        MEDIUM_LEVEL_RADIOACTIVE_WASTE = new Material.Builder(PhoenixFission.id("medium_level_radioactive_waste"))
                .ingot()
                .color(0x4d5447).secondaryColor(0x1a2e0a)
                .iconSet(MaterialIconSet.RADIOACTIVE)
                .buildAndRegister();

        NIOBIUM_MODIFIED_SILICON_CARBIDE = new Material.Builder(PhoenixFission.id("niobium_modified_silicon_carbide"))
                .ingot()
                .color(0x4A4B6B)
                .secondaryColor(0x101021)
                .iconSet(MaterialIconSet.METALLIC)
                .flags(MaterialFlags.GENERATE_PLATE, MaterialFlags.GENERATE_FRAME, MaterialFlags.GENERATE_FOIL)
                .blastTemp(4500, BlastProperty.GasTier.MID,
                        2000, 1800)
                .formula("Nb(SiC)x")
                .buildAndRegister();

        CRYO_GRAPHITE_BINDING_SOLUTION = new Material.Builder(PhoenixFission.id("cryo_graphite_binding_solution"))
                .fluid()
                .color(0x507080)
                .secondaryColor(0x7090A0)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        CRYO_ZIRCONIUM_BINDING_SOLUTION = new Material.Builder(PhoenixFission.id("cryo_zirconium_binding_solution"))
                .fluid()
                .color(0x80B0CC)
                .secondaryColor(0xA0D0E0)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        HAFNIUM_CHLORIDE = new Material.Builder(PhoenixFission.id("hafnium_chloride"))
                .fluid()
                .color(0xC0C0C0)
                .iconSet(MaterialIconSet.BRIGHT)
                .buildAndRegister();

        ZIRCALLOY = new Material.Builder(PhoenixFission.id("zircalloy"))
                .ingot().dust()
                .color(0x002327)
                .secondaryColor(0x000000)
                .iconSet(MaterialIconSet.DULL)
                .flags(
                        MaterialFlags.GENERATE_PLATE,
                        MaterialFlags.GENERATE_GEAR,
                        MaterialFlags.GENERATE_SMALL_GEAR,
                        MaterialFlags.GENERATE_ROD,
                        MaterialFlags.GENERATE_DENSE,
                        MaterialFlags.GENERATE_FOIL,
                        MaterialFlags.GENERATE_SPRING,
                        MaterialFlags.GENERATE_FRAME)
                .buildAndRegister();

        HOT_SODIUM_POTASSIUM = new Material.Builder(PhoenixFission.id("hot_sodium_potassium"))
                .fluid()
                .color(0xFF4500)
                .secondaryColor(0xFFD700)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        MEDIUM_PRESSURE_FISSILE_STEAM = new Material.Builder(PhoenixFission.id("medium_pressure_fissile_steam"))
                .fluid()
                .color(0x7DA10E)
                .iconSet(MaterialIconSet.BRIGHT)
                .buildAndRegister();

        CRITICAL_STEAM = new Material.Builder(PhoenixFission.id("critical_steam"))
                .gas()
                .color(0xF0F8FF)
                .secondaryColor(0xADD8E6)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        FISSION_PRODUCTS_FLUID = new Material.Builder(PhoenixFission.id("fission_products_fluid"))
                .fluid()
                .color(0x556B2F)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        RADIOACTIVE_SLUDGE = new Material.Builder(PhoenixFission.id("radioactive_sludge"))
                .fluid()
                .color(0x2F4F4F)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        RADIOACTIVE_GAS_MIXTURE = new Material.Builder(PhoenixFission.id("radioactive_gas_mixture"))
                .gas()
                .color(0x6B8E23)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        RHODIUM_PALLADIUM_SOLUTION = new Material.Builder(PhoenixFission.id("rhodium_palladium_solution"))
                .fluid()
                .color(0x9E9E9E)
                .iconSet(MaterialIconSet.SHINY)
                .buildAndRegister();

        TECHNETIUM_STRONTIUM_SOLUTION = new Material.Builder(PhoenixFission.id("technetium_strontium_solution"))
                .fluid()
                .color(0x7CFC00)
                .iconSet(MaterialIconSet.BRIGHT)
                .buildAndRegister();

        GASEOUS_FISSION_BYPRODUCTS = new Material.Builder(PhoenixFission.id("gaseous_fission_byproducts"))
                .gas()
                .color(0xB0C4DE)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        ACIDIC_WASTE = new Material.Builder(PhoenixFission.id("acidic_waste"))
                .fluid()
                .color(0x9ACD32)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        TRACE_FISSION_GASES = new Material.Builder(PhoenixFission.id("trace_fission_gases"))
                .gas()
                .color(0xC0FFEE)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        PURIFIED_RADIOACTIVE_WASTE_FLUID = new Material.Builder(PhoenixFission.id("purified_radioactive_waste_fluid"))
                .fluid()
                .color(0x3B5323)
                .iconSet(MaterialIconSet.DULL)
                .buildAndRegister();

        EXOTIC_FISSION_CONCENTRATE = new Material.Builder(PhoenixFission.id("exotic_fission_concentrate"))
                .dust()
                .color(0x800080)
                .secondaryColor(0x00FF00)
                .iconSet(MaterialIconSet.RADIOACTIVE)
                .buildAndRegister();

        EXOTIC_FISSILE_MATERIALS_CLUMP = new Material.Builder(PhoenixFission.id("exotic_fissile_materials_clump"))
                .dust()
                .color(0x556B2F)
                .secondaryColor(0x00FF00)
                .iconSet(MaterialIconSet.RADIOACTIVE)
                .buildAndRegister();

        FISSILE_ASH = new Material.Builder(PhoenixFission.id("fissile_ash"))
                .dust()
                .color(0x444444)
                .iconSet(MaterialIconSet.RADIOACTIVE)
                .buildAndRegister();

        PLUTONIUM_FISSION_ASH = new Material.Builder(PhoenixFission.id("plutonium_fission_ash"))
                .dust()
                .color(0x4B0082)
                .secondaryColor(0x8A2BE2)
                .iconSet(MaterialIconSet.RADIOACTIVE)
                .buildAndRegister();

        TRACE_ACTINIDES = new Material.Builder(PhoenixFission.id("trace_actinides"))
                .dust()
                .color(0x2E8B57)
                .iconSet(MaterialIconSet.RADIOACTIVE)
                .buildAndRegister();
        URANIUM_233 = new Material.Builder(PhoenixFission.id("uranium_233"))
                .ingot().fluid().color(0x33FF33).secondaryColor(0x00CC00).element(PhoenixElements.URANIUM_233)
                .iconSet(MaterialIconSet.RADIOACTIVE).buildAndRegister();

        URANIUM_236 = new Material.Builder(PhoenixFission.id("uranium_236"))
                .ingot().fluid().color(0x33CCFF).secondaryColor(0x0099EE).element(PhoenixElements.URANIUM_236)
                .iconSet(MaterialIconSet.RADIOACTIVE).buildAndRegister();

        AMERICIUM_241 = new Material.Builder(PhoenixFission.id("americium_241"))
                .ingot().color(0xCDC9C9).secondaryColor(0x8B8B7A).element(PhoenixElements.AMERICIUM_241)
                .iconSet(MaterialIconSet.RADIOACTIVE).buildAndRegister();

        AMERICIUM_HEXAFLUORIDE = new Material.Builder(PhoenixFission.id("americium_hexafluoride"))
                .gas().color(0xFFFFFF).secondaryColor(0xADD8E6).iconSet(MaterialIconSet.DULL).buildAndRegister();

        URANIUM_OXIDE = new Material.Builder(PhoenixFission.id("uranium_oxide"))
                .fluid().color(0x00FF00).secondaryColor(0x000000).iconSet(MaterialIconSet.DULL).buildAndRegister();

        INERT_GAS_WASTE = new Material.Builder(PhoenixFission.id("inert_gas_waste"))
                .gas().color(0xC0C0C0).secondaryColor(0xD0D0D0).iconSet(MaterialIconSet.DULL).buildAndRegister();

        IRRADIATED_THORIUM = new Material.Builder(PhoenixFission.id("irradiated_thorium"))
                .ingot().fluid().color(0x90A090).secondaryColor(0x708070).element(GTElements.Th)
                .iconSet(MaterialIconSet.RADIOACTIVE).buildAndRegister();

        IRRADIATED_URANIUM_236 = new Material.Builder(PhoenixFission.id("irradiated_uranium_236"))
                .ingot().fluid().color(0x90A090).secondaryColor(0x708070).element(PhoenixElements.URANIUM_236)
                .iconSet(MaterialIconSet.RADIOACTIVE).buildAndRegister();

        SPENT_URANIUM_233 = new Material.Builder(PhoenixFission.id("spent_uranium_233"))
                .dust().ingot().color(0x503040).secondaryColor(0x705060).element(PhoenixElements.URANIUM_233)
                .iconSet(MaterialIconSet.RADIOACTIVE).buildAndRegister();

        SPENT_URANIUM_235 = new Material.Builder(PhoenixFission.id("spent_uranium_235"))
                .ingot().color(0x603030).secondaryColor(0x402020).element(GTElements.U235)
                .iconSet(MaterialIconSet.RADIOACTIVE).buildAndRegister();

        SPENT_URANIUM_235 = new Material.Builder(PhoenixFission.id("spent_uranium_236"))
                .ingot()
                .color(0x8B8878)
                .secondaryColor(0x556B2F)
                .element(PhoenixElements.URANIUM_236)
                .iconSet(MaterialIconSet.RADIOACTIVE).buildAndRegister();

        DEPLETED_URANIUM = new Material.Builder(PhoenixFission.id("depleted_uranium"))
                .ingot().color(0x555030).secondaryColor(0x333010).element(GTElements.U238)
                .iconSet(MaterialIconSet.RADIOACTIVE).buildAndRegister();

        DEPLETED_THORIUM = new Material.Builder(PhoenixFission.id("depleted_thorium"))
                .ingot()
                .color(0x505050)
                .secondaryColor(0x707070)
                .element(GTElements.Th)
                .iconSet(MaterialIconSet.RADIOACTIVE).buildAndRegister();

        DEPLETED_PLUTONIUM_241 = new Material.Builder(PhoenixFission.id("depleted_plutonium_241"))
                .ingot()
                .color(0x2F4F4F)
                .secondaryColor(0x696969)
                .element(GTElements.Pu241)
                .iconSet(MaterialIconSet.RADIOACTIVE).buildAndRegister();
    }
}
