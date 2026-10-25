package com.mahghuuuls.setworks.core.effect;

import com.mahghuuuls.setworks.api.CombinationRule;
import com.mahghuuuls.setworks.api.IEffectKind;
import com.mahghuuuls.setworks.core.definition.FrozenDefinitions;
import com.mahghuuuls.setworks.core.diagnostic.DiagnosticLog;
import net.minecraft.entity.player.EntityPlayerMP;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The manager's bookkeeping with fake kinds and no game: call order, refresh after a batch,
 * failure recording, the rebuild sweep over a fake modifier store. The entity is null throughout;
 * the fakes never touch it.
 */
class EffectManagerTest {

    private static final UUID ALICE = UUID.nameUUIDFromBytes("alice".getBytes());

    /** Records every call as "<action> <instanceId>"; throws on apply when told to. */
    private static final class RecordingKind implements IEffectKind {
        final String id;
        final boolean refreshable;
        final boolean failOnApply;
        final List<String> calls = new ArrayList<String>();

        RecordingKind(String id, boolean refreshable, boolean failOnApply) {
            this.id = id;
            this.refreshable = refreshable;
            this.failOnApply = failOnApply;
        }

        public String id() { return id; }
        public CombinationRule combinationRule() { return CombinationRule.SUM; }
        public String describe(Map<String, String> params) { return id; }
        public boolean needsRefresh() { return refreshable; }

        public void apply(EntityPlayerMP player, String instanceId, Map<String, String> params) {
            calls.add("apply " + instanceId);
            if (failOnApply) {
                throw new IllegalStateException("fake failure");
            }
        }

        public void withdraw(EntityPlayerMP player, String instanceId, Map<String, String> params) {
            calls.add("withdraw " + instanceId);
        }

        public void refresh(EntityPlayerMP player, String instanceId, Map<String, String> params) {
            calls.add("refresh " + instanceId);
        }
    }

    private static final class FakeStore implements ModifierStore {
        final Map<String, List<UUID>> modifiers = new HashMap<String, List<UUID>>();
        final List<String> removals = new ArrayList<String>();

        void put(String attribute, UUID id) {
            List<UUID> list = modifiers.get(attribute);
            if (list == null) {
                list = new ArrayList<UUID>();
                modifiers.put(attribute, list);
            }
            list.add(id);
        }

        public boolean remove(String attributeName, UUID modifierId) {
            removals.add(attributeName + " " + modifierId);
            List<UUID> list = modifiers.get(attributeName);
            return list != null && list.remove(modifierId);
        }
    }

    private static EffectKindRegistry kinds(IEffectKind... kinds) {
        EffectKindRegistry registry = new EffectKindRegistry();
        for (IEffectKind kind : kinds) {
            assertTrue(registry.register(kind).isOk());
        }
        return registry;
    }

    private static EffectManager manager(FrozenDefinitions definitions, EffectKindRegistry kinds) {
        return new EffectManager(new EffectLedger(definitions), kinds, LogManager.getLogger("test"), new DiagnosticLog(LogManager.getLogger("test"), false));
    }

    @Test
    void activationsApplyOnceWithdrawalsReturnWhatWasAppliedAndRefreshFollowsTheBatch() {
        RecordingKind attribute = new RecordingKind("setworks:attribute", false, false);
        RecordingKind potion = new RecordingKind("setworks:potion", true, false);
        EffectManager manager = manager(EffectLedgerTest.definitions(), kinds(attribute, potion));
        // Two pieces on: iron2 active.
        assertTrue(manager.activate(ALICE, "alice", null, "t:iron2"));
        assertFalse(manager.activate(ALICE, "alice", null, "t:iron2"), "a second activation applies nothing");
        // Full set: iron4 joins. The deactivate-before-activate order of a batch is the sink adapter's job.
        assertTrue(manager.activate(ALICE, "alice", null, "t:iron4"));
        manager.afterTransitions(ALICE, "alice", null);
        assertEquals(Arrays.asList("apply t:iron/t:iron2/0", "apply t:iron/t:iron4/0"), attribute.calls);
        assertEquals(Arrays.asList("apply t:iron/t:iron4/1", "refresh t:iron/t:iron4/1"), potion.calls,
                "only the refreshable kind is renewed after the batch");
        // Boots off: iron4 withdrawn, iron2 stays; the attribute kind is never refreshed.
        potion.calls.clear();
        attribute.calls.clear();
        assertTrue(manager.deactivate(ALICE, "alice", null, "t:iron4"));
        assertFalse(manager.deactivate(ALICE, "alice", null, "t:iron4"));
        manager.afterTransitions(ALICE, "alice", null);
        assertEquals(Arrays.asList("withdraw t:iron/t:iron4/0"), attribute.calls);
        assertEquals(Arrays.asList("withdraw t:iron/t:iron4/1"), potion.calls, "nothing of the potion kind is left to refresh");
        assertTrue(manager.ledger().isApplied(ALICE, "t:iron/t:iron2/0"));
        assertFalse(manager.ledger().isApplied(ALICE, "t:iron/t:iron4/0"));
        // Logout withdraws what is left and forgets the player.
        attribute.calls.clear();
        manager.onLogout(ALICE, "alice", null);
        assertEquals(Arrays.asList("withdraw t:iron/t:iron2/0"), attribute.calls);
        assertEquals(0, manager.ledger().playerCount());
    }

    @Test
    void aThrowingKindIsRecordedAsFailedAndNeverWithdrawn() {
        RecordingKind attribute = new RecordingKind("setworks:attribute", false, true);
        RecordingKind potion = new RecordingKind("setworks:potion", true, false);
        EffectManager manager = manager(EffectLedgerTest.definitions(), kinds(attribute, potion));
        assertTrue(manager.activate(ALICE, "alice", null, "t:iron4"));
        String failure = manager.ledger().failure(ALICE, "t:iron/t:iron4/0");
        assertNotNull(failure);
        assertTrue(failure.contains("fake failure"), failure);
        assertFalse(manager.ledger().isApplied(ALICE, "t:iron/t:iron4/0"));
        assertTrue(manager.ledger().isApplied(ALICE, "t:iron/t:iron4/1"), "the other effect of the bonus still applies");
        assertTrue(manager.hasLoggedFailure("t:iron/t:iron4/0", "apply"));
        assertFalse(manager.hasLoggedFailure("t:iron/t:iron4/0", "withdraw"));
        // Deactivation withdraws only what was applied.
        manager.deactivate(ALICE, "alice", null, "t:iron4");
        assertEquals(Arrays.asList("apply t:iron/t:iron4/0"), attribute.calls, "a failed instance is not withdrawn");
        assertEquals(Arrays.asList("apply t:iron/t:iron4/1", "withdraw t:iron/t:iron4/1"), potion.calls);
        assertNull(manager.ledger().failure(ALICE, "t:iron/t:iron4/0"));
    }

    @Test
    void unknownKindAtApplyIsAFailureNotACrash() {
        RecordingKind attribute = new RecordingKind("setworks:attribute", false, false);
        EffectManager manager = manager(EffectLedgerTest.definitions(), kinds(attribute));
        assertTrue(manager.activate(ALICE, "alice", null, "t:iron4"));
        assertTrue(manager.ledger().isApplied(ALICE, "t:iron/t:iron4/0"));
        String failure = manager.ledger().failure(ALICE, "t:iron/t:iron4/1");
        assertNotNull(failure);
        assertTrue(failure.contains("not registered"), failure);
    }

    @Test
    void rebuildForgetsTheLedgerAndSweepsEverySetworksModifierOffTheStore() {
        RecordingKind attribute = new RecordingKind("setworks:attribute", false, false);
        RecordingKind potion = new RecordingKind("setworks:potion", true, false);
        EffectManager manager = manager(EffectLedgerTest.definitions(), kinds(attribute, potion));
        manager.activate(ALICE, "alice", null, "t:iron2");
        manager.activate(ALICE, "alice", null, "t:iron4");
        // The entity after a portal trip still carries both modifiers, plus one that is not ours.
        FakeStore store = new FakeStore();
        UUID iron2 = EffectIds.attributeUuid("t:iron/t:iron2/0");
        UUID iron4 = EffectIds.attributeUuid("t:iron/t:iron4/0");
        UUID foreign = UUID.nameUUIDFromBytes("someone else".getBytes());
        store.put("generic.armor", iron2);
        store.put("generic.maxHealth", iron4);
        store.put("generic.armor", foreign);
        // Without an entity onRebuild only clears; the sweep it would run over the player's map runs here over the fake store.
        manager.onRebuild(ALICE, "alice", null);
        manager.reconcile("alice", store);
        assertEquals(0, manager.ledger().playerCount(), "the ledger forgot the player");
        assertFalse(store.modifiers.get("generic.armor").contains(iron2));
        assertTrue(store.modifiers.get("generic.maxHealth").isEmpty());
        assertTrue(store.modifiers.get("generic.armor").contains(foreign), "modifiers of others stay");
        // Exactly the attribute instances are swept, with their own attribute names; potions are not.
        assertEquals(Arrays.asList("generic.armor " + iron2, "generic.maxHealth " + iron4), store.removals);
        attribute.calls.clear();
        // The evaluator then re-activates from an empty state: every instance applies exactly once more.
        assertTrue(manager.activate(ALICE, "alice", null, "t:iron2"));
        assertTrue(manager.activate(ALICE, "alice", null, "t:iron4"));
        assertEquals(Arrays.asList("apply t:iron/t:iron2/0", "apply t:iron/t:iron4/0"), attribute.calls);
    }
}
