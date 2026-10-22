package com.mahghuuuls.setworks.core.eval;

import com.mahghuuuls.setworks.core.match.SlotCandidate;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * One evaluation's inputs, gathered by the tracker on the server: what the player holds in every
 * referenced location, which locations changed since the last evaluation (or all of them on a
 * rebuild), the current tick, and the player flags that override everything.
 */
public final class EvaluationInput {

    private final List<SlotCandidate> slots;
    private final Set<String> changedLocations;
    private final boolean rebuild;
    private final long tick;
    private final boolean spectator;
    private final boolean dead;
    private final Object player;

    public EvaluationInput(List<SlotCandidate> slots, Set<String> changedLocations, boolean rebuild, long tick,
                           boolean spectator, boolean dead, Object player) {
        this.slots = Collections.unmodifiableList(slots);
        this.changedLocations = changedLocations == null ? Collections.<String>emptySet() : changedLocations;
        this.rebuild = rebuild;
        this.tick = tick;
        this.spectator = spectator;
        this.dead = dead;
        this.player = player;
    }

    /** Every occupied referenced slot (empty stacks are omitted). */
    public List<SlotCandidate> slots() {
        return slots;
    }

    public Set<String> changedLocations() {
        return changedLocations;
    }

    /** True to recount every set and re-evaluate every condition, ignoring the schedule. */
    public boolean rebuild() {
        return rebuild;
    }

    public long tick() {
        return tick;
    }

    public boolean spectator() {
        return spectator;
    }

    public boolean dead() {
        return dead;
    }

    /** The server player, passed through to conditions; {@code null} in tests. */
    public Object player() {
        return player;
    }
}
