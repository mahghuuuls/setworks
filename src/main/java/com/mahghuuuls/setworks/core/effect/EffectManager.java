package com.mahghuuuls.setworks.core.effect;

import com.mahghuuuls.setworks.api.IEffectKind;
import com.mahghuuuls.setworks.api.definition.BonusDefinition;
import com.mahghuuuls.setworks.core.diagnostic.DiagnosticLog;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Turns bonus activations and deactivations into effect kind calls and keeps the
 * {@link EffectLedger}. The caller (the evaluation side) reports deactivations before activations
 * and then calls {@link #afterTransitions}, so a bonus swap never has two contributions at once. A
 * rebuild (login, respawn, dimension change) empties the player's ledger and removes any Setworks
 * attribute modifier still on the entity, found by its derived UUID, before the active bonuses are
 * re-applied; the modifiers then exist exactly once. Potion instances are renewed every 20 ticks.
 * A kind that throws is recorded as failed for that instance and logged once.
 *
 * <p>Players are addressed by UUID and name; the entity is handed to the kinds and used for the
 * attribute sweep and the health clamp. It is {@code null} only where no game runs (tests).
 */
public final class EffectManager {

    private static final class AttributeInstance {
        final EffectLedger.Instance instance;
        final String attributeName;

        AttributeInstance(EffectLedger.Instance instance, String attributeName) {
            this.instance = instance;
            this.attributeName = attributeName;
        }
    }

    private final EffectLedger ledger;
    private final EffectKindRegistry kinds;
    private final Logger logger;
    private final DiagnosticLog diagnostic;
    private final Set<String> loggedFailures = new HashSet<String>();
    private final List<AttributeInstance> attributeInstances = new ArrayList<AttributeInstance>();

    public EffectManager(EffectLedger ledger, EffectKindRegistry kinds, Logger logger, DiagnosticLog diagnostic) {
        this.ledger = ledger;
        this.kinds = kinds;
        this.logger = logger;
        this.diagnostic = diagnostic;
        for (EffectLedger.Instance instance : ledger.allInstances()) {
            if (AttributeEffectKind.ID.equals(instance.kindId()) && AttributeParams.problem(instance.spec().params()) == null) {
                attributeInstances.add(new AttributeInstance(instance, AttributeParams.parse(instance.spec().params()).attribute()));
            }
        }
    }

    public EffectLedger ledger() {
        return ledger;
    }

    public EffectKindRegistry kinds() {
        return kinds;
    }

    // ---- lifecycle, called by the evaluation side ----

    /** The player's state is rebuilt from nothing: forget the ledger and sweep stale modifiers off the entity. */
    public void onRebuild(UUID playerId, String playerName, EntityPlayerMP entity) {
        ledger.clear(playerId);
        if (entity != null) {
            reconcile(playerName, AttributeEffectKind.storeOf(entity));
        }
    }

    /** Removes every Setworks attribute modifier the store still carries. Exposed for the fake-store test. */
    void reconcile(String playerName, ModifierStore store) {
        int removed = 0;
        for (AttributeInstance attribute : attributeInstances) {
            if (store.remove(attribute.attributeName, EffectIds.attributeUuid(attribute.instance.id()))) {
                removed++;
            }
        }
        if (removed > 0) {
            diagnostic.line("effect reconcile player=" + playerName + " removed " + removed + " stale attribute modifier(s)");
        }
    }

    /** The bonus became inactive: withdraw its applied instances. Returns true when anything was withdrawn. */
    public boolean deactivate(UUID playerId, String playerName, EntityPlayerMP entity, String bonusId) {
        boolean changed = false;
        for (EffectLedger.Instance instance : ledger.deactivate(playerId, bonusId)) {
            withdraw(playerId, playerName, entity, instance);
            changed = true;
        }
        return changed;
    }

    /** The bonus became active: apply its instances that are not applied yet. Returns true when anything was applied. */
    public boolean activate(UUID playerId, String playerName, EntityPlayerMP entity, String bonusId) {
        boolean changed = false;
        for (EffectLedger.Instance instance : ledger.activate(playerId, bonusId)) {
            apply(playerId, playerName, entity, instance);
            changed = true;
        }
        return changed;
    }

    /**
     * After a batch of deactivations and activations: a withdrawn potion instance may have removed
     * the shared effect, so the remaining ones renew it now, and current health is clamped to a
     * possibly lowered maximum.
     */
    public void afterTransitions(UUID playerId, String playerName, EntityPlayerMP entity) {
        refreshAll(playerId, playerName, entity);
        if (entity != null) {
            clampHealth(entity);
        }
    }

    /** The player left: withdraw everything and forget the player. */
    public void onLogout(UUID playerId, String playerName, EntityPlayerMP entity) {
        for (EffectLedger.Instance instance : ledger.clear(playerId)) {
            withdraw(playerId, playerName, entity, instance);
        }
    }

    // ---- refresh ----

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !event.side.isServer()
                || !(event.player instanceof EntityPlayerMP) || event.player instanceof FakePlayer) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        if (player.ticksExisted % PotionSignature.REFRESH_INTERVAL_TICKS != 0) {
            return;
        }
        refreshAll(player.getUniqueID(), player.getName(), player);
    }

    private void refreshAll(UUID playerId, String playerName, EntityPlayerMP entity) {
        Collection<EffectLedger.Instance> applied = ledger.applied(playerId);
        if (applied.isEmpty()) {
            return;
        }
        // A copy: a failing refresh removes its instance from the applied records.
        for (EffectLedger.Instance instance : new ArrayList<EffectLedger.Instance>(applied)) {
            IEffectKind kind = kinds.get(instance.kindId());
            if (kind == null || !kind.needsRefresh()) {
                continue;
            }
            try {
                kind.refresh(entity, instance.id(), instance.spec().params());
            } catch (Throwable t) {
                fail(playerId, playerName, instance, "refresh", t);
            }
        }
    }

    // ---- kinds ----

    private void apply(UUID playerId, String playerName, EntityPlayerMP entity, EffectLedger.Instance instance) {
        IEffectKind kind = kinds.get(instance.kindId());
        if (kind == null) {
            // Validation disables a bonus with an unknown kind, so this is reached only by a kind registry
            // that changed after the freeze; recorded like any other failure.
            fail(playerId, playerName, instance, "apply", new IllegalStateException("effect kind " + instance.kindId() + " is not registered"));
            return;
        }
        try {
            kind.apply(entity, instance.id(), instance.spec().params());
            diagnostic.line("effect player=" + playerName + " applied " + instance.id());
        } catch (Throwable t) {
            fail(playerId, playerName, instance, "apply", t);
        }
    }

    private void withdraw(UUID playerId, String playerName, EntityPlayerMP entity, EffectLedger.Instance instance) {
        IEffectKind kind = kinds.get(instance.kindId());
        if (kind == null) {
            return;
        }
        try {
            kind.withdraw(entity, instance.id(), instance.spec().params());
            diagnostic.line("effect player=" + playerName + " withdrew " + instance.id());
        } catch (Throwable t) {
            fail(playerId, playerName, instance, "withdraw", t);
        }
    }

    private void fail(UUID playerId, String playerName, EffectLedger.Instance instance, String action, Throwable t) {
        ledger.markFailed(playerId, instance, action + " threw " + t.toString());
        if (loggedFailures.add(instance.id() + "|" + action)) {
            logger.warn("effect {} of bonus {} failed to {} for {} (logged once per effect)", instance.id(),
                    instance.bonusId(), action, playerName, t);
        }
    }

    /** True once a failure of this instance and action was logged; later ones are recorded in the ledger only. */
    boolean hasLoggedFailure(String instanceId, String action) {
        return loggedFailures.contains(instanceId + "|" + action);
    }

    private static void clampHealth(EntityPlayerMP player) {
        float health = player.getHealth();
        float max = player.getMaxHealth();
        if (HealthPolicy.needsClamp(health, max)) {
            player.setHealth(HealthPolicy.healthAfterMaximumChange(health, max));
        }
    }

    // ---- inspect ----

    /** An addon's description, or its failure text; one broken describe must not break the inspect command. */
    private static String safeDescribe(IEffectKind kind, Map<String, String> params) {
        try {
            String text = kind.describe(params);
            return text == null ? "" : text;
        } catch (RuntimeException e) {
            return "(describe threw " + e + ")";
        }
    }

    /** One line per effect of the bonus: description, combination rule, and whether it is applied, failed, or inactive for the player. */
    public List<String> effectLines(UUID player, BonusDefinition bonus) {
        return effectLines(player, bonus, null);
    }

    /** As above; with the entity, an attribute effect's line also reads the attribute's current value from the player (vanilla's own number). */
    public List<String> effectLines(UUID player, BonusDefinition bonus, EntityPlayerMP entity) {
        List<String> lines = new ArrayList<String>();
        for (EffectLedger.Instance instance : ledger.instancesOf(bonus.id())) {
            IEffectKind kind = kinds.get(instance.kindId());
            StringBuilder line = new StringBuilder("  effect ").append(instance.index()).append(": ");
            if (kind == null) {
                line.append(instance.kindId()).append(" (unknown kind)");
            } else {
                line.append(safeDescribe(kind, instance.spec().params())).append(" (").append(kind.combinationRule().describe()).append(')');
            }
            String failure = ledger.failure(player, instance.id());
            if (failure != null) {
                line.append(" failed: ").append(failure);
            } else if (ledger.isApplied(player, instance.id())) {
                line.append(" applied");
            } else {
                line.append(" not applied");
            }
            if (entity != null && AttributeEffectKind.ID.equals(instance.kindId())) {
                String attribute = AttributeParams.problem(instance.spec().params()) == null ? AttributeParams.parse(instance.spec().params()).attribute() : null;
                if (attribute != null) {
                    net.minecraft.entity.ai.attributes.IAttributeInstance live = entity.getAttributeMap().getAttributeInstanceByName(attribute);
                    if (live != null) {
                        line.append(" now ").append(AttributeParams.formatAmount(live.getAttributeValue()));
                    }
                }
            }
            line.append(" [").append(instance.id()).append(']');
            lines.add(line.toString());
        }
        return lines;
    }
}
