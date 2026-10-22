package com.mahghuuuls.setworks.api;

import net.minecraft.item.ItemStack;

/** One stack read from a location, with a key that identifies its physical slot within that location. */
public final class SlotStack {

    private final String slotKey;
    private final ItemStack stack;

    public SlotStack(String slotKey, ItemStack stack) {
        this.slotKey = slotKey;
        this.stack = stack == null ? ItemStack.EMPTY : stack;
    }

    public String slotKey() {
        return slotKey;
    }

    public ItemStack stack() {
        return stack;
    }
}
