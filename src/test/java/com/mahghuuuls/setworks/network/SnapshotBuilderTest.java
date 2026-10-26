package com.mahghuuuls.setworks.network;

import com.mahghuuuls.setworks.api.BonusBuilder;
import com.mahghuuuls.setworks.api.DefinitionSource;
import com.mahghuuuls.setworks.api.IEffectKind;
import com.mahghuuuls.setworks.api.ItemFilter;
import com.mahghuuuls.setworks.api.PieceBuilder;
import com.mahghuuuls.setworks.api.SetBuilder;
import com.mahghuuuls.setworks.api.ThresholdMode;
import com.mahghuuuls.setworks.api.Visibility;
import com.mahghuuuls.setworks.api.definition.BonusDefinition;
import com.mahghuuuls.setworks.api.definition.ConditionSpec;
import com.mahghuuuls.setworks.api.definition.EffectSpec;
import com.mahghuuuls.setworks.core.definition.DefinitionRegistry;
import com.mahghuuuls.setworks.core.definition.FrozenDefinitions;
import com.mahghuuuls.setworks.core.definition.ValidationContext;
import com.mahghuuuls.setworks.core.eval.ConditionEvaluator;
import com.mahghuuuls.setworks.core.eval.EvaluationInput;
import com.mahghuuuls.setworks.core.eval.Evaluator;
import com.mahghuuuls.setworks.core.eval.PlayerSetState;
import com.mahghuuuls.setworks.core.match.CompiledDefinitions;
import com.mahghuuuls.setworks.core.match.FakeItem;
import com.mahghuuuls.setworks.core.match.SlotCandidate;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What reaches the client: filtering, thresholds per set, labels, and a byte round trip. */
class SnapshotBuilderTest {

    static final DefinitionSource SRC = DefinitionSource.mod("test");

    static final ValidationContext ANYTHING = new ValidationContext() {
        public boolean itemExists(String itemId) { return true; }
        public boolean oreHasEntries(String oreName) { return true; }
        public boolean locationExists(String locationId) { return true; }
        public boolean effectKindExists(String kindId) { return true; }
        public String effectProblem(EffectSpec effect) { return null; }
        public boolean conditionKindExists(String kindId) { return true; }
        public String conditionProblem(ConditionSpec condition) { return null; }
        public boolean exactNbtParses(String text) { return true; }
    };

    static final DescriptionGenerator DESCRIPTIONS = new DescriptionGenerator(new DescriptionGenerator.PotionNames() {
        public String translationKey(String potionId) { return "minecraft:speed".equals(potionId) ? "effect.moveSpeed" : null; }
    }, new DescriptionGenerator.Kinds() {
        public IEffectKind kind(String kindId) { return null; }
    });

    static final SnapshotBuilder.Labels KEYS = new SnapshotBuilder.Labels() {
        public String itemLabel(String itemId, int metadata) { return "item." + itemId.replace("minecraft:", "") + ".name"; }
    };

    /** Iron set (4), weapons set, a two-piece and a full bonus, a multi-set bonus displayed under iron only, a hidden and a discovered bonus. */
    static FrozenDefinitions definitions() {
        DefinitionRegistry registry = new DefinitionRegistry();
        registry.registerSet(SetBuilder.set("t:iron").displayName("Iron Armor").armor(ItemFilter.item("minecraft:iron_helmet"), ItemFilter.item("minecraft:iron_chestplate"),
                ItemFilter.item("minecraft:iron_leggings"), ItemFilter.item("minecraft:iron_boots")).build(), SRC, false);
        registry.registerSet(SetBuilder.set("t:weapons").displayName("Iron Weapons")
                .piece(PieceBuilder.piece("weapon").accept("minecraft:iron_sword").accept(ItemFilter.ore("ingotIron")).at("mainhand")).build(), SRC, false);
        registry.registerBonus(BonusBuilder.bonus("t:full").requiresAll("t:iron").attribute("generic.maxHealth", 4, "add").potion("minecraft:speed", 1, false).build(), SRC, false);
        registry.registerBonus(BonusBuilder.bonus("t:two").requires("t:iron", 2).attribute("generic.armor", 2, "add").build(), SRC, false);
        registry.registerBonus(BonusBuilder.bonus("t:strike").requiresAll("t:iron").requires("t:weapons", 1).description("Strike").displayUnder("t:iron").build(), SRC, false);
        registry.registerBonus(BonusBuilder.bonus("t:secret").requiresAll("t:iron").visibility(Visibility.HIDDEN).description("Secret").build(), SRC, false);
        registry.registerBonus(BonusBuilder.bonus("t:memory").requires("t:iron", 3).visibility(Visibility.DISCOVERED).description("t.memory.key").build(), SRC, false);
        registry.freeze(ANYTHING);
        return registry.definitions();
    }

    @Test
    void hiddenAndUndiscoveredBonusesNeverReachTheSnapshotAndThresholdsAreResolved() {
        DisplaySnapshot snapshot = new SnapshotBuilder(DESCRIPTIONS, KEYS).build(definitions(), SnapshotBuilder.VISIBLE_ONLY);
        assertEquals(2, snapshot.sets().size());
        DisplaySnapshot.SetDisplay iron = snapshot.set("t:iron");
        assertEquals("Iron Armor", iron.displayName());
        assertEquals(ThresholdMode.CUMULATIVE, iron.thresholdMode());
        assertEquals(4, iron.pieces().size());
        assertEquals("item.iron_boots.name", iron.pieces().get(3).alternatives().get(0).label());
        assertTrue(iron.pieces().get(3).alternatives().get(0).labelIsKey());
        DisplaySnapshot.PieceDisplay weapon = snapshot.set("t:weapons").pieces().get(0);
        assertEquals("#ingotIron", weapon.alternatives().get(1).label());
        assertFalse(weapon.alternatives().get(1).labelIsKey());
        List<String> ids = new ArrayList<String>();
        for (DisplaySnapshot.BonusDisplay bonus : snapshot.bonuses()) {
            ids.add(bonus.id());
        }
        assertEquals(Arrays.asList("t:full", "t:two", "t:strike"), ids, "hidden and undiscovered are absent; author order kept");
        DisplaySnapshot.BonusDisplay full = snapshot.bonuses().get(0);
        assertEquals(4, (int) full.thresholds().get("t:iron"), "all pieces resolves to the piece count");
        assertEquals("+4 Max Health, ${effect.moveSpeed} I", full.description(), "generated when the author gave none");
        assertFalse(full.multiSet());
        DisplaySnapshot.BonusDisplay strike = snapshot.bonuses().get(2);
        assertTrue(strike.multiSet());
        assertEquals(Arrays.asList("t:iron"), new ArrayList<String>(strike.thresholds().keySet()), "displayed under iron only");
        assertEquals("Strike", strike.description());
        // A discovery filter that allows everything non-hidden brings the discovered bonus in with its key as description.
        DisplaySnapshot all = new SnapshotBuilder(DESCRIPTIONS, KEYS).build(definitions(), new SnapshotBuilder.VisibilityFilter() {
            public boolean mayShow(BonusDefinition bonus) { return true; }
        });
        assertEquals(4, all.bonuses().size());
        assertEquals("t.memory.key", all.bonuses().get(3).description());
        assertNull(all.set("t:none"));
    }

    @Test
    void stateNamesOnlyBonusesTheSnapshotCarriesEvenWhenAHiddenOneIsEquipped() {
        FrozenDefinitions frozen = definitions();
        DisplaySnapshot snapshot = new SnapshotBuilder(DESCRIPTIONS, KEYS).build(frozen, SnapshotBuilder.VISIBLE_ONLY);
        Evaluator evaluator = new Evaluator(new CompiledDefinitions(frozen), ConditionEvaluator.NONE, 20);
        List<SlotCandidate> fullIron = Arrays.asList(new SlotCandidate("head", "head", new FakeItem("minecraft:iron_helmet")),
                new SlotCandidate("chest", "chest", new FakeItem("minecraft:iron_chestplate")), new SlotCandidate("legs", "legs", new FakeItem("minecraft:iron_leggings")),
                new SlotCandidate("feet", "feet", new FakeItem("minecraft:iron_boots")));
        PlayerSetState state = evaluator.evaluate(PlayerSetState.empty(java.util.UUID.randomUUID(), "p"),
                new EvaluationInput(fullIron, java.util.Collections.<String>emptySet(), true, 1, false, false, null)).state();
        assertTrue(state.isActive("t:secret"), "the hidden bonus is active on the server");
        assertTrue(state.isActive("t:memory"), "the undiscovered bonus is active on the server");
        DisplayState display = ServerStateSink.toDisplay(state, snapshot.bonusIds());
        assertTrue(display.isEquipped("t:full"));
        assertTrue(display.isEquipped("t:two"));
        assertFalse(display.isEquipped("t:secret"), "hidden: never sent");
        assertFalse(display.isEquipped("t:memory"), "undiscovered: never sent");
        assertFalse(display.isEquipped("t:strike"), "its weapon piece is missing");
        assertEquals(2, display.equippedBonuses().size());
        assertEquals(4, display.count("t:iron"));
        assertEquals(0, display.count("t:weapons"));
    }

    @Test
    void stateNamesABonusWhosePiecesAreOnEvenWhenItsConditionFails() {
        DefinitionRegistry registry = new DefinitionRegistry();
        registry.registerSet(SetBuilder.set("t:iron").armor(ItemFilter.item("minecraft:iron_helmet"), ItemFilter.item("minecraft:iron_chestplate"),
                ItemFilter.item("minecraft:iron_leggings"), ItemFilter.item("minecraft:iron_boots")).build(), SRC, false);
        registry.registerBonus(BonusBuilder.bonus("t:sneaky").requires("t:iron", 2).description("Sneaky")
                .condition("t:never", java.util.Collections.<String, String>emptyMap()).build(), SRC, false);
        registry.registerBonus(BonusBuilder.bonus("t:all").requiresAll("t:iron").description("All").build(), SRC, false);
        registry.freeze(ANYTHING);
        FrozenDefinitions frozen = registry.definitions();
        DisplaySnapshot snapshot = new SnapshotBuilder(DESCRIPTIONS, KEYS).build(frozen, SnapshotBuilder.VISIBLE_ONLY);
        ConditionEvaluator failing = new ConditionEvaluator() {
            public Result evaluate(BonusDefinition bonus, Object player, com.mahghuuuls.setworks.api.IConditionContext context) {
                return new Result(Outcome.FAIL, "t:never");
            }
        };
        Evaluator evaluator = new Evaluator(new CompiledDefinitions(frozen), failing, 20);
        List<SlotCandidate> twoIron = Arrays.asList(new SlotCandidate("head", "head", new FakeItem("minecraft:iron_helmet")),
                new SlotCandidate("chest", "chest", new FakeItem("minecraft:iron_chestplate")));
        PlayerSetState state = evaluator.evaluate(PlayerSetState.empty(java.util.UUID.randomUUID(), "p"),
                new EvaluationInput(twoIron, java.util.Collections.<String>emptySet(), true, 1, false, false, null)).state();
        assertFalse(state.isActive("t:sneaky"), "the condition fails on the server");
        DisplayState display = ServerStateSink.toDisplay(state, snapshot.bonusIds());
        assertTrue(display.isEquipped("t:sneaky"), "its two pieces are on, so the line is green whatever the condition says");
        assertFalse(display.isEquipped("t:all"), "two of four pieces: gray");
        assertEquals(1, display.equippedBonuses().size());
    }

    @Test
    void perPlayerFilterAddsOnlyThatPlayersDiscoveriesAndTheDiscoveryMessageCarriesBonusesOnly() {
        FrozenDefinitions frozen = definitions();
        SnapshotBuilder builder = new SnapshotBuilder(DESCRIPTIONS, KEYS);
        DisplaySnapshot nothing = builder.build(frozen, SnapshotBuilder.forPlayer(java.util.Collections.<String>emptySet()));
        assertEquals(3, nothing.bonuses().size(), "no discovery: visible only");
        DisplaySnapshot discovered = builder.build(frozen, SnapshotBuilder.forPlayer(java.util.Collections.singleton("t:memory")));
        assertEquals(4, discovered.bonuses().size());
        assertTrue(discovered.bonusIds().contains("t:memory"));
        assertFalse(discovered.bonusIds().contains("t:secret"), "hidden stays out whatever the record says");
        DisplaySnapshot stale = builder.build(frozen, SnapshotBuilder.forPlayer(new java.util.HashSet<String>(Arrays.asList("t:secret", "t:full", "t:nope"))));
        assertEquals(3, stale.bonuses().size(), "a record naming hidden, visible, or unknown ids changes nothing");
        DisplaySnapshot message = builder.bonusesOnly(frozen, Arrays.asList("t:memory", "t:secret", "t:nope"));
        assertTrue(message.sets().isEmpty());
        assertEquals(1, message.bonuses().size());
        assertEquals("t:memory", message.bonuses().get(0).id());
        assertEquals(3, (int) message.bonuses().get(0).thresholds().get("t:iron"));
        // The state filter for a player: visible ids plus their discovered ones, never a hidden id.
        java.util.Set<String> shown = ServerStateSink.shownBonusIds(nothing.bonusIds(), new java.util.HashSet<String>(Arrays.asList("t:memory", "t:secret")), frozen);
        assertTrue(shown.contains("t:memory"));
        assertFalse(shown.contains("t:secret"));
        assertEquals(4, shown.size());
    }

    @Test
    void snapshotAndStateSurviveTheWire() {
        DisplaySnapshot snapshot = new SnapshotBuilder(DESCRIPTIONS, KEYS).build(definitions(), SnapshotBuilder.VISIBLE_ONLY);
        ByteBuf buf = Unpooled.buffer();
        snapshot.write(buf);
        int bytes = buf.readableBytes();
        DisplaySnapshot back = DisplaySnapshot.read(buf);
        assertEquals(0, buf.readableBytes(), "everything written was read");
        assertTrue(bytes > 0 && bytes < 2000, "a small snapshot: " + bytes);
        assertEquals(snapshot.sets().size(), back.sets().size());
        assertEquals("item.iron_boots.name", back.set("t:iron").pieces().get(3).alternatives().get(0).label());
        assertEquals(ItemFilter.ore("ingotIron"), back.set("t:weapons").pieces().get(0).alternatives().get(1).filter());
        assertEquals(ItemFilter.item("minecraft:iron_sword"), back.set("t:weapons").pieces().get(0).alternatives().get(0).filter());
        assertEquals(3, back.bonuses().size());
        assertEquals("+4 Max Health, ${effect.moveSpeed} I", back.bonuses().get(0).description());
        assertEquals(4, (int) back.bonuses().get(0).thresholds().get("t:iron"));
        assertTrue(back.bonuses().get(2).multiSet());
        // A filter with NBT rules and metadata also round-trips.
        ItemFilter nbt = ItemFilter.item("minecraft:wool", 11).withRequiredNbt("display/Name", "Blue").withAbsentNbt("ench").withExactNbt("{a:1}");
        ByteBuf fb = Unpooled.buffer();
        Codec.writeFilter(fb, nbt);
        assertEquals(nbt, Codec.readFilter(fb));
        // State.
        java.util.Map<String, List<String>> pieces = new java.util.LinkedHashMap<String, List<String>>();
        pieces.put("t:iron", Arrays.asList("head", "chest", "legs"));
        pieces.put("t:weapons", new ArrayList<String>());
        DisplayState state = new DisplayState(pieces, new java.util.LinkedHashSet<String>(Arrays.asList("t:two")));
        ByteBuf sb = Unpooled.buffer();
        state.write(sb);
        DisplayState stateBack = DisplayState.read(sb);
        assertEquals(3, stateBack.count("t:iron"));
        assertEquals(0, stateBack.count("t:weapons"));
        assertEquals(0, stateBack.count("t:unknown"));
        assertTrue(stateBack.isSatisfied("t:iron", "legs"));
        assertFalse(stateBack.isSatisfied("t:iron", "feet"));
        assertTrue(stateBack.isEquipped("t:two"));
        assertFalse(stateBack.isEquipped("t:full"));
    }
}
