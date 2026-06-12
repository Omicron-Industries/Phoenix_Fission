package net.phoenix.phoenix_fission.configs;

import net.phoenix.phoenix_fission.PhoenixFission;

import dev.toma.configuration.Configuration;
import dev.toma.configuration.config.Config;
import dev.toma.configuration.config.ConfigHolder;
import dev.toma.configuration.config.Configurable;
import dev.toma.configuration.config.format.ConfigFormats;

@Config(id = PhoenixFission.MOD_ID)
public class PhoenixFissionConfigs {

    public static PhoenixFissionConfigs INSTANCE;
    public static ConfigHolder<PhoenixFissionConfigs> CONFIG_HOLDER;

    public static void init() {
        CONFIG_HOLDER = Configuration.registerConfig(PhoenixFissionConfigs.class, ConfigFormats.yaml());
        INSTANCE = CONFIG_HOLDER.getConfigInstance();
    }

    @Configurable
    public FissionConfigs fission = new FissionConfigs();

    @Configurable
    public FissionBlockStatsConfigs fissionStats = new FissionBlockStatsConfigs();

    public static class FissionConfigs {

        @Configurable
        @Configurable.Comment("Enable the nuke block.")
        public boolean nukeEnabled = true;

        @Configurable
        @Configurable.Comment("Enable the base fission reactor.")
        public boolean fissionReactorEnabled = true;

        @Configurable
        @Configurable.Comment("Enable the High Performance Breeder Reactor.")
        public boolean breederReactorEnabled = true;

        @Configurable
        @Configurable.Comment("Enable the Molten Salt Reactor.")
        public boolean msrEnabled = true;

        @Configurable
        @Configurable.Comment("Cube radius in blocks. Total affected volume is (2r+1)^3.")
        public int nukeCubeRadius = 16;

        @Configurable
        @Configurable.Comment("Hard cap to prevent insane values.")
        public int nukeCubeRadiusCap = 48;

        @Configurable
        @Configurable.Comment("Fuse time in ticks (20 ticks = 1 second). TNT is 80.")
        public int nukeFuseTicks = 120;

        @Configurable
        @Configurable.Comment("How many blocks to process per tick during cube wipe.")
        public int nukeBatchPerTick = 4000;

        @Configurable
        @Configurable.Comment("Skip blocks that have a BlockEntity (machines/chests).")
        public boolean nukeSkipBlockEntities = true;

        @Configurable
        @Configurable.Comment("Skip blocks in unloaded chunks (prevents chunk-forcing).")
        public boolean nukeSkipUnloadedChunks = true;

        @Configurable
        @Configurable.Comment("If true, replace removed blocks with fire instead of air.")
        public boolean nukeReplaceWithFire = false;

        @Configurable
        @Configurable.Comment("Flat heat added per tick while running (before rods/moderators/parallels).")
        public double baseHeatPerTick = 0.0;

        @Configurable
        @Configurable.Comment("Max bonus percent from continuous running (power + breeder output). Example: 60 = up to +60%.")
        public double burnBonusMaxPercent = 30.0;

        @Configurable
        @Configurable.Comment("Seconds of continuous running required to reach the max burn bonus. Example: 1200 = 20 minutes.")
        public double burnBonusRampSeconds = 1200.0;

        @Configurable
        @Configurable.Comment("The maximum heat a reactor can hold before starting the meltdown timer.")
        public double maxSafeHeat = 100000.0;

        @Configurable
        @Configurable.Comment("Minimum heat clamp.")
        public double minHeat = 0.0;

        @Configurable
        @Configurable.Comment("Maximum heat clamp to prevent runaway numeric overflow (meltdown still happens).")
        public double maxHeatClamp = 250000.0;

        @Configurable
        @Configurable.Comment("Does the reactor naturally lose heat when not running?")
        public boolean passiveCooling = true;

        @Configurable
        @Configurable.Comment("Heat lost per tick when idling.")
        public double idleHeatLoss = 1.0;

        @Configurable
        @Configurable.Comment("Base parallels added per fuel rod (before heat-based parallels).")
        public int parallelsPerFuelRod = 1;

        @Configurable
        @Configurable.Comment("How much heat is required to add +1 to the recipe parallel multiplier.")
        public double heatPerParallel = 10000.0;

        @Configurable
        @Configurable.Comment("Hard cap for parallels.")
        public int maxParallels = 256;

        @Configurable
        @Configurable.Comment("How much EU/t is generated per unit of CURRENT heat (power scales with current heat).")
        public double euPerHeatUnit = 1.0;

        @Configurable
        @Configurable.Comment("Optional cap on generated EU/t. Set <= 0 for no cap.")
        public long maxGeneratedEUt = 0;

        @Configurable
        @Configurable.Comment("If true, fuel usage scales with parallels (recommended).")
        public boolean fuelUsageScalesWithParallels = true;

        @Configurable
        @Configurable.Comment("If true, fuel usage scales with rod count (usually true).")
        public boolean fuelUsageScalesWithRodCount = true;

        @Configurable
        @Configurable.Comment("If true, blanket usage/output is additive across ALL blankets. If false, only the primary blanket is processed.")
        public boolean blanketUsageAdditive = true;

        @Configurable
        @Configurable.Comment("Clamp for total fuel discount percent from moderators.")
        public int maxFuelDiscountPercent = 90;

        @Configurable
        @Configurable.Comment("Clamp for total EU boost percent from moderators.")
        public int maxEUBoostPercent = 100;

        @Configurable
        @Configurable.Comment("If true, cooling only applies when coolant is present.")
        public boolean coolingRequiresCoolant = true;

        @Configurable
        @Configurable.Comment("If true, coolant usage is additive across all coolers. If false, uses primary cooler only.")
        public boolean coolantUsageAdditive = false;

        @Configurable
        @Configurable.Comment("Minimum EU/t produced while running (prevents 0). Set 0 to allow 0.")
        public long minGeneratedEUt = 1024;

        @Configurable
        @Configurable.Comment("Exponent for heat->power curve. 1 = linear, >1 rewards high heat.")
        public double powerCurveExponent = 2.0;

        @Configurable
        @Configurable.Comment("Heat fraction of maxSafeHeat where generation begins. 0.0 = always, 0.1 = starts at 10% of maxSafeHeat.")
        public double powerStartFraction = 0.0;

        @Configurable
        public MeltdownConfigs meltdown = new MeltdownConfigs();

        @Configurable
        public ExplosionConfigs explosion = new ExplosionConfigs();

        @Configurable
        @Configurable.Comment("If true, all fuel rods in the multiblock must be the same tier.")
        public boolean restrictFuelRodTier = true;

        @Configurable
        @Configurable.Comment("If true, all coolers in the multiblock must be the same tier.")
        public boolean restrictCoolerTier = true;

        @Configurable
        @Configurable.Comment("If >= 0, all fuel rods must be exactly this tier. Set to -1 to disable.")
        public int requiredFuelRodTier = -1;

        @Configurable
        @Configurable.Comment("If >= 0, all coolers must be exactly this tier. Set to -1 to disable.")
        public int requiredCoolerTier = -1;
    }

    public static class MeltdownConfigs {

        @Configurable
        @Configurable.Comment("Base grace seconds when barely above safe heat.")
        public double baseGraceSeconds = 60.0;

        @Configurable
        @Configurable.Comment("Minimum grace seconds when extremely above safe heat.")
        public double minGraceSeconds = 15.0;

        @Configurable
        @Configurable.Comment("Severity multiplier: higher = faster meltdown when over safe heat.")
        public double excessHeatSeverity = 1.0;

        @Configurable
        @Configurable.Comment("If true, falling back under safe heat clears the timer.")
        public boolean clearTimerWhenSafe = true;
    }

    public static class FissionBlockStatsConfigs {

        @Configurable
        @Configurable.Comment("Configuration for Fission Coolers")
        public CoolerConfigs coolers = new CoolerConfigs();

        @Configurable
        @Configurable.Comment("Configuration for Fission Moderators")
        public ModeratorConfigs moderators = new ModeratorConfigs();

        @Configurable
        @Configurable.Comment("Configuration for Fission Fuel Rods")
        public FuelRodConfigs fuelRods = new FuelRodConfigs();

        @Configurable
        @Configurable.Comment("Configuration for Molten Salt Reactor Liners")
        public MSRConfigs msrLiners = new MSRConfigs();

        public static class CoolerConfigs {
            @Configurable
            @Configurable.Comment("Enable the Basic cooler block registration.")
            public boolean enableBasicCooler = true;
            @Configurable
            @Configurable.Comment("Cooling power (HU/t) for the Basic cooler.")
            public int tempBasicCooler = 10000;
            @Configurable
            @Configurable.Comment("Coolant usage (mb/t) for the Basic cooler.")
            public int usageBasicCooler = 100;
            @Configurable
            @Configurable.Comment("Input coolant fluid ID for the Basic cooler.")
            public String inputFluidBasicCooler = "minecraft:water";
            @Configurable
            @Configurable.Comment("Output coolant fluid ID for the Basic cooler.")
            public String outputFluidBasicCooler = "phoenix_fission:critical_steam";

            @Configurable
            @Configurable.Comment("Enable the EV cooler block registration.")
            public boolean enableEVCooler = true;
            @Configurable
            @Configurable.Comment("Cooling power (HU/t) for the EV cooler.")
            public int tempEVCooler = 20000;
            @Configurable
            @Configurable.Comment("Coolant usage (mb/t) for the EV cooler.")
            public int usageEVCooler = 10;
            @Configurable
            @Configurable.Comment("Input coolant fluid ID for the EV cooler.")
            public String inputFluidEVCooler = "gtceu:sodium_potassium";
            @Configurable
            @Configurable.Comment("Output coolant fluid ID for the EV cooler.")
            public String outputFluidEVCooler = "phoenix_fission:hot_sodium_potassium";

            @Configurable
            @Configurable.Comment("Enable the IV cooler block registration.")
            public boolean enableIVCooler = true;
            @Configurable
            @Configurable.Comment("Cooling power (HU/t) for the IV cooler.")
            public int tempIVCooler = 30000;
            @Configurable
            @Configurable.Comment("Coolant usage (mb/t) for the IV cooler.")
            public int usageIVCooler = 30;
            @Configurable
            @Configurable.Comment("Input coolant fluid ID for the IV cooler.")
            public String inputFluidIVCooler = "gtceu:sodium_potassium";
            @Configurable
            @Configurable.Comment("Output coolant fluid ID for the IV cooler.")
            public String outputFluidIVCooler = "phoenix_fission:hot_sodium_potassium";

            @Configurable
            @Configurable.Comment("Enable the LuV cooler block registration.")
            public boolean enableLuVCooler = true;
            @Configurable
            @Configurable.Comment("Cooling power (HU/t) for the LuV cooler.")
            public int tempLuVCooler = 40000;
            @Configurable
            @Configurable.Comment("Coolant usage (mb/t) for the LuV cooler.")
            public int usageLuVCooler = 35;
            @Configurable
            @Configurable.Comment("Input coolant fluid ID for the LuV cooler.")
            public String inputFluidLuVCooler = "gtceu:liquid_helium";
            @Configurable
            @Configurable.Comment("Output coolant fluid ID for the LuV cooler.")
            public String outputFluidLuVCooler = "gtceu:helium";
        }

        public static class MSRConfigs {
            @Configurable
            @Configurable.Comment("Enable Graphite liner registration.")
            public boolean enableGraphiteLiner = true;
            @Configurable
            @Configurable.Comment("Tier for the Graphite liner.")
            public int tierGraphiteLiner = 1;
            @Configurable
            @Configurable.Comment("Flow Efficiency (mb/t) for the Graphite liner.")
            public int flowRateGraphiteLiner = 10;
            @Configurable
            @Configurable.Comment("Thermal Dissipation (Heat/mb) for the Graphite liner.")
            public double heatGraphiteLiner = 10.0;
            @Configurable
            @Configurable.Comment("Input fluid ID for the Graphite liner.")
            public String inputFluidGraphiteLiner = "phoenix_fission:u235_molten_salt";
            @Configurable
            @Configurable.Comment("Output fluid ID for the Graphite liner.")
            public String outputFluidGraphiteLiner = "phoenix_fission:depleted_u235_molten_salt";

            @Configurable
            @Configurable.Comment("Enable Hastelloy liner registration.")
            public boolean enableHastelloyLiner = true;
            @Configurable
            @Configurable.Comment("Tier for the Hastelloy liner.")
            public int tierHastelloyLiner = 2;
            @Configurable
            @Configurable.Comment("Flow Efficiency (mb/t) for the Hastelloy liner.")
            public int flowRateHastelloyLiner = 25;
            @Configurable
            @Configurable.Comment("Thermal Dissipation (Heat/mb) for the Hastelloy liner.")
            public double heatHastelloyLiner = 15.0;
            @Configurable
            @Configurable.Comment("Input fluid ID for the Hastelloy liner.")
            public String inputFluidHastelloyLiner = "phoenix_fission:thorium_u233_molten_salt";
            @Configurable
            @Configurable.Comment("Output fluid ID for the Hastelloy liner.")
            public String outputFluidHastelloyLiner = "phoenix_fission:depleted_thorium_molten_salt";

            @Configurable
            @Configurable.Comment("Enable Titanium liner registration.")
            public boolean enableTitaniumLiner = true;
            @Configurable
            @Configurable.Comment("Tier for the Titanium liner.")
            public int tierTitaniumLiner = 3;
            @Configurable
            @Configurable.Comment("Flow Efficiency (mb/t) for the Titanium liner.")
            public int flowRateTitaniumLiner = 50;
            @Configurable
            @Configurable.Comment("Thermal Dissipation (Heat/mb) for the Titanium liner.")
            public double heatTitaniumLiner = 25.0;
            @Configurable
            @Configurable.Comment("Input fluid ID for the Titanium liner.")
            public String inputFluidTitaniumLiner = "phoenix_fission:plutonium_molten_salt";
            @Configurable
            @Configurable.Comment("Output fluid ID for the Titanium liner.")
            public String outputFluidTitaniumLiner = "phoenix_fission:irradiated_actinide_waste";

            @Configurable
            @Configurable.Comment("Enable Netherite liner registration.")
            public boolean enableNetheriteLiner = true;
            @Configurable
            @Configurable.Comment("Tier for the Netherite liner.")
            public int tierNetheriteLiner = 4;
            @Configurable
            @Configurable.Comment("Flow Efficiency (mb/t) for the Netherite liner.")
            public int flowRateNetheriteLiner = 100;
            @Configurable
            @Configurable.Comment("Thermal Dissipation (Heat/mb) for the Netherite liner.")
            public double heatNetheriteLiner = 40.0;
            @Configurable
            @Configurable.Comment("Input fluid ID for the Netherite liner.")
            public String inputFluidNetheriteLiner = "phoenix_fission:californium_molten_salt";
            @Configurable
            @Configurable.Comment("Output fluid ID for the Netherite liner.")
            public String outputFluidNetheriteLiner = "phoenix_fission:transuranic_sludge_waste";
        }

        public static class ModeratorConfigs {
            @Configurable
            @Configurable.Comment("Enable Graphite moderator registration.")
            public boolean enableGraphiteModerator = true;
            @Configurable
            @Configurable.Comment("EU/t boost multiplier for the graphite moderator.")
            public int euBoostGraphiteModerator = 2;
            @Configurable
            @Configurable.Comment("Fuel Discount for the graphite moderator.")
            public int fuelDiscountGraphiteModerator = 1;
            @Configurable
            @Configurable.Comment("Tier for the graphite moderator.")
            public int tierGraphiteModerator = 1;

            @Configurable
            @Configurable.Comment("Enable Beryllium moderator registration.")
            public boolean enableBerylliumModerator = true;
            @Configurable
            @Configurable.Comment("EU/t boost multiplier for the beryllium moderator.")
            public int euBoostBerylliumModerator = 5;
            @Configurable
            @Configurable.Comment("Fuel Discount for the beryllium moderator.")
            public int fuelDiscountBerylliumModerator = 2;
            @Configurable
            @Configurable.Comment("Tier for the beryllium moderator.")
            public int tierBerylliumModerator = 2;

            @Configurable
            @Configurable.Comment("Enable Heavy Water moderator registration.")
            public boolean enableHeavyWaterModerator = true;
            @Configurable
            @Configurable.Comment("EU/t boost multiplier for the heavy water moderator.")
            public int euBoostHeavyWaterModerator = 12;
            @Configurable
            @Configurable.Comment("Fuel Discount for the heavy water moderator.")
            public int fuelDiscountHeavyWaterModerator = 5;
            @Configurable
            @Configurable.Comment("Tier for the heay water moderator.")
            public int tierHeavyWaterModerator = 3;

            @Configurable
            @Configurable.Comment("Enable Niobium SiC moderator registration.")
            public boolean enableNiobiumSicModerator = true;
            @Configurable
            @Configurable.Comment("EU/t boost multiplier for the niobium sic moderator.")
            public int euBoostNiobiumSicModerator = 30;
            @Configurable
            @Configurable.Comment("Fuel Discount for the niobium sic moderator.")
            public int fuelDiscountNiobiumSicModerator = 10;
            @Configurable
            @Configurable.Comment("Tier for the niobium sic moderator.")
            public int tierNiobiumSicModerator = 4;
        }

        public static class FuelRodConfigs {
            @Configurable
            @Configurable.Comment("Enable T1 fuel rod registration.")
            public boolean enableFuelRodT1 = true;
            @Configurable
            @Configurable.Comment("Base heat production for the T1 fuel rod.")
            public int heatProductionT1 = 50;
            @Configurable
            @Configurable.Comment("NeutronBias for the T1 fuel rod.")
            public int neutronBiasT1 = 0;
            @Configurable
            @Configurable.Comment("Fuel cycle duration in ticks for the T1 fuel rod.")
            public int cycleDurationT1 = 2500;
            @Configurable
            @Configurable.Comment("Fuel amount used per cycle for the T1 fuel rod.")
            public int cycleAmountT1 = 1;
            @Configurable
            @Configurable.Comment("Fuel used for the T1 fuel rod. (Registry name string)")
            public String fuelUsedT1 = "phoenix_fission:basic_fuel_rod";
            @Configurable
            @Configurable.Comment("Depleted Fuel produced for the T1 fuel rod. (Registry name string)")
            public String depletedGivenT1 = "phoenix_fission:low_level_radioactive_waste";

            @Configurable
            @Configurable.Comment("Enable T2 fuel rod registration.")
            public boolean enableFuelRodT2 = true;
            @Configurable
            @Configurable.Comment("Base heat production for the T2 fuel rod.")
            public int heatProductionT2 = 150;
            @Configurable
            @Configurable.Comment("NeutronBias for the T2 fuel rod.")
            public int neutronBiasT2 = 1;
            @Configurable
            @Configurable.Comment("Fuel cycle duration in ticks for the T2 fuel rod.")
            public int cycleDurationT2 = 3000;
            @Configurable
            @Configurable.Comment("Fuel amount used per cycle for the T2 fuel rod.")
            public int cycleAmountT2 = 1;
            @Configurable
            @Configurable.Comment("Fuel used for the T2 fuel rod. (Registry name string)")
            public String fuelUsedT2 = "phoenix_fission:basic_fuel_rod";
            @Configurable
            @Configurable.Comment("Depleted Fuel produced for the T2 fuel rod. (Registry name string)")
            public String depletedGivenT2 = "phoenix_fission:low_level_radioactive_waste";

            @Configurable
            @Configurable.Comment("Enable T3 fuel rod registration.")
            public boolean enableFuelRodT3 = true;
            @Configurable
            @Configurable.Comment("Base heat production for the T3 fuel rod.")
            public int heatProductionT3 = 500;
            @Configurable
            @Configurable.Comment("NeutronBias for the T3 fuel rod.")
            public int neutronBiasT3 = 5;
            @Configurable
            @Configurable.Comment("Fuel cycle duration in ticks for the T3 fuel rod.")
            public int cycleDurationT3 = 3500;
            @Configurable
            @Configurable.Comment("Fuel amount used per cycle for the T3 fuel rod.")
            public int cycleAmountT3 = 1;
            @Configurable
            @Configurable.Comment("Fuel used for the T3 fuel rod. (Registry name string)")
            public String fuelUsedT3 = "phoenix_fission:u235_fuel_pellet";
            @Configurable
            @Configurable.Comment("Depleted Fuel produced for the T3 fuel rod. (Registry name string)")
            public String depletedGivenT3 = "phoenix_fission:spent_uranium_235_nugget";

            @Configurable
            @Configurable.Comment("Enable T4 fuel rod registration.")
            public boolean enableFuelRodT4 = true;
            @Configurable
            @Configurable.Comment("Base heat production for the T4 fuel rod.")
            public int heatProductionT4 = 1200;
            @Configurable
            @Configurable.Comment("NeutronBias for the T4 fuel rod.")
            public int neutronBiasT4 = 12;
            @Configurable
            @Configurable.Comment("Fuel cycle duration in ticks for the T4 fuel rod.")
            public int cycleDurationT4 = 4000;
            @Configurable
            @Configurable.Comment("Fuel amount used per cycle for the T4 fuel rod.")
            public int cycleAmountT4 = 1;
            @Configurable
            @Configurable.Comment("Fuel used for the T4 fuel rod. (Registry name string)")
            public String fuelUsedT4 = "phoenix_fission:plutonium_241_fuel_pellet";
            @Configurable
            @Configurable.Comment("Depleted Fuel produced for the T4 fuel rod. (Registry name string)")
            public String depletedGivenT4 = "phoenix_fission:depleted_plutonium_241_nugget";

            @Configurable
            @Configurable.Comment("Enable T5 fuel rod registration.")
            public boolean enableFuelRodT5 = true;
            @Configurable
            @Configurable.Comment("Base heat production for the T5 fuel rod.")
            public int heatProductionT5 = 3000;
            @Configurable
            @Configurable.Comment("NeutronBias for the T5 fuel rod.")
            public int neutronBiasT5 = 30;
            @Configurable
            @Configurable.Comment("Fuel cycle duration in ticks for the T5 fuel rod.")
            public int cycleDurationT5 = 8000;
            @Configurable
            @Configurable.Comment("Fuel amount used per cycle for the T5 fuel rod.")
            public int cycleAmountT5 = 1;
            @Configurable
            @Configurable.Comment("Fuel used for the T5 fuel rod. (Registry name string)")
            public String fuelUsedT5 = "phoenix_fission:u242_fuel_pellet";
            @Configurable
            @Configurable.Comment("Depleted Fuel produced for the T5 fuel rod. (Registry name string)")
            public String depletedGivenT5 = "phoenix_fission:spent_uranium_242_nugget";
        }
    }

    public static class ExplosionConfigs {

        @Configurable
        @Configurable.Comment("If true, replaces blocks with air/fire. If false, uses standard GT explosion (drops items).")
        public boolean destructiveExplosion = false;

        @Configurable
        @Configurable.Comment("Explosion power scales with fuel rod count: power = base + rods * multiplier.")
        public double explosionPowerPerFuelRod = 1.5;

        @Configurable
        @Configurable.Comment("The base power of the meltdown explosion.")
        public float baseExplosionPower = 2.0f;

        @Configurable
        @Configurable.Comment("Max radius used for destructive bypass block wiping.")
        public int maxDestructiveRadius = 6;
    }
}
