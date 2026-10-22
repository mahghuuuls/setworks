package com.mahghuuuls.setworks.api;

/** The state of one bonus for one player, with the reason it is not active. */
public enum BonusState {
    /** Equipment requirements and all conditions pass. */
    ACTIVE,
    /** At least one set requirement is not met. */
    MISSING_EQUIPMENT,
    /** Equipment is complete but a condition returned false. */
    CONDITION_FAILED,
    /** Equipment is complete but a condition threw an error. */
    CONDITION_ERROR,
    /** Equipment and conditions pass, but a higher threshold of the same set applies (highest-only mode). */
    SUPERSEDED,
    /** The player is in spectator mode. */
    SPECTATOR,
    /** The player is dead. */
    DEAD;

    public boolean isActive() {
        return this == ACTIVE;
    }

    public String reasonText() {
        return name().toLowerCase().replace('_', ' ');
    }
}
