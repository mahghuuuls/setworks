package com.mahghuuuls.setworks.core.match;

import com.mahghuuuls.setworks.api.ItemFilter;
import com.mahghuuuls.setworks.api.definition.PieceDefinition;
import com.mahghuuuls.setworks.api.definition.SetDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** A set with its pieces' filters compiled to predicates. Built once at freeze. */
public final class CompiledSet {

    /** One piece: its predicates and the locations where a match counts. */
    public static final class Piece {
        private final PieceDefinition definition;
        private final List<ItemPredicate> predicates;

        Piece(PieceDefinition definition) {
            this.definition = definition;
            List<ItemPredicate> list = new ArrayList<ItemPredicate>();
            for (ItemFilter filter : definition.alternatives()) {
                list.add(ItemPredicate.compile(filter));
            }
            this.predicates = Collections.unmodifiableList(list);
        }

        public String name() {
            return definition.name();
        }

        public PieceDefinition definition() {
            return definition;
        }

        public boolean acceptsLocation(String locationId) {
            return definition.locations().contains(locationId);
        }

        public boolean matches(ItemView item) {
            for (ItemPredicate predicate : predicates) {
                if (predicate.matches(item)) {
                    return true;
                }
            }
            return false;
        }
    }

    private final SetDefinition definition;
    private final List<Piece> pieces;
    private final Set<String> locations;

    public CompiledSet(SetDefinition definition) {
        this.definition = definition;
        List<Piece> list = new ArrayList<Piece>();
        Set<String> locationIds = new LinkedHashSet<String>();
        for (PieceDefinition piece : definition.pieces()) {
            list.add(new Piece(piece));
            locationIds.addAll(piece.locations());
        }
        this.pieces = Collections.unmodifiableList(list);
        this.locations = Collections.unmodifiableSet(locationIds);
    }

    public SetDefinition definition() {
        return definition;
    }

    public String id() {
        return definition.id();
    }

    public List<Piece> pieces() {
        return pieces;
    }

    /** Location ids any piece of this set references. */
    public Set<String> locations() {
        return locations;
    }

    /** True when some piece of this set accepts the item in some location. Used for tooltips. */
    public boolean mentions(ItemView item) {
        for (Piece piece : pieces) {
            if (piece.matches(item)) {
                return true;
            }
        }
        return false;
    }
}
