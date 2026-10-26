package com.mahghuuuls.setworks.network;

import com.mahghuuuls.setworks.api.BonusBuilder;
import com.mahghuuuls.setworks.api.CombinationRule;
import com.mahghuuuls.setworks.api.IEffectKind;
import com.mahghuuuls.setworks.api.definition.BonusDefinition;
import net.minecraft.entity.player.EntityPlayerMP;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DescriptionGeneratorTest {

    private static final DescriptionGenerator GENERATOR = new DescriptionGenerator(new DescriptionGenerator.PotionNames() {
        public String translationKey(String potionId) {
            return "minecraft:speed".equals(potionId) ? "effect.moveSpeed" : null;
        }
    }, new DescriptionGenerator.Kinds() {
        public IEffectKind kind(String kindId) {
            if (!"mymod:stamina".equals(kindId)) {
                return null;
            }
            return new IEffectKind() {
                public String id() { return "mymod:stamina"; }
                public CombinationRule combinationRule() { return CombinationRule.SUM; }
                public String describe(Map<String, String> params) { return "+" + params.get("amount") + " Stamina"; }
                public void apply(EntityPlayerMP player, String instanceId, Map<String, String> params) { }
                public void withdraw(EntityPlayerMP player, String instanceId, Map<String, String> params) { }
            };
        }
    });

    private static BonusDefinition bonus(BonusBuilder builder) {
        return builder.build();
    }

    @Test
    void generatesFromBuiltInEffectsAndKeepsAuthorText() {
        assertEquals("+2 Armor", GENERATOR.describe(bonus(BonusBuilder.bonus("t:a").requires("t:s", 2).attribute("generic.armor", 2, "add"))));
        assertEquals("+10% Max Health", GENERATOR.describe(bonus(BonusBuilder.bonus("t:b").requires("t:s", 2).attribute("generic.maxHealth", 0.1, "multiply_total"))));
        assertEquals("-5% Movement Speed", GENERATOR.describe(bonus(BonusBuilder.bonus("t:c").requires("t:s", 2).attribute("generic.movementSpeed", -0.05, "multiply_base"))));
        assertEquals("+0.5 Knockback Resistance", GENERATOR.describe(bonus(BonusBuilder.bonus("t:d").requires("t:s", 2).attribute("generic.knockbackResistance", 0.5, "add"))));
        assertEquals("${effect.moveSpeed} II", GENERATOR.describe(bonus(BonusBuilder.bonus("t:e").requires("t:s", 2).potion("minecraft:speed", 2, false))));
        assertEquals("minecraft:luck I", GENERATOR.describe(bonus(BonusBuilder.bonus("t:f").requires("t:s", 2).potion("minecraft:luck", 1, false))), "an unknown potion keeps its id");
        assertEquals("+2 Armor, ${effect.moveSpeed} II", GENERATOR.describe(bonus(BonusBuilder.bonus("t:g").requires("t:s", 2).attribute("generic.armor", 2, "add").potion("minecraft:speed", 2, false))));
        assertEquals("+3 Stamina", GENERATOR.describe(bonus(BonusBuilder.bonus("t:h").requires("t:s", 2).effect("mymod:stamina", Collections.singletonMap("amount", "3")))));
        assertEquals("other:thing", GENERATOR.describe(bonus(BonusBuilder.bonus("t:i").requires("t:s", 2).effect("other:thing", Collections.<String, String>emptyMap()))), "an unknown kind keeps its id");
        assertEquals("+1 mymod.custom", GENERATOR.describe(bonus(BonusBuilder.bonus("t:j").requires("t:s", 2).attribute("mymod.custom", 1, "add"))), "an addon attribute keeps its name");
        assertEquals("Author text", GENERATOR.describe(bonus(BonusBuilder.bonus("t:k").requires("t:s", 2).attribute("generic.armor", 2, "add").description("Author text"))));
        assertEquals("X", DescriptionGenerator.roman(10));
        assertEquals("11", DescriptionGenerator.roman(11));
    }
}
