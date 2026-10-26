package com.mahghuuuls.setworks.network;

import com.mahghuuuls.setworks.api.Visibility;
import com.mahghuuuls.setworks.api.definition.BonusDefinition;
import com.mahghuuuls.setworks.core.definition.FrozenDefinitions;
import com.mahghuuuls.setworks.core.discovery.DiscoveryCapabilities;
import com.mahghuuuls.setworks.core.discovery.DiscoveryCapability;
import com.mahghuuuls.setworks.core.eval.DiscoverySink;
import com.mahghuuuls.setworks.core.diagnostic.DiagnosticLog;
import com.mahghuuuls.setworks.core.eval.PlayerSetState;
import com.mahghuuuls.setworks.core.eval.StateSink;
import com.mahghuuuls.setworks.core.match.SlotCandidate;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The server side of the sync: sends the display snapshot when a player joins and the player's own
 * state after each evaluation that changed what the client shows, and the display data of a bonus
 * the moment the player discovers it. A player who discovered nothing gets the visible-only snapshot,
 * built once per server start; a player with discoveries gets one built for them at join. A state
 * message names only bonuses the player may see (visible, or discovered by that player), so a hidden
 * or undiscovered bonus never reaches the client even while it is active. Runs on the server only; it
 * reads the evaluation state and the discovery record directly.
 */
public final class ServerStateSink implements StateSink, DiscoverySink {

    private final FrozenDefinitions definitions;
    private final SnapshotBuilder builder;
    private final DiagnosticLog diagnostic;
    private DisplaySnapshot visibleOnly;
    private Set<String> visibleIds;

    public ServerStateSink(FrozenDefinitions definitions, SnapshotBuilder builder, DiagnosticLog diagnostic) {
        this.definitions = definitions;
        this.builder = builder;
        this.diagnostic = diagnostic;
    }

    /** Builds the visible-only snapshot now, so a definition that cannot be displayed fails at load complete, not at the first login. */
    public void prepare() {
        visibleOnly = builder.build(definitions, SnapshotBuilder.VISIBLE_ONLY);
        visibleIds = visibleOnly.bonusIds();
    }

    @Override
    public void onJoin(EntityPlayerMP player) {
        DisplaySnapshot snapshot = snapshotFor(player);
        SetworksNetwork.SnapshotMessage message = new SetworksNetwork.SnapshotMessage(snapshot);
        SetworksNetwork.CHANNEL.sendTo(message, player);
        if (diagnostic.isEnabled()) {
            ByteBuf measure = Unpooled.buffer();
            try {
                snapshot.write(measure);
                diagnostic.line("snapshot player=" + player.getName() + " sets=" + snapshot.sets().size()
                        + " bonuses=" + snapshot.bonuses().size() + " bytes=" + measure.readableBytes() + " ids=" + snapshot.bonusIds());
            } finally {
                measure.release();
            }
        }
    }

    @Override
    public void onState(EntityPlayerMP player, PlayerSetState state) {
        SetworksNetwork.CHANNEL.sendTo(new SetworksNetwork.StateMessage(toDisplay(state, shownBonusIds(player))), player);
    }

    @Override
    public void onDiscovered(EntityPlayerMP player, List<String> bonusIds) {
        DisplaySnapshot added = builder.bonusesOnly(definitions, bonusIds);
        if (!added.bonuses().isEmpty()) {
            SetworksNetwork.CHANNEL.sendTo(new SetworksNetwork.DiscoveryMessage(added), player);
        }
    }

    /** The visible-only snapshot for a player without discoveries, otherwise one built for the player. */
    private DisplaySnapshot snapshotFor(EntityPlayerMP player) {
        if (visibleOnly == null) {
            prepare();
        }
        Set<String> discovered = discoveredIds(player);
        if (discovered.isEmpty()) {
            return visibleOnly;
        }
        return builder.build(definitions, SnapshotBuilder.forPlayer(discovered));
    }

    private static Set<String> discoveredIds(EntityPlayerMP player) {
        DiscoveryCapability record = DiscoveryCapabilities.of(player);
        return record == null ? Collections.<String>emptySet() : record.all();
    }

    /** The ids a state message may name for this player: every visible bonus plus the discovered ones in the player's record. */
    Set<String> shownBonusIds(EntityPlayerMP player) {
        if (visibleOnly == null) {
            prepare();
        }
        return shownBonusIds(visibleIds, discoveredIds(player), definitions);
    }

    static Set<String> shownBonusIds(Set<String> visible, Set<String> discovered, FrozenDefinitions definitions) {
        if (discovered.isEmpty()) {
            return visible;
        }
        Set<String> shown = new LinkedHashSet<String>(visible);
        for (String id : discovered) {
            BonusDefinition bonus = definitions.bonus(id);
            if (bonus != null && bonus.visibility() == Visibility.DISCOVERED) {
                shown.add(id);
            }
        }
        return shown;
    }

    /** The displayed part of a state: piece names per set and the active bonus ids among {@code shownBonuses}. */
    public static DisplayState toDisplay(PlayerSetState state, Set<String> shownBonuses) {
        Map<String, List<String>> pieces = new LinkedHashMap<String, List<String>>();
        for (Map.Entry<String, PlayerSetState.SetState> set : state.sets().entrySet()) {
            List<String> names = new ArrayList<String>();
            for (Map.Entry<String, SlotCandidate> piece : set.getValue().satisfiedPieces().entrySet()) {
                names.add(piece.getKey());
            }
            pieces.put(set.getKey(), names);
        }
        Set<String> active = new LinkedHashSet<String>();
        for (Map.Entry<String, PlayerSetState.BonusEntry> bonus : state.bonuses().entrySet()) {
            if (bonus.getValue().isActive() && shownBonuses.contains(bonus.getKey())) {
                active.add(bonus.getKey());
            }
        }
        return new DisplayState(pieces, active);
    }
}
