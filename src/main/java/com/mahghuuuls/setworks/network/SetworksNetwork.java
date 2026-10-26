package com.mahghuuuls.setworks.network;

import com.mahghuuuls.setworks.SetworksMod;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * The one channel, {@code setworks}, with three server-to-client messages. The handlers below are
 * created on both sides by Forge, so they hold no client code: they hand the payload to the proxy,
 * which on the client schedules it onto the client thread and on a dedicated server ignores it.
 */
public final class SetworksNetwork {

    public static final String CHANNEL_NAME = "setworks";
    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL_NAME);

    private SetworksNetwork() {
    }

    /** Called once at pre-initialization on both sides. */
    public static void register() {
        CHANNEL.registerMessage(SnapshotMessage.Handler.class, SnapshotMessage.class, 0, Side.CLIENT);
        CHANNEL.registerMessage(StateMessage.Handler.class, StateMessage.class, 1, Side.CLIENT);
        CHANNEL.registerMessage(DiscoveryMessage.Handler.class, DiscoveryMessage.class, 2, Side.CLIENT);
    }

    /** The display snapshot for the receiving player. */
    public static final class SnapshotMessage implements IMessage {
        private DisplaySnapshot snapshot = DisplaySnapshot.empty();

        public SnapshotMessage() {
        }

        public SnapshotMessage(DisplaySnapshot snapshot) {
            this.snapshot = snapshot;
        }

        public DisplaySnapshot snapshot() {
            return snapshot;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            snapshot = DisplaySnapshot.read(buf);
        }

        @Override
        public void toBytes(ByteBuf buf) {
            snapshot.write(buf);
        }

        public static final class Handler implements IMessageHandler<SnapshotMessage, IMessage> {
            @Override
            public IMessage onMessage(SnapshotMessage message, MessageContext context) {
                SetworksMod.proxy.receiveSnapshot(message.snapshot());
                return null;
            }
        }
    }

    /** The display data of bonuses the receiving player just discovered; the client adds them to its snapshot. */
    public static final class DiscoveryMessage implements IMessage {
        private DisplaySnapshot added = DisplaySnapshot.empty();

        public DiscoveryMessage() {
        }

        public DiscoveryMessage(DisplaySnapshot added) {
            this.added = added;
        }

        public DisplaySnapshot added() {
            return added;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            added = DisplaySnapshot.read(buf);
        }

        @Override
        public void toBytes(ByteBuf buf) {
            added.write(buf);
        }

        public static final class Handler implements IMessageHandler<DiscoveryMessage, IMessage> {
            @Override
            public IMessage onMessage(DiscoveryMessage message, MessageContext context) {
                SetworksMod.proxy.receiveDiscovery(message.added());
                return null;
            }
        }
    }

    /** The receiving player's own state. */
    public static final class StateMessage implements IMessage {
        private DisplayState state = DisplayState.empty();

        public StateMessage() {
        }

        public StateMessage(DisplayState state) {
            this.state = state;
        }

        public DisplayState state() {
            return state;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            state = DisplayState.read(buf);
        }

        @Override
        public void toBytes(ByteBuf buf) {
            state.write(buf);
        }

        public static final class Handler implements IMessageHandler<StateMessage, IMessage> {
            @Override
            public IMessage onMessage(StateMessage message, MessageContext context) {
                SetworksMod.proxy.receiveState(message.state());
                return null;
            }
        }
    }
}
