package com.mahghuuuls.setworks.core.definition;

import com.mahghuuuls.setworks.api.BonusBuilder;
import com.mahghuuuls.setworks.api.DefinitionSource;
import com.mahghuuuls.setworks.api.ItemFilter;
import com.mahghuuuls.setworks.api.SetBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** An effect whose kind exists but whose parameters the kind rejects disables its bonus with the kind's text. */
class EffectValidationTest {

    private static final DefinitionSource SRC = DefinitionSource.mod("test");

    private static DefinitionRegistry ironRegistry() {
        DefinitionRegistry registry = new DefinitionRegistry();
        registry.registerSet(SetBuilder.set("a:iron").armor(ItemFilter.item("minecraft:iron_helmet"), ItemFilter.item("minecraft:iron_chestplate"),
                ItemFilter.item("minecraft:iron_leggings"), ItemFilter.item("minecraft:iron_boots")).build(), SRC, false);
        return registry;
    }

    @Test
    void badOperationIsReportedAsInvalidEffectAndDisablesTheSet() {
        DefinitionRegistry registry = ironRegistry();
        registry.registerBonus(BonusBuilder.bonus("a:good").requires("a:iron", 2).attribute("generic.armor", 2, "add").build(), SRC, false);
        registry.registerBonus(BonusBuilder.bonus("a:bad").requires("a:iron", 2).attribute("generic.armor", 2, "times").build(), SRC, false);
        ValidationReport report = registry.freeze(new FakeContext());
        // The effect error, the set disabled because of it, and the sibling bonus disabled through the set.
        assertEquals(3, report.errors().size(), report.lines().toString());
        ValidationReport.Error error = report.errors().get(0);
        assertEquals("a:bad", error.definitionId());
        assertEquals("invalid-effect", error.rule());
        assertTrue(error.describe().contains("operation times"), error.describe());
        assertEquals("invalid-bonus", report.errors().get(1).rule());
        assertEquals("requires-disabled-set", report.errors().get(2).rule());
        assertNull(registry.definitions().bonus("a:bad"));
        assertNull(registry.definitions().bonus("a:good"));
        assertNull(registry.definitions().set("a:iron"));
    }

    @Test
    void unknownAttributeNameIsReportedAtLoadNamingTheAttribute() {
        DefinitionRegistry registry = ironRegistry();
        registry.registerBonus(BonusBuilder.bonus("a:typo").requires("a:iron", 2).attribute("generic.armour", 2, "add").build(), SRC, false);
        ValidationReport report = registry.freeze(new FakeContext());
        assertEquals("invalid-effect", report.errors().get(0).rule(), report.lines().toString());
        assertTrue(report.errors().get(0).describe().contains("generic.armour does not exist on players"), report.errors().get(0).describe());
        assertNull(registry.definitions().set("a:iron"));
    }

    @Test
    void unknownKindIsStillReportedOnceWithoutAParameterCheck() {
        DefinitionRegistry registry = ironRegistry();
        registry.registerBonus(BonusBuilder.bonus("a:addon").requires("a:iron", 1)
                .effect("mymod:stamina", java.util.Collections.singletonMap("amount", "x")).build(), SRC, false);
        ValidationReport report = registry.freeze(new FakeContext());
        assertEquals(2, report.errors().size(), report.lines().toString());
        assertEquals("unknown-effect-kind", report.errors().get(0).rule());
        assertEquals("invalid-bonus", report.errors().get(1).rule());
        assertFalse(report.lines().toString().contains("invalid-effect"));
    }
}
