package com.mahghuuuls.setworks.core.eval;

import com.mahghuuuls.setworks.core.effect.EffectManager;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.List;
import java.util.UUID;

/**
 * Feeds the evaluator's transitions to the {@link EffectManager}: deactivations first, then
 * activations, then the after-batch step. The manager knows players by UUID and name and never
 * sees evaluation types; this adapter is the only place that knows both sides.
 */
public final class EffectSink implements TransitionSink {

    private final EffectManager effects;

    public EffectSink(EffectManager effects) {
        this.effects = effects;
    }

    @Override
    public void onRebuild(EntityPlayerMP player) {
        effects.onRebuild(player.getUniqueID(), player.getName(), player);
    }

    @Override
    public void onTransitions(EntityPlayerMP player, PlayerSetState state, List<Transition> transitions) {
        UUID id = player.getUniqueID();
        String name = player.getName();
        boolean changed = false;
        for (Transition transition : transitions) {
            if (transition.deactivated()) {
                changed |= effects.deactivate(id, name, player, transition.bonusId());
            }
        }
        for (Transition transition : transitions) {
            if (transition.activated()) {
                changed |= effects.activate(id, name, player, transition.bonusId());
            }
        }
        if (changed) {
            effects.afterTransitions(id, name, player);
        }
    }

    @Override
    public void onLogout(EntityPlayerMP player, PlayerSetState lastState) {
        effects.onLogout(player.getUniqueID(), player.getName(), player);
    }
}
