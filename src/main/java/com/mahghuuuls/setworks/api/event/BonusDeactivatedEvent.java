package com.mahghuuuls.setworks.api.event;

import com.mahghuuuls.setworks.api.BonusState;
import net.minecraft.entity.player.EntityPlayer;

import java.util.List;

/** A bonus stopped being active for a player; {@link #getState()} says why. */
public final class BonusDeactivatedEvent extends BonusEvent {

    public BonusDeactivatedEvent(EntityPlayer player, String bonusId, List<String> setIds, BonusState state, String detail) {
        super(player, bonusId, setIds, state, detail);
    }
}
