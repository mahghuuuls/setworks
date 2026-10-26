package com.mahghuuuls.setworks.compat.crafttweaker;

import com.mahghuuuls.setworks.api.RegistrationResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Script calls to {@code mods.setworks.Setworks} are queued in call order and applied once, at
 * load complete right before the definitions freeze, whichever loader ran the script. A handle
 * keeps changing its builder until then, so {@code Setworks.bonus("x").attribute(...)} needs no
 * closing call. A call made after the queue was applied is reported and dropped: the registry is
 * frozen and a second world load must not register anything twice. Pure; the CraftTweaker-facing
 * reporter is injected.
 */
public final class ScriptQueue {

    /** One queued call: what it is, for messages, and the registration it performs when applied. */
    public interface Entry {
        String describe();

        /** Builds the definition and registers it; may throw an {@link IllegalArgumentException} from a builder. */
        RegistrationResult apply();
    }

    /** Where problems go; the bridge sends them to the mod log, {@code crafttweaker.log}, and the validation report. */
    public interface Reporter {
        /** The registration was rejected (duplicate id, after freeze, and so on). */
        void rejected(String describe, String message);

        /** Building or registering threw. */
        void failed(String describe, RuntimeException error);

        /** The call came after the queue was applied. */
        void late(String describe);
    }

    private static final class Holder {
        static final ScriptQueue INSTANCE = new ScriptQueue(new CraftTweakerReporter());
    }

    private final Reporter reporter;
    private final List<Entry> entries = new ArrayList<Entry>();
    private boolean applied;

    public ScriptQueue(Reporter reporter) {
        this.reporter = reporter;
    }

    /** The queue the script classes use; created on first use so a game without CraftTweaker never loads the reporter. */
    static ScriptQueue get() {
        return Holder.INSTANCE;
    }

    /** True when queued; false when the queue was already applied (the call is reported and dropped). */
    public synchronized boolean enqueue(Entry entry) {
        if (applied) {
            reporter.late(entry.describe());
            return false;
        }
        entries.add(entry);
        return true;
    }

    /** Applies every entry in order, reports each problem, empties the queue, and returns how many registered without a problem. */
    public synchronized int applyAll() {
        applied = true;
        int ok = 0;
        for (Entry entry : entries) {
            try {
                RegistrationResult result = entry.apply();
                if (result == null || result.isOk()) {
                    ok++;
                } else {
                    reporter.rejected(entry.describe(), result.message());
                }
            } catch (RuntimeException error) {
                reporter.failed(entry.describe(), error);
            }
        }
        entries.clear();
        return ok;
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized boolean isApplied() {
        return applied;
    }
}
