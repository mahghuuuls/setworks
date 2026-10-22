package com.mahghuuuls.setworks.core.match;

import net.minecraft.nbt.NBTTagCompound;

import java.util.Set;

/**
 * What matching needs to know about one item stack. The server adapts a real stack; tests use a
 * fake, so predicates and assignment run without registries.
 */
public interface ItemView {

    /** Registry name such as {@code minecraft:iron_helmet}, or {@code null} for an empty stack. */
    String registryName();

    int metadata();

    /** The stack's compound, or {@code null} when it has none. Never modified by matching. */
    NBTTagCompound nbt();

    /** Ore dictionary names the item is registered under; empty when none. */
    Set<String> oreNames();

    boolean isEmpty();
}
