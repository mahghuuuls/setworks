package com.mahghuuuls.setworks.compat.crafttweaker;

import com.mahghuuuls.setworks.api.BonusListener;
import com.mahghuuuls.setworks.api.BonusState;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.event.IEventHandle;
import crafttweaker.api.event.IEventManager;
import crafttweaker.util.EventList;
import crafttweaker.util.IEventHandler;
import net.minecraft.entity.player.EntityPlayer;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenExpansion;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.List;

/**
 * Script subscriptions to bonus transitions: {@code events.onSetworksBonusActivated(function(event as mods.setworks.BonusEvent) {...})}
 * and {@code onSetworksBonusDeactivated}, or the same two on {@code mods.setworks.Events}. One
 * Setworks listener, registered by the bridge at load complete, publishes to the two lists;
 * CraftTweaker's event list catches a handler that throws, logs it to crafttweaker.log, and goes on
 * with the others.
 */
@ZenRegister
@ZenClass("mods.setworks.Events")
public final class ZenEvents {

    private static final EventList<ZenBonusEvent> ACTIVATED = new EventList<ZenBonusEvent>();
    private static final EventList<ZenBonusEvent> DEACTIVATED = new EventList<ZenBonusEvent>();

    private ZenEvents() {
    }

    @ZenMethod
    public static IEventHandle onBonusActivated(IEventHandler<ZenBonusEvent> handler) {
        return ACTIVATED.add(handler);
    }

    @ZenMethod
    public static IEventHandle onBonusDeactivated(IEventHandler<ZenBonusEvent> handler) {
        return DEACTIVATED.add(handler);
    }

    /** The Setworks listener that feeds the script handlers. */
    static BonusListener listener() {
        return new BonusListener() {
            @Override
            public void onActivated(EntityPlayer player, String bonusId, List<String> setIds) {
                if (ACTIVATED.hasHandlers()) {
                    ACTIVATED.publish(new ZenBonusEvent(player, bonusId, setIds, BonusState.ACTIVE, ""));
                }
            }

            @Override
            public void onDeactivated(EntityPlayer player, String bonusId, List<String> setIds, BonusState state, String detail) {
                if (DEACTIVATED.hasHandlers()) {
                    DEACTIVATED.publish(new ZenBonusEvent(player, bonusId, setIds, state, detail));
                }
            }
        };
    }

    /** {@code events.onSetworksBonusActivated(...)} and {@code events.onSetworksBonusDeactivated(...)}. */
    @ZenRegister
    @ZenExpansion("crafttweaker.events.IEventManager")
    public static final class ManagerExpansion {
        private ManagerExpansion() {
        }

        @ZenMethod
        public static IEventHandle onSetworksBonusActivated(IEventManager manager, IEventHandler<ZenBonusEvent> handler) {
            return onBonusActivated(handler);
        }

        @ZenMethod
        public static IEventHandle onSetworksBonusDeactivated(IEventManager manager, IEventHandler<ZenBonusEvent> handler) {
            return onBonusDeactivated(handler);
        }
    }
}
