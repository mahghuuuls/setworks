package com.mahghuuuls.setworks.core.match;

/** One occupied slot offered to piece assignment: where it is and what it holds. */
public final class SlotCandidate {

    private final String locationId;
    private final String slotKey;
    private final ItemView item;

    public SlotCandidate(String locationId, String slotKey, ItemView item) {
        this.locationId = locationId;
        this.slotKey = slotKey;
        this.item = item;
    }

    public String locationId() {
        return locationId;
    }

    public String slotKey() {
        return slotKey;
    }

    public ItemView item() {
        return item;
    }
}
