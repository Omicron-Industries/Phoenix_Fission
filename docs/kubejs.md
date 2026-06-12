# KubeJS Integration

Phoenix's Fission provides a dedicated KubeJS plugin to add new reactor components easily.

## Adding Components
You can register new Fuel Rods, Coolers, Moderators, and Blankets in your `startup_scripts`.

Below is an example of each type.

StartupEvents.registry("block", event => {

    // 1. CUSTOM FISSION COOLER
    event.create('aether_flow_cooler', 'phoenix_fission:fission_cooler')
        .displayName('Aether-Flow Cooler')
        .tier(3)                                 // Determines explosion scaling and tier-matching logic
        .coolerTemperature(1025)                // Active heat dissipation capacity (HU/t)
        .coolantUsagePerTick(10)                // Millibuckets of coolant fluid consumed per operation tick
        .requiredCoolantMaterialId('phoenixcore:frost')  // Input fluid registry ID
        .outputCoolantFluidId('phoenixcore:warm_frost')  // Output heated fluid registry ID
        .tintColor(0xFF7DE7FF)                  // Does nothing but nessecary, leave default
        .texture('kubejs:block/fission/aether_flow_cooler')  // Note: Requires a matching block and active block texture (active is appended to the end)       
        .maskTexture('phoenix_fission:block/fission/cooler_mask'); // Optional mask texture layer

    // 2. CUSTOM FISSION FUEL ROD
    event.create('high_density_driver_rod', 'phoenix_fission:fission_fuel_rod')
        .displayName('High-Density Driver Rod')
        .tier(3)                                 // Controls internal multi-block priority and explosion tiers
        .baseHeatProduction(50)                 // Core HU/t heat produced before moderator modifiers
        .durationTicks(1200)                    // Lifetime in ticks before burning another cycle of fuel
        .amountPerCycle(1)                      // Quantity of fuel items consumed per cycle completion
        .neutronBias(1)                         // Interacts with nearby Breeder/Blanket rod output instability weights
        .fuelKey('gtceu:uranium_235_nugget')     // Input item ID (Supports tags or specific registry IDs)
        .outputKey('gtceu:depleted_uranium_235_nugget') // Output waste item ID
        .texture('kubejs:block/fission/high_density_driver_rod'); // Note: Requires a matching block and active block texture (active is appended to the end)



// 3. CUSTOM FISSION MODERATOR

    event.create('niobium_modified_silicon_carbide_moderator', 'phoenix_fission:fission_moderator')
        .displayName('Nb-SiC Moderator')
        .tier(4)                                 // Primary moderator selection hierarchy weight
        .euBoost(15)                            // Scaling factor for increasing EU generation output
        .fuelDiscount(5)                        // Efficiency discount: Extends fuel rod duration ticks between cycles
        .texture('kubejs:block/fission/niobium_sic_moderator');

    // 4. CUSTOM BREEDER / BLANKET ROD
    event.create('uranium_blanket_rod', 'phoenix_fission:fission_blanket_rod')
        .displayName('U-238 Blanket Rod')
        .tier(2)                                 // Priority logic tier matching
        .durationTicks(2400)                    // Total tick lifetime per transformation cycle
        .amountPerCycle(1)                      // Item consumption rate per cycle completion
        .inputKey('gtceu:uranium_238_nugget')    // Target breedable material registry entry
        // Dynamic Outputs: .addOutput(RegistryKey, Weight, Instability)
        .addOutput('gtceu:plutonium_nugget', 70, 1)    
        .addOutput('gtceu:plutonium_241_nugget', 20, 3) 
        .addOutput('gtceu:plutonium_238_nugget', 10, 4) // High instability means higher output if neutronBias matches
        .texture('kubejs:block/fission/uranium_blanket_rod');

    // 5. CUSTOM MOLTEN SALT REACTOR (MSR) LINER
    event.create('advanced_msr_liner', 'phoenix_fission:msr_core_liner')
        .displayName('Advanced MSR Core Liner')
        .tier(3)
        .fluidFlowRate(40)                      // Max mB/t fluid flow processing rate through the liner
        .heatPerMb(15.5)                        // Thermal energy generation constant per millibucket processed
        .inputFluidId('gtceu:enriched_naquadah_salt') // Molten salt fuel blend input
        .outputFluidId('gtceu:depleted_naquadah_salt')
        .texture('phoenix_fission:block/fission/liner_base'); // Also needs an active texture
});


