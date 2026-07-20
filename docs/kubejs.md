# Extending Phoenix's Fission (KubeJS & Java)

This doc covers everything an addon/pack author needs to hook into Phoenix's Fission:

1. [Adding new fission components (KubeJS)](#1-adding-new-fission-components-kubejs)
2. [Building a new reactor (Java)](#2-building-a-new-reactor-java)
3. [Reusing our reactor machine classes from KubeJS](#3-reusing-our-reactor-machine-classes-from-kubejs)
4. [Fission pattern predicates](#4-fission-pattern-predicates)
5. [EMI integration](#5-emi-integration)

---

## 1. Adding New Fission Components (KubeJS)

You can register new Fuel Rods, Coolers, Moderators, Blankets, and MSR Core Liners in your
`startup_scripts`. Every one of these is a real block, backed by an entry in a `PhoenixAPI` registry
map (`FISSION_FUEL_RODS`, `FISSION_COOLERS`, `FISSION_MODERATORS`, `FISSION_BLANKETS`, `MSR_LINERS`),
and every one of them automatically gets its own EMI info page — see [section 5](#5-emi-integration).

Below is an example of each type.

```js
StartupEvents.registry("block", event => {

    // 1. CUSTOM FISSION COOLER
    event.create('aether_flow_cooler', 'phoenix_fission:fission_cooler')
        .displayName('Aether-Flow Cooler')
        .tier(3)                                 // Determines explosion scaling and tier-matching logic
        .coolerTemperature(1025)                // Active heat dissipation capacity (HU/t)
        .coolantUsagePerTick(10)                // Millibuckets of coolant fluid consumed per operation tick
        .requiredCoolantMaterialId('phoenix_fission:frost')  // Input fluid registry ID
        .outputCoolantFluidId('phoenix_fission:warm_frost')  // Output heated fluid registry ID
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
```

That's it — no other registration step is needed for components. Each builder puts its type into the
matching `PhoenixAPI` map on `createObject()`, and everything downstream (multiblock pattern matching,
EMI pages, workstation lookups) reads from those maps automatically.

---

## 2. Building a New Reactor (Java)

Unlike components, a whole new **reactor multiblock** can't be defined from KubeJS alone — KubeJS can't
subclass a Java `MetaMachine`. If you want a genuinely new reactor with its own machine logic, you need
a Java-side addon mod. (If you just want a new *pattern shape* using logic we already wrote, see
[section 3](#3-reusing-our-reactor-machine-classes-from-kubejs) instead — that path doesn't need Java.)

### 2.1 Pick a base class

All of our reactors live in `net.phoenix.phoenix_fission.common.data.multiblock.fission` and form this
hierarchy:

```
FissionWorkableElectricMultiblockMachine   (heat/coolant simulation, meltdown handling, EMI baseline)
├── MoltenSaltReactorMultiblockMachine     (fluid-fuel MSR: coolers + core liners)
└── DynamicFissionReactorMachine           (solid-fuel reactor: fuel rods + moderators/blankets)
    └── BreederWorkableElectricMultiblockMachine  (adds breeder-blanket transmutation)
```

Extend whichever one already has the behavior closest to what you want, and override only what differs.
Extending `FissionWorkableElectricMultiblockMachine` directly gets you heat/coolant/meltdown handling for
free with no assumptions about fuel rods, moderators, or blankets.

### 2.2 Register it like our own reactors

Register your multiblock the normal Registrate way — this mirrors `PhoenixFissionMachines.java`:

```java
public static MultiblockMachineDefinition MY_CUSTOM_REACTOR = PHOENIX_REGISTRATE // or your own Registrate instance
        .multiblock("my_custom_reactor", MyCustomReactorMachine::new)
        .langValue("My Custom Reactor")
        .recipeType(PhoenixRecipeTypes.PRESSURIZED_FISSION_REACTOR_RECIPES)
        .generator(true)
        .pattern(definition -> FactoryBlockPattern.start()
                // ... your aisles ...
                .where("K", PhoenixFissionPredicates.fissionCoolers())
                .where("F", PhoenixFissionPredicates.fissionFuelRods())
                .where("M", PhoenixFissionPredicates.fissionModerators().or(PhoenixFissionPredicates.fissionBlankets()))
                .build())
        .register();
```

### 2.3 Register it with our EMI/workstation system

Call `PhoenixFissionMachines.registerFissionReactor(...)` right after `.register()`:

```java
PhoenixFissionMachines.registerFissionReactor(MY_CUSTOM_REACTOR, MyCustomReactorMachine.class);
```

This adds your reactor to `PhoenixFissionMachines.ALL_FISSION_REACTORS`, a list of
`(MultiblockMachineDefinition, machineClass)` pairs. Our EMI plugin walks that list and decides which
EMI categories your reactor becomes a workstation for **purely by `instanceof`-checking `machineClass`
against the base classes above** — it never hardcodes reactor names. Concretely:

| Your machine class extends...                     | Becomes a workstation for                       |
|-----------------------------------------------------|--------------------------------------------------|
| `FissionWorkableElectricMultiblockMachine` (any)     | Coolant Cycle                                     |
| `MoltenSaltReactorMultiblockMachine`                 | + MSR Core Liner                                  |
| `DynamicFissionReactorMachine` (or `BreederWorkableElectricMultiblockMachine`) | + Fuel Burn, Moderator, Blanket |

So you don't need to touch `PhoenixFissionEmiPlugin.java` at all — picking the right base class (or the
closest one) is enough for your reactor to show up in the right EMI pages, and this keeps working even
if a pack disables all of our built-in reactors via config, since every entry in `ALL_FISSION_REACTORS`
is independent.

---

## 3. Reusing Our Reactor Machine Classes from KubeJS

If GTCEu's own KubeJS multiblock tooling lets you define a brand-new multiblock (new pattern, new id)
but point its machine supplier at an **existing Java class**, you can reuse one of our reactor machine
classes directly — no new Java code required. In that case, just call the same registration hook from
your script so it's recognized as a workstation.

`PhoenixFissionMachines`, `FissionWorkableElectricMultiblockMachine`, `DynamicFissionReactorMachine`,
`BreederWorkableElectricMultiblockMachine`, and `MoltenSaltReactorMultiblockMachine` are all bound as
KubeJS globals (see `PhoenixKubeJSPlugin.registerBindings`), so you can reference any of them directly
with no import:

```js
StartupEvents.postInit(() => {
    // myDefinition = whatever MultiblockMachineDefinition your multiblock builder returned
    PhoenixFissionMachines.registerFissionReactor(myDefinition, MoltenSaltReactorMultiblockMachine.class)
})
```

Anything else under `net.phoenix.phoenix_fission` that isn't explicitly bound is still reachable via
`Java.loadClass(...)`, since the plugin's `ClassFilter` allows scripts into the whole package.

Use whichever base class from the table in [section 2.3](#23-register-it-with-our-emiworkstation-system)
matches the behavior you reused, and your reactor gets classified into the right EMI categories exactly
like a Java-registered one would.

---

## 4. Fission Pattern Predicates

`net.phoenix.phoenix_fission.api.pattern.PhoenixFissionPredicates` provides the `where(...)` predicates
used to accept our component blocks in a multiblock pattern. It's bound as a KubeJS global too, so
`PhoenixFissionPredicates.fissionCoolers()` works directly from a script with no import. Use these
instead of hand-rolling your own `blocks(...)` checks — they also populate match-context lists that our machine managers
(`FissionComponentManager`, `FissionThermalManager`, etc.) read to know which component *types* actually
formed, which drives heat/EU/cooling simulation:

| Predicate                              | Accepts                                   | Match-context key(s) populated      |
|-----------------------------------------|--------------------------------------------|----------------------------------------|
| `PhoenixFissionPredicates.fissionCoolers()`   | Any registered `FISSION_COOLERS` block      | `CoolerTypes`                          |
| `PhoenixFissionPredicates.fissionFuelRods()`  | Any registered `FISSION_FUEL_RODS` block    | `FuelRodTypes`                         |
| `PhoenixFissionPredicates.fissionModerators()`| Any registered `FISSION_MODERATORS` block   | `ModeratorTypes`                       |
| `PhoenixFissionPredicates.fissionBlankets()`  | Any registered `FISSION_BLANKETS` block     | `BlanketTypes`                         |
| `PhoenixFissionPredicates.msrCoreLiner()`     | Any registered `MSR_LINERS` block           | `LinerTypes`, `MSRLinerCount`, `MSRMinTier` |

All five behave the same way: they check the current block against every entry in the matching
`PhoenixAPI` map, and — like GTCEu's own `Predicates.blocks(...)` — can be `.or()`'d together (e.g.
`fissionModerators().or(fissionBlankets())` to accept either type in the same slot, as both our
Pressurized and Breeder reactors do).

**Empty-map safety:** every predicate has a fallback so pattern preview/EMI never crashes if no fission
blocks of that type are registered yet (e.g. a pack removed the KubeJS scripts, or hasn't loaded them
yet). Instead of showing nothing, the preview falls back to
`PhoenixFissionBlocks.EMPTY_REACTOR_COMPONENT` — a plain placeholder block dedicated to this purpose, so
you'll never see a `NullPointerException`/`ArrayIndexOutOfBounds` from an empty preview array. You don't
need to do anything to get this — it's automatic for all five predicates.

---

## 5. EMI Integration

`net.phoenix.phoenix_fission.integration.emi.PhoenixFissionEmiPlugin` registers five EMI categories:

| Category           | Backed by                     | Lang key                                      |
|----------------------|---------------------------------|--------------------------------------------------|
| Fission Fuel Burn    | `PhoenixAPI.FISSION_FUEL_RODS`  | `emi.category.phoenix_fission.fission_fuel_burn`    |
| Fission Coolant Cycle| `PhoenixAPI.FISSION_COOLERS`    | `emi.category.phoenix_fission.fission_coolant_cycle`|
| Fission Moderator    | `PhoenixAPI.FISSION_MODERATORS` | `emi.category.phoenix_fission.fission_moderator`    |
| Breeder Blanket      | `PhoenixAPI.FISSION_BLANKETS`   | `emi.category.phoenix_fission.fission_blanket`      |
| MSR Core Liner       | `PhoenixAPI.MSR_LINERS`         | `emi.category.phoenix_fission.fission_msr_liner`    |

**Components are automatic.** Every component you register (section 1) is looped over when the plugin
registers, and gets its own EMI recipe/info page (`FissionFuelBurnEmiRecipe`, `FissionCoolantCycleEmiRecipe`,
`FissionModeratorEmiRecipe`, `FissionBlanketEmiRecipe`, `FissionMsrLinerEmiRecipe`) built purely from the
type's own getters (tier, heat, duration, etc.) — you never write EMI code for a new component.

**Reactors need one call.** A reactor only becomes a clickable *workstation* for these categories
(so players can jump straight to "what fuel does this reactor take?" from the machine itself) if it's
been added via `PhoenixFissionMachines.registerFissionReactor(...)` — see [section 2.3](#23-register-it-with-our-emiworkstation-system).
Component blocks themselves (fuel rods, coolers, etc.) are also automatically registered as workstations
for their own category, so clicking a fuel rod in EMI opens the Fuel Burn page too.

**If you add a new component or reactor type from an addon mod** and want it to show up with a translated
category/name instead of raw keys, make sure your lang file (or KubeJS `displayName(...)`, which already
handles this for you) provides the matching key — component display names come from `displayName(...)`
directly, but if you ever add a brand-new *category* (not just a new component within an existing one),
remember to add its `emi.category.<namespace>.<path>` lang key yourself, the same way
`PhoenixMachineLangHandler.java` does for ours.
