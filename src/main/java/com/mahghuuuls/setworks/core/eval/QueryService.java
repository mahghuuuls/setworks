package com.mahghuuuls.setworks.core.eval;

import com.mahghuuuls.setworks.api.BonusState;
import com.mahghuuuls.setworks.api.ISetworksQuery;
import com.mahghuuuls.setworks.core.location.ItemStackView;
import com.mahghuuuls.setworks.core.match.CompiledDefinitions;
import com.mahghuuuls.setworks.core.match.SlotCandidate;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Answers queries from the tracker's last evaluated state. Never evaluates anything. */
public final class QueryService implements ISetworksQuery {

    private final PlayerStateTracker tracker;
    private final CompiledDefinitions definitions;

    public QueryService(PlayerStateTracker tracker, CompiledDefinitions definitions) {
        this.tracker = tracker;
        this.definitions = definitions;
    }

    private PlayerSetState state(EntityPlayer player) {
        return player == null ? null : tracker.stateOf(player.getUniqueID());
    }

    @Override
    public int pieceCount(EntityPlayer player, String setId) {
        PlayerSetState state = state(player);
        return state == null ? 0 : state.pieceCount(setId);
    }

    @Override
    public boolean isActive(EntityPlayer player, String bonusId) {
        PlayerSetState state = state(player);
        return state != null && state.isActive(bonusId);
    }

    @Override
    public BonusState bonusState(EntityPlayer player, String bonusId) {
        PlayerSetState state = state(player);
        return state == null ? BonusState.MISSING_EQUIPMENT : state.bonusState(bonusId);
    }

    @Override
    public List<String> setsOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Collections.emptyList();
        }
        return definitions.setsMentioning(new ItemStackView(stack));
    }

    @Override
    public Map<String, String> satisfiedPieces(EntityPlayer player, String setId) {
        PlayerSetState state = state(player);
        if (state == null) {
            return Collections.emptyMap();
        }
        PlayerSetState.SetState set = state.sets().get(setId);
        if (set == null) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (Map.Entry<String, SlotCandidate> entry : set.satisfiedPieces().entrySet()) {
            result.put(entry.getKey(), entry.getValue().slotKey());
        }
        return result;
    }
}
