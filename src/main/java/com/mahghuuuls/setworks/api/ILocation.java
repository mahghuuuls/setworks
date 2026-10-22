package com.mahghuuuls.setworks.api;

import net.minecraft.entity.player.EntityPlayer;

import java.util.List;

/**
 * A place where an item can sit and be counted. Built-in locations cover the vanilla armor slots,
 * hands, hotbar, and inventory; an addon registers more (for example Baubles slots). Each id maps
 * to one physical slot or slot group; never re-expose a slot another location already covers.
 */
public interface ILocation {

    /** Unique id, for example {@code head} or {@code baubles:ring}. */
    String id();

    /** Who registered it. */
    DefinitionSource source();

    /** The stacks currently in this location for the player. Must be cheap; called every tick on the server. */
    List<SlotStack> read(EntityPlayer player);
}
