package com.mahghuuuls.setworks.core.match;

import com.mahghuuuls.setworks.api.ItemFilter;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;

import java.util.ArrayList;
import java.util.List;

/**
 * An {@link ItemFilter} compiled once at freeze into direct checks: item identity first, then
 * metadata, then NBT, then ore dictionary. No text is parsed during evaluation.
 */
public final class ItemPredicate {

    private static final class NbtRequirement {
        final String[] path;
        final String value;

        NbtRequirement(String path, String value) {
            this.path = path.split("/");
            this.value = value;
        }
    }

    private final ItemFilter filter;
    private final String itemId;
    private final int metadata;
    private final String oreName;
    private final List<NbtRequirement> required = new ArrayList<NbtRequirement>();
    private final List<String[]> absent = new ArrayList<String[]>();
    private final NBTTagCompound exact;

    private ItemPredicate(ItemFilter filter) {
        this.filter = filter;
        this.itemId = filter.itemId();
        this.metadata = filter.metadata();
        this.oreName = filter.oreName();
        for (ItemFilter.NbtRule rule : filter.requiredNbt()) {
            required.add(new NbtRequirement(rule.path(), rule.value()));
        }
        for (String path : filter.absentNbt()) {
            absent.add(path.split("/"));
        }
        NBTTagCompound parsed = null;
        if (filter.exactNbt() != null) {
            try {
                parsed = JsonToNBT.getTagFromJson(filter.exactNbt());
            } catch (NBTException e) {
                throw new IllegalArgumentException("exact NBT text is not valid: " + filter.exactNbt(), e);
            }
        }
        this.exact = parsed;
    }

    /** Throws {@link IllegalArgumentException} for an exact-NBT text that does not parse; freeze validation rejects such a set first. */
    public static ItemPredicate compile(ItemFilter filter) {
        return new ItemPredicate(filter);
    }

    public ItemFilter filter() {
        return filter;
    }

    public boolean matches(ItemView item) {
        if (item == null || item.isEmpty()) {
            return false;
        }
        if (oreName != null) {
            return item.oreNames().contains(oreName);
        }
        if (!itemId.equals(item.registryName())) {
            return false;
        }
        if (metadata != ItemFilter.ANY_METADATA && metadata != item.metadata()) {
            return false;
        }
        if (required.isEmpty() && absent.isEmpty() && exact == null) {
            return true;
        }
        NBTTagCompound nbt = item.nbt();
        if (exact != null) {
            return nbt != null && exact.equals(nbt);
        }
        for (NbtRequirement requirement : required) {
            NBTBase tag = find(nbt, requirement.path);
            if (tag == null) {
                return false;
            }
            if (requirement.value != null && !valueMatches(tag, requirement.value)) {
                return false;
            }
        }
        for (String[] path : absent) {
            if (find(nbt, path) != null) {
                return false;
            }
        }
        return true;
    }

    private static NBTBase find(NBTTagCompound root, String[] path) {
        if (root == null) {
            return null;
        }
        NBTBase current = root;
        for (String segment : path) {
            if (!(current instanceof NBTTagCompound)) {
                return null;
            }
            NBTTagCompound compound = (NBTTagCompound) current;
            if (!compound.hasKey(segment)) {
                return null;
            }
            current = compound.getTag(segment);
        }
        return current;
    }

    private static boolean valueMatches(NBTBase tag, String expected) {
        if (tag instanceof NBTTagString) {
            String text = ((NBTTagString) tag).getString();
            String wanted = expected;
            if (wanted.length() >= 2 && wanted.startsWith("\"") && wanted.endsWith("\"")) {
                wanted = wanted.substring(1, wanted.length() - 1);
            }
            return text.equals(wanted);
        }
        return tag.toString().equals(expected);
    }

    @Override
    public String toString() {
        return filter.describe();
    }
}
