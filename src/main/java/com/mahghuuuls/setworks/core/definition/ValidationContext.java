package com.mahghuuuls.setworks.core.definition;

/**
 * What validation needs from the game. Abstracted so the registry is tested without registries.
 */
public interface ValidationContext {

    boolean itemExists(String itemId);

    boolean oreHasEntries(String oreName);

    boolean locationExists(String locationId);

    boolean effectKindExists(String kindId);

    /** The kind's own problem with the effect's parameters, or {@code null} when they are usable or the kind is unknown. */
    String effectProblem(com.mahghuuuls.setworks.api.definition.EffectSpec effect);

    boolean conditionKindExists(String kindId);

    /** The kind's own problem with the condition's parameters, or {@code null} when they are usable, the kind is unknown, or the condition is direct. */
    String conditionProblem(com.mahghuuuls.setworks.api.definition.ConditionSpec condition);

    /** True when the exact-NBT text parses as a compound. */
    boolean exactNbtParses(String text);
}
