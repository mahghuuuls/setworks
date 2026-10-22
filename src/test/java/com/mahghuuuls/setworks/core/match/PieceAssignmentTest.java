package com.mahghuuuls.setworks.core.match;

import com.mahghuuuls.setworks.api.ItemFilter;
import com.mahghuuuls.setworks.api.PieceBuilder;
import com.mahghuuuls.setworks.api.SetBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PieceAssignmentTest {

    private static SlotCandidate slot(String location, String key, String item) {
        return new SlotCandidate(location, key, new FakeItem(item));
    }

    @Test
    void overlappingAlternativesAreAssignedToMaximizeSatisfiedPieces() {
        // "weapon" accepts sword or dagger; "sidearm" accepts sword only. With one sword and one dagger
        // both pieces must be satisfied regardless of order.
        CompiledSet set = new CompiledSet(SetBuilder.set("a:s")
                .piece(PieceBuilder.piece("weapon").accept("a:sword").accept("a:dagger").at("mainhand").at("offhand"))
                .piece(PieceBuilder.piece("sidearm").accept("a:sword").at("mainhand").at("offhand"))
                .build());
        List<SlotCandidate> swordFirst = Arrays.asList(slot("mainhand", "mainhand", "a:sword"), slot("offhand", "offhand", "a:dagger"));
        List<SlotCandidate> daggerFirst = Arrays.asList(slot("offhand", "offhand", "a:dagger"), slot("mainhand", "mainhand", "a:sword"));
        for (List<SlotCandidate> order : Arrays.asList(swordFirst, daggerFirst)) {
            Map<String, SlotCandidate> result = PieceAssignment.assign(set, order);
            assertEquals(2, result.size(), order.toString());
            assertEquals("a:dagger", result.get("weapon").item().registryName());
            assertEquals("a:sword", result.get("sidearm").item().registryName());
        }
    }

    @Test
    void oneStackFillsAtMostOnePieceAndEachPieceCountsOnce() {
        CompiledSet set = new CompiledSet(SetBuilder.set("a:s")
                .piece(PieceBuilder.piece("h1").accept("a:helmet").at("head").at("hotbar"))
                .piece(PieceBuilder.piece("h2").accept("a:helmet").at("head").at("hotbar"))
                .build());
        Map<String, SlotCandidate> one = PieceAssignment.assign(set, Arrays.asList(slot("head", "head", "a:helmet")));
        assertEquals(1, one.size());
        Map<String, SlotCandidate> two = PieceAssignment.assign(set,
                Arrays.asList(slot("head", "head", "a:helmet"), slot("hotbar", "slot:3", "a:helmet")));
        assertEquals(2, two.size());
    }

    @Test
    void locationMustBePermitted() {
        CompiledSet set = new CompiledSet(SetBuilder.set("a:s")
                .piece(PieceBuilder.piece("compass").accept("a:compass").at("hotbar")).build());
        assertTrue(PieceAssignment.assign(set, Arrays.asList(slot("hotbar", "slot:5", "a:compass"))).containsKey("compass"));
        assertFalse(PieceAssignment.assign(set, Arrays.asList(slot("inventory", "slot:12", "a:compass"))).containsKey("compass"));
    }

    @Test
    void emptyInputsGiveNoAssignment() {
        CompiledSet set = new CompiledSet(SetBuilder.set("a:s")
                .armor(ItemFilter.item("a:h"), ItemFilter.item("a:c"), ItemFilter.item("a:l"), ItemFilter.item("a:f")).build());
        assertTrue(PieceAssignment.assign(set, new ArrayList<SlotCandidate>()).isEmpty());
    }

    @Test
    void fullArmorSetCountsFour() {
        CompiledSet set = new CompiledSet(SetBuilder.set("a:s")
                .armor(ItemFilter.item("a:h"), ItemFilter.item("a:c"), ItemFilter.item("a:l"), ItemFilter.item("a:f")).build());
        Map<String, SlotCandidate> result = PieceAssignment.assign(set, Arrays.asList(
                slot("head", "head", "a:h"), slot("chest", "chest", "a:c"), slot("legs", "legs", "a:l"), slot("feet", "feet", "a:f")));
        assertEquals(4, result.size());
        assertEquals("head", result.get("head").slotKey());
    }
}
