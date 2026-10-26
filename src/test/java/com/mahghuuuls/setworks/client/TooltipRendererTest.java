package com.mahghuuuls.setworks.client;

import com.mahghuuuls.setworks.api.ItemFilter;
import com.mahghuuuls.setworks.api.ThresholdMode;
import com.mahghuuuls.setworks.core.match.FakeItem;
import com.mahghuuuls.setworks.network.DisplaySnapshot;
import com.mahghuuuls.setworks.network.DisplayState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The block layout: order, colours, counts, Shift pieces, translation, and the store's set lookup. */
class TooltipRendererTest {

    private static DisplaySnapshot.AlternativeDisplay alt(String itemId, String label) {
        return new DisplaySnapshot.AlternativeDisplay(ItemFilter.item(itemId), label, true);
    }

    /** Iron (4 pieces, bonuses at 2 and all, a multi-set bonus under it) and a weapon set with two alternatives. */
    static DisplaySnapshot snapshot() {
        List<DisplaySnapshot.PieceDisplay> iron = Arrays.asList(
                new DisplaySnapshot.PieceDisplay("head", Collections.singletonList(alt("minecraft:iron_helmet", "item.helmetIron.name"))),
                new DisplaySnapshot.PieceDisplay("chest", Collections.singletonList(alt("minecraft:iron_chestplate", "item.chestplateIron.name"))),
                new DisplaySnapshot.PieceDisplay("legs", Collections.singletonList(alt("minecraft:iron_leggings", "item.leggingsIron.name"))),
                new DisplaySnapshot.PieceDisplay("feet", Collections.singletonList(alt("minecraft:iron_boots", "item.bootsIron.name"))));
        List<DisplaySnapshot.PieceDisplay> weapons = Collections.singletonList(
                new DisplaySnapshot.PieceDisplay("weapon", Arrays.asList(alt("minecraft:iron_sword", "item.swordIron.name"),
                        new DisplaySnapshot.AlternativeDisplay(ItemFilter.ore("ingotIron"), "#ingotIron", false))));
        Map<String, Integer> four = new LinkedHashMap<String, Integer>();
        four.put("t:iron", 4);
        Map<String, Integer> two = new LinkedHashMap<String, Integer>();
        two.put("t:iron", 2);
        Map<String, Integer> strike = new LinkedHashMap<String, Integer>();
        strike.put("t:iron", 4);
        strike.put("t:weapons", 1);
        return new DisplaySnapshot(
                Arrays.asList(new DisplaySnapshot.SetDisplay("t:iron", "Iron Armor", ThresholdMode.CUMULATIVE, iron),
                        new DisplaySnapshot.SetDisplay("t:weapons", "t.weapons.name", ThresholdMode.CUMULATIVE, weapons)),
                Arrays.asList(new DisplaySnapshot.BonusDisplay("t:full", "+4 Max Health, ${effect.moveSpeed} I", false, four),
                        new DisplaySnapshot.BonusDisplay("t:two", "+2 Armor", false, two),
                        new DisplaySnapshot.BonusDisplay("t:strike", "Strike", true, strike)));
    }

    static DisplayState threeOfFour() {
        Map<String, List<String>> pieces = new LinkedHashMap<String, List<String>>();
        pieces.put("t:iron", Arrays.asList("head", "chest", "legs"));
        return new DisplayState(pieces, new LinkedHashSet<String>(Collections.singletonList("t:two")));
    }

    private static final TooltipRenderer.Translator FAKE_LANG = new TooltipRenderer.Translator() {
        public String translate(String textOrKey) {
            return textOrKey.replace("${effect.moveSpeed}", "Speed").replace("t.weapons.name", "Iron Weapons").replace("item.bootsIron.name", "Iron Boots")
                    .replace("item.helmetIron.name", "Iron Helmet").replace("item.chestplateIron.name", "Iron Chestplate")
                    .replace("item.leggingsIron.name", "Iron Leggings").replace("item.swordIron.name", "Iron Sword");
        }
    };

    @Test
    void blockReadsAsSpecifiedWithThreeOfFourPieces() {
        List<String> lines = TooltipRenderer.render(snapshot(), threeOfFour(), Collections.singletonList("t:iron"), false, FAKE_LANG);
        assertEquals(Arrays.asList(
                "",
                "§6Iron Armor (3/4)",
                "§a(2) +2 Armor",
                "§7(4) +4 Max Health, Speed I",
                "§7(4) Strike"), lines, "blank, gold name with count, single-set ascending, then multi-set; green only with the pieces on");
    }

    @Test
    void shiftAddsNoSeparatorToABlockWithoutBonusLines() {
        List<DisplaySnapshot.PieceDisplay> hat = Collections.singletonList(
                new DisplaySnapshot.PieceDisplay("head", Collections.singletonList(alt("minecraft:leather_helmet", "Leather Cap"))));
        DisplaySnapshot bare = new DisplaySnapshot(
                Collections.singletonList(new DisplaySnapshot.SetDisplay("t:hat", "Hat", ThresholdMode.CUMULATIVE, hat)),
                Collections.<DisplaySnapshot.BonusDisplay>emptyList());
        List<String> lines = TooltipRenderer.render(bare, DisplayState.empty(), Collections.singletonList("t:hat"), true, FAKE_LANG);
        assertEquals(Arrays.asList("", "§6Hat (0/1)", "§7Leather Cap"), lines, "no bonus lines, so no separator and no trailing blank");
    }

    @Test
    void shiftInsertsPieceLinesAfterTheNameWhiteWhenWornGrayWhenMissing() {
        List<String> lines = TooltipRenderer.render(snapshot(), threeOfFour(), Collections.singletonList("t:iron"), true, FAKE_LANG);
        assertEquals("§6Iron Armor (3/4)", lines.get(1));
        assertEquals("§fIron Helmet", lines.get(2));
        assertEquals("§fIron Chestplate", lines.get(3));
        assertEquals("§fIron Leggings", lines.get(4));
        assertEquals("§7Iron Boots", lines.get(5), "the missing piece names its items in gray");
        assertEquals("", lines.get(6), "one blank line separates the piece lines from the bonus lines");
        assertEquals("§a(2) +2 Armor", lines.get(7));
        assertEquals(10, lines.size());
        // Alternatives are joined by " or "; an ore label is shown as given.
        List<String> weapon = TooltipRenderer.render(snapshot(), threeOfFour(), Collections.singletonList("t:weapons"), true, FAKE_LANG);
        assertEquals("§6Iron Weapons (0/1)", weapon.get(1), "a set name that is a key is translated");
        assertEquals("§7Iron Sword or #ingotIron", weapon.get(2));
        assertEquals("", weapon.get(3));
        assertEquals("§7(1) Strike", weapon.get(4), "the multi-set bonus shows under the weapon set with its own threshold");
        assertEquals(5, weapon.size());
    }

    @Test
    void twoSetsGiveTwoBlocksAndNoSetGivesNothing() {
        List<String> lines = TooltipRenderer.render(snapshot(), threeOfFour(), Arrays.asList("t:iron", "t:weapons"), false, FAKE_LANG);
        assertEquals(8, lines.size());
        assertEquals("", lines.get(5));
        assertEquals("§6Iron Weapons (0/1)", lines.get(6));
        assertEquals("§7(1) Strike", lines.get(7));
        assertTrue(TooltipRenderer.render(snapshot(), threeOfFour(), Collections.<String>emptyList(), false, FAKE_LANG).isEmpty());
        assertTrue(TooltipRenderer.render(snapshot(), threeOfFour(), Collections.singletonList("t:gone"), false, FAKE_LANG).isEmpty());
    }

    @Test
    void orderingPutsSingleSetBonusesAscendingThenMultiSetInAuthorOrder() {
        Map<String, Integer> six = new LinkedHashMap<String, Integer>();
        six.put("t:iron", 6);
        Map<String, Integer> three = new LinkedHashMap<String, Integer>();
        three.put("t:iron", 3);
        Map<String, Integer> multiA = new LinkedHashMap<String, Integer>();
        multiA.put("t:iron", 2);
        multiA.put("t:x", 1);
        Map<String, Integer> multiB = new LinkedHashMap<String, Integer>();
        multiB.put("t:iron", 1);
        multiB.put("t:x", 1);
        DisplaySnapshot snapshot = new DisplaySnapshot(Collections.<DisplaySnapshot.SetDisplay>emptyList(), Arrays.asList(
                new DisplaySnapshot.BonusDisplay("m:a", "a", true, multiA),
                new DisplaySnapshot.BonusDisplay("s:six", "six", false, six),
                new DisplaySnapshot.BonusDisplay("m:b", "b", true, multiB),
                new DisplaySnapshot.BonusDisplay("s:three", "three", false, three)));
        List<String> ids = new ArrayList<String>();
        for (DisplaySnapshot.BonusDisplay bonus : TooltipRenderer.bonusesUnder(snapshot, "t:iron")) {
            ids.add(bonus.id());
        }
        assertEquals(Arrays.asList("s:three", "s:six", "m:a", "m:b"), ids);
    }

    @Test
    void storeMergesADiscoveryIntoItsSnapshot() {
        ClientStore store = new ClientStore();
        store.acceptSnapshot(snapshot());
        Map<String, Integer> three = new LinkedHashMap<String, Integer>();
        three.put("t:iron", 3);
        store.acceptDiscovery(new DisplaySnapshot(Collections.<DisplaySnapshot.SetDisplay>emptyList(),
                Collections.singletonList(new DisplaySnapshot.BonusDisplay("t:memory", "Memory", false, three))));
        assertEquals(4, store.snapshot().bonuses().size());
        assertEquals(2, store.snapshot().sets().size(), "sets untouched");
        List<String> lines = TooltipRenderer.render(store.snapshot(), threeOfFour(), Collections.singletonList("t:iron"), false, FAKE_LANG);
        assertEquals("§7(3) Memory", lines.get(3), "the discovered bonus sorts by threshold among the single-set lines");
        store.acceptDiscovery(new DisplaySnapshot(Collections.<DisplaySnapshot.SetDisplay>emptyList(),
                Collections.singletonList(new DisplaySnapshot.BonusDisplay("t:memory", "Memory again", false, three))));
        assertEquals(4, store.snapshot().bonuses().size(), "a repeated id replaces, never duplicates");
    }

    @Test
    void storeFindsTheSetsOfAStackFromTheSnapshotFilters() {
        ClientStore store = new ClientStore();
        store.acceptSnapshot(snapshot());
        assertEquals(Collections.singletonList("t:iron"), store.setsOf(new FakeItem("minecraft:iron_boots")));
        assertEquals(Collections.singletonList("t:weapons"), store.setsOf(new FakeItem("minecraft:iron_sword")));
        assertTrue(store.setsOf(new FakeItem("minecraft:stone")).isEmpty());
        store.clear();
        assertTrue(store.setsOf(new FakeItem("minecraft:iron_boots")).isEmpty());
    }
}
