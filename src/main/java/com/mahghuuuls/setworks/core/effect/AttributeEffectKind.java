package com.mahghuuuls.setworks.core.effect;

import com.mahghuuuls.setworks.api.CombinationRule;
import com.mahghuuuls.setworks.api.IEffectKind;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.Map;
import java.util.UUID;

/**
 * The built-in attribute modifier effect: one vanilla {@link AttributeModifier} per instance, keyed
 * by a UUID derived from the instance id, never saved to the player file. Independent instances
 * add up (two {@code generic.armor add 2} give +4). The modifier is applied once: a second apply of
 * the same instance finds the UUID already present and does nothing.
 */
public final class AttributeEffectKind implements IEffectKind {

    public static final String ID = "setworks:attribute";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public CombinationRule combinationRule() {
        return CombinationRule.SUM;
    }

    @Override
    public String validate(Map<String, String> params) {
        return AttributeParams.problem(params);
    }

    @Override
    public String describe(Map<String, String> params) {
        try {
            return AttributeParams.parse(params).describe();
        } catch (IllegalArgumentException e) {
            return "attribute (invalid: " + e.getMessage() + ")";
        }
    }

    @Override
    public void apply(EntityPlayerMP player, String instanceId, Map<String, String> params) {
        AttributeParams parsed = AttributeParams.parse(params);
        IAttributeInstance attribute = player.getAttributeMap().getAttributeInstanceByName(parsed.attribute());
        if (attribute == null) {
            throw new IllegalArgumentException("attribute " + parsed.attribute() + " does not exist on players");
        }
        UUID uuid = EffectIds.attributeUuid(instanceId);
        if (attribute.getModifier(uuid) == null) {
            attribute.applyModifier(new AttributeModifier(uuid, "setworks " + instanceId, parsed.amount(), parsed.operation())
                    .setSaved(false));
        }
    }

    @Override
    public void withdraw(EntityPlayerMP player, String instanceId, Map<String, String> params) {
        AttributeParams parsed = AttributeParams.parse(params);
        removeIfPresent(player, instanceId, parsed.attribute());
    }

    /** Removes this instance's modifier when the player carries it; used by withdraw and by the rebuild reconciliation. */
    public static boolean removeIfPresent(EntityPlayerMP player, String instanceId, String attributeName) {
        IAttributeInstance attribute = player.getAttributeMap().getAttributeInstanceByName(attributeName);
        if (attribute == null) {
            return false;
        }
        UUID uuid = EffectIds.attributeUuid(instanceId);
        if (attribute.getModifier(uuid) == null) {
            return false;
        }
        attribute.removeModifier(uuid);
        return true;
    }

    /** The player's attribute map as a {@link ModifierStore}, for the rebuild sweep. */
    public static ModifierStore storeOf(final EntityPlayerMP player) {
        return new ModifierStore() {
            @Override
            public boolean remove(String attributeName, UUID modifierId) {
                IAttributeInstance attribute = player.getAttributeMap().getAttributeInstanceByName(attributeName);
                if (attribute == null || attribute.getModifier(modifierId) == null) {
                    return false;
                }
                attribute.removeModifier(modifierId);
                return true;
            }
        };
    }

    /** True when the player currently carries this instance's modifier. */
    public static boolean isPresent(EntityPlayerMP player, String instanceId, String attributeName) {
        IAttributeInstance attribute = player.getAttributeMap().getAttributeInstanceByName(attributeName);
        return attribute != null && attribute.getModifier(EffectIds.attributeUuid(instanceId)) != null;
    }
}
