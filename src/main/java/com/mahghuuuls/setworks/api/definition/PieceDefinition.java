package com.mahghuuuls.setworks.api.definition;

import com.mahghuuuls.setworks.api.ItemFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A named requirement inside a set: accepted items and the locations where they count. Immutable. */
public final class PieceDefinition {

    private final String name;
    private final List<ItemFilter> alternatives;
    private final List<String> locations;

    public PieceDefinition(String name, List<ItemFilter> alternatives, List<String> locations) {
        this.name = name;
        this.alternatives = Collections.unmodifiableList(new ArrayList<ItemFilter>(alternatives));
        this.locations = Collections.unmodifiableList(new ArrayList<String>(locations));
    }

    public String name() {
        return name;
    }

    public List<ItemFilter> alternatives() {
        return alternatives;
    }

    public List<String> locations() {
        return locations;
    }

    /** Copy with more alternatives, used by script modifications. */
    public PieceDefinition withAlternatives(List<ItemFilter> more) {
        List<ItemFilter> all = new ArrayList<ItemFilter>(alternatives);
        for (ItemFilter filter : more) {
            if (!all.contains(filter)) {
                all.add(filter);
            }
        }
        return new PieceDefinition(name, all, locations);
    }
}
