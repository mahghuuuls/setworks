package com.mahghuuuls.setworks.core.diagnostic;

/**
 * Nanosecond accumulators for the per-tick location scan, the change-driven evaluations, and the
 * full rebuilds, summarized every 1200 ticks when the diagnostic log is on. The scan runs once per
 * player per tick, so its average is the per-player-per-tick cost; the rebuild average and maximum
 * are the full-recount cost. Resolution one microsecond in the summary.
 */
public final class Timings {

    public static final int SUMMARY_INTERVAL_TICKS = 1200;

    private long scanNanos;
    private long scanCount;
    private long evaluateNanos;
    private long evaluateCount;
    private long rebuildNanos;
    private long rebuildCount;
    private long rebuildMaxNanos;

    public void scan(long nanos) {
        scanNanos += nanos;
        scanCount++;
    }

    public void evaluate(long nanos) {
        evaluateNanos += nanos;
        evaluateCount++;
    }

    public void rebuild(long nanos) {
        rebuildNanos += nanos;
        rebuildCount++;
        if (nanos > rebuildMaxNanos) {
            rebuildMaxNanos = nanos;
        }
    }

    /** One line of averages in microseconds, then resets the accumulators. */
    public String summaryAndReset(int players) {
        String line = "timing summary over " + SUMMARY_INTERVAL_TICKS + " ticks: players " + players
                + ", scans " + scanCount + " avg " + micros(scanNanos, scanCount) + " us per player tick"
                + ", change evaluations " + evaluateCount + " avg " + micros(evaluateNanos, evaluateCount) + " us"
                + ", rebuilds " + rebuildCount + " avg " + micros(rebuildNanos, rebuildCount) + " us max "
                + (rebuildMaxNanos / 1000L) + " us";
        scanNanos = 0;
        scanCount = 0;
        evaluateNanos = 0;
        evaluateCount = 0;
        rebuildNanos = 0;
        rebuildCount = 0;
        rebuildMaxNanos = 0;
        return line;
    }

    private static String micros(long nanos, long count) {
        if (count == 0) {
            return "0";
        }
        return Long.toString(nanos / count / 1000L);
    }
}
