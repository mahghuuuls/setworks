package com.mahghuuuls.setworks.compat.baubles;

import baubles.api.BaubleType;
import baubles.api.cap.BaublesCapabilities;
import baubles.api.cap.IBaublesItemHandler;
import com.mahghuuuls.setworks.api.DefinitionSource;
import com.mahghuuuls.setworks.api.ILocation;
import com.mahghuuuls.setworks.api.RegistrationResult;
import com.mahghuuuls.setworks.api.SetworksApi;
import com.mahghuuuls.setworks.api.SlotStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Baubles slots as locations, registered through the public location API the way an addon
 * would: {@code baubles:amulet}, {@code baubles:ring} (both ring slots, slot keys {@code baubles:1}
 * and {@code baubles:2}), {@code baubles:belt}, {@code baubles:head}, {@code baubles:body},
 * {@code baubles:charm}. The trinket type covers every slot and is not a location, because a slot
 * may not be exposed twice. Loaded only when Baubles is present; reads the player's Baubles handler
 * each scan and reports nothing for a player without one. {@code baubles:head} is the Baubles head
 * slot, not the vanilla {@code head} armor location.
 */
public final class BaublesLocations {

    public static final DefinitionSource SOURCE = DefinitionSource.builtin("setworks:baubles");
    public static final String PREFIX = "baubles:";

    private BaublesLocations() {
    }

    /** Called at initialization, on both sides, when Baubles is loaded. */
    public static void register(Logger log) {
        List<String> ids = new ArrayList<String>();
        for (BaubleType type : BaubleType.values()) {
            if (type == BaubleType.TRINKET) {
                continue;
            }
            String id = PREFIX + type.name().toLowerCase();
            RegistrationResult result = SetworksApi.registerLocation(location(id, type.getValidSlots()));
            if (result.isOk()) {
                ids.add(id); // a rejection is already logged by the registrar
            }
        }
        log.info("Baubles locations registered: {}", ids);
    }

    static ILocation location(final String id, final int[] slots) {
        final String[] keys = new String[slots.length];
        for (int i = 0; i < slots.length; i++) {
            keys[i] = PREFIX + slots[i];
        }
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
                // The capability directly: BaublesApi.getBaublesHandler would write the player into it every tick
                // and would throw for a player without one.
                IBaublesItemHandler handler = player == null ? null
                        : player.getCapability(BaublesCapabilities.CAPABILITY_BAUBLES, null);
                if (handler == null) {
                    return Collections.emptyList();
                }
                List<SlotStack> stacks = new ArrayList<SlotStack>(slots.length);
                for (int i = 0; i < slots.length; i++) {
                    ItemStack stack = slots[i] < handler.getSlots() ? handler.getStackInSlot(slots[i]) : ItemStack.EMPTY;
                    stacks.add(new SlotStack(keys[i], stack));
                }
                return stacks;
            }
        };
    }
}
