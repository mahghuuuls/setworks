package com.mahghuuuls.setworks.api;

/** What a condition may read besides the player: the piece counts Setworks already computed for this evaluation. */
public interface IConditionContext {

    /** The bonus whose condition is being tested. */
    String bonusId();

    /** Pieces of the set the player currently satisfies; 0 for an unknown or disabled set. */
    int pieceCount(String setId);

    /** The server world tick of this evaluation. */
    long tick();
}
