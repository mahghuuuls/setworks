package com.mahghuuuls.setworks.compat.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.player.IPlayer;
import stanhebben.zenscript.annotations.ZenClass;

/**
 * A script function used as a bonus condition: {@code function(player as IPlayer) as bool}. It
 * runs on the server, right after the player's equipment changed and otherwise every condition
 * interval, never on the client and never from a query or a tooltip. It must have no side effects.
 * One that throws makes its bonus inactive with reason "condition error", logged once.
 */
@ZenRegister
@ZenClass("mods.setworks.ICondition")
public interface IZenCondition {
    boolean test(IPlayer player);
}
