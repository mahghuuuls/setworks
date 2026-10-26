package com.mahghuuuls.setworks.proxy;

import com.mahghuuuls.setworks.network.DisplaySnapshot;
import com.mahghuuuls.setworks.network.DisplayState;

import java.io.File;

/**
 * Side hook. On a dedicated server nothing here does anything: the client settings are not read,
 * no tooltip handler exists, and a received message (never sent to a server) is dropped.
 */
public class CommonProxy {

    public void preInit(File configFile) {
    }

    public void init() {
    }

    /** A display snapshot arrived; the client proxy stores it on the client thread. */
    public void receiveSnapshot(DisplaySnapshot snapshot) {
    }

    /** The player's own state arrived; the client proxy stores it on the client thread. */
    public void receiveState(DisplayState state) {
    }

    /** Display data of newly discovered bonuses arrived; the client proxy adds it on the client thread. */
    public void receiveDiscovery(DisplaySnapshot added) {
    }
}
