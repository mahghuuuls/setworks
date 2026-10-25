package com.mahghuuuls.setworks.core.effect;

import com.mahghuuuls.setworks.api.IEffectKind;
import com.mahghuuuls.setworks.api.RegistrationResult;
import com.mahghuuuls.setworks.core.definition.IdRules;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Every effect kind Setworks knows, by id: the two built-in kinds first, then addon kinds registered during startup. */
public final class EffectKindRegistry {

    private final Map<String, IEffectKind> kinds = new LinkedHashMap<String, IEffectKind>();
    private volatile boolean frozen;

    public RegistrationResult register(IEffectKind kind) {
        if (kind == null || kind.id() == null || !IdRules.isValid(kind.id())) {
            return RegistrationResult.error("effect kind id " + (kind == null ? "null" : kind.id()) + " is invalid; " + IdRules.RULE_TEXT);
        }
        if (kind.combinationRule() == null) {
            return RegistrationResult.error("effect kind " + kind.id() + " declares no combination rule");
        }
        if (frozen) {
            return RegistrationResult.error("effect kinds are frozen; " + kind.id() + " was ignored");
        }
        IEffectKind existing = kinds.get(kind.id());
        if (existing != null) {
            return RegistrationResult.error("effect kind " + kind.id() + " is already registered by " + existing.getClass().getName());
        }
        kinds.put(kind.id(), kind);
        return RegistrationResult.ok();
    }

    public void freeze() {
        frozen = true;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public boolean exists(String id) {
        return kinds.containsKey(id);
    }

    public IEffectKind get(String id) {
        return kinds.get(id);
    }

    public Collection<IEffectKind> all() {
        return Collections.unmodifiableCollection(kinds.values());
    }
}
