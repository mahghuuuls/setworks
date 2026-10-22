package com.mahghuuuls.setworks.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * Read-only questions about the last evaluated state. Queries never evaluate, apply, or withdraw
 * anything. On the client, player queries answer from the synced state when available and
 * otherwise report nothing active.
 */
public interface ISetworksQuery {

    /** Satisfied pieces of the set for the player; 0 for an unknown set or player. */
    int pieceCount(EntityPlayer player, String setId);

    boolean isActive(EntityPlayer player, String bonusId);

    /** The bonus's state for the player; {@link BonusState#MISSING_EQUIPMENT} when unknown. */
    BonusState bonusState(EntityPlayer player, String bonusId);

    /** Ids of every enabled set some piece of which accepts the stack. */
    List<String> setsOf(ItemStack stack);

    /** Piece name to the slot key filling it, for the set and player; empty when none. */
    Map<String, String> satisfiedPieces(EntityPlayer player, String setId);
}
