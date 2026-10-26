package com.mahghuuuls.setworks.core.eval;

import net.minecraft.entity.player.EntityPlayerMP;

import java.util.List;

/** Told when a player discovers bonuses for the first time; the network side sends their display data. */
public interface DiscoverySink {

    void onDiscovered(EntityPlayerMP player, List<String> bonusIds);
}
