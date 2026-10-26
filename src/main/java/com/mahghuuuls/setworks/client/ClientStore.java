package com.mahghuuuls.setworks.client;

import com.mahghuuuls.setworks.core.match.ItemPredicate;
import com.mahghuuuls.setworks.core.match.ItemStackView;
import com.mahghuuuls.setworks.core.match.ItemView;
import com.mahghuuuls.setworks.network.DisplaySnapshot;
import com.mahghuuuls.setworks.network.DisplayState;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What this client was told: the display snapshot and the player's own state. Finds the sets a
 * stack belongs to by compiling the snapshot's filters once; that is matching, not evaluation, and
 * it is the only rule-like thing the client does. Updated on the client thread only.
 */
public final class ClientStore {

    private static final ClientStore INSTANCE = new ClientStore();

    public static ClientStore get() {
        return INSTANCE;
    }

    private DisplaySnapshot snapshot = DisplaySnapshot.empty();
    private DisplayState state = DisplayState.empty();
    private Map<String, List<ItemPredicate>> predicatesBySet = Collections.emptyMap();

    ClientStore() {
    }

    public void acceptSnapshot(DisplaySnapshot received) {
        Map<String, List<ItemPredicate>> compiled = new LinkedHashMap<String, List<ItemPredicate>>();
        for (DisplaySnapshot.SetDisplay set : received.sets()) {
            List<ItemPredicate> predicates = new ArrayList<ItemPredicate>();
            for (DisplaySnapshot.PieceDisplay piece : set.pieces()) {
                for (DisplaySnapshot.AlternativeDisplay alternative : piece.alternatives()) {
                    predicates.add(ItemPredicate.compile(alternative.filter()));
                }
            }
            compiled.put(set.id(), predicates);
        }
        this.predicatesBySet = compiled;
        this.snapshot = received;
    }

    public void acceptState(DisplayState received) {
        this.state = received;
    }

    /** Adds the bonuses of a discovery message to the snapshot; an id already present is replaced. */
    public void acceptDiscovery(DisplaySnapshot added) {
        List<DisplaySnapshot.BonusDisplay> merged = new ArrayList<DisplaySnapshot.BonusDisplay>();
        for (DisplaySnapshot.BonusDisplay bonus : snapshot.bonuses()) {
            boolean replaced = false;
            for (DisplaySnapshot.BonusDisplay fresh : added.bonuses()) {
                if (fresh.id().equals(bonus.id())) {
                    replaced = true;
                }
            }
            if (!replaced) {
                merged.add(bonus);
            }
        }
        merged.addAll(added.bonuses());
        snapshot = new DisplaySnapshot(snapshot.sets(), merged);
    }

    /** Forgets everything, for a disconnect. */
    public void clear() {
        snapshot = DisplaySnapshot.empty();
        state = DisplayState.empty();
        predicatesBySet = Collections.emptyMap();
    }

    public DisplaySnapshot snapshot() {
        return snapshot;
    }

    public DisplayState state() {
        return state;
    }

    /** The ids of the sets any piece of which accepts this stack, in snapshot order. */
    public List<String> setsOf(ItemStack stack) {
        if (stack == null || stack.isEmpty() || predicatesBySet.isEmpty()) {
            return Collections.emptyList();
        }
        return setsOf(new ItemStackView(stack));
    }

    List<String> setsOf(ItemView item) {
        List<String> result = new ArrayList<String>();
        for (Map.Entry<String, List<ItemPredicate>> entry : predicatesBySet.entrySet()) {
            for (ItemPredicate predicate : entry.getValue()) {
                if (predicate.matches(item)) {
                    result.add(entry.getKey());
                    break;
                }
            }
        }
        return result;
    }
}
