package com.mahghuuuls.setworks.core.eval;

import com.mahghuuuls.setworks.api.ILocation;
import com.mahghuuuls.setworks.core.diagnostic.DiagnosticLog;
import com.mahghuuuls.setworks.core.discovery.DiscoveryCapabilities;
import com.mahghuuuls.setworks.core.diagnostic.Timings;
import com.mahghuuuls.setworks.core.location.LocationRegistry;
import com.mahghuuuls.setworks.core.location.LocationView;
import com.mahghuuuls.setworks.core.location.VanillaLocations;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps one {@link PlayerSetState} per online player, keyed by UUID, and drives the evaluator from
 * the server lifecycle: create at login, rebuild at respawn and dimension change, discard at
 * logout, record the slot on an equipment change, scan the referenced locations each tick,
 * evaluate when a location changed, a flag changed, or a condition is due. Fake players are
 * ignored. Results go to the registered sinks and to the diagnostic log; the tracker itself posts
 * nothing.
 */
public final class PlayerStateTracker {

    private static final class Entry {
        PlayerSetState state;
        final LocationView view;
        final Set<String> pending = new LinkedHashSet<String>();
        long nextConditionTick = Long.MAX_VALUE;
        boolean lastSpectator;
        boolean lastDead;

        Entry(PlayerSetState state, LocationView view) {
            this.state = state;
            this.view = view;
        }
    }

    private final Evaluator evaluator;
    private final List<ILocation> referencedLocations;
    private final DiagnosticLog diagnostic;
    private final Timings timings;
    private final List<TransitionSink> sinks = new ArrayList<TransitionSink>();
    private StateSink stateSink;
    private DiscoverySink discoverySink;
    private final Map<UUID, Entry> entries = new HashMap<UUID, Entry>();
    private long serverTicks;

    public PlayerStateTracker(Evaluator evaluator, LocationRegistry locations, DiagnosticLog diagnostic, Timings timings) {
        this.evaluator = evaluator;
        this.diagnostic = diagnostic;
        this.timings = timings;
        List<ILocation> referenced = new ArrayList<ILocation>();
        for (String id : evaluator.definitions().referencedLocations()) {
            ILocation location = locations.get(id);
            if (location != null) {
                referenced.add(location);
            }
        }
        this.referencedLocations = referenced;
    }

    public void addSink(TransitionSink sink) {
        sinks.add(sink);
    }

    /** The one receiver of what the client must be told; {@code null} sends nothing. */
    public void setStateSink(StateSink sink) {
        this.stateSink = sink;
    }

    /** Told about first activations of discovered bonuses; {@code null} tells nobody. */
    public void setDiscoverySink(DiscoverySink sink) {
        this.discoverySink = sink;
    }

    /** The last evaluated state, or {@code null} for an unknown player. */
    public PlayerSetState stateOf(UUID playerId) {
        Entry entry = entries.get(playerId);
        return entry == null ? null : entry.state;
    }

    public Evaluator evaluator() {
        return evaluator;
    }

    // ---- lifecycle ----

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        EntityPlayerMP player = serverPlayer(event.player);
        if (player == null) {
            return;
        }
        Entry entry = new Entry(PlayerSetState.empty(player.getUniqueID(), player.getName()), new LocationView(referencedLocations));
        entries.put(player.getUniqueID(), entry);
        if (stateSink != null) {
            stateSink.onJoin(player);
        }
        rebuildEvaluate(player, entry);
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        EntityPlayerMP player = serverPlayer(event.player);
        if (player == null) {
            return;
        }
        Entry entry = entries.remove(player.getUniqueID());
        if (entry == null) {
            return;
        }
        for (TransitionSink sink : sinks) {
            sink.onLogout(player, entry.state);
        }
        for (Map.Entry<String, PlayerSetState.BonusEntry> bonus : entry.state.bonuses().entrySet()) {
            if (bonus.getValue().isActive()) {
                diagnostic.transition(player.getName(), bonus.getKey(), "inactive", "logout", player.world.getTotalWorldTime());
            }
        }
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        rebuild(event.player, "respawn");
    }

    @SubscribeEvent
    public void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        rebuild(event.player, "dimension change");
    }

    /** Fast path: the changed equipment slot is recounted next tick even when the scan sees the same item and metadata. */
    @SubscribeEvent
    public void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = serverPlayer((EntityPlayerMP) event.getEntityLiving());
        if (player == null) {
            return;
        }
        Entry entry = entries.get(player.getUniqueID());
        if (entry != null) {
            entry.pending.add(locationOf(event.getSlot()));
        }
    }

    static String locationOf(EntityEquipmentSlot slot) {
        switch (slot) {
            case HEAD:
                return VanillaLocations.HEAD;
            case CHEST:
                return VanillaLocations.CHEST;
            case LEGS:
                return VanillaLocations.LEGS;
            case FEET:
                return VanillaLocations.FEET;
            case OFFHAND:
                return VanillaLocations.OFFHAND;
            default:
                return VanillaLocations.MAINHAND;
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !event.side.isServer()) {
            return;
        }
        EntityPlayerMP player = serverPlayer(event.player);
        if (player == null) {
            return;
        }
        Entry entry = entries.get(player.getUniqueID());
        if (entry == null) {
            return;
        }
        boolean timed = diagnostic.isEnabled();
        long start = timed ? System.nanoTime() : 0L;
        Set<String> changed = entry.view.scan(player);
        if (timed) {
            timings.scan(System.nanoTime() - start);
        }
        long tick = player.world.getTotalWorldTime();
        boolean spectator = player.isSpectator();
        boolean dead = !player.isEntityAlive();
        boolean flagsChanged = spectator != entry.lastSpectator || dead != entry.lastDead;
        if (changed.isEmpty() && entry.pending.isEmpty() && tick < entry.nextConditionTick && !flagsChanged) {
            return;
        }
        Set<String> all = changed;
        if (!entry.pending.isEmpty()) {
            all = new LinkedHashSet<String>(changed);
            all.addAll(entry.pending);
            entry.pending.clear();
        }
        evaluate(player, entry, all, false, timed);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        serverTicks++;
        if (serverTicks % Timings.SUMMARY_INTERVAL_TICKS == 0 && diagnostic.isEnabled()) {
            diagnostic.line(timings.summaryAndReset(entries.size()));
        }
    }

    // ---- evaluation ----

    private void rebuild(EntityPlayer who, String reason) {
        EntityPlayerMP player = serverPlayer(who);
        if (player == null) {
            return;
        }
        Entry entry = entries.get(player.getUniqueID());
        if (entry == null) {
            entry = new Entry(PlayerSetState.empty(player.getUniqueID(), player.getName()), new LocationView(referencedLocations));
            entries.put(player.getUniqueID(), entry);
        } else {
            // The old entity's effects are gone (respawn) or still on the same entity (dimension change);
            // the sinks are told before the evaluation so the effect ledger can reconcile and re-apply.
            entry.state = PlayerSetState.empty(player.getUniqueID(), player.getName());
            entry.view.reset();
            entry.pending.clear();
        }
        diagnostic.line("rebuild player=" + player.getName() + " reason=" + reason);
        rebuildEvaluate(player, entry);
    }

    private void rebuildEvaluate(EntityPlayerMP player, Entry entry) {
        for (TransitionSink sink : sinks) {
            sink.onRebuild(player);
        }
        entry.view.scan(player);
        evaluate(player, entry, Collections.<String>emptySet(), true, diagnostic.isEnabled());
    }

    private void evaluate(EntityPlayerMP player, Entry entry, Set<String> changed, boolean rebuild, boolean timed) {
        long start = timed ? System.nanoTime() : 0L;
        boolean spectator = player.isSpectator();
        boolean dead = !player.isEntityAlive();
        EvaluationInput input = new EvaluationInput(entry.view.candidates(), changed, rebuild,
                player.world.getTotalWorldTime(), spectator, dead, player);
        PlayerSetState before = entry.state;
        Evaluator.Result result = evaluator.evaluate(entry.state, input);
        if (timed) {
            long nanos = System.nanoTime() - start;
            if (rebuild) {
                timings.rebuild(nanos);
            } else {
                timings.evaluate(nanos);
            }
        }
        entry.state = result.state();
        entry.lastSpectator = spectator;
        entry.lastDead = dead;
        entry.nextConditionTick = evaluator.nextConditionDue(result.state(), input.tick());
        boolean displayChanged = stateSink != null && !PlayerSetState.sameDisplay(before, result.state());
        if (result.transitions().isEmpty()) {
            if (displayChanged) {
                stateSink.onState(player, result.state());
            }
            return;
        }
        for (Transition transition : result.transitions()) {
            diagnostic.transition(player.getName(), transition.bonusId(),
                    transition.to().isActive() ? "active" : "inactive",
                    transition.to().isActive() ? "requirements met" : transition.to().reasonText()
                            + (transition.detail().isEmpty() ? "" : " " + transition.detail()), input.tick());
        }
        for (TransitionSink sink : sinks) {
            sink.onTransitions(player, result.state(), result.transitions());
        }
        List<String> discovered = DiscoveryRecorder.record(result.transitions(), evaluator.definitions(), DiscoveryCapabilities.of(player));
        for (String bonusId : discovered) {
            diagnostic.line("discovery player=" + player.getName() + " bonus=" + bonusId);
        }
        if (!discovered.isEmpty() && discoverySink != null) {
            discoverySink.onDiscovered(player, discovered);
        }
        if (displayChanged) {
            stateSink.onState(player, result.state());
        }
    }

    /** The server-side player, or {@code null} for a client player, a fake player, or null. */
    private static EntityPlayerMP serverPlayer(EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP) || player instanceof FakePlayer) {
            return null;
        }
        return (EntityPlayerMP) player;
    }
}
