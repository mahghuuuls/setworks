package com.mahghuuuls.setworks.core.eval;

import com.mahghuuuls.setworks.api.IConditionKind;
import com.mahghuuuls.setworks.api.RegistrationResult;
import com.mahghuuuls.setworks.core.definition.IdRules;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Every condition kind Setworks knows, by id: the built-in kinds first, then addon kinds registered during startup. */
public final class ConditionKindRegistry {

    private final Map<String, IConditionKind> kinds = new LinkedHashMap<String, IConditionKind>();
    private volatile boolean frozen;

    public RegistrationResult register(IConditionKind kind) {
        if (kind == null || kind.id() == null || !IdRules.isValid(kind.id())) {
            return RegistrationResult.error("condition kind id " + (kind == null ? "null" : kind.id()) + " is invalid; " + IdRules.RULE_TEXT);
        }
        if (frozen) {
            return RegistrationResult.error("condition kinds are frozen; " + kind.id() + " was ignored");
        }
        IConditionKind existing = kinds.get(kind.id());
        if (existing != null) {
            return RegistrationResult.error("condition kind " + kind.id() + " is already registered by " + existing.getClass().getName());
        }
        kinds.put(kind.id(), kind);
        return RegistrationResult.ok();
    }

    public void freeze() {
        frozen = true;
    }

    public boolean exists(String id) {
        return kinds.containsKey(id);
    }

    public IConditionKind get(String id) {
        return kinds.get(id);
    }

    public Collection<IConditionKind> all() {
        return Collections.unmodifiableCollection(kinds.values());
    }
}
