package com.mahghuuuls.setworks.core.definition;

import com.mahghuuuls.setworks.api.DefinitionSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Everything the freeze found wrong, plus the script-loading failures reported to Setworks. Printed once at startup and by the validate command. */
public final class ValidationReport {

    public static final class Error {
        private final String definitionId;
        private final String rule;
        private final String message;
        private final DefinitionSource source;

        public Error(String definitionId, String rule, String message, DefinitionSource source) {
            this.definitionId = definitionId;
            this.rule = rule;
            this.message = message;
            this.source = source;
        }

        public String definitionId() {
            return definitionId;
        }

        public String rule() {
            return rule;
        }

        public String message() {
            return message;
        }

        public DefinitionSource source() {
            return source;
        }

        public String describe() {
            return definitionId + " [" + rule + "] " + message + " (source " + (source == null ? "unknown" : source.describe()) + ")";
        }
    }

    private final List<Error> errors = new ArrayList<Error>();
    private final Set<String> disabledSets = new LinkedHashSet<String>();
    private final Set<String> disabledBonuses = new LinkedHashSet<String>();
    private final List<String> scriptFailures = new ArrayList<String>();

    void add(String definitionId, String rule, String message, DefinitionSource source) {
        errors.add(new Error(definitionId, rule, message, source));
    }

    void disableSet(String setId) {
        disabledSets.add(setId);
    }

    void disableBonus(String bonusId) {
        disabledBonuses.add(bonusId);
    }

    /** A script engine failure at the scope the engine applies (a file, or the rest of a loader). */
    public void addScriptFailure(String description) {
        scriptFailures.add(description);
    }

    public List<Error> errors() {
        return Collections.unmodifiableList(errors);
    }

    public Set<String> disabledSets() {
        return Collections.unmodifiableSet(disabledSets);
    }

    public Set<String> disabledBonuses() {
        return Collections.unmodifiableSet(disabledBonuses);
    }

    public List<String> scriptFailures() {
        return Collections.unmodifiableList(scriptFailures);
    }

    public boolean hasProblems() {
        return !errors.isEmpty() || !scriptFailures.isEmpty();
    }

    /** One line per error and failure, then the disabled sets, then the restart note. */
    public List<String> lines() {
        List<String> lines = new ArrayList<String>();
        if (!hasProblems()) {
            lines.add("no errors");
        }
        for (Error error : errors) {
            lines.add("error: " + error.describe());
        }
        for (String failure : scriptFailures) {
            lines.add("script failure: " + failure);
        }
        if (!disabledSets.isEmpty()) {
            lines.add("disabled sets: " + String.join(", ", disabledSets));
        }
        if (!disabledBonuses.isEmpty()) {
            lines.add("disabled bonuses: " + String.join(", ", disabledBonuses));
        }
        lines.add("restart required to apply changes");
        return lines;
    }
}
