package com.mahghuuuls.setworks.api;

import com.mahghuuuls.setworks.api.definition.BonusDefinition;
import com.mahghuuuls.setworks.api.definition.SetDefinition;
import com.mahghuuuls.setworks.api.definition.SetRequirement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildersTest {

    @Test
    void itemFilterFormsDescribeThemselves() {
        assertEquals("minecraft:wool", ItemFilter.item("minecraft:wool").describe());
        assertEquals("minecraft:wool@5", ItemFilter.item("minecraft:wool", 5).describe());
        assertEquals("ore:ingotIron", ItemFilter.ore("ingotIron").describe());
        ItemFilter nbt = ItemFilter.item("minecraft:leather_leggings")
                .withRequiredNbt("display/Name", "Wirt's Leggings")
                .withAbsentNbt("Unbreakable");
        assertEquals("minecraft:leather_leggings +display/Name=Wirt's Leggings -Unbreakable", nbt.describe());
        assertTrue(nbt.hasNbtRules());
        assertFalse(ItemFilter.item("minecraft:wool").hasNbtRules());
        assertEquals(ItemFilter.ANY_METADATA, ItemFilter.item("minecraft:wool").metadata());
    }

    @Test
    void itemFilterRejectsBadInput() {
        assertThrows(IllegalArgumentException.class, () -> ItemFilter.item(" "));
        assertThrows(IllegalArgumentException.class, () -> ItemFilter.item("minecraft:wool", -2));
        assertThrows(IllegalStateException.class, () -> ItemFilter.ore("ingotIron").withAbsentNbt("x"));
    }

    @Test
    void armorHelperMakesFourPiecesInOrder() {
        SetDefinition set = SetBuilder.set("a:iron")
                .armor(ItemFilter.item("a:h"), ItemFilter.item("a:c"), ItemFilter.item("a:l"), ItemFilter.item("a:f"))
                .build();
        assertEquals(4, set.pieces().size());
        assertEquals("head", set.pieces().get(0).name());
        assertEquals("feet", set.pieces().get(3).name());
        assertEquals("feet", set.pieces().get(3).locations().get(0));
        assertEquals("a:iron", set.displayName());
        assertEquals(ThresholdMode.CUMULATIVE, set.thresholdMode());
    }

    @Test
    void pieceDeduplicatesAlternativesAndLocations() {
        PieceBuilder piece = PieceBuilder.piece("weapon").accept("a:sword").accept("a:sword").at("mainhand").at("mainhand");
        assertEquals(1, piece.build().alternatives().size());
        assertEquals(1, piece.build().locations().size());
    }

    @Test
    void bonusBuilderRecordsRequirementsEffectsAndPresentation() {
        BonusDefinition bonus = BonusBuilder.bonus("a:strike")
                .requiresAll("a:iron").requires("a:weapons", 1)
                .attribute("generic.armor", 2, "add").potion("minecraft:speed", 2, false)
                .visibility(Visibility.DISCOVERED).displayUnder("a:iron").conditionInterval(5)
                .build();
        assertTrue(bonus.isMultiSet());
        assertEquals(2, bonus.effects().size());
        assertEquals("setworks:attribute", bonus.effects().get(0).kindId());
        assertEquals("2", bonus.effects().get(1).param("level"));
        assertFalse(bonus.isScripted());
        assertNull(bonus.description());
        assertTrue(bonus.displayedUnder("a:iron"));
        assertFalse(bonus.displayedUnder("a:weapons"));
        assertEquals(5, bonus.conditionInterval());
        assertEquals(SetRequirement.ALL_PIECES, bonus.requirementFor("a:iron").threshold());
        assertEquals(1, bonus.requirementFor("a:weapons").threshold());
    }

    @Test
    void displayUnderNeedsAtLeastOneSet() {
        assertThrows(IllegalArgumentException.class, () -> BonusBuilder.bonus("a:b").displayUnder());
    }

    @Test
    void bonusWithoutDisplaySetsShowsUnderEveryRequiredSet() {
        BonusDefinition bonus = BonusBuilder.bonus("a:b").requires("a:x", 1).requires("a:y", 1).description("d").build();
        assertTrue(bonus.displayedUnder("a:x"));
        assertTrue(bonus.displayedUnder("a:y"));
        assertFalse(bonus.displayedUnder("a:z"));
    }

    @Test
    void effectiveThresholdUsesPieceCountForAllPieces() {
        SetDefinition set = SetBuilder.set("a:iron")
                .armor(ItemFilter.item("a:h"), ItemFilter.item("a:c"), ItemFilter.item("a:l"), ItemFilter.item("a:f")).build();
        assertEquals(4, SetRequirement.all("a:iron").effectiveThreshold(set));
        assertEquals(2, SetRequirement.atLeast("a:iron", 2).effectiveThreshold(set));
        assertThrows(IllegalArgumentException.class, () -> SetRequirement.atLeast("a:iron", 0));
    }

    @Test
    void sourcesDescribeThemselves() {
        assertEquals("mod:jass", DefinitionSource.mod("jass").describe());
        assertEquals("script:sets.zs:12", DefinitionSource.script("sets.zs", "12").describe());
        assertEquals("script:sets.zs", DefinitionSource.script("sets.zs", null).describe());
        assertEquals("setworks:examples", DefinitionSource.builtin("setworks:examples").describe());
    }
}
