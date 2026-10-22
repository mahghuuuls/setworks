package com.mahghuuuls.setworks.core.eval;

import com.mahghuuuls.setworks.api.BonusState;

/** One bonus changing state for one player. Activation and deactivation drive effects and events. */
public final class Transition {

    private final String bonusId;
    private final BonusState from;
    private final BonusState to;
    private final String detail;

    public Transition(String bonusId, BonusState from, BonusState to, String detail) {
        this.bonusId = bonusId;
        this.from = from;
        this.to = to;
        this.detail = detail == null ? "" : detail;
    }

    public String bonusId() {
        return bonusId;
    }

    public BonusState from() {
        return from;
    }

    public BonusState to() {
        return to;
    }

    public String detail() {
        return detail;
    }

    public boolean activated() {
        return to.isActive() && !from.isActive();
    }

    public boolean deactivated() {
        return from.isActive() && !to.isActive();
    }

    @Override
    public String toString() {
        return bonusId + ": " + from + " -> " + to + (detail.isEmpty() ? "" : " (" + detail + ")");
    }
}
