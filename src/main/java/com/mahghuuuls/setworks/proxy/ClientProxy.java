package com.mahghuuuls.setworks.proxy;

import com.mahghuuuls.setworks.client.ClientConfig;
import com.mahghuuuls.setworks.client.ClientStore;
import com.mahghuuuls.setworks.client.TooltipHandler;
import com.mahghuuuls.setworks.network.DisplaySnapshot;
import com.mahghuuuls.setworks.network.DisplayState;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.io.File;

/** Client-side setup: the client settings, the tooltip handler, and the message delivery onto the client thread. */
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(File configFile) {
        ClientConfig.load(configFile);
    }

    @Override
    public void init() {
        MinecraftForge.EVENT_BUS.register(new TooltipHandler());
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void receiveSnapshot(final DisplaySnapshot snapshot) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                ClientStore.get().acceptSnapshot(snapshot);
            }
        });
    }

    @Override
    public void receiveState(final DisplayState state) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                ClientStore.get().acceptState(state);
            }
        });
    }

    @Override
    public void receiveDiscovery(final DisplaySnapshot added) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                ClientStore.get().acceptDiscovery(added);
            }
        });
    }

    /** Leaving a server forgets its data, so the next server's snapshot starts clean. */
    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                ClientStore.get().clear();
            }
        });
    }
}
