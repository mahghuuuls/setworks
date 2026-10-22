package com.mahghuuuls.setworks.api.event;

import com.mahghuuuls.setworks.api.BonusState;
import net.minecraft.entity.player.EntityPlayer;

import java.util.List;

/** A bonus became active for a player. */
public final class BonusActivatedEvent extends BonusEvent {

    public BonusActivatedEvent(EntityPlayer player, String bonusId, List<String> setIds) {
        super(player, bonusId, setIds, BonusState.ACTIVE, "");
    }
}
