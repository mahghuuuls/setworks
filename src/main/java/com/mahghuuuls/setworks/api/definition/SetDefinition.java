package com.mahghuuuls.setworks.api.definition;

import com.mahghuuuls.setworks.api.ThresholdMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** An author-defined collection of named pieces. Immutable. */
public final class SetDefinition {

    private final String id;
    private final String displayName;
    private final ThresholdMode thresholdMode;
    private final List<PieceDefinition> pieces;

    public SetDefinition(String id, String displayName, ThresholdMode thresholdMode, List<PieceDefinition> pieces) {
        this.id = id;
        this.displayName = displayName;
        this.thresholdMode = thresholdMode;
        this.pieces = Collections.unmodifiableList(new ArrayList<PieceDefinition>(pieces));
    }

    public String id() {
        return id;
    }

    /** Plain text or a language key. */
    public String displayName() {
        return displayName;
    }

    public ThresholdMode thresholdMode() {
        return thresholdMode;
    }

    public List<PieceDefinition> pieces() {
        return pieces;
    }

    public PieceDefinition piece(String name) {
        for (PieceDefinition piece : pieces) {
            if (piece.name().equals(name)) {
                return piece;
            }
        }
        return null;
    }

    public SetDefinition withPieces(List<PieceDefinition> newPieces) {
        return new SetDefinition(id, displayName, thresholdMode, newPieces);
    }
}
