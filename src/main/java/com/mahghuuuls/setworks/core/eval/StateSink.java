package com.mahghuuuls.setworks.core.eval;

import net.minecraft.entity.player.EntityPlayerMP;

/**
 * Receives what a client must be told: once when the player joins (the display snapshot), and
 * every time the player's displayed state changed (counts, satisfied pieces, active bonuses). The
 * network side implements it; evaluation never knows how the data travels.
 */
public interface StateSink {

    void onJoin(EntityPlayerMP player);

    void onState(EntityPlayerMP player, PlayerSetState state);
}
