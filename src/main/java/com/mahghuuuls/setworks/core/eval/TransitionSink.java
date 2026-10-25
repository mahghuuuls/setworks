package com.mahghuuuls.setworks.core.eval;

import net.minecraft.entity.player.EntityPlayerMP;

import java.util.List;

/** Receives the result of every evaluation that changed something, and the player's departure. */
public interface TransitionSink {

    /** The player's state is about to be rebuilt from nothing (login, respawn, dimension change); whatever was applied before is forgotten. */
    void onRebuild(EntityPlayerMP player);

    void onTransitions(EntityPlayerMP player, PlayerSetState state, List<Transition> transitions);

    /** The player left; every active bonus is to be treated as deactivated. */
    void onLogout(EntityPlayerMP player, PlayerSetState lastState);
}
