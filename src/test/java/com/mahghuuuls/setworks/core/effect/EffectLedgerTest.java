package com.mahghuuuls.setworks.core.effect;

import com.mahghuuuls.setworks.api.BonusBuilder;
import com.mahghuuuls.setworks.api.DefinitionSource;
import com.mahghuuuls.setworks.api.ItemFilter;
import com.mahghuuuls.setworks.api.SetBuilder;
import com.mahghuuuls.setworks.api.definition.EffectSpec;
import com.mahghuuuls.setworks.core.definition.DefinitionRegistry;
import com.mahghuuuls.setworks.core.definition.FrozenDefinitions;
import com.mahghuuuls.setworks.core.definition.ValidationContext;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectLedgerTest {

    private static final DefinitionSource SRC = DefinitionSource.mod("test");
    private static final UUID ALICE = UUID.nameUUIDFromBytes("alice".getBytes());
    private static final UUID BOB = UUID.nameUUIDFromBytes("bob".getBytes());

    static final ValidationContext ANYTHING = new ValidationContext() {
        public boolean itemExists(String itemId) { return true; }
        public boolean oreHasEntries(String oreName) { return true; }
        public boolean locationExists(String locationId) { return true; }
        public boolean effectKindExists(String kindId) { return true; }
        public String effectProblem(EffectSpec effect) { return null; }
        public boolean conditionKindExists(String kindId) { return true; }
        public String conditionProblem(com.mahghuuuls.setworks.api.definition.ConditionSpec condition) { return null; }
        public boolean exactNbtParses(String text) { return true; }
    };

    /** Iron set; two-piece bonus with armor; full bonus with max health and a potion; a scripted bonus. */
    static FrozenDefinitions definitions() {
        DefinitionRegistry registry = new DefinitionRegistry();
        registry.registerSet(SetBuilder.set("t:iron").armor(ItemFilter.item("a:ih"), ItemFilter.item("a:ic"), ItemFilter.item("a:il"), ItemFilter.item("a:if")).build(), SRC, false);
        registry.registerBonus(BonusBuilder.bonus("t:iron2").requires("t:iron", 2).attribute("generic.armor", 2, "add").build(), SRC, false);
        registry.registerBonus(BonusBuilder.bonus("t:iron4").requiresAll("t:iron")
                .attribute("generic.maxHealth", 4, "add").potion("minecraft:speed", 1, false).build(), SRC, false);
        registry.registerBonus(BonusBuilder.bonus("t:scripted").requires("t:iron", 1).description("scripted").build(), SRC, false);
        registry.freeze(ANYTHING);
        return registry.definitions();
    }

    @Test
    void instancesCarryStableIdsInDefinitionOrder() {
        EffectLedger ledger = new EffectLedger(definitions());
        List<EffectLedger.Instance> iron4 = ledger.instancesOf("t:iron4");
        assertEquals(2, iron4.size());
        assertEquals("t:iron/t:iron4/0", iron4.get(0).id());
        assertEquals("setworks:attribute", iron4.get(0).kindId());
        assertEquals("t:iron/t:iron4/1", iron4.get(1).id());
        assertEquals("setworks:potion", iron4.get(1).kindId());
        assertEquals(1, iron4.get(1).index());
        assertTrue(ledger.instancesOf("t:scripted").isEmpty());
        assertTrue(ledger.instancesOf("t:missing").isEmpty());
        assertEquals(3, ledger.allInstances().size());
        // A second ledger over the same definitions derives the same ids.
        assertEquals(iron4.get(0).id(), new EffectLedger(definitions()).instancesOf("t:iron4").get(0).id());
    }

    @Test
    void activateIsIdempotentAndDeactivateReturnsOnlyWhatWasApplied() {
        EffectLedger ledger = new EffectLedger(definitions());
        assertEquals(2, ledger.activate(ALICE, "t:iron4").size());
        assertTrue(ledger.activate(ALICE, "t:iron4").isEmpty(), "second activation applies nothing");
        assertTrue(ledger.isApplied(ALICE, "t:iron/t:iron4/0"));
        assertFalse(ledger.isApplied(BOB, "t:iron/t:iron4/0"), "ledgers are per player");
        assertTrue(ledger.deactivate(ALICE, "t:iron2").isEmpty(), "withdrawing an unapplied bonus is a no-op");
        assertEquals(2, ledger.deactivate(ALICE, "t:iron4").size());
        assertTrue(ledger.deactivate(ALICE, "t:iron4").isEmpty());
        assertFalse(ledger.isApplied(ALICE, "t:iron/t:iron4/0"));
    }

    @Test
    void failedInstanceIsNotAppliedAndIsForgottenOnReactivationOrClear() {
        EffectLedger ledger = new EffectLedger(definitions());
        EffectLedger.Instance armor = ledger.activate(ALICE, "t:iron2").get(0);
        ledger.markFailed(ALICE, armor, "boom");
        assertFalse(ledger.isApplied(ALICE, armor.id()));
        assertEquals("boom", ledger.failure(ALICE, armor.id()));
        assertNull(ledger.failure(BOB, armor.id()));
        // Deactivation of a failed bonus withdraws nothing but clears the failure.
        assertTrue(ledger.deactivate(ALICE, "t:iron2").isEmpty());
        assertNull(ledger.failure(ALICE, armor.id()));
        ledger.activate(ALICE, "t:iron2");
        ledger.markFailed(ALICE, armor, "boom again");
        // A fresh activation tries again and drops the old failure text.
        ledger.deactivate(ALICE, "t:iron2");
        assertEquals(1, ledger.activate(ALICE, "t:iron2").size());
        assertNull(ledger.failure(ALICE, armor.id()));
        ledger.markFailed(ALICE, armor, "third");
        assertTrue(ledger.clear(ALICE).isEmpty(), "a failed instance is not returned as applied");
        assertNull(ledger.failure(ALICE, armor.id()));
    }

    @Test
    void clearReturnsAppliedInstancesAndEmptiesThePlayer() {
        EffectLedger ledger = new EffectLedger(definitions());
        ledger.activate(ALICE, "t:iron2");
        ledger.activate(ALICE, "t:iron4");
        ledger.activate(BOB, "t:iron2");
        assertEquals(3, ledger.applied(ALICE).size());
        assertEquals(2, ledger.playerCount());
        List<EffectLedger.Instance> cleared = ledger.clear(ALICE);
        assertEquals(3, cleared.size());
        assertTrue(ledger.applied(ALICE).isEmpty());
        assertEquals(1, ledger.playerCount());
        assertTrue(ledger.isApplied(BOB, "t:iron/t:iron2/0"), "other players keep their records");
        // After a clear (rebuild) the same bonus applies again in full.
        assertEquals(2, ledger.activate(ALICE, "t:iron4").size());
        assertTrue(ledger.clear(UUID.randomUUID()).isEmpty());
    }

    @Test
    void attributeUuidsAreStablePerInstanceAndDistinctBetweenInstances() {
        EffectLedger ledger = new EffectLedger(definitions());
        UUID a = EffectIds.attributeUuid(ledger.instancesOf("t:iron2").get(0).id());
        UUID b = EffectIds.attributeUuid(ledger.instancesOf("t:iron4").get(0).id());
        // The literal value pins the derivation: a namespace or algorithm change would move every modifier.
        assertEquals(UUID.fromString("e12e9ea7-a494-3f5c-8665-0fc30f0eb81c"), a);
        assertNotEquals(a, b);
        assertNotEquals(EffectIds.attributeUuid("t:iron/t:iron4/0"), EffectIds.attributeUuid("t:iron/t:iron4/1"));
        assertEquals("s:a/s:b/3", EffectIds.instanceId("s:a", "s:b", 3));
        assertEquals(Collections.singletonList("t:iron/t:iron2/0"),
                Collections.singletonList(ledger.instancesOf("t:iron2").get(0).toString()));
    }
}
