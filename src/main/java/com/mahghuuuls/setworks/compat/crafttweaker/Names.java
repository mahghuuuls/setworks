package com.mahghuuuls.setworks.compat.crafttweaker;

import com.mahghuuuls.setworks.api.ThresholdMode;
import com.mahghuuuls.setworks.api.Visibility;

import java.util.LinkedHashMap;
import java.util.Map;

/** Script words for the API enums, and a copy of a script map as string parameters. Pure. */
final class Names {

    private Names() {
    }

    static ThresholdMode thresholdMode(String text) {
        String name = normalize(text, "threshold mode");
        if (name.equals("cumulative")) {
            return ThresholdMode.CUMULATIVE;
        }
        if (name.equals("highest_only") || name.equals("highestonly") || name.equals("highest")) {
            return ThresholdMode.HIGHEST_ONLY;
        }
        throw new IllegalArgumentException("threshold mode must be \"cumulative\" or \"highest_only\", not \"" + text + "\"");
    }

    static Visibility visibility(String text) {
        String name = normalize(text, "visibility");
        if (name.equals("visible")) {
            return Visibility.VISIBLE;
        }
        if (name.equals("discovered")) {
            return Visibility.DISCOVERED;
        }
        if (name.equals("hidden")) {
            return Visibility.HIDDEN;
        }
        throw new IllegalArgumentException("visibility must be \"visible\", \"discovered\", or \"hidden\", not \"" + text + "\"");
    }

    /** Script maps arrive as {@code Map<String, ?>}; every value is kept as its text. */
    static Map<String, String> params(Map<String, ?> map) {
        Map<String, String> copy = new LinkedHashMap<String, String>();
        if (map != null) {
            for (Map.Entry<String, ?> entry : map.entrySet()) {
                copy.put(entry.getKey(), entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
            }
        }
        return copy;
    }

    private static String normalize(String text, String what) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException(what + " must not be empty");
        }
        return text.trim().toLowerCase().replace('-', '_').replace(' ', '_');
    }
}
