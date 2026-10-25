package com.mahghuuuls.setworks.core.eval;

import com.mahghuuuls.setworks.api.BonusBuilder;
import com.mahghuuuls.setworks.api.DefinitionSource;
import com.mahghuuuls.setworks.api.ICondition;
import com.mahghuuuls.setworks.api.IConditionContext;
import com.mahghuuuls.setworks.api.IConditionKind;
import com.mahghuuuls.setworks.api.ItemFilter;
import com.mahghuuuls.setworks.api.RegistrationResult;
import com.mahghuuuls.setworks.api.SetBuilder;
import com.mahghuuuls.setworks.api.definition.BonusDefinition;
import com.mahghuuuls.setworks.api.definition.ConditionSpec;
import com.mahghuuuls.setworks.core.definition.DefinitionRegistry;
import com.mahghuuuls.setworks.core.definition.ValidationContext;
import com.mahghuuuls.setworks.core.eval.condition.HealthBelowKind;
import com.mahghuuuls.setworks.core.eval.condition.SneakingKind;
import com.mahghuuuls.setworks.core.match.CompiledDefinitions;
import net.minecraft.entity.player.EntityPlayerMP;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** AND semantics, the failing condition's name, and the error-once rule, with fake kinds and no player. */
class KindConditionEvaluatorTest {

    private static final DefinitionSource SRC = DefinitionSource.mod("test");

    private static final ValidationContext ANYTHING = new ValidationContext() {
        public boolean itemExists(String itemId) { return true; }
        public boolean oreHasEntries(String oreName) { return true; }
        public boolean locationExists(String locationId) { return true; }
        public boolean effectKindExists(String kindId) { return true; }
        public String effectProblem(com.mahghuuuls.setworks.api.definition.EffectSpec effect) { return null; }
        public boolean conditionKindExists(String kindId) { return true; }
        public String conditionProblem(ConditionSpec condition) { return null; }
        public boolean exactNbtParses(String text) { return true; }
    };

    /** A kind whose condition answers from a switch the test flips: pass, fail, or throw. */
    private static final class SwitchKind implements IConditionKind {
        final String id;
        volatile String mode = "pass";
        int calls;

        SwitchKind(String id) {
            this.id = id;
        }

        public String id() { return id; }
        public String describe(Map<String, String> params) { return id + " switch"; }

        public ICondition create(Map<String, String> params) {
            return new ICondition() {
                public boolean test(EntityPlayerMP player, IConditionContext context) {
                    calls++;
                    if ("throw".equals(mode)) {
                        throw new IllegalStateException("switch is broken");
                    }
                    return "pass".equals(mode);
                }
            };
        }
    }

    private static final IConditionContext NO_COUNTS = new IConditionContext() {
        public String bonusId() { return "t:bonus"; }
        public int pieceCount(String setId) { return 0; }
        public long tick() { return 0; }
    };

    private static CompiledDefinitions definitions(String... conditionKinds) {
        DefinitionRegistry registry = new DefinitionRegistry();
        registry.registerSet(SetBuilder.set("t:iron").armor(ItemFilter.item("a:ih"), ItemFilter.item("a:ic"), ItemFilter.item("a:il"), ItemFilter.item("a:if")).build(), SRC, false);
        BonusBuilder bonus = BonusBuilder.bonus("t:bonus").requires("t:iron", 1).description("b");
        for (String kind : conditionKinds) {
            bonus.condition(kind, Collections.<String, String>emptyMap());
        }
        registry.registerBonus(bonus.build(), SRC, false);
        registry.registerBonus(BonusBuilder.bonus("t:plain").requires("t:iron", 1).description("p").build(), SRC, false);
        registry.freeze(ANYTHING);
        return new CompiledDefinitions(registry.definitions());
    }

    private static ConditionKindRegistry kinds(IConditionKind... kinds) {
        ConditionKindRegistry registry = new ConditionKindRegistry();
        for (IConditionKind kind : kinds) {
            assertTrue(registry.register(kind).isOk());
        }
        return registry;
    }

    @Test
    void allConditionsMustPassAndTheFirstFailingOneNamesItself() {
        SwitchKind a = new SwitchKind("t:a");
        SwitchKind b = new SwitchKind("t:b");
        CompiledDefinitions definitions = definitions("t:a", "t:b");
        KindConditionEvaluator evaluator = new KindConditionEvaluator(definitions, kinds(a, b), LogManager.getLogger("test"));
        BonusDefinition bonus = definitions.bonus("t:bonus");
        assertEquals(ConditionEvaluator.Outcome.PASS, evaluator.evaluate(bonus, null, NO_COUNTS).outcome());
        b.mode = "fail";
        ConditionEvaluator.Result result = evaluator.evaluate(bonus, null, NO_COUNTS);
        assertEquals(ConditionEvaluator.Outcome.FAIL, result.outcome());
        assertEquals("t:b", result.detail());
        a.mode = "fail";
        result = evaluator.evaluate(bonus, null, NO_COUNTS);
        assertEquals("t:a", result.detail(), "the first failing condition decides");
        assertEquals(3, a.calls);
        assertEquals(2, b.calls, "conditions after a failing one are not evaluated");
        assertEquals(ConditionEvaluator.Outcome.PASS, evaluator.evaluate(definitions.bonus("t:plain"), null, NO_COUNTS).outcome());
    }

    @Test
    void anErrorIsLoggedOnceUntilTheConditionRecovers() {
        SwitchKind a = new SwitchKind("t:a");
        CompiledDefinitions definitions = definitions("t:a");
        KindConditionEvaluator evaluator = new KindConditionEvaluator(definitions, kinds(a), LogManager.getLogger("test"));
        BonusDefinition bonus = definitions.bonus("t:bonus");
        a.mode = "throw";
        for (int i = 0; i < 30; i++) {
            ConditionEvaluator.Result result = evaluator.evaluate(bonus, null, NO_COUNTS);
            assertEquals(ConditionEvaluator.Outcome.ERROR, result.outcome());
            assertTrue(result.detail().startsWith("t:a: java.lang.IllegalStateException: switch is broken"), result.detail());
        }
        assertEquals(1, evaluator.warningsLogged(), "thirty errors, one warning");
        assertTrue(evaluator.isErroring("t:bonus", 0));
        // Recovery: a pass or a fail ends the episode.
        a.mode = "fail";
        assertEquals(ConditionEvaluator.Outcome.FAIL, evaluator.evaluate(bonus, null, NO_COUNTS).outcome());
        assertFalse(evaluator.isErroring("t:bonus", 0));
        assertEquals(1, evaluator.warningsLogged());
        // A new error episode logs again.
        a.mode = "throw";
        evaluator.evaluate(bonus, null, NO_COUNTS);
        evaluator.evaluate(bonus, null, NO_COUNTS);
        assertEquals(2, evaluator.warningsLogged());
        List<String> lines = evaluator.conditionLines(bonus);
        assertEquals(Collections.singletonList("  condition 0: t:a t:a switch [error]"), lines);
    }

    @Test
    void aKindThatCannotCreateItsConditionIsAnErrorNotACrash() {
        IConditionKind broken = new IConditionKind() {
            public String id() { return "t:broken"; }
            public String describe(Map<String, String> params) { return "broken"; }
            public ICondition create(Map<String, String> params) { throw new IllegalArgumentException("cannot build"); }
        };
        CompiledDefinitions definitions = definitions("t:broken", "t:missing");
        KindConditionEvaluator evaluator = new KindConditionEvaluator(definitions, kinds(broken), LogManager.getLogger("test"));
        BonusDefinition bonus = definitions.bonus("t:bonus");
        ConditionEvaluator.Result result = evaluator.evaluate(bonus, null, NO_COUNTS);
        assertEquals(ConditionEvaluator.Outcome.ERROR, result.outcome());
        assertTrue(result.detail().contains("failed to create"), result.detail());
        assertEquals(1, evaluator.warningsLogged());
        List<String> lines = evaluator.conditionLines(bonus);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("[condition kind t:broken failed to create"), lines.get(0));
        assertTrue(lines.get(1).contains("[condition kind t:missing is not registered]"), lines.get(1));
    }

    @Test
    void aDirectConditionIsUsedAsSuppliedAndSeesTheCounts() {
        final int[] seen = {-1};
        ICondition direct = new ICondition() {
            public boolean test(EntityPlayerMP player, IConditionContext context) {
                seen[0] = context.pieceCount("t:iron");
                return context.pieceCount("t:iron") >= 3 && "t:bonus".equals(context.bonusId());
            }
        };
        DefinitionRegistry registry = new DefinitionRegistry();
        registry.registerSet(SetBuilder.set("t:iron").armor(ItemFilter.item("a:ih"), ItemFilter.item("a:ic"), ItemFilter.item("a:il"), ItemFilter.item("a:if")).build(), SRC, false);
        registry.registerBonus(BonusBuilder.bonus("t:bonus").requires("t:iron", 1).description("b").condition("three or more", direct).build(), SRC, false);
        registry.freeze(ANYTHING);
        CompiledDefinitions definitions = new CompiledDefinitions(registry.definitions());
        KindConditionEvaluator evaluator = new KindConditionEvaluator(definitions, kinds(), LogManager.getLogger("test"));
        final Map<String, Integer> counts = new HashMap<String, Integer>();
        IConditionContext context = new IConditionContext() {
            public String bonusId() { return "t:bonus"; }
            public int pieceCount(String setId) { return counts.containsKey(setId) ? counts.get(setId) : 0; }
            public long tick() { return 7; }
        };
        counts.put("t:iron", 2);
        ConditionEvaluator.Result result = evaluator.evaluate(definitions.bonus("t:bonus"), null, context);
        assertEquals(ConditionEvaluator.Outcome.FAIL, result.outcome());
        assertEquals("three or more", result.detail());
        assertEquals(2, seen[0]);
        counts.put("t:iron", 3);
        assertEquals(ConditionEvaluator.Outcome.PASS, evaluator.evaluate(definitions.bonus("t:bonus"), null, context).outcome());
        assertEquals(Collections.singletonList("  condition 0: three or more (direct)"), evaluator.conditionLines(definitions.bonus("t:bonus")));
    }

    @Test
    void registryRejectsBadIdsDuplicatesAndLateRegistrations() {
        ConditionKindRegistry registry = new ConditionKindRegistry();
        assertTrue(registry.register(new HealthBelowKind()).isOk());
        assertTrue(registry.register(new SneakingKind()).isOk());
        RegistrationResult duplicate = registry.register(new SneakingKind());
        assertFalse(duplicate.isOk());
        assertTrue(duplicate.message().contains("already registered"), duplicate.message());
        assertFalse(registry.register(new SwitchKind("Bad Id")).isOk());
        assertFalse(registry.register(null).isOk());
        registry.freeze();
        assertFalse(registry.register(new SwitchKind("t:late")).isOk());
        assertNull(registry.get("t:late"));
        assertNotNull(registry.get("setworks:health_below"));
        assertEquals(2, registry.all().size());
    }

    @Test
    void healthBelowParsesDescribesAndCompares() {
        assertEquals(0.5, HealthBelowKind.parseFraction(Collections.singletonMap("fraction", "0.5")));
        assertEquals("health below 50%", new HealthBelowKind().describe(Collections.singletonMap("fraction", "0.5")));
        assertEquals("health below 100%", new HealthBelowKind().describe(Collections.singletonMap("fraction", "1")));
        assertNull(new HealthBelowKind().validate(Collections.singletonMap("fraction", "0.25")));
        assertNotNull(new HealthBelowKind().validate(Collections.singletonMap("fraction", "2")));
        assertNotNull(new HealthBelowKind().validate(Collections.singletonMap("fraction", "0")));
        assertNotNull(new HealthBelowKind().validate(Collections.singletonMap("fraction", "half")));
        assertNotNull(new HealthBelowKind().validate(Collections.<String, String>emptyMap()));
        // Strictly below: 10 of 20 is not below half; 9.9 is.
        assertFalse(HealthBelowKind.isBelow(10f, 20f, 0.5));
        assertTrue(HealthBelowKind.isBelow(9.9f, 20f, 0.5));
        assertTrue(HealthBelowKind.isBelow(11f, 24f, 0.5), "max health 24 after a bonus: half is 12");
        assertNull(new SneakingKind().validate(Collections.<String, String>emptyMap()));
        assertNotNull(new SneakingKind().validate(Collections.singletonMap("x", "1")));
        assertEquals("sneaking", new SneakingKind().describe(Collections.<String, String>emptyMap()));
    }
}
