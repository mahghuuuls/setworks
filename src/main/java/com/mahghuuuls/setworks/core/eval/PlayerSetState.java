package com.mahghuuuls.setworks.core.eval;

import com.mahghuuuls.setworks.api.BonusState;
import com.mahghuuuls.setworks.core.match.SlotCandidate;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Everything Setworks knows about one player: per set, the satisfied pieces and the slot that
 * fills each; per bonus, its state, the detail of its reason, and the condition schedule.
 * Owned by the evaluator; read by the inspect command, queries, and sync.
 */
public final class PlayerSetState {

    public static final class SetState {
        private final Map<String, SlotCandidate> satisfied;

        SetState(Map<String, SlotCandidate> satisfied) {
            this.satisfied = Collections.unmodifiableMap(new LinkedHashMap<String, SlotCandidate>(satisfied));
        }

        /** Piece name to the slot filling it. */
        public Map<String, SlotCandidate> satisfiedPieces() {
            return satisfied;
        }

        public int count() {
            return satisfied.size();
        }

        public boolean isSatisfied(String pieceName) {
            return satisfied.containsKey(pieceName);
        }
    }

    public static final class BonusEntry {
        private final BonusState state;
        private final String detail;
        private final long lastConditionTick;

        BonusEntry(BonusState state, String detail, long lastConditionTick) {
            this.state = state;
            this.detail = detail == null ? "" : detail;
            this.lastConditionTick = lastConditionTick;
        }

        public BonusState state() {
            return state;
        }

        /** For example the missing set and count, or the condition that failed. */
        public String detail() {
            return detail;
        }

        /** Tick of the last condition evaluation, or -1 when never evaluated. */
        public long lastConditionTick() {
            return lastConditionTick;
        }

        public boolean isActive() {
            return state.isActive();
        }

        /**
         * True when the player wears every piece the bonus requires: active, or held back only by a
         * condition, a condition error, or a higher threshold of the same set. False when a piece is
         * missing, and for a dead or spectating player, whose sets do not apply. The tooltip colors by
         * this flag (REQ-028); conditions do not change the color.
         */
        public boolean equipmentMet() {
            switch (state) {
                case ACTIVE:
                case CONDITION_FAILED:
                case CONDITION_ERROR:
                case SUPERSEDED:
                    return true;
                default:
                    return false;
            }
        }
    }

    private final UUID playerId;
    private final String playerName;
    private final Map<String, SetState> sets;
    private final Map<String, BonusEntry> bonuses;

    PlayerSetState(UUID playerId, String playerName, Map<String, SetState> sets, Map<String, BonusEntry> bonuses) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.sets = Collections.unmodifiableMap(new LinkedHashMap<String, SetState>(sets));
        this.bonuses = Collections.unmodifiableMap(new LinkedHashMap<String, BonusEntry>(bonuses));
    }

    public static PlayerSetState empty(UUID playerId, String playerName) {
        return new PlayerSetState(playerId, playerName, Collections.<String, SetState>emptyMap(),
                Collections.<String, BonusEntry>emptyMap());
    }

    public UUID playerId() {
        return playerId;
    }

    public String playerName() {
        return playerName;
    }

    public Map<String, SetState> sets() {
        return sets;
    }

    public Map<String, BonusEntry> bonuses() {
        return bonuses;
    }

    public int pieceCount(String setId) {
        SetState set = sets.get(setId);
        return set == null ? 0 : set.count();
    }

    public boolean isActive(String bonusId) {
        BonusEntry entry = bonuses.get(bonusId);
        return entry != null && entry.isActive();
    }

    public BonusState bonusState(String bonusId) {
        BonusEntry entry = bonuses.get(bonusId);
        return entry == null ? BonusState.MISSING_EQUIPMENT : entry.state();
    }

    /**
     * True when the two states show the same thing: the same satisfied piece names per set and the
     * same bonuses with their pieces on. A condition outcome alone changes nothing on the client.
     */
    public static boolean sameDisplay(PlayerSetState a, PlayerSetState b) {
        if (!a.sets.keySet().equals(b.sets.keySet()) || !a.bonuses.keySet().equals(b.bonuses.keySet())) {
            return false;
        }
        for (Map.Entry<String, SetState> set : a.sets.entrySet()) {
            if (!set.getValue().satisfiedPieces().keySet().equals(b.sets.get(set.getKey()).satisfiedPieces().keySet())) {
                return false;
            }
        }
        for (Map.Entry<String, BonusEntry> bonus : a.bonuses.entrySet()) {
            if (bonus.getValue().equipmentMet() != b.bonuses.get(bonus.getKey()).equipmentMet()) {
                return false;
            }
        }
        return true;
    }
}
