package com.mahghuuuls.setworks.core.definition;

import com.mahghuuuls.setworks.api.BonusModification;
import com.mahghuuuls.setworks.api.DefinitionSource;
import com.mahghuuuls.setworks.api.ItemFilter;
import com.mahghuuuls.setworks.api.RegistrationResult;
import com.mahghuuuls.setworks.api.definition.BonusDefinition;
import com.mahghuuuls.setworks.api.definition.ConditionSpec;
import com.mahghuuuls.setworks.api.definition.EffectSpec;
import com.mahghuuuls.setworks.api.definition.PieceDefinition;
import com.mahghuuuls.setworks.api.definition.SetDefinition;
import com.mahghuuuls.setworks.api.definition.SetRequirement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Collects definitions during startup and turns them into {@link FrozenDefinitions} once.
 *
 * Owns the ordering rules: registrations are applied in call order, then modifications,
 * replacements, and disables in call order; a replacement without the replace marker is an error
 * and the earlier definition stays. Owns validation: a set with any invalid piece, or referenced by
 * a bonus that is itself invalid, is disabled as a whole with its bonuses; valid sets continue.
 * Contains no Minecraft code; the game is reached through {@link ValidationContext}.
 */
public final class DefinitionRegistry {

    public static final int MAX_PIECES = 32;

    private static final class SetRegistration {
        final SetDefinition set;
        final DefinitionSource source;
        final boolean replace;

        SetRegistration(SetDefinition set, DefinitionSource source, boolean replace) {
            this.set = set;
            this.source = source;
            this.replace = replace;
        }
    }

    private static final class BonusRegistration {
        final BonusDefinition bonus;
        final DefinitionSource source;
        final boolean replace;

        BonusRegistration(BonusDefinition bonus, DefinitionSource source, boolean replace) {
            this.bonus = bonus;
            this.source = source;
            this.replace = replace;
        }
    }

    private abstract static class Change {
        final DefinitionSource source;

        Change(DefinitionSource source) {
            this.source = source;
        }

        abstract void apply(DefinitionRegistry registry, Map<String, SetRegistration> sets,
                            Map<String, BonusRegistration> bonuses, ValidationReport report);
    }

    private final List<SetRegistration> setRegistrations = new ArrayList<SetRegistration>();
    private final List<BonusRegistration> bonusRegistrations = new ArrayList<BonusRegistration>();
    private final List<Change> changes = new ArrayList<Change>();
    private final List<String> rejected = new ArrayList<String>();
    private final List<String> scriptFailures = new ArrayList<String>();
    private volatile boolean frozen;
    private FrozenDefinitions frozenDefinitions = FrozenDefinitions.empty();
    private ValidationReport report = new ValidationReport();

    // ---- registration (before freeze) ----

    public RegistrationResult registerSet(SetDefinition set, DefinitionSource source, boolean replace) {
        RegistrationResult open = requireOpen("register set " + (set == null ? "null" : set.id()));
        if (open != null) {
            return open;
        }
        if (set == null) {
            return reject("set is null");
        }
        if (!IdRules.isValid(set.id())) {
            return reject("set id \"" + set.id() + "\" is not valid: " + IdRules.RULE_TEXT);
        }
        setRegistrations.add(new SetRegistration(set, source, replace));
        return RegistrationResult.ok();
    }

    public RegistrationResult registerBonus(BonusDefinition bonus, DefinitionSource source, boolean replace) {
        RegistrationResult open = requireOpen("register bonus " + (bonus == null ? "null" : bonus.id()));
        if (open != null) {
            return open;
        }
        if (bonus == null) {
            return reject("bonus is null");
        }
        if (!IdRules.isValid(bonus.id())) {
            return reject("bonus id \"" + bonus.id() + "\" is not valid: " + IdRules.RULE_TEXT);
        }
        bonusRegistrations.add(new BonusRegistration(bonus, source, replace));
        return RegistrationResult.ok();
    }

    public RegistrationResult disableSet(final String setId, DefinitionSource source) {
        RegistrationResult open = requireOpen("disable set " + setId);
        if (open != null) {
            return open;
        }
        changes.add(new Change(source) {
            @Override
            void apply(DefinitionRegistry registry, Map<String, SetRegistration> sets,
                       Map<String, BonusRegistration> bonuses, ValidationReport report) {
                if (!sets.containsKey(setId)) {
                    report.add(setId, "unknown-set", "disable names a set that does not exist", this.source);
                    return;
                }
                registry.explicitlyDisabledSets.add(setId);
            }
        });
        return RegistrationResult.ok();
    }

    public RegistrationResult disableBonus(final String bonusId, DefinitionSource source) {
        RegistrationResult open = requireOpen("disable bonus " + bonusId);
        if (open != null) {
            return open;
        }
        changes.add(new Change(source) {
            @Override
            void apply(DefinitionRegistry registry, Map<String, SetRegistration> sets,
                       Map<String, BonusRegistration> bonuses, ValidationReport report) {
                if (!bonuses.containsKey(bonusId)) {
                    report.add(bonusId, "unknown-bonus", "disable names a bonus that does not exist", this.source);
                    return;
                }
                registry.explicitlyDisabledBonuses.add(bonusId);
            }
        });
        return RegistrationResult.ok();
    }

    public RegistrationResult modifyBonus(final String bonusId, final BonusModification modification, DefinitionSource source) {
        RegistrationResult open = requireOpen("modify bonus " + bonusId);
        if (open != null) {
            return open;
        }
        if (modification == null || modification.isEmpty()) {
            return reject("modification of " + bonusId + " changes nothing");
        }
        changes.add(new Change(source) {
            @Override
            void apply(DefinitionRegistry registry, Map<String, SetRegistration> sets,
                       Map<String, BonusRegistration> bonuses, ValidationReport report) {
                BonusRegistration existing = bonuses.get(bonusId);
                if (existing == null) {
                    report.add(bonusId, "unknown-bonus", "modify names a bonus that does not exist", this.source);
                    return;
                }
                BonusDefinition changed = existing.bonus.withPresentation(modification.description(),
                        modification.visibility(), modification.displaySets());
                bonuses.put(bonusId, new BonusRegistration(changed, existing.source, existing.replace));
            }
        });
        return RegistrationResult.ok();
    }

    public RegistrationResult addAlternatives(final String setId, final String pieceName, final List<ItemFilter> filters,
                                              DefinitionSource source) {
        RegistrationResult open = requireOpen("add alternatives to " + setId + "/" + pieceName);
        if (open != null) {
            return open;
        }
        if (filters == null || filters.isEmpty()) {
            return reject("no alternatives given for " + setId + "/" + pieceName);
        }
        final List<ItemFilter> copy = new ArrayList<ItemFilter>(filters);
        changes.add(new Change(source) {
            @Override
            void apply(DefinitionRegistry registry, Map<String, SetRegistration> sets,
                       Map<String, BonusRegistration> bonuses, ValidationReport report) {
                SetRegistration existing = sets.get(setId);
                if (existing == null) {
                    report.add(setId, "unknown-set", "add alternatives names a set that does not exist", this.source);
                    return;
                }
                PieceDefinition piece = existing.set.piece(pieceName);
                if (piece == null) {
                    report.add(setId, "unknown-piece", "set has no piece named " + pieceName, this.source);
                    return;
                }
                List<PieceDefinition> pieces = new ArrayList<PieceDefinition>();
                for (PieceDefinition p : existing.set.pieces()) {
                    pieces.add(p.name().equals(pieceName) ? p.withAlternatives(copy) : p);
                }
                sets.put(setId, new SetRegistration(existing.set.withPieces(pieces), existing.source, existing.replace));
            }
        });
        return RegistrationResult.ok();
    }

    /**
     * A script engine failure to be printed with the report. Accepted before and after freeze: it is the
     * one thing that may be added to a frozen report, because script loaders can fail after the freeze.
     */
    public void reportScriptFailure(String description) {
        if (frozen) {
            report.addScriptFailure(description);
        } else {
            scriptFailures.add(description);
        }
    }

    public boolean isFrozen() {
        return frozen;
    }

    // ---- freeze ----

    private final Set<String> explicitlyDisabledSets = new HashSet<String>();
    private final Set<String> explicitlyDisabledBonuses = new HashSet<String>();

    /**
     * Applies registrations, then changes, validates everything, and makes the registry read-only.
     * Returns the report; the definitions are available through {@link #definitions()}.
     */
    public ValidationReport freeze(ValidationContext context) {
        if (frozen) {
            return report;
        }
        ValidationReport result = new ValidationReport();
        for (String failure : scriptFailures) {
            result.addScriptFailure(failure);
        }
        for (String message : rejected) {
            result.add("-", "rejected-call", message, null);
        }
        Map<String, SetRegistration> sets = new LinkedHashMap<String, SetRegistration>();
        Map<String, BonusRegistration> bonuses = new LinkedHashMap<String, BonusRegistration>();
        // Plain registrations first, then replacements, so a replacer that loaded before the original still
        // wins and the original is never blamed for a duplicate.
        for (SetRegistration registration : setRegistrations) {
            if (!registration.replace) {
                applyRegistration(sets, registration.set.id(), registration, false, registration.source, result, "set");
            }
        }
        for (SetRegistration registration : setRegistrations) {
            if (registration.replace) {
                applyRegistration(sets, registration.set.id(), registration, true, registration.source, result, "set");
            }
        }
        for (BonusRegistration registration : bonusRegistrations) {
            if (!registration.replace) {
                applyRegistration(bonuses, registration.bonus.id(), registration, false, registration.source, result, "bonus");
            }
        }
        for (BonusRegistration registration : bonusRegistrations) {
            if (registration.replace) {
                applyRegistration(bonuses, registration.bonus.id(), registration, true, registration.source, result, "bonus");
            }
        }
        for (Change change : changes) {
            change.apply(this, sets, bonuses, result);
        }
        Map<String, Boolean> setEnabled = validateSets(sets, context, result);
        Map<String, Boolean> bonusEnabled = validateBonuses(bonuses, sets, setEnabled, context, result);
        // A set referenced by an invalid bonus is disabled as a whole, with its other bonuses.
        for (Map.Entry<String, BonusRegistration> entry : bonuses.entrySet()) {
            if (bonusEnabled.get(entry.getKey())) {
                continue;
            }
            if (explicitlyDisabledBonuses.contains(entry.getKey())) {
                continue;
            }
            for (SetRequirement requirement : entry.getValue().bonus.requirements()) {
                if (sets.containsKey(requirement.setId()) && setEnabled.get(requirement.setId())
                        && !explicitlyDisabledSets.contains(requirement.setId())) {
                    setEnabled.put(requirement.setId(), false);
                    result.add(requirement.setId(), "invalid-bonus", "disabled because its bonus " + entry.getKey()
                            + " is invalid", sets.get(requirement.setId()).source);
                }
            }
        }
        for (Map.Entry<String, BonusRegistration> entry : bonuses.entrySet()) {
            for (SetRequirement requirement : entry.getValue().bonus.requirements()) {
                Boolean enabled = setEnabled.get(requirement.setId());
                if (enabled != null && !enabled && bonusEnabled.get(entry.getKey())) {
                    bonusEnabled.put(entry.getKey(), false);
                    result.add(entry.getKey(), "requires-disabled-set", "disabled because set " + requirement.setId()
                            + " is disabled", entry.getValue().source);
                }
            }
        }
        Map<String, FrozenDefinitions.Entry<SetDefinition>> frozenSets = new LinkedHashMap<String, FrozenDefinitions.Entry<SetDefinition>>();
        for (Map.Entry<String, SetRegistration> entry : sets.entrySet()) {
            boolean enabled = setEnabled.get(entry.getKey());
            if (!enabled) {
                result.disableSet(entry.getKey());
            }
            frozenSets.put(entry.getKey(), new FrozenDefinitions.Entry<SetDefinition>(entry.getValue().set, entry.getValue().source, enabled));
        }
        Map<String, FrozenDefinitions.Entry<BonusDefinition>> frozenBonuses = new LinkedHashMap<String, FrozenDefinitions.Entry<BonusDefinition>>();
        for (Map.Entry<String, BonusRegistration> entry : bonuses.entrySet()) {
            boolean enabled = bonusEnabled.get(entry.getKey());
            if (!enabled) {
                result.disableBonus(entry.getKey());
            }
            frozenBonuses.put(entry.getKey(), new FrozenDefinitions.Entry<BonusDefinition>(entry.getValue().bonus, entry.getValue().source, enabled));
        }
        frozenDefinitions = new FrozenDefinitions(frozenSets, frozenBonuses);
        report = result;
        frozen = true;
        return result;
    }

    public FrozenDefinitions definitions() {
        return frozenDefinitions;
    }

    public ValidationReport report() {
        return report;
    }

    // ---- internals ----

    private static <T> void applyRegistration(Map<String, T> target, String id, T registration, boolean replace,
                                              DefinitionSource source, ValidationReport report, String kind) {
        if (!IdRules.isValid(id)) {
            report.add(id, "invalid-id", kind + " id is not valid: " + IdRules.RULE_TEXT, source);
            return;
        }
        T existing = target.get(id);
        if (existing != null && !replace) {
            DefinitionSource existingSource = sourceOf(existing);
            report.add(id, "duplicate-id", kind + " id is already registered by "
                    + (existingSource == null ? "unknown" : existingSource.describe())
                    + "; use replace to override it on purpose", source);
            return;
        }
        target.put(id, registration);
    }

    private static DefinitionSource sourceOf(Object registration) {
        if (registration instanceof SetRegistration) {
            return ((SetRegistration) registration).source;
        }
        if (registration instanceof BonusRegistration) {
            return ((BonusRegistration) registration).source;
        }
        return null;
    }

    private Map<String, Boolean> validateSets(Map<String, SetRegistration> sets, ValidationContext context, ValidationReport report) {
        Map<String, Boolean> enabled = new LinkedHashMap<String, Boolean>();
        for (Map.Entry<String, SetRegistration> entry : sets.entrySet()) {
            String id = entry.getKey();
            SetDefinition set = entry.getValue().set;
            DefinitionSource source = entry.getValue().source;
            boolean ok = true;
            if (explicitlyDisabledSets.contains(id)) {
                ok = false;
            }
            if (set.pieces().isEmpty()) {
                report.add(id, "no-pieces", "set has no pieces", source);
                ok = false;
            }
            if (set.pieces().size() > MAX_PIECES) {
                report.add(id, "too-many-pieces", "set has more than " + MAX_PIECES + " pieces", source);
                ok = false;
            }
            Set<String> names = new HashSet<String>();
            for (PieceDefinition piece : set.pieces()) {
                if (!names.add(piece.name())) {
                    report.add(id, "duplicate-piece", "piece name " + piece.name() + " is used twice", source);
                    ok = false;
                }
                if (piece.alternatives().isEmpty()) {
                    report.add(id, "no-alternatives", "piece " + piece.name() + " accepts no items", source);
                    ok = false;
                }
                if (piece.locations().isEmpty()) {
                    report.add(id, "no-locations", "piece " + piece.name() + " has no location", source);
                    ok = false;
                }
                for (ItemFilter filter : piece.alternatives()) {
                    if (filter.isOre()) {
                        if (!context.oreHasEntries(filter.oreName())) {
                            report.add(id, "unknown-ore", "piece " + piece.name() + ": ore dictionary name "
                                    + filter.oreName() + " has no entries", source);
                            ok = false;
                        }
                    } else if (!context.itemExists(filter.itemId())) {
                        report.add(id, "unknown-item", "piece " + piece.name() + ": item " + filter.itemId()
                                + " is not registered", source);
                        ok = false;
                    }
                    if (filter.exactNbt() != null && !context.exactNbtParses(filter.exactNbt())) {
                        report.add(id, "invalid-exact-nbt", "piece " + piece.name() + ": exact NBT text does not parse: "
                                + filter.exactNbt(), source);
                        ok = false;
                    }
                }
                for (String location : piece.locations()) {
                    if (!context.locationExists(location)) {
                        report.add(id, "unknown-location", "piece " + piece.name() + ": location " + location
                                + " is not registered (is the addon that provides it installed?)", source);
                        ok = false;
                    }
                }
            }
            enabled.put(id, ok);
        }
        return enabled;
    }

    private Map<String, Boolean> validateBonuses(Map<String, BonusRegistration> bonuses, Map<String, SetRegistration> sets,
                                                 Map<String, Boolean> setEnabled, ValidationContext context,
                                                 ValidationReport report) {
        Map<String, Boolean> enabled = new LinkedHashMap<String, Boolean>();
        for (Map.Entry<String, BonusRegistration> entry : bonuses.entrySet()) {
            String id = entry.getKey();
            BonusDefinition bonus = entry.getValue().bonus;
            DefinitionSource source = entry.getValue().source;
            boolean ok = true;
            if (explicitlyDisabledBonuses.contains(id)) {
                ok = false;
            }
            if (bonus.requirements().isEmpty()) {
                report.add(id, "no-requirements", "bonus requires no set", source);
                ok = false;
            }
            Set<String> seen = new HashSet<String>();
            for (SetRequirement requirement : bonus.requirements()) {
                if (!seen.add(requirement.setId())) {
                    report.add(id, "duplicate-requirement", "set " + requirement.setId() + " is required twice", source);
                    ok = false;
                }
                SetRegistration set = sets.get(requirement.setId());
                if (set == null) {
                    report.add(id, "unknown-set", "requires set " + requirement.setId() + " which does not exist", source);
                    ok = false;
                } else if (!requirement.isAllPieces() && requirement.threshold() > set.set.pieces().size()) {
                    report.add(id, "threshold-too-high", "requires " + requirement.threshold() + " pieces of "
                            + requirement.setId() + " which has " + set.set.pieces().size(), source);
                    ok = false;
                }
            }
            if (bonus.displaySets() != null) {
                if (bonus.displaySets().isEmpty()) {
                    report.add(id, "no-display-sets", "display sets list is empty; omit it to display under every required set", source);
                    ok = false;
                }
                for (String setId : bonus.displaySets()) {
                    if (bonus.requirementFor(setId) == null) {
                        report.add(id, "display-set-not-required", "display set " + setId + " is not a required set", source);
                        ok = false;
                    }
                }
            }
            if (bonus.isScripted() && bonus.description() == null) {
                report.add(id, "no-description", "a bonus without effects needs a description", source);
                ok = false;
            }
            for (EffectSpec effect : bonus.effects()) {
                if (!context.effectKindExists(effect.kindId())) {
                    report.add(id, "unknown-effect-kind", "effect kind " + effect.kindId()
                            + " is not registered (is the addon that provides it installed?)", source);
                    ok = false;
                }
            }
            for (ConditionSpec condition : bonus.conditions()) {
                if (condition.instance() == null && !context.conditionKindExists(condition.kindId())) {
                    report.add(id, "unknown-condition-kind", "condition kind " + condition.kindId()
                            + " is not registered (is the addon that provides it installed?)", source);
                    ok = false;
                }
            }
            enabled.put(id, ok);
        }
        return enabled;
    }

    private RegistrationResult requireOpen(String action) {
        if (frozen) {
            return RegistrationResult.error("Setworks definitions are frozen; " + action
                    + " was ignored. Register during game startup; a change needs a restart.");
        }
        return null;
    }

    private RegistrationResult reject(String message) {
        rejected.add(message);
        return RegistrationResult.error(message);
    }
}
