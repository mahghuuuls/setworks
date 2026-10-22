package com.mahghuuuls.setworks.core.definition;

import java.util.regex.Pattern;

/** The one place that knows what a valid definition id looks like. */
public final class IdRules {

    /** Lowercase letters, digits, underscores, with an optional namespace of the same form. */
    private static final Pattern ID = Pattern.compile("^([a-z0-9_]+:)?[a-z0-9_]+$");

    public static final String RULE_TEXT = "ids use lowercase letters, digits, and underscores, with an optional namespace: prefix";

    private IdRules() {
    }

    public static boolean isValid(String id) {
        return id != null && ID.matcher(id).matches();
    }
}
