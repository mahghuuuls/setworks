package com.mahghuuuls.setworks.core.effect;

import java.util.UUID;

/**
 * The attribute modifiers of one entity, seen only as "remove this modifier of this attribute".
 * The rebuild reconciliation sweeps Setworks modifiers through this so it can be tested with a
 * fake store; in the game the store is the player's attribute map.
 */
public interface ModifierStore {

    /** Removes the modifier when the attribute exists and carries it; returns true when something was removed. */
    boolean remove(String attributeName, UUID modifierId);
}
