package com.mahghuuuls.setworks.compat.crafttweaker;

import com.mahghuuuls.setworks.api.ItemFilter;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

/**
 * How a CraftTweaker item stack pattern becomes an {@link ItemFilter}: the item id; the metadata
 * only when the item has sub-types and the pattern names one (a damageable item's metadata is its
 * damage, and the wildcard means any); each top-level NBT key of the pattern as a required NBT
 * rule, which is CraftTweaker's containment match. Pure; the CraftTweaker types are unwrapped in
 * {@link Ingredients}.
 */
final class FilterRules {

    private FilterRules() {
    }

    static ItemFilter filterFor(String itemId, int metadata, boolean hasSubtypes, NBTTagCompound tag) {
        ItemFilter filter = metadata != OreDictionary.WILDCARD_VALUE && hasSubtypes
                ? ItemFilter.item(itemId, metadata)
                : ItemFilter.item(itemId);
        if (tag != null) {
            for (String key : tag.getKeySet()) {
                filter = filter.withRequiredNbt(key, tag.getTag(key).toString());
            }
        }
        return filter;
    }
}
