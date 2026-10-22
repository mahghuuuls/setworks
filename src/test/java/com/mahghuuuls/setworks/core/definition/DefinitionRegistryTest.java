package com.mahghuuuls.setworks.core.definition;

import com.mahghuuuls.setworks.api.BonusBuilder;
import com.mahghuuuls.setworks.api.BonusModification;
import com.mahghuuuls.setworks.api.DefinitionSource;
import com.mahghuuuls.setworks.api.ItemFilter;
import com.mahghuuuls.setworks.api.PieceBuilder;
import com.mahghuuuls.setworks.api.RegistrationResult;
import com.mahghuuuls.setworks.api.SetBuilder;
import com.mahghuuuls.setworks.api.Visibility;
import com.mahghuuuls.setworks.api.definition.SetDefinition;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefinitionRegistryTest {

    private static final DefinitionSource MOD_A = DefinitionSource.mod("moda");
    private static final DefinitionSource SCRIPT = DefinitionSource.script("sets.zs", "12");

    private static SetDefinition ironArmor() {
        return SetBuilder.set("a:iron").displayName("Iron")
                .armor(ItemFilter.item("minecraft:iron_helmet"), ItemFilter.item("minecraft:iron_chestplate"),
                        ItemFilter.item("minecraft:iron_leggings"), ItemFilter.item("minecraft:iron_boots"))
                .build();
    }

    private static List<String> rules(ValidationReport report) {
        java.util.List<String> rules = new java.util.ArrayList<String>();
        for (ValidationReport.Error error : report.errors()) {
            rules.add(error.rule());
        }
        return rules;
    }

    @Test
    void validSetAndBonusAreEnabled() {
        DefinitionRegistry registry = new DefinitionRegistry();
        assertTrue(registry.registerSet(ironArmor(), MOD_A, false).isOk());
        assertTrue(registry.registerBonus(BonusBuilder.bonus("a:full").requiresAll("a:iron").description("Full").build(), MOD_A, false).isOk());
        ValidationReport report = registry.freeze(new FakeContext());
        assertFalse(report.hasProblems());
        assertNotNull(registry.definitions().set("a:iron"));
        assertNotNull(registry.definitions().bonus("a:full"));
        assertEquals(1, registry.definitions().bonusesRequiring("a:iron").size());
        assertEquals(4, registry.definitions().referencedLocations().size());
    }

    @Test
    void duplicateIdWithoutReplaceIsRejectedAndFirstStays() {
        DefinitionRegistry registry = new DefinitionRegistry();
        registry.registerSet(ironArmor(), MOD_A, false);
        SetDefinition scripted = SetBuilder.set("a:iron").displayName("Script Iron")
                .piece(PieceBuilder.piece("head").accept("minecraft:iron_helmet").at("head")).build();
        registry.registerSet(scripted, SCRIPT, false);
        ValidationReport report = registry.freeze(new FakeContext());
        assertTrue(rules(report).contains("duplicate-id"));
        ValidationReport.Error error = report.errors().get(0);
        assertEquals(SCRIPT, error.source());
        assertTrue(error.message().contains("mod:moda"));
        assertEquals("Iron", registry.definitions().set("a:iron").displayName());
    }

    @Test
    void replaceMarkerOverridesAndRecordsNewSource() {
        DefinitionRegistry registry = new DefinitionRegistry();
        registry.registerSet(ironArmor(), MOD_A, false);
        SetDefinition scripted = SetBuilder.set("a:iron").displayName("Script Iron")
                .piece(PieceBuilder.piece("head").accept("minecraft:iron_helmet").at("head")).build();
        registry.registerSet(scripted, SCRIPT, true);
        ValidationReport report = registry.freeze(new FakeContext());
        assertFalse(report.hasProblems());
        assertEquals("Script Iron", registry.definitions().set("a:iron").displayName());
        assertEquals(SCRIPT, registry.definitions().setEntry("a:iron").source());
    }

    @Test
    void replaceBeforeOriginalStillWinsWithoutError() {
        DefinitionRegistry registry = new DefinitionRegistry();
        SetDefinition scripted = SetBuilder.set("a:iron").displayName("Script Iron")
                .piece(PieceBuilder.piece("head").accept("minecraft:iron_helmet").at("head")).build();
        registry.registerSet(scripted, SCRIPT, true);
        registry.registerSet(ironArmor(), MOD_A, false);
        ValidationReport report = registry.freeze(new FakeContext());
        assertFalse(report.hasProblems(), report.lines().toString());
        assertEquals("Script Iron", registry.definitions().set("a:iron").displayName());
        assertEquals(SCRIPT, registry.definitions().setEntry("a:iron").source());
    }

    @Test
    void invalidIdIsRejectedAtTheCallAndListedInTheReport() {
        DefinitionRegistry registry = new DefinitionRegistry();
        RegistrationResult result = registry.registerSet(SetBuilder.set("Bad Id")
                .piece(PieceBuilder.piece("x").accept("minecraft:compass").at("hotbar")).build(), MOD_A, false);
        assertFalse(result.isOk());
        assertTrue(result.message().contains("not valid"));
        ValidationReport report = registry.freeze(new FakeContext());
        assertTrue(rules(report).contains("rejected-call"));
    }

    @Test
    void invalidPieceInOneSetDisablesOnlyThatSetForATwoSetBonus() {
        DefinitionRegistry registry = new DefinitionRegistry();
        registry.registerSet(ironArmor(), MOD_A, false);
        SetDefinition broken = SetBuilder.set("a:broken")
                .piece(PieceBuilder.piece("x").accept("minecraft:no_such_item").at("hotbar")).build();
        registry.registerSet(broken, MOD_A, false);
        registry.registerBonus(BonusBuilder.bonus("a:both").requiresAll("a:iron").requires("a:broken", 1).description("x").build(), MOD_A, false);
        registry.registerBonus(BonusBuilder.bonus("a:iron_only").requires("a:iron", 2).description("y").build(), MOD_A, false);
        ValidationReport report = registry.freeze(new FakeContext());
        assertNull(registry.definitions().set("a:broken"));
        assertNotNull(registry.definitions().set("a:iron"));
        assertNull(registry.definitions().bonus("a:both"));
        assertNotNull(registry.definitions().bonus("a:iron_only"));
        assertTrue(rules(report).contains("requires-disabled-set"));
    }

    @Test
    void unparseableExactNbtDisablesTheSet() {
        DefinitionRegistry registry = new DefinitionRegistry();
        registry.registerSet(SetBuilder.set("a:nbt")
                .piece(PieceBuilder.piece("p").accept(ItemFilter.item("minecraft:compass").withExactNbt("{broken")).at("hotbar")).build(), MOD_A, false);
        ValidationReport report = registry.freeze(new FakeContext());
        assertTrue(rules(report).contains("invalid-exact-nbt"));
        assertNull(registry.definitions().set("a:nbt"));
    }

    @Test
    void tooManyPiecesDisablesTheSet() {
        SetBuilder builder = SetBuilder.set("a:big");
        for (int i = 0; i <= DefinitionRegistry.MAX_PIECES; i++) {
            builder.piece(PieceBuilder.piece("p" + i).accept("minecraft:compass").at("hotbar"));
        }
        DefinitionRegistry registry = new DefinitionRegistry();
        registry.registerSet(builder.build(), MOD_A, false);
        ValidationReport report = registry.freeze(new FakeContext());
        assertTrue(rules(report).contains("too-many-pieces"));
        assertNull(registry.definitions().set("a:big"));
    }

    @Test
    void modificationsApplyAfterRegistrationsRegardlessOfCallOrder() {
        DefinitionRegistry registry = new DefinitionRegistry();
        // The script modifies before the mod registers; it still applies because changes run after registrations.
        registry.modifyBonus("a:full", BonusModification.modify().visibility(Visibility.HIDDEN).description("Changed"), SCRIPT);
        registry.registerSet(ironArmor(), MOD_A, false);
        registry.registerBonus(BonusBuilder.bonus("a:full").requiresAll("a:iron").description("Full").build(), MOD_A, false);
        ValidationReport report = registry.freeze(new FakeContext());
        assertFalse(report.hasProblems(), report.lines().toString());
        assertEquals(Visibility.HIDDEN, registry.definitions().bonus("a:full").visibility());
        assertEquals("Changed", registry.definitions().bonus("a:full").description());
    }

    @Test
    void disableSetRemovesItAndItsBonuses() {
        DefinitionRegistry registry = new DefinitionRegistry();
        registry.registerSet(ironArmor(), MOD_A, false);
        registry.registerBonus(BonusBuilder.bonus("a:full").requiresAll("a:iron").description("Full").build(), MOD_A, false);
        registry.disableSet("a:iron", SCRIPT);
        ValidationReport report = registry.freeze(new FakeContext());
        assertNull(registry.definitions().set("a:iron"));
        assertNull(registry.definitions().bonus("a:full"));
        assertTrue(report.disabledSets().contains("a:iron"));
        assertTrue(report.disabledBonuses().contains("a:full"));
        assertFalse(registry.definitions().setEntry("a:iron").enabled());
    }

    @Test
    void addAlternativesExtendsAnExistingPiece() {
        DefinitionRegistry registry = new DefinitionRegistry();
        registry.registerSet(ironArmor(), MOD_A, false);
        registry.addAlternatives("a:iron", "head", Collections.singletonList(ItemFilter.item("minecraft:iron_sword")), SCRIPT);
        registry.freeze(new FakeContext());
        assertEquals(2, registry.definitions().set("a:iron").piece("head").alternatives().size());
    }

    @Test
    void invalidPieceDisablesWholeSetAndKeepsOtherSets() {
        DefinitionRegistry registry = new DefinitionRegistry();
        registry.registerSet(ironArmor(), MOD_A, false);
        SetDefinition broken = SetBuilder.set("a:broken")
                .piece(PieceBuilder.piece("head").accept("minecraft:iron_helmet").at("head"))
                .piece(PieceBuilder.piece("legs").accept("minecraft:no_such_item").at("legs")).build();
        registry.registerSet(broken, SCRIPT, false);
        registry.registerBonus(BonusBuilder.bonus("a:broken_full").requiresAll("a:broken").description("x").build(), SCRIPT, false);
        ValidationReport report = registry.freeze(new FakeContext());
        assertTrue(rules(report).contains("unknown-item"));
        ValidationReport.Error error = report.errors().get(0);
        assertEquals("a:broken", error.definitionId());
        assertTrue(error.message().contains("legs"));
        assertTrue(error.message().contains("minecraft:no_such_item"));
        assertEquals(SCRIPT, error.source());
        assertNull(registry.definitions().set("a:broken"));
        assertNull(registry.definitions().bonus("a:broken_full"));
        assertNotNull(registry.definitions().set("a:iron"));
    }

    @Test
    void everyValidationRuleFires() {
        DefinitionRegistry registry = new DefinitionRegistry();
        registry.registerSet(ironArmor(), MOD_A, false);
        registry.registerSet(SetBuilder.set("Bad Id").piece(PieceBuilder.piece("x").accept("minecraft:compass").at("hotbar")).build(), MOD_A, false);
        registry.registerSet(SetBuilder.set("a:empty").build(), MOD_A, false);
        registry.registerSet(SetBuilder.set("a:dup")
                .piece(PieceBuilder.piece("p").accept("minecraft:compass").at("hotbar"))
                .piece(PieceBuilder.piece("p").accept("minecraft:compass").at("hotbar")).build(), MOD_A, false);
        registry.registerSet(SetBuilder.set("a:noalt").piece(PieceBuilder.piece("p").at("hotbar")).build(), MOD_A, false);
        registry.registerSet(SetBuilder.set("a:noloc").piece(PieceBuilder.piece("p").accept("minecraft:compass")).build(), MOD_A, false);
        registry.registerSet(SetBuilder.set("a:badore").piece(PieceBuilder.piece("p").accept(ItemFilter.ore("gemNothing")).at("hotbar")).build(), MOD_A, false);
        registry.registerSet(SetBuilder.set("a:badloc").piece(PieceBuilder.piece("p").accept("minecraft:compass").at("baubles:ring")).build(), MOD_A, false);
        registry.registerBonus(BonusBuilder.bonus("a:noreq").description("x").build(), MOD_A, false);
        registry.registerBonus(BonusBuilder.bonus("a:unknownset").requires("a:nope", 1).description("x").build(), MOD_A, false);
        registry.registerBonus(BonusBuilder.bonus("a:toohigh").requires("a:iron", 5).description("x").build(), MOD_A, false);
        registry.registerBonus(BonusBuilder.bonus("a:dupreq").requires("a:iron", 1).requires("a:iron", 2).description("x").build(), MOD_A, false);
        registry.registerBonus(BonusBuilder.bonus("a:baddisplay").requires("a:iron", 1).displayUnder("a:other").description("x").build(), MOD_A, false);
        registry.registerBonus(BonusBuilder.bonus("a:nodesc").requires("a:iron", 1).build(), MOD_A, false);
        registry.registerBonus(BonusBuilder.bonus("a:badeffect").requires("a:iron", 1).effect("addon:nothing", new LinkedHashMap<String, String>()).build(), MOD_A, false);
        registry.registerBonus(BonusBuilder.bonus("a:badcond").requires("a:iron", 1).description("x").condition("addon:nocond", new LinkedHashMap<String, String>()).build(), MOD_A, false);
        registry.disableSet("a:ghost", SCRIPT);
        ValidationReport report = registry.freeze(new FakeContext());
        List<String> rules = rules(report);
        for (String expected : new String[] {"rejected-call", "no-pieces", "duplicate-piece", "no-alternatives", "no-locations",
                "unknown-ore", "unknown-location", "no-requirements", "unknown-set", "threshold-too-high",
                "duplicate-requirement", "display-set-not-required", "no-description", "unknown-effect-kind",
                "unknown-condition-kind"}) {
            assertTrue(rules.contains(expected), "missing rule " + expected + " in " + rules);
        }
        // Every invalid bonus on a:iron disables a:iron as a whole; the registry says so.
        assertTrue(rules.contains("invalid-bonus"));
        assertNull(registry.definitions().set("a:iron"));
    }

    @Test
    void validBonusWithKnownEffectAndConditionKindsIsEnabled() {
        DefinitionRegistry registry = new DefinitionRegistry();
        registry.registerSet(ironArmor(), MOD_A, false);
        registry.registerBonus(BonusBuilder.bonus("a:armored").requires("a:iron", 2)
                .attribute("generic.armor", 2, "add")
                .condition("setworks:health_below", Collections.singletonMap("fraction", "0.5")).build(), MOD_A, false);
        ValidationReport report = registry.freeze(new FakeContext());
        assertFalse(report.hasProblems(), report.lines().toString());
        assertNull(registry.definitions().bonus("a:armored").description());
        assertFalse(registry.definitions().bonus("a:armored").isScripted());
    }

    @Test
    void registrationAfterFreezeIsRejected() {
        DefinitionRegistry registry = new DefinitionRegistry();
        registry.freeze(new FakeContext());
        RegistrationResult result = registry.registerSet(ironArmor(), MOD_A, false);
        assertFalse(result.isOk());
        assertTrue(result.message().contains("frozen"));
        assertTrue(registry.isFrozen());
        assertNull(registry.definitions().set("a:iron"));
    }

    @Test
    void scriptFailuresAppearInTheReportLines() {
        DefinitionRegistry registry = new DefinitionRegistry();
        registry.reportScriptFailure("sets.zs line 3: parse error; later files of loader setworks were skipped");
        ValidationReport report = registry.freeze(new FakeContext());
        assertTrue(report.hasProblems());
        List<String> lines = report.lines();
        assertTrue(lines.get(0).startsWith("script failure: sets.zs"));
        assertEquals("restart required to apply changes", lines.get(lines.size() - 1));
    }

    @Test
    void cleanReportSaysNoErrorsAndRestartNote() {
        List<String> lines = new DefinitionRegistry().freeze(new FakeContext()).lines();
        assertEquals("no errors", lines.get(0));
        assertEquals("restart required to apply changes", lines.get(1));
    }
}
