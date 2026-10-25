package com.mahghuuuls.setworks.api.definition;

import com.mahghuuuls.setworks.api.ICondition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One condition attached to a bonus: a condition kind id with parameters, or a direct condition
 * object supplied by Java or a script lambda (then {@code kindId} is the registered name of that
 * instance and {@code params} is empty). Immutable.
 */
public final class ConditionSpec {

    private final String kindId;
    private final Map<String, String> params;
    private final ICondition instance;

    public ConditionSpec(String kindId, Map<String, String> params) {
        this(kindId, params, null);
    }

    public ConditionSpec(String kindId, Map<String, String> params, ICondition instance) {
        this.kindId = kindId;
        this.params = Collections.unmodifiableMap(new LinkedHashMap<String, String>(params));
        this.instance = instance;
    }

    public String kindId() {
        return kindId;
    }

    public Map<String, String> params() {
        return params;
    }

    /** A directly supplied condition, or {@code null} when the kind registry creates it from the parameters. */
    public ICondition instance() {
        return instance;
    }

    @Override
    public String toString() {
        return kindId + params;
    }
}
