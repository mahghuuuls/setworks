package com.mahghuuuls.setworks.core.diagnostic;

import org.apache.logging.log4j.Logger;

/**
 * The optional diagnostic output: a load summary, one line per bonus state transition, and a
 * periodic timing summary. Off by default; never writes per tick. Everything goes through the mod
 * logger with the {@code [setworks-diag]} prefix so it can be grepped.
 */
public final class DiagnosticLog {

    private static final String PREFIX = "[setworks-diag] ";

    private final Logger logger;
    private final boolean enabled;

    public DiagnosticLog(Logger logger, boolean enabled) {
        this.logger = logger;
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void loadSummary(int enabledSets, int totalSets, int enabledBonuses, int totalBonuses, int pieces,
                            int errors, int scriptFailures, int locations, int sources) {
        if (!enabled) {
            return;
        }
        logger.info(PREFIX + "load summary: sets {}/{} enabled, bonuses {}/{} enabled, pieces {}, errors {}, script failures {}, locations {}, sources {}",
                enabledSets, totalSets, enabledBonuses, totalBonuses, pieces, errors, scriptFailures, locations, sources);
    }

    /** One bonus changed state for a player; the world tick lets a reader measure reaction time against the toolkit's marks. */
    public void transition(String player, String bonusId, String state, String reason, long tick) {
        if (!enabled) {
            return;
        }
        logger.info(PREFIX + "transition player={} bonus={} state={} reason={} tick={}", player, bonusId, state, reason, tick);
    }

    public void line(String message) {
        if (!enabled) {
            return;
        }
        logger.info(PREFIX + message);
    }
}
