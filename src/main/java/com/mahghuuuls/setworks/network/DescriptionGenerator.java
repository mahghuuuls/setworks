package com.mahghuuuls.setworks.network;

import com.mahghuuuls.setworks.api.IEffectKind;
import com.mahghuuuls.setworks.api.definition.BonusDefinition;
import com.mahghuuuls.setworks.api.definition.EffectSpec;
import com.mahghuuuls.setworks.core.effect.AttributeEffectKind;
import com.mahghuuuls.setworks.core.effect.AttributeParams;
import com.mahghuuuls.setworks.core.effect.PotionEffectKind;
import com.mahghuuuls.setworks.core.effect.PotionSignature;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The player-facing description of a bonus the author left without one: one part per effect,
 * joined by ", ". Attribute modifiers read {@code +2 Armor} or {@code +10% Movement Speed};
 * potions read as the potion's translation key wrapped in {@code ${...}} plus a roman level
 * ({@code ${effect.moveSpeed} II}), which the client translates; an addon kind contributes its own
 * {@code describe} text. A scripted bonus has no effects and is rejected at validation without a
 * description, so this never runs for one.
 */
public final class DescriptionGenerator {

    /** Looks a potion's translation key up by registry id; {@code null} when unknown. The server reads the Forge registry. */
    public interface PotionNames {
        String translationKey(String potionId);
    }

    /** Finds an addon effect kind by id for its {@code describe}; {@code null} when unknown. */
    public interface Kinds {
        IEffectKind kind(String kindId);
    }

    private static final Map<String, String> ATTRIBUTE_NAMES = new HashMap<String, String>();
    private static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    static {
        ATTRIBUTE_NAMES.put("generic.maxHealth", "Max Health");
        ATTRIBUTE_NAMES.put("generic.knockbackResistance", "Knockback Resistance");
        ATTRIBUTE_NAMES.put("generic.movementSpeed", "Movement Speed");
        ATTRIBUTE_NAMES.put("generic.armor", "Armor");
        ATTRIBUTE_NAMES.put("generic.armorToughness", "Armor Toughness");
        ATTRIBUTE_NAMES.put("generic.attackDamage", "Attack Damage");
        ATTRIBUTE_NAMES.put("generic.attackSpeed", "Attack Speed");
        ATTRIBUTE_NAMES.put("generic.luck", "Luck");
        ATTRIBUTE_NAMES.put("generic.reachDistance", "Reach");
        ATTRIBUTE_NAMES.put("forge.swimSpeed", "Swim Speed");
    }

    private final PotionNames potionNames;
    private final Kinds kinds;

    public DescriptionGenerator(PotionNames potionNames, Kinds kinds) {
        this.potionNames = potionNames;
        this.kinds = kinds;
    }

    /** The author's description when given, otherwise the generated one. */
    public String describe(BonusDefinition bonus) {
        if (bonus.description() != null) {
            return bonus.description();
        }
        return generate(bonus.effects());
    }

    public String generate(List<EffectSpec> effects) {
        StringBuilder text = new StringBuilder();
        for (EffectSpec effect : effects) {
            String part = part(effect);
            if (part == null || part.isEmpty()) {
                continue;
            }
            if (text.length() > 0) {
                text.append(", ");
            }
            text.append(part);
        }
        return text.toString();
    }

    private String part(EffectSpec effect) {
        if (AttributeEffectKind.ID.equals(effect.kindId())) {
            return attributePart(effect.params());
        }
        if (PotionEffectKind.ID.equals(effect.kindId())) {
            return potionPart(effect.params());
        }
        IEffectKind kind = kinds == null ? null : kinds.kind(effect.kindId());
        if (kind == null) {
            return effect.kindId();
        }
        try {
            return kind.describe(effect.params());
        } catch (RuntimeException e) {
            return effect.kindId();
        }
    }

    static String attributePart(Map<String, String> params) {
        AttributeParams parsed;
        try {
            parsed = AttributeParams.parse(params);
        } catch (IllegalArgumentException e) {
            return null;
        }
        String name = ATTRIBUTE_NAMES.get(parsed.attribute());
        if (name == null) {
            name = parsed.attribute();
        }
        double amount = parsed.amount();
        String sign = amount < 0 ? "-" : "+";
        if (parsed.operation() == AttributeParams.ADD) {
            return sign + AttributeParams.formatAmount(Math.abs(amount)) + " " + name;
        }
        return sign + AttributeParams.formatAmount(Math.abs(amount) * 100) + "% " + name;
    }

    String potionPart(Map<String, String> params) {
        PotionSignature.Params parsed;
        try {
            parsed = PotionSignature.parse(params);
        } catch (IllegalArgumentException e) {
            return null;
        }
        String key = potionNames == null ? null : potionNames.translationKey(parsed.potionId());
        String name = key == null ? parsed.potionId() : "${" + key + "}";
        return name + " " + roman(parsed.level());
    }

    static String roman(int level) {
        if (level >= 1 && level <= ROMAN.length) {
            return ROMAN[level - 1];
        }
        return Integer.toString(level);
    }

    /** Attribute names this generator spells out; others keep their raw id. */
    public static List<String> knownAttributes() {
        return Collections.unmodifiableList(Arrays.asList(ATTRIBUTE_NAMES.keySet().toArray(new String[0])));
    }
}
