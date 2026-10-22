package com.mahghuuuls.setworks.api;

import com.mahghuuuls.setworks.api.definition.BonusDefinition;
import com.mahghuuuls.setworks.api.definition.ConditionSpec;
import com.mahghuuuls.setworks.api.definition.EffectSpec;
import com.mahghuuuls.setworks.api.definition.SetRequirement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds a bonus definition. */
public final class BonusBuilder {

    private final String id;
    private final List<SetRequirement> requirements = new ArrayList<SetRequirement>();
    private final List<ConditionSpec> conditions = new ArrayList<ConditionSpec>();
    private final List<EffectSpec> effects = new ArrayList<EffectSpec>();
    private String description;
    private Visibility visibility = Visibility.VISIBLE;
    private List<String> displaySets;
    private int conditionInterval;

    private BonusBuilder(String id) {
        this.id = SetBuilder.requireText(id, "bonus id");
    }

    public static BonusBuilder bonus(String id) {
        return new BonusBuilder(id);
    }

    /** Requires at least {@code threshold} pieces of the set. */
    public BonusBuilder requires(String setId, int threshold) {
        requirements.add(SetRequirement.atLeast(SetBuilder.requireText(setId, "set id"), threshold));
        return this;
    }

    /** Requires every piece of the set. */
    public BonusBuilder requiresAll(String setId) {
        requirements.add(SetRequirement.all(SetBuilder.requireText(setId, "set id")));
        return this;
    }

    /** Author text or a language key. Required for a bonus without effects. */
    public BonusBuilder description(String text) {
        this.description = SetBuilder.requireText(text, "description");
        return this;
    }

    public BonusBuilder visibility(Visibility value) {
        if (value == null) {
            throw new IllegalArgumentException("visibility must not be null");
        }
        this.visibility = value;
        return this;
    }

    /** Restricts the sets under which a multi-set bonus is displayed. Default: every required set. */
    public BonusBuilder displayUnder(String... setIds) {
        if (setIds == null || setIds.length == 0) {
            throw new IllegalArgumentException("displayUnder needs at least one set id; omit it to display under every required set");
        }
        List<String> ids = new ArrayList<String>();
        for (String setId : setIds) {
            ids.add(SetBuilder.requireText(setId, "set id"));
        }
        this.displaySets = ids;
        return this;
    }

    /** Per-bonus condition interval in ticks (1 or more). Default: the global setting. */
    public BonusBuilder conditionInterval(int ticks) {
        if (ticks < 1) {
            throw new IllegalArgumentException("condition interval must be 1 or more ticks");
        }
        this.conditionInterval = ticks;
        return this;
    }

    public BonusBuilder condition(String kindId, Map<String, String> params) {
        conditions.add(new ConditionSpec(SetBuilder.requireText(kindId, "condition kind"), params));
        return this;
    }

    /** A condition object supplied directly (Java or a script lambda); {@code label} names it in diagnostics. */
    public BonusBuilder condition(String label, Object conditionInstance) {
        if (conditionInstance == null) {
            throw new IllegalArgumentException("condition must not be null");
        }
        conditions.add(new ConditionSpec(SetBuilder.requireText(label, "condition label"),
                new LinkedHashMap<String, String>(), conditionInstance));
        return this;
    }

    public BonusBuilder effect(String kindId, Map<String, String> params) {
        effects.add(new EffectSpec(SetBuilder.requireText(kindId, "effect kind"), params));
        return this;
    }

    /** Built-in attribute modifier effect. Operation: {@code add}, {@code multiply_base}, or {@code multiply_total}. */
    public BonusBuilder attribute(String attributeName, double amount, String operation) {
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("attribute", SetBuilder.requireText(attributeName, "attribute"));
        params.put("amount", Double.toString(amount));
        params.put("operation", SetBuilder.requireText(operation, "operation"));
        return effect("setworks:attribute", params);
    }

    /** Built-in potion effect. Level is 1-based. */
    public BonusBuilder potion(String potionId, int level, boolean particles) {
        if (level < 1) {
            throw new IllegalArgumentException("potion level must be 1 or more");
        }
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("potion", SetBuilder.requireText(potionId, "potion"));
        params.put("level", Integer.toString(level));
        params.put("particles", Boolean.toString(particles));
        return effect("setworks:potion", params);
    }

    public BonusDefinition build() {
        return new BonusDefinition(id, requirements, conditions, effects, description, visibility, displaySets,
                conditionInterval);
    }
}
