package com.mahghuuuls.setworks.compat.crafttweaker;

import com.mahghuuuls.setworks.api.DefinitionSource;

/**
 * Turns CraftTweaker's "file:line" text for the running script into a {@link DefinitionSource},
 * so a rejected definition names the script line that made it. Pure; the CraftTweaker call that
 * supplies the text lives in {@link ZenSetworks}.
 */
final class ScriptSource {

    static final String UNKNOWN_FILE = "script";

    private ScriptSource() {
    }

    /** {@code "sets.zs:12"} gives {@code script:sets.zs:12}; a text without a line number is the file alone; blank gives {@code script:script}. */
    static DefinitionSource parse(String fileAndLine) {
        if (fileAndLine == null || fileAndLine.trim().isEmpty() || fileAndLine.trim().equals("<?>")) {
            return DefinitionSource.script(UNKNOWN_FILE, "");
        }
        String text = fileAndLine.trim();
        int colon = text.lastIndexOf(':');
        if (colon > 0 && colon < text.length() - 1 && isDigits(text.substring(colon + 1))) {
            return DefinitionSource.script(text.substring(0, colon), text.substring(colon + 1));
        }
        return DefinitionSource.script(text, "");
    }

    private static boolean isDigits(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isDigit(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
