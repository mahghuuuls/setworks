package com.mahghuuuls.setworks.core.match;

import net.minecraft.nbt.NBTTagCompound;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** A stack stand-in for matching tests: registry name, metadata, NBT, ore names. */
public final class FakeItem implements ItemView {

    private final String name;
    private final int meta;
    private final NBTTagCompound nbt;
    private final Set<String> ores;

    public FakeItem(String name) {
        this(name, 0, null);
    }

    public FakeItem(String name, int meta) {
        this(name, meta, null);
    }

    public FakeItem(String name, int meta, NBTTagCompound nbt, String... ores) {
        this.name = name;
        this.meta = meta;
        this.nbt = nbt;
        this.ores = ores.length == 0 ? Collections.<String>emptySet() : new HashSet<String>(Arrays.asList(ores));
    }

    public static FakeItem empty() {
        return new FakeItem(null);
    }

    @Override
    public String registryName() {
        return name;
    }

    @Override
    public int metadata() {
        return meta;
    }

    @Override
    public NBTTagCompound nbt() {
        return nbt;
    }

    @Override
    public Set<String> oreNames() {
        return ores;
    }

    @Override
    public boolean isEmpty() {
        return name == null;
    }

    @Override
    public String toString() {
        return name == null ? "empty" : name + "@" + meta;
    }
}
