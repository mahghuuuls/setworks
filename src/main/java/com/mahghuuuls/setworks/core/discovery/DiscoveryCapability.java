package com.mahghuuuls.setworks.core.discovery;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The one thing Setworks saves per player: the ids of the {@code discovered} bonuses this player
 * has activated at least once. It only ever grows; Setworks never removes an id. Stored with the
 * player as a list of strings, so it survives death, logout, and restart, and a modpack can read
 * it with any NBT tool.
 */
public final class DiscoveryCapability {

    public static final String TAG_DISCOVERED = "Discovered";

    private final Set<String> discovered = new LinkedHashSet<String>();

    /** Records a discovery; true when the id was new. */
    public boolean discover(String bonusId) {
        return bonusId != null && discovered.add(bonusId);
    }

    public boolean has(String bonusId) {
        return discovered.contains(bonusId);
    }

    public Set<String> all() {
        return Collections.unmodifiableSet(discovered);
    }

    /** Adds the other record's ids (the respawn copy); never removes any. */
    public void copyFrom(DiscoveryCapability other) {
        if (other != null) {
            discovered.addAll(other.discovered);
        }
    }

    public NBTTagCompound writeNBT() {
        NBTTagList list = new NBTTagList();
        for (String id : discovered) {
            list.appendTag(new NBTTagString(id));
        }
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag(TAG_DISCOVERED, list);
        return tag;
    }

    /** Replaces the record with the saved list; a tag without the list leaves the record untouched. */
    public void readNBT(NBTTagCompound tag) {
        if (tag == null || !tag.hasKey(TAG_DISCOVERED, Constants.NBT.TAG_LIST)) {
            return;
        }
        discovered.clear();
        NBTTagList list = tag.getTagList(TAG_DISCOVERED, Constants.NBT.TAG_STRING);
        for (int i = 0; i < list.tagCount(); i++) {
            String id = list.getStringTagAt(i);
            if (!id.isEmpty()) {
                discovered.add(id);
            }
        }
    }
}
