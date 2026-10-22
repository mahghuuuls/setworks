package com.mahghuuuls.setworks.api;

import net.minecraft.entity.player.EntityPlayer;

import java.util.List;

/**
 * Receives bonus transitions on the server. Register with {@link SetworksApi#addBonusListener}.
 * Each listener is called in its own guard: one that throws is logged once and never stops the
 * others, unlike a Forge event handler. The same transitions are also posted as Forge events
 * ({@code BonusActivatedEvent}, {@code BonusDeactivatedEvent}) for mods that prefer the bus.
 *
 * After a respawn or dimension change Setworks rebuilds the player's state from scratch, so an
 * activation may arrive without a preceding deactivation for the same bonus.
 */
public interface BonusListener {

    void onActivated(EntityPlayer player, String bonusId, List<String> setIds);

    void onDeactivated(EntityPlayer player, String bonusId, List<String> setIds, BonusState state, String detail);
}
