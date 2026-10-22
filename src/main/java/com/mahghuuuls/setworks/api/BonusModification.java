package com.mahghuuuls.setworks.api;

import java.util.ArrayList;
import java.util.List;

/** Presentation changes a script or mod applies to an existing bonus. Unset fields keep their value. */
public final class BonusModification {

    private String description;
    private Visibility visibility;
    private List<String> displaySets;

    public static BonusModification modify() {
        return new BonusModification();
    }

    public BonusModification description(String text) {
        this.description = SetBuilder.requireText(text, "description");
        return this;
    }

    public BonusModification visibility(Visibility value) {
        this.visibility = value;
        return this;
    }

    public BonusModification displayUnder(String... setIds) {
        List<String> ids = new ArrayList<String>();
        for (String setId : setIds) {
            ids.add(SetBuilder.requireText(setId, "set id"));
        }
        this.displaySets = ids;
        return this;
    }

    public String description() {
        return description;
    }

    public Visibility visibility() {
        return visibility;
    }

    public List<String> displaySets() {
        return displaySets;
    }

    public boolean isEmpty() {
        return description == null && visibility == null && displaySets == null;
    }
}
