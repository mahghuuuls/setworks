package com.mahghuuuls.setworks.api;

/**
 * Outcome of one registration call. Registration never throws into a caller: a script or mod
 * gets a message it can log, and the same message is recorded for the validate command.
 */
public final class RegistrationResult {

    private static final RegistrationResult OK = new RegistrationResult(true, "");

    private final boolean ok;
    private final String message;

    private RegistrationResult(boolean ok, String message) {
        this.ok = ok;
        this.message = message;
    }

    public static RegistrationResult ok() {
        return OK;
    }

    public static RegistrationResult error(String message) {
        return new RegistrationResult(false, message);
    }

    public boolean isOk() {
        return ok;
    }

    /** Empty when ok. */
    public String message() {
        return message;
    }

    @Override
    public String toString() {
        return ok ? "ok" : "error: " + message;
    }
}
