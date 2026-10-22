package com.mahghuuuls.setworks.api.definition;

/** One set requirement of a bonus: at least {@code threshold} pieces of the set, or all of them. */
public final class SetRequirement {

    /** Threshold value meaning every defined piece of the set. */
    public static final int ALL_PIECES = 0;

    private final String setId;
    private final int threshold;

    private SetRequirement(String setId, int threshold) {
        this.setId = setId;
        this.threshold = threshold;
    }

    public static SetRequirement atLeast(String setId, int threshold) {
        if (threshold < 1) {
            throw new IllegalArgumentException("threshold must be 1 or more; use all() for the full set");
        }
        return new SetRequirement(setId, threshold);
    }

    public static SetRequirement all(String setId) {
        return new SetRequirement(setId, ALL_PIECES);
    }

    public String setId() {
        return setId;
    }

    /** {@link #ALL_PIECES} for a full-set requirement. */
    public int threshold() {
        return threshold;
    }

    public boolean isAllPieces() {
        return threshold == ALL_PIECES;
    }

    /** The number shown in tooltips and used for ordering: the threshold, or the set's piece count for a full-set requirement. */
    public int effectiveThreshold(SetDefinition set) {
        return isAllPieces() ? set.pieces().size() : threshold;
    }

    @Override
    public String toString() {
        return isAllPieces() ? setId + "(all)" : setId + "(" + threshold + ")";
    }
}
