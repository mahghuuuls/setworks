package com.mahghuuuls.setworks.api.event;

import com.mahghuuuls.setworks.api.BonusState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.Collections;
import java.util.List;

/**
 * Base of the Setworks events posted on the Forge event bus on the server when a bonus changes
 * state for a player. Not cancelable; handlers observe, they do not decide. Forge stops a post at
 * a handler that throws; {@link com.mahghuuuls.setworks.api.BonusListener} is the guarded route.
 * After a respawn or dimension change the state is rebuilt from scratch, so an activation may
 * arrive without a preceding deactivation.
 */
public abstract class BonusEvent extends Event {

    private final EntityPlayer player;
    private final String bonusId;
    private final List<String> setIds;
    private final BonusState state;
    private final String detail;

    protected BonusEvent(EntityPlayer player, String bonusId, List<String> setIds, BonusState state, String detail) {
        this.player = player;
        this.bonusId = bonusId;
        this.setIds = Collections.unmodifiableList(setIds);
        this.state = state;
        this.detail = detail == null ? "" : detail;
    }

    public EntityPlayer getPlayer() {
        return player;
    }

    public String getBonusId() {
        return bonusId;
    }

    /** The sets the bonus requires. */
    public List<String> getSetIds() {
        return setIds;
    }

    /** The new state; for an activation always {@link BonusState#ACTIVE}. */
    public BonusState getState() {
        return state;
    }

    /** The reason detail, for example the missing set and count or the failing condition. */
    public String getDetail() {
        return detail;
    }
}
