package com.mahghuuuls.setworks.compat.crafttweaker;

import com.mahghuuuls.setworks.api.BonusState;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.minecraft.CraftTweakerMC;
import crafttweaker.api.player.IPlayer;
import net.minecraft.entity.player.EntityPlayer;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;

import java.util.List;

/**
 * A bonus became active or inactive for a player, on the server. Read-only: handlers observe.
 * After a respawn or dimension change the state is rebuilt, so an activation may arrive without a
 * deactivation before it.
 */
@ZenRegister
@ZenClass("mods.setworks.BonusEvent")
public final class ZenBonusEvent {

    private final EntityPlayer player;
    private final String bonusId;
    private final String[] setIds;
    private final BonusState state;
    private final String detail;

    ZenBonusEvent(EntityPlayer player, String bonusId, List<String> setIds, BonusState state, String detail) {
        this.player = player;
        this.bonusId = bonusId;
        this.setIds = setIds.toArray(new String[setIds.size()]);
        this.state = state;
        this.detail = detail == null ? "" : detail;
    }

    @ZenGetter("player")
    public IPlayer getPlayer() {
        return CraftTweakerMC.getIPlayer(player);
    }

    @ZenGetter("bonusId")
    public String getBonusId() {
        return bonusId;
    }

    /** The sets the bonus requires. */
    @ZenGetter("setIds")
    public String[] getSetIds() {
        return setIds.clone();
    }

    @ZenGetter("active")
    public boolean isActive() {
        return state.isActive();
    }

    /** {@code "active"} for an activation; otherwise the reason: {@code "missing equipment"}, {@code "condition failed"}, {@code "condition error"}, {@code "superseded"}, {@code "spectator"}, {@code "dead"}. */
    @ZenGetter("reason")
    public String getReason() {
        return state.reasonText();
    }

    /** The reason detail, for example the missing set and count or the failing condition. */
    @ZenGetter("detail")
    public String getDetail() {
        return detail;
    }
}
