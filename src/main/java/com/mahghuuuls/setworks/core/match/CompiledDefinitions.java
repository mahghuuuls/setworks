package com.mahghuuuls.setworks.core.match;

import com.mahghuuuls.setworks.api.definition.BonusDefinition;
import com.mahghuuuls.setworks.api.definition.SetDefinition;
import com.mahghuuuls.setworks.api.definition.SetRequirement;
import com.mahghuuuls.setworks.core.definition.FrozenDefinitions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The frozen definitions with every filter compiled, plus the indexes evaluation needs. Built once at freeze. */
public final class CompiledDefinitions {

    private final Map<String, CompiledSet> sets;
    private final Map<String, List<BonusDefinition>> bonusesBySet;
    private final Map<String, List<String>> setsByLocation;
    private final Set<String> referencedLocations;
    private final List<BonusDefinition> bonuses;
    private final Map<String, BonusDefinition> bonusesById;

    public CompiledDefinitions(FrozenDefinitions frozen) {
        Map<String, CompiledSet> compiled = new LinkedHashMap<String, CompiledSet>();
        Map<String, List<String>> byLocation = new LinkedHashMap<String, List<String>>();
        for (SetDefinition set : frozen.enabledSets()) {
            CompiledSet compiledSet = new CompiledSet(set);
            compiled.put(set.id(), compiledSet);
            for (String location : compiledSet.locations()) {
                List<String> list = byLocation.get(location);
                if (list == null) {
                    list = new ArrayList<String>();
                    byLocation.put(location, list);
                }
                list.add(set.id());
            }
        }
        Map<String, List<BonusDefinition>> bySet = new LinkedHashMap<String, List<BonusDefinition>>();
        List<BonusDefinition> all = new ArrayList<BonusDefinition>();
        for (FrozenDefinitions.Entry<BonusDefinition> entry : frozen.allBonuses()) {
            if (!entry.enabled()) {
                continue;
            }
            all.add(entry.definition());
            for (SetRequirement requirement : entry.definition().requirements()) {
                List<BonusDefinition> list = bySet.get(requirement.setId());
                if (list == null) {
                    list = new ArrayList<BonusDefinition>();
                    bySet.put(requirement.setId(), list);
                }
                list.add(entry.definition());
            }
        }
        this.sets = Collections.unmodifiableMap(compiled);
        this.bonusesBySet = Collections.unmodifiableMap(bySet);
        this.setsByLocation = Collections.unmodifiableMap(byLocation);
        this.referencedLocations = Collections.unmodifiableSet(new LinkedHashSet<String>(byLocation.keySet()));
        this.bonuses = Collections.unmodifiableList(all);
        Map<String, BonusDefinition> byId = new LinkedHashMap<String, BonusDefinition>();
        for (BonusDefinition bonus : all) {
            byId.put(bonus.id(), bonus);
        }
        this.bonusesById = Collections.unmodifiableMap(byId);
    }

    public static CompiledDefinitions empty() {
        return new CompiledDefinitions(FrozenDefinitions.empty());
    }

    public Collection<CompiledSet> sets() {
        return sets.values();
    }

    public CompiledSet set(String id) {
        return sets.get(id);
    }

    public List<BonusDefinition> bonuses() {
        return bonuses;
    }

    public BonusDefinition bonus(String id) {
        return bonusesById.get(id);
    }

    /** Enabled bonuses requiring the set, registration order. */
    public List<BonusDefinition> bonusesRequiring(String setId) {
        List<BonusDefinition> list = bonusesBySet.get(setId);
        return list == null ? Collections.<BonusDefinition>emptyList() : list;
    }

    /** Set ids that reference the location. */
    public List<String> setsReferencing(String locationId) {
        List<String> list = setsByLocation.get(locationId);
        return list == null ? Collections.<String>emptyList() : list;
    }

    public Set<String> referencedLocations() {
        return referencedLocations;
    }

    /** Ids of every set some piece of which accepts the item somewhere. */
    public List<String> setsMentioning(ItemView item) {
        List<String> result = new ArrayList<String>();
        for (CompiledSet set : sets.values()) {
            if (set.mentions(item)) {
                result.add(set.id());
            }
        }
        return result;
    }
}
