package com.mahghuuuls.setworks.core.effect;

import com.mahghuuuls.setworks.api.CombinationRule;
import com.mahghuuuls.setworks.api.IEffectKind;
import com.mahghuuuls.setworks.api.RegistrationResult;
import com.mahghuuuls.setworks.api.definition.BonusDefinition;
import com.mahghuuuls.setworks.core.diagnostic.DiagnosticLog;
import com.mahghuuuls.setworks.core.definition.FrozenDefinitions;
import net.minecraft.entity.player.EntityPlayerMP;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The kind registry rules and the inspect lines, which need no player. */
class EffectManagerLinesTest {

    private static final UUID ALICE = UUID.nameUUIDFromBytes("alice".getBytes());

    /** A kind that records nothing; never applied in these tests. */
    private static IEffectKind kind(final String id, final CombinationRule rule) {
        return new IEffectKind() {
            public String id() { return id; }
            public CombinationRule combinationRule() { return rule; }
            public String describe(Map<String, String> params) { return id + " " + params.get("amount"); }
            public void apply(EntityPlayerMP player, String instanceId, Map<String, String> params) { }
            public void withdraw(EntityPlayerMP player, String instanceId, Map<String, String> params) { }
        };
    }

    @Test
    void registryRejectsBadIdsDuplicatesAndLateRegistrations() {
        EffectKindRegistry registry = new EffectKindRegistry();
        assertTrue(registry.register(new AttributeEffectKind()).isOk());
        assertTrue(registry.register(kind("mymod:stamina", CombinationRule.SUM)).isOk());
        RegistrationResult duplicate = registry.register(kind("mymod:stamina", CombinationRule.SUM));
        assertFalse(duplicate.isOk());
        assertTrue(duplicate.message().contains("already registered"), duplicate.message());
        assertFalse(registry.register(kind("Bad Id", CombinationRule.SUM)).isOk());
        assertFalse(registry.register(kind("mymod:norule", null)).isOk());
        assertFalse(registry.register(null).isOk());
        assertTrue(registry.exists("setworks:attribute"));
        assertEquals(2, registry.all().size());
        registry.freeze();
        RegistrationResult late = registry.register(kind("mymod:late", CombinationRule.REPLACE));
        assertFalse(late.isOk());
        assertTrue(late.message().contains("frozen"), late.message());
        assertNull(registry.get("mymod:late"));
    }

    @Test
    void inspectLinesShowDescriptionRuleAndStatusPerEffect() {
        FrozenDefinitions definitions = EffectLedgerTest.definitions();
        EffectKindRegistry kinds = new EffectKindRegistry();
        kinds.register(new AttributeEffectKind());
        // The potion kind is left out on purpose: its effect must read as an unknown kind.
        EffectLedger ledger = new EffectLedger(definitions);
        EffectManager manager = new EffectManager(ledger, kinds, LogManager.getLogger("test"), new DiagnosticLog(LogManager.getLogger("test"), false));
        BonusDefinition iron4 = definitions.bonus("t:iron4");
        List<String> before = manager.effectLines(ALICE, iron4);
        assertEquals(2, before.size());
        assertEquals("  effect 0: generic.maxHealth add 4 (sum) not applied [t:iron/t:iron4/0]", before.get(0));
        assertEquals("  effect 1: setworks:potion (unknown kind) not applied [t:iron/t:iron4/1]", before.get(1));
        ledger.activate(ALICE, "t:iron4");
        ledger.markFailed(ALICE, ledger.instancesOf("t:iron4").get(1), "effect kind setworks:potion is not registered");
        List<String> after = manager.effectLines(ALICE, iron4);
        assertEquals("  effect 0: generic.maxHealth add 4 (sum) applied [t:iron/t:iron4/0]", after.get(0));
        assertEquals("  effect 1: setworks:potion (unknown kind) failed: effect kind setworks:potion is not registered [t:iron/t:iron4/1]", after.get(1));
        assertTrue(manager.effectLines(ALICE, definitions.bonus("t:scripted")).isEmpty());
    }
}
