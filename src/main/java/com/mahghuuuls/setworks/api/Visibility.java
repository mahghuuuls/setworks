package com.mahghuuuls.setworks.api;

/** When a bonus is shown to a player. Never changes whether the bonus is active. */
public enum Visibility {
    /** Always shown. */
    VISIBLE,
    /** Shown to a player only after that player first activated it. */
    DISCOVERED,
    /** Never shown and never sent to clients. */
    HIDDEN
}
