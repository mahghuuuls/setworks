package com.mahghuuuls.setworks.core.definition;

import com.mahghuuuls.setworks.api.DefinitionSource;
import com.mahghuuuls.setworks.api.definition.BonusDefinition;
import com.mahghuuuls.setworks.api.definition.SetDefinition;
import com.mahghuuuls.setworks.api.definition.SetRequirement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The read-only result of the freeze: enabled definitions, their sources, and the disabled ones for listing. */
public final class FrozenDefinitions {

    public static final class Entry<T> {
        private final T definition;
        private final DefinitionSource source;
        private final boolean enabled;

        Entry(T definition, DefinitionSource source, boolean enabled) {
            this.definition = definition;
            this.source = source;
            this.enabled = enabled;
        }

        public T definition() {
            return definition;
        }

        public DefinitionSource source() {
            return source;
        }

        public boolean enabled() {
            return enabled;
        }
    }

    private final Map<String, Entry<SetDefinition>> sets;
    private final Map<String, Entry<BonusDefinition>> bonuses;
    private final Map<String, List<BonusDefinition>> bonusesBySet;
    private final Set<String> referencedLocations;

    FrozenDefinitions(Map<String, Entry<SetDefinition>> sets, Map<String, Entry<BonusDefinition>> bonuses) {
        this.sets = Collections.unmodifiableMap(new LinkedHashMap<String, Entry<SetDefinition>>(sets));
        this.bonuses = Collections.unmodifiableMap(new LinkedHashMap<String, Entry<BonusDefinition>>(bonuses));
        Map<String, List<BonusDefinition>> bySet = new LinkedHashMap<String, List<BonusDefinition>>();
        Set<String> locations = new LinkedHashSet<String>();
        for (Entry<SetDefinition> entry : this.sets.values()) {
            if (!entry.enabled()) {
                continue;
            }
            bySet.put(entry.definition().id(), new ArrayList<BonusDefinition>());
            for (int i = 0; i < entry.definition().pieces().size(); i++) {
                locations.addAll(entry.definition().pieces().get(i).locations());
            }
        }
        for (Entry<BonusDefinition> entry : this.bonuses.values()) {
            if (!entry.enabled()) {
                continue;
            }
            for (SetRequirement requirement : entry.definition().requirements()) {
                List<BonusDefinition> list = bySet.get(requirement.setId());
                if (list != null) {
                    list.add(entry.definition());
                }
            }
        }
        for (Map.Entry<String, List<BonusDefinition>> e : bySet.entrySet()) {
            e.setValue(Collections.unmodifiableList(e.getValue()));
        }
        this.bonusesBySet = Collections.unmodifiableMap(bySet);
        this.referencedLocations = Collections.unmodifiableSet(locations);
    }

    public static FrozenDefinitions empty() {
        return new FrozenDefinitions(new LinkedHashMap<String, Entry<SetDefinition>>(),
                new LinkedHashMap<String, Entry<BonusDefinition>>());
    }

    /** Every set, enabled or not, in registration order. */
    public Collection<Entry<SetDefinition>> allSets() {
        return sets.values();
    }

    public Collection<Entry<BonusDefinition>> allBonuses() {
        return bonuses.values();
    }

    public List<SetDefinition> enabledSets() {
        List<SetDefinition> result = new ArrayList<SetDefinition>();
        for (Entry<SetDefinition> entry : sets.values()) {
            if (entry.enabled()) {
                result.add(entry.definition());
            }
        }
        return result;
    }

    /** The enabled set with this id, or {@code null}. */
    public SetDefinition set(String id) {
        Entry<SetDefinition> entry = sets.get(id);
        return entry != null && entry.enabled() ? entry.definition() : null;
    }

    public BonusDefinition bonus(String id) {
        Entry<BonusDefinition> entry = bonuses.get(id);
        return entry != null && entry.enabled() ? entry.definition() : null;
    }

    public Entry<SetDefinition> setEntry(String id) {
        return sets.get(id);
    }

    public Entry<BonusDefinition> bonusEntry(String id) {
        return bonuses.get(id);
    }

    /** Enabled bonuses that require the set, in registration order. */
    public List<BonusDefinition> bonusesRequiring(String setId) {
        List<BonusDefinition> list = bonusesBySet.get(setId);
        return list == null ? Collections.<BonusDefinition>emptyList() : list;
    }

    /** Location ids any enabled set references. */
    public Set<String> referencedLocations() {
        return referencedLocations;
    }

    public int enabledSetCount() {
        return enabledSets().size();
    }

    public int enabledBonusCount() {
        int count = 0;
        for (Entry<BonusDefinition> entry : bonuses.values()) {
            if (entry.enabled()) {
                count++;
            }
        }
        return count;
    }
}
