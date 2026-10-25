package com.mahghuuuls.setworks.api;

import java.util.Map;

/**
 * A named condition authors can write by id with parameters, registered with
 * {@link SetworksApi#registerConditionKind} during startup. Setworks checks the parameters once at
 * validation, creates one {@link ICondition} per use when evaluation starts, and reuses it for every
 * player. For example {@code setworks:health_below} with {@code fraction=0.5}.
 */
public interface IConditionKind {

    /** The kind id authors write, for example {@code setworks:sneaking} or {@code mymod:in_biome}. */
    String id();

    /**
     * Checks parameters once, at validation. Returns {@code null} when they are usable, or a
     * short problem text; a bonus with an unusable condition is disabled with that text in the report.
     */
    default String validate(Map<String, String> params) {
        return null;
    }

    /** Creates the condition for these parameters; called once per use after validation passed. */
    ICondition create(Map<String, String> params);

    /** One short line for operators, for example {@code health below 50%}. */
    String describe(Map<String, String> params);
}
