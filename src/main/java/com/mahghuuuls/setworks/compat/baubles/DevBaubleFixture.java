package com.mahghuuuls.setworks.compat.baubles;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import baubles.api.cap.BaubleItem;
import baubles.api.cap.BaublesCapabilities;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Development only ({@code -Dsetworks.invalidExample=true}, Baubles present): makes every gold
 * nugget a ring bauble through Baubles' item capability, so the ring location can be tested with a
 * vanilla item. Baubles ships no items of its own. Registered on the Forge bus on both sides, since
 * the Baubles screen checks the capability on the client too.
 */
public final class DevBaubleFixture {

    private static final ResourceLocation KEY = new ResourceLocation("setworks", "dev_ring");
    private static final IBauble RING = new BaubleItem(BaubleType.RING);

    private DevBaubleFixture() {
    }

    public static Object handler() {
        return new Events();
    }

    public static final class Events {
        @SubscribeEvent
        public void onAttach(AttachCapabilitiesEvent<ItemStack> event) {
            ItemStack stack = event.getObject();
            if (stack != null && !stack.isEmpty() && stack.getItem() == Items.GOLD_NUGGET) {
                event.addCapability(KEY, new ICapabilityProvider() {
                    @Override
                    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
                        return capability == BaublesCapabilities.CAPABILITY_ITEM_BAUBLE;
                    }

                    @Override
                    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
                        return capability == BaublesCapabilities.CAPABILITY_ITEM_BAUBLE
                                ? BaublesCapabilities.CAPABILITY_ITEM_BAUBLE.cast(RING) : null;
                    }
                });
            }
        }
    }
}
