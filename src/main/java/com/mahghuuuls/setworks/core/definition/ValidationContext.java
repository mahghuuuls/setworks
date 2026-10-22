package com.mahghuuuls.setworks.core.definition;

/**
 * What validation needs from the game. Abstracted so the registry is tested without registries.
 */
public interface ValidationContext {

    boolean itemExists(String itemId);

    boolean oreHasEntries(String oreName);

    boolean locationExists(String locationId);

    boolean effectKindExists(String kindId);

    boolean conditionKindExists(String kindId);

    /** True when the exact-NBT text parses as a compound. */
    boolean exactNbtParses(String text);
}
