package com.mahghuuuls.setworks.compat.crafttweaker;

import com.mahghuuuls.setworks.api.SetworksApi;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.player.IPlayer;
import net.minecraft.entity.player.EntityPlayer;
import stanhebben.zenscript.annotations.ZenExpansion;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.Collections;
import java.util.Map;

/**
 * The player queries on {@code IPlayer}: {@code player.setworksPieceCount("pack:iron")},
 * {@code player.hasSetworksBonus("pack:iron_full")}, {@code player.setworksBonusState(...)},
 * {@code player.setworksSatisfiedPieces(...)}. They read the last evaluated state and never
 * evaluate, apply, or withdraw anything. On the client they answer from the synced state.
 */
@ZenRegister
@ZenExpansion("crafttweaker.player.IPlayer")
public final class ZenPlayerExpansions {

    private ZenPlayerExpansions() {
    }

    /** Satisfied pieces of the set; 0 for an unknown set or player. */
    @ZenMethod
    public static int setworksPieceCount(IPlayer player, String setId) {
        EntityPlayer entity = ZenSetworks.player(player);
        return entity == null ? 0 : SetworksApi.query().pieceCount(entity, setId);
    }

    /** True while the bonus is active for the player. */
    @ZenMethod
    public static boolean hasSetworksBonus(IPlayer player, String bonusId) {
        EntityPlayer entity = ZenSetworks.player(player);
        return entity != null && SetworksApi.query().isActive(entity, bonusId);
    }

    /** {@code "active"}, {@code "missing equipment"}, {@code "condition failed"}, {@code "condition error"}, {@code "superseded"}, {@code "spectator"}, or {@code "dead"}. */
    @ZenMethod
    public static String setworksBonusState(IPlayer player, String bonusId) {
        EntityPlayer entity = ZenSetworks.player(player);
        if (entity == null) {
            return "missing equipment";
        }
        return SetworksApi.query().bonusState(entity, bonusId).reasonText();
    }

    /** Piece name to the slot filling it, for the set and player; empty when none. */
    @ZenMethod
    public static Map<String, String> setworksSatisfiedPieces(IPlayer player, String setId) {
        EntityPlayer entity = ZenSetworks.player(player);
        if (entity == null) {
            return Collections.emptyMap();
        }
        return SetworksApi.query().satisfiedPieces(entity, setId);
    }
}
