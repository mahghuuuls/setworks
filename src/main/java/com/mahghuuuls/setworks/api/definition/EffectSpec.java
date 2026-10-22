package com.mahghuuuls.setworks.api.definition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** One effect carried by a bonus: an effect kind id and its parameters, as the author wrote them. Immutable. */
public final class EffectSpec {

    private final String kindId;
    private final Map<String, String> params;

    public EffectSpec(String kindId, Map<String, String> params) {
        this.kindId = kindId;
        this.params = Collections.unmodifiableMap(new LinkedHashMap<String, String>(params));
    }

    public String kindId() {
        return kindId;
    }

    public Map<String, String> params() {
        return params;
    }

    public String param(String key) {
        return params.get(key);
    }

    @Override
    public String toString() {
        return kindId + params;
    }
}
