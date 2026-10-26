package com.mahghuuuls.setworks.core.match;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adapts a real stack to {@link ItemView}. Ore dictionary names are resolved once per item and
 * metadata and cached for the process; the ore dictionary is complete after load, which is when
 * evaluation starts.
 */
public final class ItemStackView implements ItemView {

    private static final Map<String, Set<String>> ORE_CACHE = new ConcurrentHashMap<String, Set<String>>();

    private final ItemStack stack;
    private final String registryName;
    private Set<String> oreNames;

    public ItemStackView(ItemStack stack) {
        this.stack = stack == null ? ItemStack.EMPTY : stack;
        ResourceLocation key = this.stack.isEmpty() ? null : this.stack.getItem().getRegistryName();
        this.registryName = key == null ? null : key.toString();
    }

    public ItemStack stack() {
        return stack;
    }

    @Override
    public String registryName() {
        return registryName;
    }

    @Override
    public int metadata() {
        return stack.isEmpty() ? 0 : stack.getMetadata();
    }

    @Override
    public NBTTagCompound nbt() {
        return stack.isEmpty() ? null : stack.getTagCompound();
    }

    @Override
    public Set<String> oreNames() {
        if (oreNames != null) {
            return oreNames;
        }
        if (stack.isEmpty() || registryName == null) {
            oreNames = Collections.emptySet();
            return oreNames;
        }
        String cacheKey = registryName + '@' + stack.getItemDamage();
        Set<String> names = ORE_CACHE.get(cacheKey);
        if (names == null) {
            names = resolve(stack);
            ORE_CACHE.put(cacheKey, names);
        }
        oreNames = names;
        return names;
    }

    private static Set<String> resolve(ItemStack stack) {
        int[] ids = OreDictionary.getOreIDs(stack);
        if (ids.length == 0) {
            return Collections.emptySet();
        }
        Set<String> names = new HashSet<String>(ids.length);
        for (int id : ids) {
            names.add(OreDictionary.getOreName(id));
        }
        return Collections.unmodifiableSet(names);
    }

    @Override
    public boolean isEmpty() {
        return stack.isEmpty();
    }

    /** Clears the cache; for tests and for a world reload where ore registrations may differ. */
    public static void clearCache() {
        ORE_CACHE.clear();
    }

    /** True when both stacks hold the same item and metadata, ignoring count and NBT. */
    public static boolean sameItemAndMeta(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty()) {
            return a.isEmpty() == b.isEmpty();
        }
        Item itemA = a.getItem();
        return itemA == b.getItem() && a.getMetadata() == b.getMetadata();
    }
}
