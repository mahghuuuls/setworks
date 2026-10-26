package com.mahghuuuls.setworks.compat.crafttweaker;

import com.mahghuuuls.setworks.api.SetworksApi;
import crafttweaker.CraftTweakerAPI;
import crafttweaker.runtime.CrTTweaker;
import crafttweaker.runtime.ITweaker;
import crafttweaker.runtime.events.CrTScriptLoadingEvent;
import crafttweaker.socket.SingleError;
import crafttweaker.util.IEventHandler;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs the {@code setworks} script loader at load complete and then applies every queued script
 * call, once. Loaded by the mod only when CraftTweaker is present. A file of the loader that fails
 * to parse makes CraftTweaker refuse every later file of that loader; the bridge reports that scope
 * (the failing file with its line, and the files that did not run after it) as a script failure,
 * so {@code /setworks validate} says which definitions are missing and why. It cannot isolate the
 * failure; it names it. A file that compiles but throws while running is CraftTweaker's own
 * per-file error in {@code crafttweaker.log}; the files after it still run.
 */
public final class CraftTweakerBridge {

    public static final String LOADER_NAME = "setworks";

    private static boolean ran;
    private static boolean recording;
    private static final List<String> STARTED = new ArrayList<String>();

    private CraftTweakerBridge() {
    }

    public static void loadScriptsAndApply(Logger log) {
        if (ran) {
            log.warn("CraftTweaker: the Setworks script loader already ran in this game; not running it again");
            return;
        }
        ran = true;
        SetworksApi.addBonusListener(ZenEvents.listener());
        ITweaker tweaker = CraftTweakerAPI.tweaker;
        installRecorder(tweaker);
        List<SingleError> parseErrors = new ArrayList<SingleError>();
        boolean loaded = false;
        recording = true;
        STARTED.clear();
        try {
            if (tweaker instanceof CrTTweaker) {
                loaded = ((CrTTweaker) tweaker).loadScript(false, parseErrors, false, LOADER_NAME);
            } else {
                loaded = tweaker.loadScript(false, LOADER_NAME);
            }
        } catch (RuntimeException error) {
            log.error("Setworks script loader threw", error);
            SetworksApi.reportScriptFailure("loader " + LOADER_NAME + " threw " + error + "; every Setworks definition from its files is missing");
            loaded = true; // reported above
        } finally {
            recording = false;
        }
        reportScope(log, loaded, parseErrors);
        int applied = ScriptQueue.get().applyAll();
        log.info("CraftTweaker: loader {} ran {} file(s), {} queued script call(s) registered", LOADER_NAME, STARTED.size(), applied);
    }

    /** Records the files of the loader in the order CraftTweaker visits them, during this run only. */
    private static void installRecorder(ITweaker tweaker) {
        tweaker.registerScriptLoadPreEvent(new IEventHandler<CrTScriptLoadingEvent.Pre>() {
            @Override
            public void handle(CrTScriptLoadingEvent.Pre event) {
                if (recording) {
                    STARTED.add(event.getFileName());
                }
            }
        });
    }

    /** Names the file that failed to parse and the files of the loader CraftTweaker refused after it. */
    static void reportScope(Logger log, boolean loaded, List<SingleError> parseErrors) {
        String failingFile = null;
        int line = 0;
        String explanation = null;
        if (parseErrors != null && !parseErrors.isEmpty()) {
            SingleError first = parseErrors.get(0);
            failingFile = first.fileName == null ? "a file" : first.fileName;
            line = first.line;
            explanation = first.explanation;
        }
        String message = LoaderScope.message(LOADER_NAME, loaded, failingFile, line, explanation, STARTED);
        if (message == null) {
            return;
        }
        // The mod logs every script failure once from the report at load complete; here only crafttweaker.log.
        CraftTweakerAPI.logError("Setworks: " + message);
        SetworksApi.reportScriptFailure(message);
    }
}
