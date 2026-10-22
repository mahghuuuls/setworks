package com.mahghuuuls.setworks.core.definition;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** A validation context with a fixed vocabulary, so registry tests need no game. */
final class FakeContext implements ValidationContext {

    final Set<String> items = new HashSet<String>(Arrays.asList(
            "minecraft:iron_helmet", "minecraft:iron_chestplate", "minecraft:iron_leggings", "minecraft:iron_boots",
            "minecraft:iron_sword", "minecraft:compass", "minecraft:wool"));
    final Set<String> ores = new HashSet<String>(Arrays.asList("ingotIron"));
    final Set<String> locations = new HashSet<String>(Arrays.asList("head", "chest", "legs", "feet", "mainhand", "offhand", "hotbar"));
    final Set<String> effectKinds = new HashSet<String>(Arrays.asList("setworks:attribute"));
    final Set<String> conditionKinds = new HashSet<String>(Arrays.asList("setworks:health_below"));

    @Override
    public boolean itemExists(String itemId) {
        return items.contains(itemId);
    }

    @Override
    public boolean oreHasEntries(String oreName) {
        return ores.contains(oreName);
    }

    @Override
    public boolean locationExists(String locationId) {
        return locations.contains(locationId);
    }

    @Override
    public boolean effectKindExists(String kindId) {
        return effectKinds.contains(kindId);
    }

    @Override
    public boolean conditionKindExists(String kindId) {
        return conditionKinds.contains(kindId);
    }

    @Override
    public boolean exactNbtParses(String text) {
        return text.startsWith("{") && text.endsWith("}");
    }
}
