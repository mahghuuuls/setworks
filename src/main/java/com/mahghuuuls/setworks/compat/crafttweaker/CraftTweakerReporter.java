package com.mahghuuuls.setworks.compat.crafttweaker;

import com.mahghuuuls.setworks.api.SetworksApi;
import crafttweaker.CraftTweakerAPI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Sends queue problems to {@code crafttweaker.log} (where script authors look) and the mod log. A
 * rejected registration is already a validation error with its source, so it is only mirrored; a
 * builder that threw is also recorded as a script failure, so {@code /setworks validate} shows it.
 */
final class CraftTweakerReporter implements ScriptQueue.Reporter {

    private static final Logger LOGGER = LogManager.getLogger("Setworks/scripts");

    @Override
    public void rejected(String describe, String message) {
        String text = "Setworks: " + describe + " was rejected: " + message;
        CraftTweakerAPI.logError(text);
        LOGGER.error(text);
    }

    @Override
    public void failed(String describe, RuntimeException error) {
        String text = "Setworks: " + describe + " failed: " + error.getMessage();
        CraftTweakerAPI.logError(text, error);
        LOGGER.error(text, error);
        SetworksApi.reportScriptFailure(describe + ": " + error.getMessage());
    }

    @Override
    public void late(String describe) {
        String text = "Setworks: " + describe + " came after load complete and was ignored; definitions are frozen then";
        CraftTweakerAPI.logWarning(text);
        LOGGER.warn(text);
    }
}
