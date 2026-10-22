package com.mahghuuuls.setworks.core.eval;

import com.mahghuuuls.setworks.api.BonusState;
import com.mahghuuuls.setworks.api.ThresholdMode;
import com.mahghuuuls.setworks.api.definition.BonusDefinition;
import com.mahghuuuls.setworks.api.definition.SetRequirement;
import com.mahghuuuls.setworks.core.match.CompiledDefinitions;
import com.mahghuuuls.setworks.core.match.CompiledSet;
import com.mahghuuuls.setworks.core.match.PieceAssignment;
import com.mahghuuuls.setworks.core.match.SlotCandidate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The one owner of the evaluation sequence: changed locations, affected sets, recount, threshold
 * selection per mode, conditions when due, bonus states with reasons, transitions. Pure: it reads
 * {@link EvaluationInput} and the previous {@link PlayerSetState} and returns the new state and
 * the transitions. Effects, events, sync, and logging are driven by the caller from the result.
 */
public final class Evaluator {

    public static final class Result {
        private final PlayerSetState state;
        private final List<Transition> transitions;

        Result(PlayerSetState state, List<Transition> transitions) {
            this.state = state;
            this.transitions = transitions;
        }

        public PlayerSetState state() {
            return state;
        }

        public List<Transition> transitions() {
            return transitions;
        }
    }

    private final CompiledDefinitions definitions;
    private final ConditionEvaluator conditions;
    private final int defaultConditionInterval;

    public Evaluator(CompiledDefinitions definitions, ConditionEvaluator conditions, int defaultConditionInterval) {
        this.definitions = definitions;
        this.conditions = conditions;
        this.defaultConditionInterval = Math.max(1, defaultConditionInterval);
    }

    public CompiledDefinitions definitions() {
        return definitions;
    }

    public Result evaluate(PlayerSetState previous, EvaluationInput input) {
        UUID playerId = previous.playerId();
        String playerName = previous.playerName();
        // 1. Recount the affected sets; keep the others.
        Map<String, PlayerSetState.SetState> setStates = new LinkedHashMap<String, PlayerSetState.SetState>();
        Set<String> recounted = new LinkedHashSet<String>();
        if (input.rebuild()) {
            for (CompiledSet set : definitions.sets()) {
                recounted.add(set.id());
            }
        } else {
            for (String location : input.changedLocations()) {
                recounted.addAll(definitions.setsReferencing(location));
            }
        }
        for (CompiledSet set : definitions.sets()) {
            PlayerSetState.SetState old = previous.sets().get(set.id());
            if (old == null || recounted.contains(set.id())) {
                List<SlotCandidate> relevant = new ArrayList<SlotCandidate>();
                for (SlotCandidate slot : input.slots()) {
                    if (set.locations().contains(slot.locationId())) {
                        relevant.add(slot);
                    }
                }
                setStates.put(set.id(), new PlayerSetState.SetState(PieceAssignment.assign(set, relevant)));
            } else {
                setStates.put(set.id(), old);
            }
        }
        // 2. Equipment check and conditions per bonus.
        Map<String, BonusState> states = new HashMap<String, BonusState>();
        Map<String, String> details = new HashMap<String, String>();
        Map<String, Long> conditionTicks = new HashMap<String, Long>();
        Map<String, Integer> singleSetThreshold = new HashMap<String, Integer>();
        for (BonusDefinition bonus : definitions.bonuses()) {
            PlayerSetState.BonusEntry old = previous.bonuses().get(bonus.id());
            long lastTick = old == null ? -1 : old.lastConditionTick();
            if (input.spectator()) {
                states.put(bonus.id(), BonusState.SPECTATOR);
                details.put(bonus.id(), "");
                conditionTicks.put(bonus.id(), lastTick);
                continue;
            }
            if (input.dead()) {
                states.put(bonus.id(), BonusState.DEAD);
                details.put(bonus.id(), "");
                conditionTicks.put(bonus.id(), lastTick);
                continue;
            }
            String missing = missingEquipment(bonus, setStates);
            if (missing != null) {
                states.put(bonus.id(), BonusState.MISSING_EQUIPMENT);
                details.put(bonus.id(), missing);
                conditionTicks.put(bonus.id(), lastTick);
                continue;
            }
            boolean equipmentTouched = input.rebuild() || touched(bonus, recounted);
            boolean due = bonus.conditions().isEmpty() || equipmentTouched || lastTick < 0
                    || input.tick() - lastTick >= interval(bonus)
                    || old == null || old.state() == BonusState.MISSING_EQUIPMENT
                    || old.state() == BonusState.SPECTATOR || old.state() == BonusState.DEAD;
            BonusState state;
            String detail;
            if (bonus.conditions().isEmpty()) {
                state = BonusState.ACTIVE;
                detail = "";
                conditionTicks.put(bonus.id(), lastTick);
            } else if (due) {
                ConditionEvaluator.Result result = conditions.evaluate(bonus, input.player());
                switch (result.outcome()) {
                    case PASS:
                        state = BonusState.ACTIVE;
                        break;
                    case FAIL:
                        state = BonusState.CONDITION_FAILED;
                        break;
                    default:
                        state = BonusState.CONDITION_ERROR;
                        break;
                }
                detail = result.detail();
                conditionTicks.put(bonus.id(), input.tick());
            } else {
                // Not due: keep the previous condition outcome; a SUPERSEDED entry was a pass, so it is
                // ACTIVE again until step 3 decides.
                boolean wasSuperseded = old.state() == BonusState.SUPERSEDED;
                state = wasSuperseded ? BonusState.ACTIVE : old.state();
                detail = wasSuperseded ? "" : old.detail();
                conditionTicks.put(bonus.id(), lastTick);
            }
            states.put(bonus.id(), state);
            details.put(bonus.id(), detail);
            if (!bonus.isMultiSet()) {
                SetRequirement requirement = bonus.requirements().get(0);
                CompiledSet set = definitions.set(requirement.setId());
                if (set != null) {
                    singleSetThreshold.put(bonus.id(), requirement.effectiveThreshold(set.definition()));
                }
            }
        }
        // 3. Highest-only selection per set: among single-set bonuses of that set that are ACTIVE, keep
        //    only the highest threshold; lower ones become SUPERSEDED. Multi-set bonuses are untouched.
        for (CompiledSet set : definitions.sets()) {
            if (set.definition().thresholdMode() != ThresholdMode.HIGHEST_ONLY) {
                continue;
            }
            int highest = -1;
            for (BonusDefinition bonus : definitions.bonusesRequiring(set.id())) {
                if (bonus.isMultiSet() || states.get(bonus.id()) != BonusState.ACTIVE) {
                    continue;
                }
                highest = Math.max(highest, singleSetThreshold.get(bonus.id()));
            }
            if (highest < 0) {
                continue;
            }
            for (BonusDefinition bonus : definitions.bonusesRequiring(set.id())) {
                if (bonus.isMultiSet() || states.get(bonus.id()) != BonusState.ACTIVE) {
                    continue;
                }
                if (singleSetThreshold.get(bonus.id()) < highest) {
                    states.put(bonus.id(), BonusState.SUPERSEDED);
                    details.put(bonus.id(), "threshold " + highest + " applies");
                }
            }
        }
        // 4. Build the new state and the transitions.
        Map<String, PlayerSetState.BonusEntry> entries = new LinkedHashMap<String, PlayerSetState.BonusEntry>();
        List<Transition> transitions = new ArrayList<Transition>();
        for (BonusDefinition bonus : definitions.bonuses()) {
            BonusState state = states.get(bonus.id());
            String detail = details.get(bonus.id());
            entries.put(bonus.id(), new PlayerSetState.BonusEntry(state, detail, conditionTicks.get(bonus.id())));
            PlayerSetState.BonusEntry old = previous.bonuses().get(bonus.id());
            BonusState from = old == null ? BonusState.MISSING_EQUIPMENT : old.state();
            if (from != state) {
                transitions.add(new Transition(bonus.id(), from, state, detail));
            }
        }
        return new Result(new PlayerSetState(playerId, playerName, setStates, entries), transitions);
    }

    private int interval(BonusDefinition bonus) {
        return bonus.conditionInterval() > 0 ? bonus.conditionInterval() : defaultConditionInterval;
    }

    /**
     * The earliest tick at which some conditioned bonus of this state is due again, or
     * {@link Long#MAX_VALUE} when none is. Bonuses without equipment, or in spectator or dead
     * state, are not scheduled; an equipment change re-evaluates them anyway.
     */
    public long nextConditionDue(PlayerSetState state, long now) {
        long next = Long.MAX_VALUE;
        for (BonusDefinition bonus : definitions.bonuses()) {
            if (bonus.conditions().isEmpty()) {
                continue;
            }
            PlayerSetState.BonusEntry entry = state.bonuses().get(bonus.id());
            if (entry == null || entry.state() == BonusState.MISSING_EQUIPMENT || entry.state() == BonusState.SPECTATOR
                    || entry.state() == BonusState.DEAD) {
                continue;
            }
            long last = entry.lastConditionTick() < 0 ? now : entry.lastConditionTick();
            next = Math.min(next, last + interval(bonus));
        }
        return next;
    }

    private static boolean touched(BonusDefinition bonus, Set<String> recounted) {
        for (SetRequirement requirement : bonus.requirements()) {
            if (recounted.contains(requirement.setId())) {
                return true;
            }
        }
        return false;
    }

    /** Null when every requirement is met; otherwise the first unmet one as "set n/needed". */
    private String missingEquipment(BonusDefinition bonus, Map<String, PlayerSetState.SetState> setStates) {
        for (SetRequirement requirement : bonus.requirements()) {
            CompiledSet set = definitions.set(requirement.setId());
            if (set == null) {
                return requirement.setId() + " is not enabled";
            }
            PlayerSetState.SetState state = setStates.get(requirement.setId());
            int have = state == null ? 0 : state.count();
            int need = requirement.effectiveThreshold(set.definition());
            if (have < need) {
                return requirement.setId() + " " + have + "/" + need;
            }
        }
        return null;
    }
}
