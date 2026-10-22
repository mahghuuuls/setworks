package com.mahghuuuls.setworks.config;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Server-side settings from {@code config/setworks.cfg}. Read once at startup. An invalid value is
 * reported and the default is used in memory; the file is never rewritten to hide the mistake.
 *
 * Value rules live in {@link #fromRaw}, which takes plain strings so it is tested without Forge;
 * {@link #load} only reads the file through Forge's configuration class.
 */
public final class SetworksConfig {

    public static final String CATEGORY = "general";
    public static final String KEY_STRICT = "strictValidation";
    public static final String KEY_INTERVAL = "conditionIntervalTicks";
    public static final String KEY_EXAMPLES = "enableExampleSets";
    public static final String KEY_DIAGNOSTIC = "diagnosticLog";
    public static final int DEFAULT_CONDITION_INTERVAL = 20;
    public static final int MIN_CONDITION_INTERVAL = 1;
    public static final int MAX_CONDITION_INTERVAL = Integer.MAX_VALUE;

    static final String COMMENT_STRICT = "Stop the game from starting when any set or bonus definition is invalid. Default false: "
            + "invalid sets are disabled and listed by /setworks validate. Needs a restart.";
    static final String COMMENT_INTERVAL = "How often bonus conditions are re-evaluated when equipment did not change, in ticks "
            + "(20 = one second). Minimum 1. A bonus may set its own interval. Needs a restart.";
    static final String COMMENT_EXAMPLES = "Register the example sets that ship with Setworks (vanilla items). Default false: "
            + "Setworks changes nothing on its own. Needs a restart.";
    static final String COMMENT_DIAGNOSTIC = "Write a load summary, one line per bonus state change per player, and a timing "
            + "summary every 1200 ticks to the game log. Never per tick. Default false. Needs a restart.";

    private final boolean strictValidation;
    private final int conditionIntervalTicks;
    private final boolean enableExampleSets;
    private final boolean diagnosticLog;

    public SetworksConfig(boolean strictValidation, int conditionIntervalTicks, boolean enableExampleSets, boolean diagnosticLog) {
        this.strictValidation = strictValidation;
        this.conditionIntervalTicks = conditionIntervalTicks;
        this.enableExampleSets = enableExampleSets;
        this.diagnosticLog = diagnosticLog;
    }

    public static SetworksConfig defaults() {
        return new SetworksConfig(false, DEFAULT_CONDITION_INTERVAL, false, false);
    }

    /** Loads the file, creating it with defaults and comments when absent. Never rewrites an existing value. */
    public static SetworksConfig load(File file, Logger logger) {
        Configuration configuration = new Configuration(file);
        configuration.load();
        Map<String, String> raw = new LinkedHashMap<String, String>();
        raw.put(KEY_STRICT, property(configuration, KEY_STRICT, false, COMMENT_STRICT).getString());
        raw.put(KEY_INTERVAL, configuration.get(CATEGORY, KEY_INTERVAL, DEFAULT_CONDITION_INTERVAL, COMMENT_INTERVAL,
                MIN_CONDITION_INTERVAL, MAX_CONDITION_INTERVAL).getString());
        raw.put(KEY_EXAMPLES, property(configuration, KEY_EXAMPLES, false, COMMENT_EXAMPLES).getString());
        raw.put(KEY_DIAGNOSTIC, property(configuration, KEY_DIAGNOSTIC, false, COMMENT_DIAGNOSTIC).getString());
        if (configuration.hasChanged()) {
            configuration.save();
        }
        return fromRaw(raw, logger);
    }

    private static Property property(Configuration configuration, String key, boolean fallback, String comment) {
        return configuration.get(CATEGORY, key, fallback, comment);
    }

    /** Builds the settings from raw text values; a missing or invalid value is reported and defaulted. */
    public static SetworksConfig fromRaw(Map<String, String> raw, Logger logger) {
        boolean strict = parseBoolean(raw.get(KEY_STRICT), KEY_STRICT, false, logger);
        int interval = parseInt(raw.get(KEY_INTERVAL), KEY_INTERVAL, DEFAULT_CONDITION_INTERVAL,
                MIN_CONDITION_INTERVAL, MAX_CONDITION_INTERVAL, logger);
        boolean examples = parseBoolean(raw.get(KEY_EXAMPLES), KEY_EXAMPLES, false, logger);
        boolean diagnostic = parseBoolean(raw.get(KEY_DIAGNOSTIC), KEY_DIAGNOSTIC, false, logger);
        return new SetworksConfig(strict, interval, examples, diagnostic);
    }

    private static boolean parseBoolean(String raw, String key, boolean fallback, Logger logger) {
        String text = raw == null ? "" : raw.trim();
        if (text.equalsIgnoreCase("true") || text.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(text);
        }
        logger.warn("setworks.cfg: {} = \"{}\" is not true or false; using {} (the file was left unchanged)", key, text, fallback);
        return fallback;
    }

    private static int parseInt(String raw, String key, int fallback, int min, int max, Logger logger) {
        String text = raw == null ? "" : raw.trim();
        try {
            int value = Integer.parseInt(text);
            if (value < min || value > max) {
                logger.warn("setworks.cfg: {} = {} is outside {} to {}; using {} (the file was left unchanged)", key, value, min, max, fallback);
                return fallback;
            }
            return value;
        } catch (NumberFormatException e) {
            logger.warn("setworks.cfg: {} = \"{}\" is not a number; using {} (the file was left unchanged)", key, text, fallback);
            return fallback;
        }
    }

    public boolean strictValidation() {
        return strictValidation;
    }

    public int conditionIntervalTicks() {
        return conditionIntervalTicks;
    }

    public boolean enableExampleSets() {
        return enableExampleSets;
    }

    public boolean diagnosticLog() {
        return diagnosticLog;
    }
}
