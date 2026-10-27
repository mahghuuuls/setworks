Setworks adds set bonuses for equipment. A modpack or a mod defines sets of items and the bonuses for wearing or carrying enough of them. Setworks counts the pieces, applies the bonuses, and shows all of it in item tooltips. It ships no sets of its own, so it changes nothing until a pack or a mod defines some.

Similar results are possible with [Set Bonus](https://www.curseforge.com/minecraft/mc-mods/set-bonus) together with [CTSetBonus](https://github.com/mahghuuuls/ctsetbonus). Setworks puts what those two do into one mod, with scripts, an API, and the tooltips.

<span style="color:#d6a100">**AI usage disclaimer:** This mod was developed with AI-agent assistance using [this agent workflow](https://github.com/mahghuuuls/minecraft-1.12.2-mod-agent-workflow). The project owner reviewed the work during development.</span>

## For players

- An item that belongs to a set shows a block in its tooltip: the set name with how many pieces you have on, then one line per bonus with the piece count it needs. A bonus line turns green once you have the pieces.
- Hold Shift to see the named pieces of the set and which ones you have.
- Some bonuses stay unlisted until you have had them active once.

## For pack authors

Sets are defined in ZenScript with [CraftTweaker](https://www.curseforge.com/minecraft/mc-mods/crafttweaker). Put the file in `scripts/` with `#loader setworks` on its first line, so it runs after every mod registered its items.

```zenscript
#loader setworks
import mods.setworks.Setworks;

Setworks.armorSet("mypack:gold_armor", "Golden Armor",
    <minecraft:golden_helmet>, <minecraft:golden_chestplate>, <minecraft:golden_leggings>, <minecraft:golden_boots>);
Setworks.bonus("mypack:gold_glow", "mypack:gold_armor", 4, "Golden Glow: +2 armor and Night Vision")
    .attribute("generic.armor", 2, "add")
    .potion("minecraft:night_vision", 1);
```

- A piece accepts any items you list, including ore dictionary entries and items with NBT.
- A piece can sit in an armor slot, a hand, the hotbar, the inventory, or a [Baubles](https://www.curseforge.com/minecraft/mc-mods/baubles) slot when Baubles is installed.
- A bonus needs a piece count of one set, or of several sets at once, for example full armor plus a weapon in hand.
- A bonus can add attribute modifiers and potion effects, or carry none and leave the behavior to your script, which reads the bonus state through `player.hasSetworksBonus(id)` and the `events.onSetworksBonusActivated` event.
- A bonus can have a condition: a built-in one, or a script function `function(player as IPlayer) as bool`.
- A bonus can be always listed, listed once a player had it, or never listed.
- A script can replace, change the text of, or disable a set that a mod defined.
- `/setworks list` and `/setworks validate` show what loaded and what was rejected and why. A changed script needs a game restart.

The [scripting reference](https://github.com/mahghuuuls/setworks/blob/main/SCRIPTING.md) describes every call with examples: locations, item patterns, effects, conditions, visibility, changing another mod's sets, queries, and events.

The [example scripts](https://github.com/mahghuuuls/setworks/tree/main/examples) show the full surface: an armor set with a weapon set, a hidden bonus that needs both, a script function as condition, event handlers, and a script that changes a set another mod defined.

## For mod developers

A [Java API](https://github.com/mahghuuuls/setworks#java-api) does the same without CraftTweaker. With it a mod can:

- Register its own sets and bonuses when it loads, or replace and disable sets from other sources.
- Add a new kind of effect, for example a stat from its own system, that any set can then grant.
- Add a new kind of condition that any bonus can use.
- Expose its own equipment slots as locations, the way the Baubles slots are exposed.
- Ask whether a player has a bonus active, or listen for bonuses turning on and off.

## Configuration

`config/setworks.cfg`: strict validation (stop at startup on an invalid definition; off by default), the condition interval (every second by default), the vanilla example sets (off by default), a diagnostic log for operators (off by default), and a client switch for the tooltip block (on by default). A change needs a restart.

## Installation

Install on the server and on every client. CraftTweaker and Baubles are optional and only needed for the features above that name them.

Source and issues: [github.com/mahghuuuls/setworks](https://github.com/mahghuuuls/setworks)
