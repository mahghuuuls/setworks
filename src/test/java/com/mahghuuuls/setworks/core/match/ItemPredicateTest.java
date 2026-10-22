package com.mahghuuuls.setworks.core.match;

import com.mahghuuuls.setworks.api.ItemFilter;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemPredicateTest {

    private static NBTTagCompound named(String name) {
        NBTTagCompound display = new NBTTagCompound();
        display.setString("Name", name);
        NBTTagCompound root = new NBTTagCompound();
        root.setTag("display", display);
        root.setByte("Unbreakable", (byte) 1);
        return root;
    }

    @Test
    void idOnlyIgnoresMetadataAndNbt() {
        ItemPredicate p = ItemPredicate.compile(ItemFilter.item("minecraft:wool"));
        assertTrue(p.matches(new FakeItem("minecraft:wool", 5)));
        assertTrue(p.matches(new FakeItem("minecraft:wool", 0, named("x"))));
        assertFalse(p.matches(new FakeItem("minecraft:stone")));
        assertFalse(p.matches(FakeItem.empty()));
        assertFalse(p.matches(null));
    }

    @Test
    void metadataMustMatchWhenGiven() {
        ItemPredicate p = ItemPredicate.compile(ItemFilter.item("minecraft:wool", 11));
        assertTrue(p.matches(new FakeItem("minecraft:wool", 11)));
        assertFalse(p.matches(new FakeItem("minecraft:wool", 3)));
    }

    @Test
    void requiredNbtPathAndValue() {
        ItemPredicate p = ItemPredicate.compile(ItemFilter.item("minecraft:leather_leggings")
                .withRequiredNbt("display/Name", "Wirt's Leggings"));
        assertTrue(p.matches(new FakeItem("minecraft:leather_leggings", 0, named("Wirt's Leggings"))));
        assertFalse(p.matches(new FakeItem("minecraft:leather_leggings", 0, named("Other"))));
        assertFalse(p.matches(new FakeItem("minecraft:leather_leggings")));
        ItemPredicate quoted = ItemPredicate.compile(ItemFilter.item("minecraft:leather_leggings")
                .withRequiredNbt("display/Name", "\"Wirt's Leggings\""));
        assertTrue(quoted.matches(new FakeItem("minecraft:leather_leggings", 0, named("Wirt's Leggings"))));
    }

    @Test
    void requiredNbtKeyOnlyAndNonStringValues() {
        ItemPredicate keyOnly = ItemPredicate.compile(ItemFilter.item("minecraft:leather_leggings").withRequiredNbt("display/Name", null));
        assertTrue(keyOnly.matches(new FakeItem("minecraft:leather_leggings", 0, named("anything"))));
        ItemPredicate unbreakable = ItemPredicate.compile(ItemFilter.item("minecraft:leather_leggings").withRequiredNbt("Unbreakable", "1b"));
        assertTrue(unbreakable.matches(new FakeItem("minecraft:leather_leggings", 0, named("x"))));
    }

    @Test
    void absentNbt() {
        ItemPredicate p = ItemPredicate.compile(ItemFilter.item("minecraft:leather_leggings").withAbsentNbt("Unbreakable"));
        assertFalse(p.matches(new FakeItem("minecraft:leather_leggings", 0, named("x"))));
        assertTrue(p.matches(new FakeItem("minecraft:leather_leggings")));
    }

    @Test
    void exactNbtMatchesWholeCompound() {
        ItemPredicate p = ItemPredicate.compile(ItemFilter.item("minecraft:stick").withExactNbt("{a:1b}"));
        NBTTagCompound exact = new NBTTagCompound();
        exact.setByte("a", (byte) 1);
        assertTrue(p.matches(new FakeItem("minecraft:stick", 0, exact)));
        NBTTagCompound extra = exact.copy();
        extra.setByte("b", (byte) 1);
        assertFalse(p.matches(new FakeItem("minecraft:stick", 0, extra)));
        assertFalse(p.matches(new FakeItem("minecraft:stick")));
        assertThrows(IllegalArgumentException.class, () -> ItemPredicate.compile(ItemFilter.item("minecraft:stick").withExactNbt("{broken")));
    }

    @Test
    void oreDictionaryName() {
        ItemPredicate p = ItemPredicate.compile(ItemFilter.ore("ingotIron"));
        assertTrue(p.matches(new FakeItem("minecraft:iron_ingot", 0, null, "ingotIron")));
        assertFalse(p.matches(new FakeItem("minecraft:gold_ingot", 0, null, "ingotGold")));
    }
}
