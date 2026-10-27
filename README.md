# Setworks

Setworks is an equipment-set bonus system for Minecraft 1.12.2 on Forge; it also runs on Cleanroom. Pack authors define sets in ZenScript, mods define them through a Java API, and players see their sets and bonuses in item tooltips. Setworks ships no sets of its own: with the default configuration it changes nothing.

- Player and pack-author copy: [MOD-PAGE.md](MOD-PAGE.md)
- Scripting reference for pack authors: [SCRIPTING.md](SCRIPTING.md)
- Release history: [CHANGELOG.md](CHANGELOG.md)
- License: MIT, see [LICENSE](LICENSE); the build scripts derive from CleanroomMC ForgeDevEnv, see [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md)

## Identity and install side

- Mod id `setworks`, version 1.0.0. Required on the server and on every client: the server decides which bonuses apply, the client only displays what the server sends.
- Optional integrations: [CraftTweaker](https://www.curseforge.com/minecraft/mc-mods/crafttweaker) 4.1.20 or later for ZenScript definitions, [Baubles](https://www.curseforge.com/minecraft/mc-mods/baubles) 1.5.2 or later for the Baubles slot locations. Neither is required; the mod declares `after:` relations only.

## What a definition is

- A **set** is a list of named **pieces**. Each piece accepts one or more items (an item id, optionally with metadata, required or absent NBT paths, or exact NBT; or an ore dictionary name) at one or more **locations**: `head`, `chest`, `legs`, `feet`, `mainhand`, `offhand`, `hotbar`, `inventory`, `slot:<index>`, the six `baubles:*` locations when Baubles is present, or a location an addon registers.
- A **bonus** requires a piece count of one or more sets (or all pieces), may carry **conditions** (a built-in kind such as `setworks:health_below` or `setworks:sneaking`, a script function, or an addon kind), and carries zero or more **effects**: the built-in attribute modifier and potion effect, or an addon kind. A bonus without effects is a **scripted bonus**; scripts and mods read its state through queries and events.
- A bonus has a **visibility mode**: `visible`, `discovered` (shown to a player once they had it active; remembered per player), or `hidden`.
- A set has a threshold mode: cumulative (every reached threshold applies) or highest-only.

## ZenScript (CraftTweaker)

```zenscript
#loader setworks
import mods.setworks.Setworks;

Setworks.armorSet("mypack:gold_armor", "Golden Armor",
    <minecraft:golden_helmet>, <minecraft:golden_chestplate>, <minecraft:golden_leggings>, <minecraft:golden_boots>);
Setworks.bonus("mypack:gold_glow", "mypack:gold_armor", 4, "Golden Glow: +2 armor and Night Vision")
    .attribute("generic.armor", 2, "add")
    .potion("minecraft:night_vision", 1);
```

- Files tagged `#loader setworks` run at load complete, after every mod registered its items. Files without the tag run in the default CraftTweaker loader; their Setworks calls are queued and applied at the same moment.
- Entry `mods.setworks.Setworks`: `set`, `armorSet`, `weaponSet`, `bonus`, `fullSetBonus`, `replaceSet`, `replaceBonus`, `disableSet`, `disableBonus`, `modifyBonus`, `addAlternatives`. The returned handles add pieces, requirements, descriptions, visibility, conditions (`.condition(kind, params)`, `.when(function(player as IPlayer) as bool {...})`), and effects (`.attribute`, `.potion`, `.effect`).
- Queries on `IPlayer`: `setworksPieceCount`, `hasSetworksBonus`, `setworksBonusState`, `setworksSatisfiedPieces`; on `IItemStack`: `setworksSets`. Events: `events.onSetworksBonusActivated` and `onSetworksBonusDeactivated`.
- Every call, with examples: [SCRIPTING.md](SCRIPTING.md). The `examples/` folder holds the migration of a CTSetBonus-style set (`setworks_iron.zs`), the two-statement form above (`setworks_helpers.zs`), and three `.disabled` samples (an override script for the default loader, a dev script with a throwing function and event handlers, a parse-error sample).

## Java API

Package `com.mahghuuuls.setworks.api`. A mod declares `after:setworks` and registers during startup, before load complete, when the definitions freeze:

- `SetworksApi`: `registerSet`, `registerBonus` (with `SetBuilder`, `BonusBuilder`, `PieceBuilder`, `ItemFilter`), `replaceSet`, `replaceBonus`, `disableSet`, `disableBonus`, `modifyBonus`, `addAlternatives`; addon registration of effect kinds (`IEffectKind`), condition kinds (`IConditionKind`), and locations (`ILocation`); `query()` (`ISetworksQuery`); `addBonusListener` (`BonusListener`).
- Forge bus events `BonusActivatedEvent` and `BonusDeactivatedEvent` on the server.
- Registration results report problems; invalid definitions disable their set at the freeze and are listed by `/setworks validate`.

## Commands

For operators (permission level 2):

- `/setworks list`: every set, bonus, and location with its source.
- `/setworks validate`: the validation report (definition errors, disabled sets and bonuses, script failures).
- `/setworks inspect <player>`: the player's sets, satisfied pieces, bonus states with reasons, and applied effects.

## Configuration

`config/setworks.cfg`. Every key needs a restart.

- `general.strictValidation` (default `false`): stop the game from starting when any definition is invalid.
- `general.conditionIntervalTicks` (default `20`): how often conditions are re-evaluated while equipment did not change; a bonus may set its own interval.
- `general.enableExampleSets` (default `false`): register the vanilla-item example sets.
- `general.diagnosticLog` (default `false`): a load summary, one line per bonus state change, and a timing summary every 1200 ticks in the game log.
- `client.showTooltips` (default `true`): the set block in item tooltips on this client.

## Scope boundaries

- Definitions are collected at startup and frozen at load complete. There is no reload; a script change needs a restart.
- A parse error in a `#loader setworks` file makes CraftTweaker skip the loader's later files; `/setworks validate` names the file, the line, and the skipped files.
- Attribute modifiers are re-applied at login, respawn, and dimension change and are never written to the player file; set potions are kept topped up while the bonus is active.
- Discovery (the `discovered` visibility mode) is saved per player with the player's data.
- Not included: a JEI plugin, other slot mods (an addon can register their locations), a reload command.

## Building

Gradle wrapper; Java 25 builds the project and the output is Java 8 bytecode.

```text
./gradlew build
```

`runClient` and `runServer` include CraftTweaker and Baubles through the `setworks_dev_crafttweaker` and `setworks_dev_baubles` properties in `gradle.properties`; set one to `false` for a development launch without that mod. The shipped jar never contains either.
