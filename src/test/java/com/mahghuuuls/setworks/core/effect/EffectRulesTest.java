package com.mahghuuuls.setworks.core.effect;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The pure rules behind the built-in kinds: health policy, potion signature, parameter parsing. */
class EffectRulesTest {

    private static Map<String, String> map(String... keyValues) {
        Map<String, String> map = new HashMap<String, String>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    @Test
    void raisingTheMaximumDoesNotHealAndLoweringClamps() {
        // 20 of 20 gains +4: stays 20 of 24.
        assertFalse(HealthPolicy.needsClamp(20f, 24f));
        assertEquals(20f, HealthPolicy.healthAfterMaximumChange(20f, 24f));
        // 24 of 24 loses the +4: clamps to 20 of 20.
        assertTrue(HealthPolicy.needsClamp(24f, 20f));
        assertEquals(20f, HealthPolicy.healthAfterMaximumChange(24f, 20f));
        // 7 of 24 loses the +4: untouched at 7 of 20.
        assertFalse(HealthPolicy.needsClamp(7f, 20f));
        assertEquals(7f, HealthPolicy.healthAfterMaximumChange(7f, 20f));
        assertFalse(HealthPolicy.needsClamp(20f, 20f));
    }

    @Test
    void potionSignatureRemovesOnlyItsOwnShortEffect() {
        int speedOne = 0;
        assertTrue(PotionSignature.shouldRemove(speedOne, 0, 300), "fresh own effect");
        assertTrue(PotionSignature.shouldRemove(speedOne, 0, 1), "nearly expired own effect");
        assertFalse(PotionSignature.shouldRemove(speedOne, 0, 301), "a longer effect belongs to someone else");
        assertFalse(PotionSignature.shouldRemove(speedOne, 0, 9600), "an eight-minute drunk potion stays");
        assertFalse(PotionSignature.shouldRemove(speedOne, 1, 300), "a stronger effect stays");
        assertFalse(PotionSignature.shouldRemove(1, 0, 300), "a weaker effect is not ours either");
        assertEquals(300, PotionSignature.DURATION_TICKS);
        assertEquals(20, PotionSignature.REFRESH_INTERVAL_TICKS);
        assertTrue(PotionSignature.DURATION_TICKS - PotionSignature.REFRESH_INTERVAL_TICKS > 200, "no HUD blink between refreshes");
    }

    @Test
    void potionParametersParseAndReject() {
        PotionSignature.Params speed = PotionSignature.parse(map("potion", "minecraft:speed", "level", "2", "particles", "false"));
        assertEquals("minecraft:speed", speed.potionId());
        assertEquals(2, speed.level());
        assertEquals(1, speed.amplifier());
        assertFalse(speed.particles());
        assertEquals("minecraft:speed 2", speed.describe());
        assertEquals("minecraft:luck 1 with particles", PotionSignature.parse(map("potion", "minecraft:luck", "particles", "true")).describe());
        assertFalse(PotionSignature.parse(map("potion", "minecraft:luck")).particles(), "particles default to off");
        assertNull(PotionSignature.problem(map("potion", "minecraft:speed", "level", "1", "particles", "false")));
        assertNotNull(PotionSignature.problem(map("level", "1")));
        assertNotNull(PotionSignature.problem(map("potion", "minecraft:speed", "level", "0")));
        assertNotNull(PotionSignature.problem(map("potion", "minecraft:speed", "level", "two")));
        assertNotNull(PotionSignature.problem(map("potion", "minecraft:speed", "particles", "yes")));
    }

    @Test
    void attributeParametersParseDescribeAndReject() {
        AttributeParams armor = AttributeParams.parse(map("attribute", "generic.armor", "amount", "2.0", "operation", "add"));
        assertEquals("generic.armor", armor.attribute());
        assertEquals(2.0, armor.amount());
        assertEquals(AttributeParams.ADD, armor.operation());
        assertEquals("generic.armor add 2", armor.describe());
        AttributeParams speed = AttributeParams.parse(map("attribute", "generic.movementSpeed", "amount", "0.1", "operation", "MULTIPLY_TOTAL"));
        assertEquals(AttributeParams.MULTIPLY_TOTAL, speed.operation());
        assertEquals("generic.movementSpeed multiply_total 0.1", speed.describe());
        assertEquals(AttributeParams.MULTIPLY_BASE, AttributeParams.parse(map("attribute", "a", "amount", "-1.5", "operation", "multiply_base")).operation());
        assertNull(AttributeParams.problem(map("attribute", "generic.armor", "amount", "2", "operation", "add")));
        assertNotNull(AttributeParams.problem(map("attribute", "generic.armor", "amount", "2", "operation", "times")));
        assertNotNull(AttributeParams.problem(map("attribute", "generic.armor", "amount", "two", "operation", "add")));
        assertNotNull(AttributeParams.problem(map("attribute", "generic.armor", "operation", "add")));
        assertNotNull(AttributeParams.problem(map("amount", "2", "operation", "add")));
        assertNotNull(AttributeParams.problem(map("attribute", "generic.armor", "amount", "NaN", "operation", "add")));
        // Vanilla player attributes are a closed list; a typo must fail at validation, not at apply.
        String unknown = AttributeParams.problem(map("attribute", "generic.armour", "amount", "2", "operation", "add"));
        assertNotNull(unknown);
        assertTrue(unknown.contains("generic.armour"), unknown);
        assertNotNull(AttributeParams.problem(map("attribute", "generic.followRange", "amount", "2", "operation", "add")), "not on players");
        assertNull(AttributeParams.problem(map("attribute", "forge.swimSpeed", "amount", "2", "operation", "add")));
        assertNull(AttributeParams.problem(map("attribute", "mymod.stamina", "amount", "2", "operation", "add")), "addon namespaces are checked at apply");
    }
}
