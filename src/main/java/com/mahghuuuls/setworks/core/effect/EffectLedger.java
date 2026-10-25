package com.mahghuuuls.setworks.core.effect;

import com.mahghuuuls.setworks.api.definition.BonusDefinition;
import com.mahghuuuls.setworks.api.definition.EffectSpec;
import com.mahghuuuls.setworks.core.definition.FrozenDefinitions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The record of which effect instances are applied to which player, and of the instances that
 * failed. Pure bookkeeping: it decides what must be applied or withdrawn and remembers the
 * outcome; calling the kinds is the manager's job. Instances are fixed at construction from the
 * frozen definitions, so every player uses the same ids.
 */
public final class EffectLedger {

    /** One effect of one bonus, with its stable id. */
    public static final class Instance {
        private final String id;
        private final String bonusId;
        private final int index;
        private final EffectSpec spec;

        Instance(String id, String bonusId, int index, EffectSpec spec) {
            this.id = id;
            this.bonusId = bonusId;
            this.index = index;
            this.spec = spec;
        }

        public String id() {
            return id;
        }

        public String bonusId() {
            return bonusId;
        }

        public int index() {
            return index;
        }

        public EffectSpec spec() {
            return spec;
        }

        public String kindId() {
            return spec.kindId();
        }

        @Override
        public String toString() {
            return id;
        }
    }

    private final Map<String, List<Instance>> byBonus = new LinkedHashMap<String, List<Instance>>();
    private final List<Instance> all = new ArrayList<Instance>();
    private final Map<UUID, Map<String, Instance>> applied = new HashMap<UUID, Map<String, Instance>>();
    private final Map<UUID, Map<String, String>> failed = new HashMap<UUID, Map<String, String>>();

    public EffectLedger(FrozenDefinitions definitions) {
        for (FrozenDefinitions.Entry<BonusDefinition> entry : definitions.allBonuses()) {
            if (!entry.enabled()) {
                continue;
            }
            BonusDefinition bonus = entry.definition();
            List<Instance> instances = new ArrayList<Instance>();
            String setId = bonus.requirements().get(0).setId();
            int index = 0;
            for (EffectSpec spec : bonus.effects()) {
                Instance instance = new Instance(EffectIds.instanceId(setId, bonus.id(), index), bonus.id(), index, spec);
                instances.add(instance);
                all.add(instance);
                index++;
            }
            byBonus.put(bonus.id(), Collections.unmodifiableList(instances));
        }
    }

    /** The instances of a bonus, in definition order; empty for a scripted or unknown bonus. */
    public List<Instance> instancesOf(String bonusId) {
        List<Instance> instances = byBonus.get(bonusId);
        return instances == null ? Collections.<Instance>emptyList() : instances;
    }

    /** Every instance of every enabled bonus. */
    public List<Instance> allInstances() {
        return Collections.unmodifiableList(all);
    }

    /** Marks the bonus's instances applied for the player and returns the ones that were not applied before. */
    public List<Instance> activate(UUID player, String bonusId) {
        Map<String, Instance> records = applied.get(player);
        if (records == null) {
            records = new LinkedHashMap<String, Instance>();
            applied.put(player, records);
        }
        List<Instance> fresh = new ArrayList<Instance>();
        for (Instance instance : instancesOf(bonusId)) {
            if (records.put(instance.id(), instance) == null) {
                fresh.add(instance);
                clearFailure(player, instance.id());
            }
        }
        return fresh;
    }

    /** Marks the bonus's instances withdrawn for the player and returns the ones that were applied. */
    public List<Instance> deactivate(UUID player, String bonusId) {
        Map<String, Instance> records = applied.get(player);
        List<Instance> gone = new ArrayList<Instance>();
        for (Instance instance : instancesOf(bonusId)) {
            if (records != null && records.remove(instance.id()) != null) {
                gone.add(instance);
            }
            clearFailure(player, instance.id());
        }
        return gone;
    }

    public boolean isApplied(UUID player, String instanceId) {
        Map<String, Instance> records = applied.get(player);
        return records != null && records.containsKey(instanceId);
    }

    /** The player's applied instances, in application order. */
    public Collection<Instance> applied(UUID player) {
        Map<String, Instance> records = applied.get(player);
        return records == null ? Collections.<Instance>emptyList() : Collections.unmodifiableCollection(records.values());
    }

    /** Records that applying the instance failed; it is no longer counted as applied. */
    public void markFailed(UUID player, Instance instance, String message) {
        Map<String, Instance> records = applied.get(player);
        if (records != null) {
            records.remove(instance.id());
        }
        Map<String, String> failures = failed.get(player);
        if (failures == null) {
            failures = new LinkedHashMap<String, String>();
            failed.put(player, failures);
        }
        failures.put(instance.id(), message == null ? "unknown error" : message);
    }

    /** The recorded failure of an instance for the player, or {@code null}. */
    public String failure(UUID player, String instanceId) {
        Map<String, String> failures = failed.get(player);
        return failures == null ? null : failures.get(instanceId);
    }

    /** Forgets everything about the player and returns the instances that were applied. */
    public List<Instance> clear(UUID player) {
        Map<String, Instance> records = applied.remove(player);
        failed.remove(player);
        return records == null ? Collections.<Instance>emptyList() : new ArrayList<Instance>(records.values());
    }

    public int playerCount() {
        return applied.size();
    }

    private void clearFailure(UUID player, String instanceId) {
        Map<String, String> failures = failed.get(player);
        if (failures != null) {
            failures.remove(instanceId);
        }
    }
}
