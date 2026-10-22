package com.mahghuuuls.setworks.api;

import com.mahghuuuls.setworks.api.definition.PieceDefinition;
import com.mahghuuuls.setworks.api.definition.SetDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a set definition. Shape checks (non-empty values) happen here; everything that needs the
 * game (known items, known locations) is checked when definitions freeze.
 */
public final class SetBuilder {

    private final String id;
    private String displayName;
    private ThresholdMode thresholdMode = ThresholdMode.CUMULATIVE;
    private final List<PieceDefinition> pieces = new ArrayList<PieceDefinition>();

    private SetBuilder(String id) {
        this.id = requireText(id, "set id");
        this.displayName = id;
    }

    public static SetBuilder set(String id) {
        return new SetBuilder(id);
    }

    /** Plain text or a language key. Defaults to the id. */
    public SetBuilder displayName(String name) {
        this.displayName = requireText(name, "display name");
        return this;
    }

    public SetBuilder thresholdMode(ThresholdMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("threshold mode must not be null");
        }
        this.thresholdMode = mode;
        return this;
    }

    public SetBuilder piece(PieceBuilder piece) {
        pieces.add(piece.build());
        return this;
    }

    /** The four vanilla armor locations, one piece each, accepting one item filter each. */
    public SetBuilder armor(ItemFilter head, ItemFilter chest, ItemFilter legs, ItemFilter feet) {
        piece(PieceBuilder.piece("head").accept(head).at("head"));
        piece(PieceBuilder.piece("chest").accept(chest).at("chest"));
        piece(PieceBuilder.piece("legs").accept(legs).at("legs"));
        piece(PieceBuilder.piece("feet").accept(feet).at("feet"));
        return this;
    }

    public SetDefinition build() {
        return new SetDefinition(id, displayName, thresholdMode, pieces);
    }

    static String requireText(String text, String what) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException(what + " must not be empty");
        }
        return text.trim();
    }
}
