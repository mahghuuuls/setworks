package com.mahghuuuls.setworks.core.discovery;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.concurrent.Callable;

/**
 * Registers the discovery capability, attaches one to every player, and copies it to the new
 * entity on respawn (with or without keepInventory) so discovery travels with the player.
 */
public final class DiscoveryCapabilities {

    public static final ResourceLocation KEY = new ResourceLocation("setworks", "discovery");

    @CapabilityInject(DiscoveryCapability.class)
    public static Capability<DiscoveryCapability> CAPABILITY = null;

    private DiscoveryCapabilities() {
    }

    /** Called once at pre-initialization on both sides. */
    public static void register() {
        CapabilityManager.INSTANCE.register(DiscoveryCapability.class, new Capability.IStorage<DiscoveryCapability>() {
            @Override
            public NBTBase writeNBT(Capability<DiscoveryCapability> capability, DiscoveryCapability instance, EnumFacing side) {
                return instance.writeNBT();
            }

            @Override
            public void readNBT(Capability<DiscoveryCapability> capability, DiscoveryCapability instance, EnumFacing side, NBTBase nbt) {
                instance.readNBT(nbt instanceof NBTTagCompound ? (NBTTagCompound) nbt : null);
            }
        }, new Callable<DiscoveryCapability>() {
            @Override
            public DiscoveryCapability call() {
                return new DiscoveryCapability();
            }
        });
    }

    /** The player's discovery record, or {@code null} before registration or for a player without the capability. */
    public static DiscoveryCapability of(EntityPlayer player) {
        if (player == null || CAPABILITY == null || !player.hasCapability(CAPABILITY, null)) {
            return null;
        }
        return player.getCapability(CAPABILITY, null);
    }

    /** The provider attached to each player; serializes as {@code {Discovered:[...]}} under the mod's key. */
    public static final class Provider implements ICapabilitySerializable<NBTTagCompound> {
        private final DiscoveryCapability instance = new DiscoveryCapability();

        @Override
        public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
            return capability == CAPABILITY;
        }

        @Override
        public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
            return capability == CAPABILITY ? CAPABILITY.cast(instance) : null;
        }

        @Override
        public NBTTagCompound serializeNBT() {
            return instance.writeNBT();
        }

        @Override
        public void deserializeNBT(NBTTagCompound nbt) {
            instance.readNBT(nbt);
        }
    }

    /** Attaches the capability to players and copies it across the respawn clone. Registered on the Forge bus at initialization. */
    public static final class Events {
        @SubscribeEvent
        public void onAttach(AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof EntityPlayer) {
                event.addCapability(KEY, new Provider());
            }
        }

        @SubscribeEvent
        public void onClone(PlayerEvent.Clone event) {
            DiscoveryCapability from = of(event.getOriginal());
            DiscoveryCapability to = of(event.getEntityPlayer());
            if (to != null) {
                to.copyFrom(from);
            }
        }
    }
}
