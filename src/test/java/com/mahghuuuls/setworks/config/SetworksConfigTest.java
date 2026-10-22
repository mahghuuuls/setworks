package com.mahghuuuls.setworks.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the value rules. Reading the file goes through Forge's configuration class, which needs
 * the mod loader, so the "file left unchanged" behavior is checked at runtime, not here.
 */
class SetworksConfigTest {

    private static final Logger LOGGER = LogManager.getLogger("test");

    private static Map<String, String> raw(String strict, String interval, String examples, String diagnostic) {
        Map<String, String> raw = new HashMap<String, String>();
        raw.put(SetworksConfig.KEY_STRICT, strict);
        raw.put(SetworksConfig.KEY_INTERVAL, interval);
        raw.put(SetworksConfig.KEY_EXAMPLES, examples);
        raw.put(SetworksConfig.KEY_DIAGNOSTIC, diagnostic);
        return raw;
    }

    @Test
    void defaultsMatchTheApprovedValues() {
        SetworksConfig config = SetworksConfig.defaults();
        assertFalse(config.strictValidation());
        assertEquals(20, config.conditionIntervalTicks());
        assertFalse(config.enableExampleSets());
        assertFalse(config.diagnosticLog());
    }

    @Test
    void validValuesAreRead() {
        SetworksConfig config = SetworksConfig.fromRaw(raw("true", "5", "true", "true"), LOGGER);
        assertTrue(config.strictValidation());
        assertEquals(5, config.conditionIntervalTicks());
        assertTrue(config.enableExampleSets());
        assertTrue(config.diagnosticLog());
    }

    @Test
    void invalidValuesFallBackToDefaultsIndividually() {
        SetworksConfig config = SetworksConfig.fromRaw(raw("maybe", "0", "true", "yes"), LOGGER);
        assertFalse(config.strictValidation());
        assertEquals(20, config.conditionIntervalTicks());
        assertTrue(config.enableExampleSets());
        assertFalse(config.diagnosticLog());
    }

    @Test
    void intervalRangeAndTextAreChecked() {
        assertEquals(20, SetworksConfig.fromRaw(raw("false", "abc", "false", "false"), LOGGER).conditionIntervalTicks());
        assertEquals(20, SetworksConfig.fromRaw(raw("false", "-5", "false", "false"), LOGGER).conditionIntervalTicks());
        assertEquals(72001, SetworksConfig.fromRaw(raw("false", "72001", "false", "false"), LOGGER).conditionIntervalTicks());
        assertEquals(1, SetworksConfig.fromRaw(raw("false", "1", "false", "false"), LOGGER).conditionIntervalTicks());
        assertEquals(20, SetworksConfig.fromRaw(raw("false", null, "false", "false"), LOGGER).conditionIntervalTicks());
    }
}
