package com.mahghuuuls.setworks.core.eval;

import com.mahghuuuls.setworks.api.BonusListener;
import com.mahghuuuls.setworks.api.definition.BonusDefinition;
import com.mahghuuuls.setworks.api.definition.SetRequirement;
import com.mahghuuuls.setworks.api.event.BonusActivatedEvent;
import com.mahghuuuls.setworks.api.event.BonusDeactivatedEvent;
import com.mahghuuuls.setworks.core.match.CompiledDefinitions;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Delivers transitions to Setworks listeners, each in its own guard, and then to the Forge event
 * bus. A listener or handler that throws is logged once per listener class and never stops the
 * other listeners or the evaluation. The Forge bus stops its own remaining handlers on a throw;
 * that is Forge's contract, not Setworks's.
 */
public final class ListenerDispatch implements TransitionSink, com.mahghuuuls.setworks.api.SetworksApi.ListenerRegistry {

    private final CompiledDefinitions definitions;
    private final Logger logger;
    private final List<BonusListener> listeners = new CopyOnWriteArrayList<BonusListener>();
    private final Set<String> logged = new HashSet<String>();

    public ListenerDispatch(CompiledDefinitions definitions, Logger logger) {
        this.definitions = definitions;
        this.logger = logger;
    }

    @Override
    public void add(BonusListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void remove(BonusListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void onTransitions(EntityPlayerMP player, PlayerSetState state, List<Transition> transitions) {
        for (Transition transition : transitions) {
            if (!transition.activated() && !transition.deactivated()) {
                continue;
            }
            BonusDefinition bonus = definitions.bonus(transition.bonusId());
            if (bonus == null) {
                continue;
            }
            List<String> setIds = new ArrayList<String>();
            for (SetRequirement requirement : bonus.requirements()) {
                setIds.add(requirement.setId());
            }
            for (BonusListener listener : listeners) {
                try {
                    if (transition.activated()) {
                        listener.onActivated(player, bonus.id(), setIds);
                    } else {
                        listener.onDeactivated(player, bonus.id(), setIds, transition.to(), transition.detail());
                    }
                } catch (Throwable t) {
                    warnOnce("listener " + listener.getClass().getName(), t);
                }
            }
            try {
                if (transition.activated()) {
                    MinecraftForge.EVENT_BUS.post(new BonusActivatedEvent(player, bonus.id(), setIds));
                } else {
                    MinecraftForge.EVENT_BUS.post(new BonusDeactivatedEvent(player, bonus.id(), setIds, transition.to(), transition.detail()));
                }
            } catch (Throwable t) {
                warnOnce("forge event handler " + rootCauseClass(t), t);
            }
        }
    }

    @Override
    public void onLogout(EntityPlayerMP player, PlayerSetState lastState) {
        // Logout is not a transition; listeners that care observe the login/logout events of Forge.
    }

    private void warnOnce(String key, Throwable t) {
        if (logged.add(key)) {
            logger.warn("a bonus {} threw and was skipped (logged once per source): {}", key, t.toString());
        }
    }

    private static String rootCauseClass(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        StackTraceElement[] trace = cause.getStackTrace();
        return cause.getClass().getName() + (trace.length > 0 ? "@" + trace[0].getClassName() : "");
    }
}
