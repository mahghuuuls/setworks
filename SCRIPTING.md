# Setworks scripting reference

Everything a pack author can do with Setworks from ZenScript, with CraftTweaker 4.1.20 or later. The examples in `examples/` show the same calls in working files.

## Loading

- Put the file in `scripts/` with `#loader setworks` as its first line. Such a file runs at load complete, after every mod registered its items, so every item id you name exists by then.
- A file without the tag runs in CraftTweaker's default loader. Its `Setworks` calls are queued and applied at the same moment, so both forms work. The tagged form keeps your set files apart from your recipe files: a parse error in a recipe file cannot skip them.
- A parse error in a `#loader setworks` file makes CraftTweaker skip the loader's later files. `/setworks validate` names the file, the line, and the skipped files.
- Definitions are frozen at load complete. A changed script needs a game restart.
- Every rejected definition is reported with its script file and line in the game log, in `crafttweaker.log`, and by `/setworks validate`. An invalid set is disabled together with its bonuses; the game still starts unless `strictValidation` is on.

```zenscript
#loader setworks
import mods.setworks.Setworks;
```

## Ids and text

- Ids are `namespace:name` in lowercase letters, digits, and underscores, for example `mypack:iron_armor`. Use your pack's namespace. A set and a bonus may share a name.
- Display names and descriptions are plain text or a language key from a resource pack. Inside a text, `${some.key}` is replaced by that key's translation.

## Sets

A set is a list of named pieces. A player satisfies a piece by having a matching item in one of the piece's locations.

```zenscript
Setworks.set("mypack:ranger")
    .name("Ranger Gear")
    .piece("hood", "head", <minecraft:leather_helmet>)
    .piece("bow", "mainhand", <minecraft:bow>)
    .pieceAny("quiver", "offhand", [<minecraft:arrow>, <minecraft:spectral_arrow>])
    .pieceAt("charm", ["hotbar", "inventory"], [<ore:gemEmerald>]);
```

- `Setworks.set(id)` starts a set. `.name(text)` sets the display name (default: the id).
- `.piece(name, location, item)`: one piece, one location, one item pattern.
- `.pieceAny(name, location, items)`: one location, any of the listed items.
- `.pieceAt(name, locations, items)`: any of the locations, any of the items.
- `.armor(head, chest, legs, feet)`: the four armor pieces at the armor locations.
- `.highestOnly()` or `.thresholdMode("highest_only")`: only the highest reached threshold applies its bonuses. The default is `cumulative`: every reached threshold applies.

Helpers:

```zenscript
Setworks.armorSet("mypack:iron_armor", "Iron Armor",
    <minecraft:iron_helmet>, <minecraft:iron_chestplate>, <minecraft:iron_leggings>, <minecraft:iron_boots>);
Setworks.weaponSet("mypack:iron_weapons", "Iron Weapons", [<minecraft:iron_sword>, <minecraft:iron_axe>]);
```

`weaponSet` makes a one-piece set with the piece `weapon` in the main hand.

### Locations

`head`, `chest`, `legs`, `feet`, `mainhand`, `offhand`, `hotbar` (the nine hotbar slots), `inventory` (the 27 main inventory slots), `slot:0` to `slot:35` (one inventory slot), and with Baubles installed `baubles:amulet`, `baubles:ring` (both ring slots), `baubles:belt`, `baubles:head`, `baubles:body`, `baubles:charm`. A mod may register more; `/setworks list` prints every location with its source.

A piece counts only in a location it lists. An item in the main hand does not count for a `hotbar` piece and the other way round.

### Item patterns

- `<minecraft:iron_sword>`: the item, any damage.
- `<minecraft:wool:5>`: the item with that metadata. For an item with sub-types the metadata counts; for a damageable item it is ignored, so a worn piece still counts.
- `<minecraft:wool:*>`: the item with any metadata.
- `<ore:ingotIron>`: any item registered under that ore dictionary name.
- `<minecraft:iron_sword>.withTag({display: {Name: "Excalibur"}})`: the item carrying every listed tag, with more tags allowed.
- `<a> | <b>`: either item. The list is expanded when the script runs.

Not accepted: `<*>`, amounts, and conditions or transformers on an ingredient. Such a call fails with a message naming the piece.

## Bonuses

A bonus needs a piece count of one or more sets. It may carry effects, conditions, and a visibility mode. A bonus with no effects is a scripted bonus: Setworks applies nothing for it, your script reads its state.

```zenscript
Setworks.bonus("mypack:iron_resolve", "mypack:iron_armor", 2, "Iron Resolve: +2 armor")
    .attribute("generic.armor", 2, "add");

Setworks.fullSetBonus("mypack:iron_wall", "mypack:iron_armor", "Iron Wall: +4 max health and Speed I")
    .attribute("generic.maxHealth", 4, "add")
    .potion("minecraft:speed", 1);

Setworks.bonus("mypack:iron_strike")
    .requiresAll("mypack:iron_armor")
    .requires("mypack:iron_weapons", 1)
    .description("Iron Strike: +2 attack damage while holding an iron weapon")
    .displayUnder("mypack:iron_armor")
    .attribute("generic.attackDamage", 2, "add");
```

- `Setworks.bonus(id)` starts a bonus; `Setworks.bonus(id, setId, threshold, description)` adds the first requirement and the text; `Setworks.fullSetBonus(id, setId, description)` requires every piece of the set.
- `.requires(setId, count)`: at least that many pieces of the set. `.requiresAll(setId)`: every piece. Call them more than once for a bonus that needs several sets.
- `.description(text)`: the tooltip line. Required for a scripted bonus; a bonus with effects gets a generated line (for example `+2 Armor, Speed I`) when you give none.
- `.displayUnder(setId)` or `.displayUnder([ids])`: for a multi-set bonus, the sets whose tooltip block shows it. Default: every required set.
- `.visibility("visible")` (default), `.discovered()` or `.visibility("discovered")` (shown to a player once they had it active; remembered per player, also after death and reconnects), `.hidden()` or `.visibility("hidden")` (never shown).
- `.conditionInterval(ticks)`: how often this bonus's conditions are rechecked while equipment did not change. Default: the `conditionIntervalTicks` setting, 20 ticks.

In the tooltip a bonus line is green once the player wears the pieces it needs, whatever its conditions say; the description tells the player the condition.

### Effects

- `.attribute(name, amount, operation)`: an attribute modifier while the bonus is active. Names: `generic.maxHealth`, `generic.armor`, `generic.armorToughness`, `generic.attackDamage`, `generic.attackSpeed`, `generic.movementSpeed`, `generic.knockbackResistance`, `generic.luck`, `generic.reachDistance`, `forge.swimSpeed`, or an attribute another mod adds under its own prefix. Operations: `add`, `multiply_base`, `multiply_total` (the vanilla operations 0, 1, 2). A raised max health never heals; a lowered one clamps.
- `.potion(id, level)` or `.potion(id, level, true)` for particles: a potion effect kept on the player while the bonus is active, removed when it ends. Level 1 is the first level. The highest level among the player's set potions of one type wins; a potion the player drank is kept.
- `.effect(kindId, {key: "value"})`: an effect kind a mod registered, with its parameters as a map of strings.
- Modifiers are re-applied at login, respawn, and dimension change and are never saved into the player file.

### Conditions

```zenscript
Setworks.bonus("mypack:last_stand", "mypack:iron_armor", 2, "Last Stand: +4 attack damage below half health")
    .condition("setworks:health_below", {fraction: "0.5"})
    .attribute("generic.attackDamage", 4, "add");

Setworks.bonus("mypack:iron_stance", "mypack:iron_armor", 2, "Iron Stance: no knockback while sneaking")
    .condition("setworks:sneaking")
    .attribute("generic.knockbackResistance", 1, "add");

Setworks.bonus("mypack:iron_steady", "mypack:iron_armor", 2, "Iron Steady: +1 armor while at full health")
    .when(function(player as IPlayer) as bool {
        return player.health >= player.maxHealth;
    })
    .attribute("generic.armor", 1, "add");
```

- `.condition(kindId)` or `.condition(kindId, {params})`: a registered condition kind. Built in: `setworks:health_below` with `fraction` (above 0, at most 1) and `setworks:sneaking`. A mod may register more.
- `.when(function)` or `.whenLabelled(label, function)`: a script function `function(player as IPlayer) as bool`. It runs on the server, right after the player's equipment changed and otherwise every condition interval. Never on the client, never from a tooltip or a query. Keep it free of side effects.
- Every condition of a bonus must pass. A function that throws makes its bonus inactive with the reason `condition error`; the error is logged once, and the other bonuses keep working.
- Modifiers are applied right after an evaluation. A function that reads a value the set itself changes, for example full health next to a bonus that raises max health, sees the raised maximum from the next check on.

## Changing what other sources defined

Sets and bonuses that a mod, another script, or the Setworks examples registered can be changed from your script.

```zenscript
Setworks.replaceSet("setworks:iron_armor")
    .name("Iron Armor (changed)")
    .armor(<minecraft:iron_helmet>, <minecraft:iron_chestplate>, <minecraft:iron_leggings>, <minecraft:iron_boots>);
Setworks.replaceBonus("othermod:their_bonus").requires("othermod:their_set", 3).description("Needs three now").attribute("generic.armor", 1, "add");
Setworks.disableSet("setworks:wayfinder");
Setworks.disableBonus("othermod:too_strong");
Setworks.modifyBonus("setworks:iron_resolve").description("Iron Resolve: +2 armor (changed by a script)").hidden();
Setworks.addAlternatives("setworks:mixed_mail", "feet", [<minecraft:golden_boots>]);
```

- `replaceSet(id)` and `replaceBonus(id)` define a set or bonus that takes the place of an existing one with the same id. Registering an id twice without `replace` is rejected, and the first definition stays.
- `disableSet(id)` disables a set and every bonus that requires it. `disableBonus(id)` disables one bonus.
- `modifyBonus(id)` changes the description, the visibility (`.visibility(mode)`, `.hidden()`), or the display sets (`.displayUnder([ids])`) of an existing bonus and keeps the rest.
- `addAlternatives(setId, pieceName, items)` adds items to an existing piece.
- Changes are applied after every registration, whichever file came first.

## Queries

On a player:

```zenscript
player.setworksPieceCount("mypack:iron_armor")      // 0 to the piece count
player.hasSetworksBonus("mypack:iron_resolve")       // true while the bonus is active
player.setworksBonusState("mypack:iron_resolve")     // "active", "missing equipment", "condition failed", "condition error", "superseded", "spectator", "dead"
player.setworksSatisfiedPieces("mypack:iron_armor")  // piece name -> slot, as a string map
```

On an item stack: `stack.setworksSets` gives the ids of every enabled set a piece of which accepts the stack.

The same calls exist as `Setworks.pieceCount(player, id)`, `Setworks.isActive(player, id)`, `Setworks.bonusState(player, id)`, `Setworks.satisfiedPieces(player, id)`, and `Setworks.setsOf(stack)`. A query reads the last evaluated state and never evaluates, applies, or withdraws anything. On the client it answers from the synced state.

## Events

```zenscript
import mods.setworks.BonusEvent;

events.onSetworksBonusActivated(function(event as BonusEvent) {
    print(event.bonusId + " on for " + event.player.name);
});
events.onSetworksBonusDeactivated(function(event as BonusEvent) {
    print(event.bonusId + " off for " + event.player.name + ": " + event.reason + " " + event.detail);
});
```

- `event.player` (IPlayer), `event.bonusId`, `event.setIds` (the sets the bonus requires), `event.active`, `event.reason` (the state words above), `event.detail` (for example the missing set and count, or the failing condition).
- Fired on the server only. After a respawn or dimension change the state is rebuilt, so an activation may arrive without a deactivation before it.
- Setworks itself sends no chat line, sound, or particle on a transition; your handler may.

## Scripted bonuses

A bonus without effects carries only the requirements, the description, and the state. Combine it with a query in a CraftTweaker event to give it behavior:

```zenscript
import crafttweaker.player.IPlayer;
import crafttweaker.entity.IEntity;
import crafttweaker.event.EntityLivingHurtEvent;

Setworks.bonus("mypack:iron_partial", "mypack:iron_armor", 2,
    "Your attacks deal 75% bonus damage to enemies at full health");

events.onEntityLivingHurt(function(event as EntityLivingHurtEvent) {
    val attackerEntity as IEntity = event.damageSource.getTrueSource();
    if (isNull(attackerEntity) || !(attackerEntity instanceof IPlayer)) {
        return;
    }
    val attacker as IPlayer = attackerEntity;
    if (attacker.hasSetworksBonus("mypack:iron_partial") && event.entityLivingBase.health >= event.entityLivingBase.maxHealth) {
        event.amount = event.amount * 1.75;
    }
});
```

A hidden scripted bonus is a convenient gate: it is never listed, but `hasSetworksBonus` answers for it.

## Checking your work

- `/setworks list`: every set, bonus, and location with the file and line it came from.
- `/setworks validate`: definition errors, disabled sets and bonuses, and script failures with their scope.
- `/setworks inspect <player>`: the player's sets, satisfied pieces, each bonus with its state and reason, and the effects applied.
- With `diagnosticLog=true` in `config/setworks.cfg`, the game log gets one line per bonus state change.

Operators need permission level 2 for the commands.
