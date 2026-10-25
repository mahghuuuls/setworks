package com.mahghuuuls.setworks.core.definition;

import com.mahghuuuls.setworks.api.BonusBuilder;
import com.mahghuuuls.setworks.api.DefinitionSource;
import com.mahghuuuls.setworks.api.ICondition;
import com.mahghuuuls.setworks.api.IConditionContext;
import com.mahghuuuls.setworks.api.ItemFilter;
import com.mahghuuuls.setworks.api.SetBuilder;
import net.minecraft.entity.player.EntityPlayerMP;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A condition whose kind exists but whose parameters the kind rejects disables its bonus; a direct condition is never checked against the kinds. */
class ConditionValidationTest {

    private static final DefinitionSource SRC = DefinitionSource.mod("test");

    private static DefinitionRegistry ironRegistry() {
        DefinitionRegistry registry = new DefinitionRegistry();
        registry.registerSet(SetBuilder.set("a:iron").armor(ItemFilter.item("minecraft:iron_helmet"), ItemFilter.item("minecraft:iron_chestplate"),
                ItemFilter.item("minecraft:iron_leggings"), ItemFilter.item("minecraft:iron_boots")).build(), SRC, false);
        return registry;
    }

    @Test
    void impossibleFractionIsReportedAsInvalidConditionAndDisablesTheSet() {
        DefinitionRegistry registry = ironRegistry();
        registry.registerBonus(BonusBuilder.bonus("a:bad").requires("a:iron", 2).description("x")
                .condition("setworks:health_below", Collections.singletonMap("fraction", "2")).build(), SRC, false);
        ValidationReport report = registry.freeze(new FakeContext());
        assertEquals("invalid-condition", report.errors().get(0).rule(), report.lines().toString());
        assertTrue(report.errors().get(0).describe().contains("fraction must be above 0 and at most 1"), report.errors().get(0).describe());
        assertNull(registry.definitions().bonus("a:bad"));
        assertNull(registry.definitions().set("a:iron"));
    }

    @Test
    void directConditionNeedsNoRegisteredKind() {
        DefinitionRegistry registry = ironRegistry();
        ICondition always = new ICondition() {
            public boolean test(EntityPlayerMP player, IConditionContext context) { return true; }
        };
        registry.registerBonus(BonusBuilder.bonus("a:direct").requires("a:iron", 2).description("x").condition("my lambda", always).build(), SRC, false);
        ValidationReport report = registry.freeze(new FakeContext());
        assertTrue(report.errors().isEmpty(), report.lines().toString());
        assertNotNull(registry.definitions().bonus("a:direct"));
    }
}
