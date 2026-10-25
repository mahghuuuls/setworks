package com.mahghuuuls.setworks.api;

import net.minecraft.entity.player.EntityPlayerMP;

import java.util.Map;

/**
 * One kind of effect a bonus can carry, registered by id with {@link SetworksApi#registerEffectKind}
 * during startup. Kinds are stateless: Setworks keeps the record of what is applied to whom, calls
 * {@link #apply} when a bonus becomes active and {@link #withdraw} when it becomes inactive, and
 * gives every effect instance a stable id derived from the set, the bonus, and the effect's
 * position in the bonus. The same instance id is used again after a respawn, a dimension change,
 * or a relog, so a kind that keys something on the instance id (an attribute modifier UUID, for
 * example) finds its own earlier contribution.
 *
 * <p>Contract: applying an instance that is already applied must be a no-op; withdrawing an
 * absent one must be a no-op. Withdraw removes only this instance's own contribution. An effect that
 * raises a resource maximum (health, or an addon's stamina) must not change the current value;
 * one that lowers it clamps the current value to the new maximum. A kind that throws is recorded as
 * failed for that instance and logged once; the other effects and bonuses continue. An instance whose
 * {@code apply} threw is never passed to {@code withdraw}, so a kind must leave nothing behind when
 * it throws.
 */
public interface IEffectKind {

    /** The kind id authors write in a bonus, for example {@code setworks:attribute} or {@code mymod:stamina}. */
    String id();

    /** What happens when two instances of this kind are active on one player. */
    CombinationRule combinationRule();

    /**
     * Checks parameters once, at validation. Returns {@code null} when they are usable, or a
     * short problem text; a bonus with an unusable effect is disabled with that text in the report.
     */
    default String validate(Map<String, String> params) {
        return null;
    }

    /** One short line for operators and generated descriptions, for example {@code generic.armor add 2}. */
    String describe(Map<String, String> params);

    void apply(EntityPlayerMP player, String instanceId, Map<String, String> params);

    void withdraw(EntityPlayerMP player, String instanceId, Map<String, String> params);

    /** Called every 20 ticks while the instance is applied, when {@link #needsRefresh()} is true. */
    default void refresh(EntityPlayerMP player, String instanceId, Map<String, String> params) {
    }

    /** True for kinds whose contribution expires unless renewed, such as a short potion effect. */
    default boolean needsRefresh() {
        return false;
    }
}
