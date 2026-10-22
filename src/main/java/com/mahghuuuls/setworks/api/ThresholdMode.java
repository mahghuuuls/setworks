package com.mahghuuuls.setworks.api;

/** How a set combines its own thresholds. Multi-set bonuses are never affected by this mode. */
public enum ThresholdMode {
    /** Every threshold that is reached and passes its conditions is active. The default. */
    CUMULATIVE,
    /** Only the highest reached threshold with an active bonus applies; lower ones apply when every bonus above fails. */
    HIGHEST_ONLY
}
