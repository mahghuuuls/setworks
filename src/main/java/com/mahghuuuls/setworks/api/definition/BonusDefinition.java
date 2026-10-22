package com.mahghuuuls.setworks.api.definition;

import com.mahghuuuls.setworks.api.Visibility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** What a player gains when the set requirements and conditions are met. Immutable. */
public final class BonusDefinition {

    private final String id;
    private final List<SetRequirement> requirements;
    private final List<ConditionSpec> conditions;
    private final List<EffectSpec> effects;
    private final String description;
    private final Visibility visibility;
    private final List<String> displaySets;
    private final int conditionInterval;

    public BonusDefinition(String id, List<SetRequirement> requirements, List<ConditionSpec> conditions,
                           List<EffectSpec> effects, String description, Visibility visibility,
                           List<String> displaySets, int conditionInterval) {
        this.id = id;
        this.requirements = Collections.unmodifiableList(new ArrayList<SetRequirement>(requirements));
        this.conditions = Collections.unmodifiableList(new ArrayList<ConditionSpec>(conditions));
        this.effects = Collections.unmodifiableList(new ArrayList<EffectSpec>(effects));
        this.description = description;
        this.visibility = visibility;
        this.displaySets = displaySets == null ? null : Collections.unmodifiableList(new ArrayList<String>(displaySets));
        this.conditionInterval = conditionInterval;
    }

    public String id() {
        return id;
    }

    public List<SetRequirement> requirements() {
        return requirements;
    }

    public SetRequirement requirementFor(String setId) {
        for (SetRequirement requirement : requirements) {
            if (requirement.setId().equals(setId)) {
                return requirement;
            }
        }
        return null;
    }

    public boolean isMultiSet() {
        return requirements.size() > 1;
    }

    public List<ConditionSpec> conditions() {
        return conditions;
    }

    public List<EffectSpec> effects() {
        return effects;
    }

    /** A bonus with no effects is a scripted bonus. */
    public boolean isScripted() {
        return effects.isEmpty();
    }

    /** Author text or language key; {@code null} means "generate from the effects". */
    public String description() {
        return description;
    }

    public Visibility visibility() {
        return visibility;
    }

    /** The sets under which this bonus is displayed; {@code null} means every required set. */
    public List<String> displaySets() {
        return displaySets;
    }

    public boolean displayedUnder(String setId) {
        if (displaySets == null) {
            return requirementFor(setId) != null;
        }
        return displaySets.contains(setId);
    }

    /** Per-bonus condition interval in ticks; 0 means the global default. */
    public int conditionInterval() {
        return conditionInterval;
    }

    public BonusDefinition withPresentation(String newDescription, Visibility newVisibility, List<String> newDisplaySets) {
        return new BonusDefinition(id, requirements, conditions, effects,
                newDescription != null ? newDescription : description,
                newVisibility != null ? newVisibility : visibility,
                newDisplaySets != null ? newDisplaySets : displaySets,
                conditionInterval);
    }
}
