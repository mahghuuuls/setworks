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
        assertTrue(PotionSignature.shouldRemove(speedOne, 0, 330), "fresh own effect");
        assertTrue(PotionSignature.shouldRemove(speedOne, 0, 300), "own effect part way down");
        assertTrue(PotionSignature.shouldRemove(speedOne, 0, 1), "nearly expired own effect");
        assertFalse(PotionSignature.shouldRemove(speedOne, 0, 331), "a longer effect belongs to someone else");
        assertFalse(PotionSignature.shouldRemove(speedOne, 0, 9600), "an eight-minute drunk potion stays");
        assertFalse(PotionSignature.shouldRemove(speedOne, 1, 300), "a stronger effect stays");
        assertFalse(PotionSignature.shouldRemove(1, 0, 300), "a weaker effect is not ours either");
        assertEquals(330, PotionSignature.DURATION_TICKS);
        assertEquals(20, PotionSignature.REFRESH_INTERVAL_TICKS);
        assertEquals(230, PotionSignature.TOP_UP_BELOW_TICKS);
        assertTrue(PotionSignature.TOP_UP_BELOW_TICKS > 200, "no HUD blink before a top-up");
    }

    @Test
    void topUpWaitsForTheFloorSoPeriodicPotionsPulseAtTheVanillaRate() {
        int one = 0, two = 1;
        assertTrue(PotionSignature.needsTopUp(false, 0, 0, one), "absent effect (milk)");
        assertFalse(PotionSignature.needsTopUp(true, one, 330, one));
        assertFalse(PotionSignature.needsTopUp(true, one, 231, one));
        assertTrue(PotionSignature.needsTopUp(true, one, 230, one));
        assertTrue(PotionSignature.needsTopUp(true, one, 1, one));
        // Levels: a level-2 instance under a healthy level-1 effect must raise it (a lower instance restored it first after milk).
        assertTrue(PotionSignature.needsTopUp(true, one, 330, two), "a lower level present is raised");
        assertFalse(PotionSignature.needsTopUp(true, two, 330, one), "a higher level present is left alone");
        assertTrue(PotionSignature.needsTopUp(true, two, 230, one), "but at the floor even a lower instance tops up (vanilla ignores the weaker add)");
        // The countdown from the top-up value down to the floor is one uninterrupted window; vanilla pulses
        // Regeneration when the duration is a multiple of 50 and Poison at 25, checked before counting down.
        int window = PotionSignature.DURATION_TICKS - PotionSignature.TOP_UP_BELOW_TICKS;
        assertEquals(100, window);
        int regeneration = 0, poison = 0;
        for (int duration = PotionSignature.DURATION_TICKS; duration > PotionSignature.TOP_UP_BELOW_TICKS; duration--) {
            if (duration % 50 == 0) regeneration++;
            if (duration % 25 == 0) poison++;
        }
        assertEquals(window / 50, regeneration, "two Regeneration pulses per 100 ticks, as vanilla");
        assertEquals(window / 25, poison, "four Poison pulses per 100 ticks, as vanilla");
        assertFalse(PotionSignature.DURATION_TICKS % 50 == 0 || PotionSignature.DURATION_TICKS % 25 == 0 || PotionSignature.DURATION_TICKS % 40 == 0,
                "a top-up itself must not pulse");
        // The old model reset to 300 every 20 ticks: 300 is a multiple of 50, so every reset pulsed.
        assertEquals(0, 300 % 50);
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
        assertEquals("0.11", AttributeParams.formatAmount(0.11000000000000001));
        assertEquals("17", AttributeParams.formatAmount(17.0));
        assertEquals("0.1", AttributeParams.formatAmount(0.1));
        assertEquals("20", AttributeParams.formatAmount(20.000000000000004), "no dangling point");
        assertEquals("0", AttributeParams.formatAmount(0.00001));
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
