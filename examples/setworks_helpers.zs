#loader setworks

// The shortest working set: one call for the armor set, one for a bonus with its effects.
// Everything the longer form offers (pieces at other locations, ore entries, visibility,
// conditions, display sets) is available on the same handles.

import mods.setworks.Setworks;

Setworks.armorSet("example:gold_armor", "Golden Armor",
    <minecraft:golden_helmet>, <minecraft:golden_chestplate>, <minecraft:golden_leggings>, <minecraft:golden_boots>);

Setworks.bonus("example:gold_glow", "example:gold_armor", 4, "Golden Glow: +2 armor and Night Vision")
    .attribute("generic.armor", 2, "add")
    .potion("minecraft:night_vision", 1);
