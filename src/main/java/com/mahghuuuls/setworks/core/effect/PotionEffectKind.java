package com.mahghuuuls.setworks.core.effect;

import com.mahghuuuls.setworks.api.CombinationRule;
import com.mahghuuuls.setworks.api.IEffectKind;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Map;

/**
 * The built-in potion effect: a short effect ({@link PotionSignature#DURATION_TICKS} ticks) that is
 * checked every {@link PotionSignature#REFRESH_INTERVAL_TICKS} ticks while the bonus is active and
 * topped up only when it has run down to {@link PotionSignature#TOP_UP_BELOW_TICKS} ticks, so the
 * countdown runs long enough between top-ups for periodic potions to pulse at the vanilla rate.
 * Vanilla merges effects of one potion by keeping the higher amplifier, so between Setworks
 * sources the highest level wins. On withdrawal the effect is removed only when it still matches
 * the Setworks signature; a longer effect from another source stays. Vanilla copies the particle
 * flag of the most recent application and never restores it, so a player's own longer effect of the
 * same potion keeps the bonus's particle setting after the bonus withdraws.
 */
public final class PotionEffectKind implements IEffectKind {

    public static final String ID = "setworks:potion";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public CombinationRule combinationRule() {
        return CombinationRule.HIGHEST_WINS;
    }

    @Override
    public String validate(Map<String, String> params) {
        String problem = PotionSignature.problem(params);
        if (problem != null) {
            return problem;
        }
        PotionSignature.Params parsed = PotionSignature.parse(params);
        if (find(parsed.potionId()) == null) {
            return "potion " + parsed.potionId() + " is not registered";
        }
        return null;
    }

    @Override
    public String describe(Map<String, String> params) {
        try {
            return PotionSignature.parse(params).describe();
        } catch (IllegalArgumentException e) {
            return "potion (invalid: " + e.getMessage() + ")";
        }
    }

    @Override
    public void apply(EntityPlayerMP player, String instanceId, Map<String, String> params) {
        renew(player, PotionSignature.parse(params));
    }

    @Override
    public void refresh(EntityPlayerMP player, String instanceId, Map<String, String> params) {
        PotionSignature.Params parsed = PotionSignature.parse(params);
        PotionEffect current = player.getActivePotionEffect(require(parsed.potionId()));
        // A lower Setworks level under a higher one sees the higher effect here and leaves it alone; vanilla would ignore the re-add anyway.
        if (PotionSignature.needsTopUp(current != null, current == null ? 0 : current.getDuration())) {
            renew(player, parsed);
        }
    }

    @Override
    public boolean needsRefresh() {
        return true;
    }

    @Override
    public void withdraw(EntityPlayerMP player, String instanceId, Map<String, String> params) {
        PotionSignature.Params parsed = PotionSignature.parse(params);
        Potion potion = require(parsed.potionId());
        PotionEffect current = player.getActivePotionEffect(potion);
        if (current != null && PotionSignature.shouldRemove(parsed.amplifier(), current.getAmplifier(), current.getDuration())) {
            player.removePotionEffect(potion);
        }
    }

    private static void renew(EntityPlayerMP player, PotionSignature.Params parsed) {
        Potion potion = require(parsed.potionId());
        player.addPotionEffect(new PotionEffect(potion, PotionSignature.DURATION_TICKS, parsed.amplifier(), false, parsed.particles()));
    }

    private static Potion require(String potionId) {
        Potion potion = find(potionId);
        if (potion == null) {
            throw new IllegalArgumentException("potion " + potionId + " is not registered");
        }
        return potion;
    }

    private static Potion find(String potionId) {
        ResourceLocation key;
        try {
            key = new ResourceLocation(potionId);
        } catch (RuntimeException e) {
            return null;
        }
        return ForgeRegistries.POTIONS.getValue(key);
    }
}
