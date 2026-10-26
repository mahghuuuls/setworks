package com.mahghuuuls.setworks.client;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.File;

/**
 * Client-side settings, read from the same {@code config/setworks.cfg} under the {@code client}
 * category. Only the client writes this category; a dedicated server never sees it.
 */
public final class ClientConfig {

    public static final String CATEGORY = "client";
    public static final String KEY_SHOW_TOOLTIPS = "showTooltips";
    static final String COMMENT_SHOW_TOOLTIPS = "Show the Setworks set block in item tooltips on this client (set name with the worn "
            + "count, the bonus lines, and the piece list while Shift is held). Default true. Does not change what the "
            + "server applies. Needs a restart.";

    private static volatile boolean showTooltips = true;

    private ClientConfig() {
    }

    public static boolean showTooltips() {
        return showTooltips;
    }

    /** For tests and the proxy. */
    public static void setShowTooltips(boolean value) {
        showTooltips = value;
    }

    /** Loads the client category, creating the key with its comment when absent. Never rewrites an existing value. */
    public static void load(File file) {
        Configuration configuration = new Configuration(file);
        configuration.load();
        Property property = configuration.get(CATEGORY, KEY_SHOW_TOOLTIPS, true, COMMENT_SHOW_TOOLTIPS);
        showTooltips = property.getBoolean(true);
        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
