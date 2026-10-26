package com.mahghuuuls.setworks.core.discovery;

import com.mahghuuuls.setworks.api.BonusBuilder;
import com.mahghuuuls.setworks.api.BonusState;
import com.mahghuuuls.setworks.api.DefinitionSource;
import com.mahghuuuls.setworks.api.ItemFilter;
import com.mahghuuuls.setworks.api.SetBuilder;
import com.mahghuuuls.setworks.api.Visibility;
import com.mahghuuuls.setworks.api.definition.ConditionSpec;
import com.mahghuuuls.setworks.api.definition.EffectSpec;
import com.mahghuuuls.setworks.core.definition.DefinitionRegistry;
import com.mahghuuuls.setworks.core.definition.ValidationContext;
import com.mahghuuuls.setworks.core.eval.DiscoveryRecorder;
import com.mahghuuuls.setworks.core.eval.Transition;
import com.mahghuuuls.setworks.core.match.CompiledDefinitions;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The record only grows, round-trips through NBT, and is written only for discovered-visibility activations. */
class DiscoveryTest {

    private static final DefinitionSource SRC = DefinitionSource.mod("test");

    private static final ValidationContext ANYTHING = new ValidationContext() {
        public boolean itemExists(String itemId) { return true; }
        public boolean oreHasEntries(String oreName) { return true; }
        public boolean locationExists(String locationId) { return true; }
        public boolean effectKindExists(String kindId) { return true; }
        public String effectProblem(EffectSpec effect) { return null; }
        public boolean conditionKindExists(String kindId) { return true; }
        public String conditionProblem(ConditionSpec condition) { return null; }
        public boolean exactNbtParses(String text) { return true; }
    };

    @Test
    void recordGrowsOnlyAndRoundTripsThroughNbt() {
        DiscoveryCapability record = new DiscoveryCapability();
        assertTrue(record.discover("t:memory"));
        assertFalse(record.discover("t:memory"), "a second activation is not a new discovery");
        assertTrue(record.discover("t:other"));
        assertFalse(record.discover(null));
        assertTrue(record.has("t:memory"));
        assertFalse(record.has("t:none"));
        NBTTagCompound tag = record.writeNBT();
        assertEquals(2, tag.getTagList(DiscoveryCapability.TAG_DISCOVERED, 8).tagCount());
        assertEquals(1, tag.getKeySet().size(), "the file holds the list and nothing else");
        assertTrue(tag.hasKey(DiscoveryCapability.TAG_DISCOVERED));
        DiscoveryCapability back = new DiscoveryCapability();
        back.readNBT(tag);
        assertEquals(Arrays.asList("t:memory", "t:other"), new java.util.ArrayList<String>(back.all()), "order kept");
        back.readNBT(null);
        assertEquals(2, back.all().size(), "a missing tag never clears a record");
        back.readNBT(new NBTTagCompound());
        assertEquals(2, back.all().size(), "a tag without the list never clears a record");
        // The respawn copy adds, never removes.
        DiscoveryCapability fresh = new DiscoveryCapability();
        fresh.discover("t:already");
        fresh.copyFrom(record);
        assertEquals(3, fresh.all().size());
        fresh.copyFrom(null);
        assertEquals(3, fresh.all().size());
    }

    @Test
    void onlyDiscoveredVisibilityActivationsAreRecorded() {
        DefinitionRegistry registry = new DefinitionRegistry();
        registry.registerSet(SetBuilder.set("t:iron").armor(ItemFilter.item("a:ih"), ItemFilter.item("a:ic"), ItemFilter.item("a:il"), ItemFilter.item("a:if")).build(), SRC, false);
        registry.registerBonus(BonusBuilder.bonus("t:visible").requires("t:iron", 1).description("v").build(), SRC, false);
        registry.registerBonus(BonusBuilder.bonus("t:memory").requires("t:iron", 3).visibility(Visibility.DISCOVERED).description("m").build(), SRC, false);
        registry.registerBonus(BonusBuilder.bonus("t:secret").requiresAll("t:iron").visibility(Visibility.HIDDEN).description("s").build(), SRC, false);
        registry.freeze(ANYTHING);
        CompiledDefinitions definitions = new CompiledDefinitions(registry.definitions());
        DiscoveryCapability record = new DiscoveryCapability();
        List<Transition> activations = Arrays.asList(
                new Transition("t:visible", BonusState.MISSING_EQUIPMENT, BonusState.ACTIVE, ""),
                new Transition("t:memory", BonusState.MISSING_EQUIPMENT, BonusState.ACTIVE, ""),
                new Transition("t:secret", BonusState.MISSING_EQUIPMENT, BonusState.ACTIVE, ""),
                new Transition("t:unknown", BonusState.MISSING_EQUIPMENT, BonusState.ACTIVE, ""));
        assertEquals(Collections.singletonList("t:memory"), DiscoveryRecorder.record(activations, definitions, record));
        assertEquals(Collections.singleton("t:memory"), record.all(), "visible and hidden bonuses leave no record");
        assertTrue(DiscoveryRecorder.record(activations, definitions, record).isEmpty(), "already discovered");
        List<Transition> deactivation = Collections.singletonList(new Transition("t:memory", BonusState.ACTIVE, BonusState.DEAD, ""));
        assertTrue(DiscoveryRecorder.record(deactivation, definitions, new DiscoveryCapability()).isEmpty(), "only activations discover");
        assertTrue(DiscoveryRecorder.record(activations, definitions, null).isEmpty(), "no record attached: nothing happens");
    }
}
