package com.mahghuuuls.setworks.api;

import com.mahghuuuls.setworks.api.definition.PieceDefinition;

import java.util.ArrayList;
import java.util.List;

/** Builds one named piece: its alternatives and the locations where they count. */
public final class PieceBuilder {

    private final String name;
    private final List<ItemFilter> alternatives = new ArrayList<ItemFilter>();
    private final List<String> locations = new ArrayList<String>();

    private PieceBuilder(String name) {
        this.name = SetBuilder.requireText(name, "piece name");
    }

    public static PieceBuilder piece(String name) {
        return new PieceBuilder(name);
    }

    /** Adds an alternative. An item that matches any alternative satisfies the piece. */
    public PieceBuilder accept(ItemFilter filter) {
        if (filter == null) {
            throw new IllegalArgumentException("filter must not be null");
        }
        if (!alternatives.contains(filter)) {
            alternatives.add(filter);
        }
        return this;
    }

    public PieceBuilder accept(String itemId) {
        return accept(ItemFilter.item(itemId));
    }

    /** Adds a permitted location id, for example {@code head} or {@code baubles:ring}. */
    public PieceBuilder at(String locationId) {
        String id = SetBuilder.requireText(locationId, "location id");
        if (!locations.contains(id)) {
            locations.add(id);
        }
        return this;
    }

    public PieceDefinition build() {
        return new PieceDefinition(name, alternatives, locations);
    }
}
