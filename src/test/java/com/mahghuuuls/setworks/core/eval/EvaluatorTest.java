package com.mahghuuuls.setworks.core.eval;

import com.mahghuuuls.setworks.api.BonusBuilder;
import com.mahghuuuls.setworks.api.BonusState;
import com.mahghuuuls.setworks.api.DefinitionSource;
import com.mahghuuuls.setworks.api.ItemFilter;
import com.mahghuuuls.setworks.api.PieceBuilder;
import com.mahghuuuls.setworks.api.SetBuilder;
import com.mahghuuuls.setworks.api.ThresholdMode;
import com.mahghuuuls.setworks.command.InspectReport;
import com.mahghuuuls.setworks.core.definition.DefinitionRegistry;
import com.mahghuuuls.setworks.core.definition.ValidationContext;
import com.mahghuuuls.setworks.core.match.CompiledDefinitions;
import com.mahghuuuls.setworks.core.match.FakeItem;
import com.mahghuuuls.setworks.core.match.SlotCandidate;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvaluatorTest {

    private static final DefinitionSource SRC = DefinitionSource.mod("test");
    private static final UUID PLAYER = UUID.nameUUIDFromBytes("player".getBytes());

    private static final ValidationContext ANYTHING = new ValidationContext() {
        public boolean itemExists(String itemId) { return true; }
        public boolean oreHasEntries(String oreName) { return true; }
        public boolean locationExists(String locationId) { return true; }
        public boolean effectKindExists(String kindId) { return true; }
        public boolean conditionKindExists(String kindId) { return true; }
        public boolean exactNbtParses(String text) { return true; }
    };

    /** Iron armor (cumulative, bonuses at 2 and all), leather armor (highest-only, 2 and all), weapons set, multi-set bonus. */
    private static CompiledDefinitions definitions() {
        DefinitionRegistry registry = new DefinitionRegistry();
        registry.registerSet(SetBuilder.set("t:iron").armor(ItemFilter.item("a:ih"), ItemFilter.item("a:ic"), ItemFilter.item("a:il"), ItemFilter.item("a:if")).build(), SRC, false);
        registry.registerBonus(BonusBuilder.bonus("t:iron2").requires("t:iron", 2).description("two").build(), SRC, false);
        registry.registerBonus(BonusBuilder.bonus("t:iron4").requiresAll("t:iron").description("four").build(), SRC, false);
        registry.registerSet(SetBuilder.set("t:leather").thresholdMode(ThresholdMode.HIGHEST_ONLY)
                .armor(ItemFilter.item("a:lh"), ItemFilter.item("a:lc"), ItemFilter.item("a:ll"), ItemFilter.item("a:lf")).build(), SRC, false);
        registry.registerBonus(BonusBuilder.bonus("t:leather2").requires("t:leather", 2).description("two").build(), SRC, false);
        registry.registerBonus(BonusBuilder.bonus("t:leather4").requiresAll("t:leather").description("four").build(), SRC, false);
        registry.registerSet(SetBuilder.set("t:weapons").piece(PieceBuilder.piece("weapon").accept("a:sword").at("mainhand")).build(), SRC, false);
        registry.registerBonus(BonusBuilder.bonus("t:strike").requiresAll("t:iron").requires("t:weapons", 1).description("strike").build(), SRC, false);
        registry.registerBonus(BonusBuilder.bonus("t:leather_strike").requires("t:leather", 2).requires("t:weapons", 1).description("ls").build(), SRC, false);
        registry.freeze(ANYTHING);
        return new CompiledDefinitions(registry.definitions());
    }

    private static Evaluator evaluator() {
        return new Evaluator(definitions(), ConditionEvaluator.NONE, 20);
    }

    private static SlotCandidate slot(String location, String item) {
        return new SlotCandidate(location, location, new FakeItem(item));
    }

    private static List<SlotCandidate> iron(int pieces) {
        List<SlotCandidate> slots = new ArrayList<SlotCandidate>();
        String[] items = {"a:ih", "a:ic", "a:il", "a:if"};
        String[] locations = {"head", "chest", "legs", "feet"};
        for (int i = 0; i < pieces; i++) {
            slots.add(slot(locations[i], items[i]));
        }
        return slots;
    }

    private static List<SlotCandidate> leather(int pieces) {
        List<SlotCandidate> slots = new ArrayList<SlotCandidate>();
        String[] items = {"a:lh", "a:lc", "a:ll", "a:lf"};
        String[] locations = {"head", "chest", "legs", "feet"};
        for (int i = 0; i < pieces; i++) {
            slots.add(slot(locations[i], items[i]));
        }
        return slots;
    }

    private static EvaluationInput rebuild(List<SlotCandidate> slots, long tick) {
        return new EvaluationInput(slots, Collections.<String>emptySet(), true, tick, false, false, null);
    }

    private static EvaluationInput changed(List<SlotCandidate> slots, long tick, String... locations) {
        return new EvaluationInput(slots, new HashSet<String>(Arrays.asList(locations)), false, tick, false, false, null);
    }

    @Test
    void cumulativeModeActivatesEveryReachedThreshold() {
        Evaluator evaluator = evaluator();
        Evaluator.Result r = evaluator.evaluate(PlayerSetState.empty(PLAYER, "p"), rebuild(iron(4), 1));
        assertEquals(4, r.state().pieceCount("t:iron"));
        assertTrue(r.state().isActive("t:iron2"));
        assertTrue(r.state().isActive("t:iron4"));
        assertEquals(BonusState.MISSING_EQUIPMENT, r.state().bonusState("t:strike"));
        assertEquals("t:weapons 0/1", r.state().bonuses().get("t:strike").detail());
        assertEquals(2, countActivations(r.transitions()));
    }

    @Test
    void partialSetReportsMissingEquipmentWithCounts() {
        Evaluator.Result r = evaluator().evaluate(PlayerSetState.empty(PLAYER, "p"), rebuild(iron(3), 1));
        assertEquals(3, r.state().pieceCount("t:iron"));
        assertTrue(r.state().isActive("t:iron2"));
        assertEquals(BonusState.MISSING_EQUIPMENT, r.state().bonusState("t:iron4"));
        assertEquals("t:iron 3/4", r.state().bonuses().get("t:iron4").detail());
        assertTrue(r.state().sets().get("t:iron").isSatisfied("head"));
        assertFalse(r.state().sets().get("t:iron").isSatisfied("feet"));
    }

    @Test
    void highestOnlyModeSupersedesLowerThresholdsAndFallsBack() {
        Evaluator evaluator = evaluator();
        Evaluator.Result full = evaluator.evaluate(PlayerSetState.empty(PLAYER, "p"), rebuild(leather(4), 1));
        assertTrue(full.state().isActive("t:leather4"));
        assertEquals(BonusState.SUPERSEDED, full.state().bonusState("t:leather2"));
        assertEquals("threshold 4 applies", full.state().bonuses().get("t:leather2").detail());
        Evaluator.Result three = evaluator.evaluate(full.state(), changed(leather(3), 2, "feet"));
        assertTrue(three.state().isActive("t:leather2"));
        assertEquals(BonusState.MISSING_EQUIPMENT, three.state().bonusState("t:leather4"));
        assertEquals(2, three.transitions().size());
    }

    @Test
    void multiSetBonusIgnoresHighestOnlyMode() {
        List<SlotCandidate> slots = new ArrayList<SlotCandidate>(leather(4));
        slots.add(slot("mainhand", "a:sword"));
        Evaluator.Result r = evaluator().evaluate(PlayerSetState.empty(PLAYER, "p"), rebuild(slots, 1));
        assertTrue(r.state().isActive("t:leather4"));
        assertTrue(r.state().isActive("t:leather_strike"));
        assertEquals(BonusState.SUPERSEDED, r.state().bonusState("t:leather2"));
    }

    @Test
    void onlyAffectedSetsAreRecountedOnAChange() {
        Evaluator evaluator = evaluator();
        List<SlotCandidate> both = new ArrayList<SlotCandidate>(iron(4));
        both.add(slot("mainhand", "a:sword"));
        Evaluator.Result first = evaluator.evaluate(PlayerSetState.empty(PLAYER, "p"), rebuild(both, 1));
        assertTrue(first.state().isActive("t:strike"));
        // The sword leaves; only mainhand changed. The input omits the sword; iron must keep its count.
        Evaluator.Result second = evaluator.evaluate(first.state(), changed(iron(4), 2, "mainhand"));
        assertEquals(4, second.state().pieceCount("t:iron"));
        assertEquals(0, second.state().pieceCount("t:weapons"));
        assertEquals(BonusState.MISSING_EQUIPMENT, second.state().bonusState("t:strike"));
        assertTrue(second.state().isActive("t:iron4"));
        assertEquals(1, second.transitions().size());
        assertTrue(second.transitions().get(0).deactivated());
    }

    @Test
    void noChangeProducesNoTransitions() {
        Evaluator evaluator = evaluator();
        Evaluator.Result first = evaluator.evaluate(PlayerSetState.empty(PLAYER, "p"), rebuild(iron(4), 1));
        Evaluator.Result again = evaluator.evaluate(first.state(), changed(iron(4), 2, "hotbar"));
        assertTrue(again.transitions().isEmpty());
    }

    @Test
    void spectatorAndDeadMakeEverythingInactive() {
        Evaluator evaluator = evaluator();
        Evaluator.Result full = evaluator.evaluate(PlayerSetState.empty(PLAYER, "p"), rebuild(iron(4), 1));
        Evaluator.Result spectator = evaluator.evaluate(full.state(),
                new EvaluationInput(iron(4), Collections.<String>emptySet(), false, 2, true, false, null));
        assertEquals(BonusState.SPECTATOR, spectator.state().bonusState("t:iron4"));
        assertEquals(4, spectator.state().pieceCount("t:iron"));
        int deactivations = 0;
        for (Transition t : spectator.transitions()) {
            if (t.deactivated()) {
                deactivations++;
            }
        }
        assertEquals(2, deactivations);
        // Every bonus changed its reason, so every bonus has a transition line.
        assertEquals(6, spectator.transitions().size());
        Evaluator.Result dead = evaluator.evaluate(full.state(),
                new EvaluationInput(iron(4), Collections.<String>emptySet(), false, 2, false, true, null));
        assertEquals(BonusState.DEAD, dead.state().bonusState("t:iron2"));
    }

    @Test
    void rebuildFromEmptyStateReportsEveryActivationExactlyOnce() {
        Evaluator evaluator = evaluator();
        Evaluator.Result first = evaluator.evaluate(PlayerSetState.empty(PLAYER, "p"), rebuild(iron(4), 1));
        Evaluator.Result rebuilt = evaluator.evaluate(PlayerSetState.empty(PLAYER, "p"), rebuild(iron(4), 50));
        assertEquals(countActivations(first.transitions()), countActivations(rebuilt.transitions()));
        assertEquals(2, countActivations(rebuilt.transitions()));
    }

    @Test
    void inspectReportNamesPiecesAndReasons() {
        Evaluator evaluator = evaluator();
        Evaluator.Result r = evaluator.evaluate(PlayerSetState.empty(PLAYER, "p"), rebuild(iron(3), 1));
        List<String> lines = InspectReport.lines(r.state(), evaluator.definitions());
        String text = String.join("\n", lines);
        assertTrue(text.contains("set t:iron 3/4"), text);
        assertTrue(text.contains("piece head: a:ih in head/head"), text);
        assertTrue(text.contains("piece feet: missing"), text);
        assertTrue(text.contains("bonus t:iron4 requires t:iron(all): missing equipment: t:iron 3/4"), text);
        assertTrue(text.contains("bonus t:iron2 requires t:iron(2): active"), text);
        assertTrue(text.contains("effects: none (scripted)"), text);
    }

    @Test
    void setsMentioningFindsSetsByItem() {
        CompiledDefinitions definitions = definitions();
        Set<String> sets = new HashSet<String>(definitions.setsMentioning(new FakeItem("a:sword")));
        assertEquals(Collections.singleton("t:weapons"), sets);
        assertTrue(definitions.setsMentioning(new FakeItem("a:nothing")).isEmpty());
    }

    @Test
    void aChangedLocationIsRecountedEvenWhenTheItemIdentityIsUnchanged() {
        // An NBT-filtered piece: the same item id without the tag does not count; the same id with the
        // tag, arriving as a change of the head location, does.
        DefinitionRegistry registry = new DefinitionRegistry();
        registry.registerSet(SetBuilder.set("t:named")
                .piece(PieceBuilder.piece("hat").accept(ItemFilter.item("a:hat").withRequiredNbt("display/Name", "Hat")).at("head")).build(), SRC, false);
        registry.registerBonus(BonusBuilder.bonus("t:hat").requiresAll("t:named").description("hat").build(), SRC, false);
        registry.freeze(ANYTHING);
        Evaluator evaluator = new Evaluator(new CompiledDefinitions(registry.definitions()), ConditionEvaluator.NONE, 20);
        Evaluator.Result plain = evaluator.evaluate(PlayerSetState.empty(PLAYER, "p"),
                rebuild(Arrays.asList(new SlotCandidate("head", "head", new FakeItem("a:hat"))), 1));
        assertFalse(plain.state().isActive("t:hat"));
        net.minecraft.nbt.NBTTagCompound display = new net.minecraft.nbt.NBTTagCompound();
        display.setString("Name", "Hat");
        net.minecraft.nbt.NBTTagCompound nbt = new net.minecraft.nbt.NBTTagCompound();
        nbt.setTag("display", display);
        Evaluator.Result named = evaluator.evaluate(plain.state(),
                changed(Arrays.asList(new SlotCandidate("head", "head", new FakeItem("a:hat", 0, nbt))), 2, "head"));
        assertTrue(named.state().isActive("t:hat"));
    }

    @Test
    void highestOnlyFallsBackWhenTheHigherBonusFailsItsCondition() {
        DefinitionRegistry registry = new DefinitionRegistry();
        registry.registerSet(SetBuilder.set("t:leather").thresholdMode(ThresholdMode.HIGHEST_ONLY)
                .armor(ItemFilter.item("a:lh"), ItemFilter.item("a:lc"), ItemFilter.item("a:ll"), ItemFilter.item("a:lf")).build(), SRC, false);
        registry.registerBonus(BonusBuilder.bonus("t:leather2").requires("t:leather", 2).description("two").build(), SRC, false);
        registry.registerBonus(BonusBuilder.bonus("t:leather4").requiresAll("t:leather").description("four")
                .condition("t:never", Collections.<String, String>emptyMap()).conditionInterval(5).build(), SRC, false);
        registry.freeze(ANYTHING);
        final boolean[] pass = {false};
        ConditionEvaluator conditions = new ConditionEvaluator() {
            public Result evaluate(com.mahghuuuls.setworks.api.definition.BonusDefinition bonus, Object player) {
                return pass[0] ? Result.pass() : new Result(Outcome.FAIL, "t:never");
            }
        };
        Evaluator evaluator = new Evaluator(new CompiledDefinitions(registry.definitions()), conditions, 20);
        Evaluator.Result failing = evaluator.evaluate(PlayerSetState.empty(PLAYER, "p"), rebuild(leather(4), 1));
        assertEquals(BonusState.CONDITION_FAILED, failing.state().bonusState("t:leather4"));
        assertEquals("t:never", failing.state().bonuses().get("t:leather4").detail());
        assertTrue(failing.state().isActive("t:leather2"));
        // Not due yet (interval 5): the previous outcome is kept and nothing changes.
        pass[0] = true;
        Evaluator.Result notDue = evaluator.evaluate(failing.state(), changed(leather(4), 3, "hotbar"));
        assertEquals(BonusState.CONDITION_FAILED, notDue.state().bonusState("t:leather4"));
        assertTrue(notDue.transitions().isEmpty());
        assertEquals(1 + 5, evaluator.nextConditionDue(notDue.state(), 3));
        // Due: the condition passes, the 4-bonus takes over and supersedes the 2-bonus.
        Evaluator.Result due = evaluator.evaluate(notDue.state(), changed(leather(4), 6, "hotbar"));
        assertTrue(due.state().isActive("t:leather4"));
        assertEquals(BonusState.SUPERSEDED, due.state().bonusState("t:leather2"));
        // Not due again: the superseded bonus stays superseded, with no spurious transition.
        Evaluator.Result stable = evaluator.evaluate(due.state(), changed(leather(4), 7, "hotbar"));
        assertEquals(BonusState.SUPERSEDED, stable.state().bonusState("t:leather2"));
        assertTrue(stable.transitions().isEmpty());
    }

    private static int countActivations(List<Transition> transitions) {
        int n = 0;
        for (Transition t : transitions) {
            if (t.activated()) {
                n++;
            }
        }
        return n;
    }
}
