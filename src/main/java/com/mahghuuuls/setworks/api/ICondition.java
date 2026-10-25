package com.mahghuuuls.setworks.api;

import net.minecraft.entity.player.EntityPlayerMP;

/**
 * A test a bonus must pass besides its equipment: all conditions of a bonus must return true for
 * it to be active. Setworks calls it on the server right after the player's equipment changed and
 * otherwise every condition interval (the global setting, or the bonus's own), never per tick
 * unless the interval is 1 and never from queries or tooltips. A condition must have no side
 * effects and must not keep state between calls: it may run more or less often than the author
 * expects, and the same instance serves every player. A condition that throws makes its bonus
 * inactive with reason "condition error"; Setworks logs the error once and again only after the
 * condition has evaluated without error for any player (the episode belongs to the condition, which
 * serves every player, not to one player).
 */
public interface ICondition {

    boolean test(EntityPlayerMP player, IConditionContext context);
}
