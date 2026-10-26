package com.mahghuuuls.setworks.core.location;

import com.mahghuuuls.setworks.api.ILocation;
import com.mahghuuuls.setworks.api.SlotStack;
import com.mahghuuuls.setworks.core.match.ItemStackView;
import com.mahghuuuls.setworks.core.match.SlotCandidate;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One player's view of the locations that any enabled definition references. Each scan reads
 * those locations and compares every slot with the previous scan by stack identity first, then by
 * item and metadata, so a durability change in place is not a change and a swapped stack is.
 * The comparison itself allocates nothing on the no-change path; the reads do, because the public
 * {@link ILocation#read} contract returns a list of stacks per call. A reusable buffer would need
 * a different public contract and is the recorded deferred alternative.
 */
public final class LocationView {

    private final List<ILocation> locations;
    private final Map<String, ItemStack[]> previous = new HashMap<String, ItemStack[]>();
    private final Map<String, String[]> slotKeys = new HashMap<String, String[]>();

    public LocationView(List<ILocation> referencedLocations) {
        this.locations = referencedLocations;
    }

    /** Reads every referenced location and returns the ids whose contents changed since the last scan. */
    public Set<String> scan(EntityPlayer player) {
        Set<String> changed = null;
        for (ILocation location : locations) {
            List<SlotStack> stacks = location.read(player);
            ItemStack[] old = previous.get(location.id());
            boolean differs = old == null || old.length != stacks.size();
            if (!differs) {
                for (int i = 0; i < old.length; i++) {
                    ItemStack now = stacks.get(i).stack();
                    if (now != old[i] && !ItemStackView.sameItemAndMeta(now, old[i])) {
                        differs = true;
                        break;
                    }
                }
            }
            ItemStack[] current = old != null && old.length == stacks.size() ? old : new ItemStack[stacks.size()];
            String[] keys = slotKeys.get(location.id());
            if (keys == null || keys.length != stacks.size()) {
                keys = new String[stacks.size()];
                slotKeys.put(location.id(), keys);
            }
            for (int i = 0; i < stacks.size(); i++) {
                current[i] = stacks.get(i).stack();
                keys[i] = stacks.get(i).slotKey();
            }
            previous.put(location.id(), current);
            if (differs) {
                if (changed == null) {
                    changed = new LinkedHashSet<String>();
                }
                changed.add(location.id());
            }
        }
        return changed == null ? java.util.Collections.<String>emptySet() : changed;
    }

    /** Every occupied referenced slot as last scanned. Call after {@link #scan}. */
    public List<SlotCandidate> candidates() {
        List<SlotCandidate> result = new ArrayList<SlotCandidate>();
        for (ILocation location : locations) {
            ItemStack[] stacks = previous.get(location.id());
            String[] keys = slotKeys.get(location.id());
            if (stacks == null) {
                continue;
            }
            for (int i = 0; i < stacks.length; i++) {
                if (!stacks[i].isEmpty()) {
                    result.add(new SlotCandidate(location.id(), keys[i], new ItemStackView(stacks[i])));
                }
            }
        }
        return result;
    }

    /** Forgets the previous scan so the next one reports every location as changed. */
    public void reset() {
        previous.clear();
        slotKeys.clear();
    }
}
