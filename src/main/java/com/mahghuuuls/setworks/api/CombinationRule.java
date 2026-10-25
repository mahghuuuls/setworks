package com.mahghuuuls.setworks.api;

/**
 * How several active instances of one effect kind combine on one player. Declared by the kind,
 * shown by {@code /setworks inspect} and by generated descriptions; Setworks does not enforce it.
 * Each kind documents what it does when two instances are applied.
 */
public enum CombinationRule {
    /** Every instance contributes; two +2 armor instances give +4. */
    SUM,
    /** The strongest instance decides; a Speed I and a Speed II instance give Speed II. */
    HIGHEST_WINS,
    /** The most recently applied instance replaces the earlier ones. */
    REPLACE;

    public String describe() {
        return name().toLowerCase().replace('_', ' ');
    }
}
