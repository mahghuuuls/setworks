package com.mahghuuuls.setworks.core.location;

import com.mahghuuuls.setworks.api.DefinitionSource;
import com.mahghuuuls.setworks.api.ILocation;
import com.mahghuuuls.setworks.api.SlotStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The built-in locations: the four armor slots, both hands, the hotbar, the main inventory, and
 * single inventory slot indices (registered on demand as {@code slot:<index>}).
 */
public final class VanillaLocations {

    public static final String HEAD = "head";
    public static final String CHEST = "chest";
    public static final String LEGS = "legs";
    public static final String FEET = "feet";
    public static final String MAINHAND = "mainhand";
    public static final String OFFHAND = "offhand";
    public static final String HOTBAR = "hotbar";
    public static final String INVENTORY = "inventory";
    public static final String SLOT_PREFIX = "slot:";

    private static final DefinitionSource SOURCE = DefinitionSource.builtin("setworks:vanilla");

    private VanillaLocations() {
    }

    public static void registerAll(LocationRegistry registry) {
        registry.register(equipment(HEAD, EntityEquipmentSlot.HEAD));
        registry.register(equipment(CHEST, EntityEquipmentSlot.CHEST));
        registry.register(equipment(LEGS, EntityEquipmentSlot.LEGS));
        registry.register(equipment(FEET, EntityEquipmentSlot.FEET));
        registry.register(equipment(MAINHAND, EntityEquipmentSlot.MAINHAND));
        registry.register(equipment(OFFHAND, EntityEquipmentSlot.OFFHAND));
        registry.register(range(HOTBAR, 0, 9));
        registry.register(range(INVENTORY, 9, 36));
        for (int i = 0; i < 36; i++) {
            registry.register(range(SLOT_PREFIX + i, i, i + 1));
        }
    }

    private static ILocation equipment(final String id, final EntityEquipmentSlot slot) {
        return new ILocation() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public DefinitionSource source() {
                return SOURCE;
            }

            @Override
            public List<SlotStack> read(EntityPlayer player) {
                return Collections.singletonList(new SlotStack(id, player.getItemStackFromSlot(slot)));
            }
        };
    }

    private static ILocation range(final String id, final int from, final int to) {
        return new ILocation() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public DefinitionSource source() {
                return SOURCE;
            }

            @Override
            public List<SlotStack> read(EntityPlayer player) {
                List<SlotStack> stacks = new ArrayList<SlotStack>(to - from);
                for (int i = from; i < to; i++) {
                    stacks.add(new SlotStack(SLOT_PREFIX + i, player.inventory.mainInventory.get(i)));
                }
                return stacks;
            }
        };
    }
}
