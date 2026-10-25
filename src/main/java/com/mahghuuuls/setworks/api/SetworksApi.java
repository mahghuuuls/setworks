package com.mahghuuuls.setworks.api;

import com.mahghuuuls.setworks.api.definition.BonusDefinition;
import com.mahghuuuls.setworks.api.definition.SetDefinition;

import java.util.List;

/**
 * Public entry point for mods and scripts. Registration is accepted during game startup only; after
 * the definitions freeze every call returns an error result and nothing changes. Calls are recorded
 * in order; modifications, replacements, and disables are applied after all registrations.
 */
public final class SetworksApi {

    /** The service behind the static entry. Installed once by the mod at construction (FMLConstructionEvent). */
    public interface Registrar {
        RegistrationResult registerSet(SetDefinition set, DefinitionSource source, boolean replace);

        RegistrationResult registerBonus(BonusDefinition bonus, DefinitionSource source, boolean replace);

        RegistrationResult disableSet(String setId, DefinitionSource source);

        RegistrationResult disableBonus(String bonusId, DefinitionSource source);

        RegistrationResult modifyBonus(String bonusId, BonusModification modification, DefinitionSource source);

        RegistrationResult addAlternatives(String setId, String pieceName, List<ItemFilter> filters, DefinitionSource source);

        RegistrationResult registerLocation(ILocation location);

        RegistrationResult registerEffectKind(IEffectKind kind);

        RegistrationResult registerConditionKind(IConditionKind kind);

        boolean isFrozen();
    }

    private static volatile Registrar registrar;

    private SetworksApi() {
    }

    /** Called once by Setworks itself at mod construction. A second call is ignored. */
    public static void install(Registrar service) {
        if (registrar == null && service != null) {
            registrar = service;
        }
    }

    public static RegistrationResult registerSet(SetDefinition set, DefinitionSource source) {
        return service().registerSet(set, source, false);
    }

    public static RegistrationResult registerSet(SetBuilder builder, DefinitionSource source) {
        return registerSet(builder.build(), source);
    }

    /** Replaces an existing set with the same id, or registers it when none exists. */
    public static RegistrationResult replaceSet(SetDefinition set, DefinitionSource source) {
        return service().registerSet(set, source, true);
    }

    public static RegistrationResult registerBonus(BonusDefinition bonus, DefinitionSource source) {
        return service().registerBonus(bonus, source, false);
    }

    public static RegistrationResult registerBonus(BonusBuilder builder, DefinitionSource source) {
        return registerBonus(builder.build(), source);
    }

    public static RegistrationResult replaceBonus(BonusDefinition bonus, DefinitionSource source) {
        return service().registerBonus(bonus, source, true);
    }

    public static RegistrationResult disableSet(String setId, DefinitionSource source) {
        return service().disableSet(setId, source);
    }

    public static RegistrationResult disableBonus(String bonusId, DefinitionSource source) {
        return service().disableBonus(bonusId, source);
    }

    public static RegistrationResult modifyBonus(String bonusId, BonusModification modification, DefinitionSource source) {
        return service().modifyBonus(bonusId, modification, source);
    }

    public static RegistrationResult addAlternatives(String setId, String pieceName, List<ItemFilter> filters,
                                                     DefinitionSource source) {
        return service().addAlternatives(setId, pieceName, filters, source);
    }

    /** Registers an equipment location. Addons call this during startup. */
    public static RegistrationResult registerLocation(ILocation location) {
        return service().registerLocation(location);
    }

    /** Registers an effect kind. Addons call this during startup, before the definitions freeze at load complete. */
    public static RegistrationResult registerEffectKind(IEffectKind kind) {
        return service().registerEffectKind(kind);
    }

    /** Registers a condition kind. Addons call this during startup, before the definitions freeze at load complete. */
    public static RegistrationResult registerConditionKind(IConditionKind kind) {
        return service().registerConditionKind(kind);
    }

    private static volatile ISetworksQuery query;

    /** Called once by Setworks itself when evaluation is ready. */
    public static void installQuery(ISetworksQuery service) {
        if (query == null && service != null) {
            query = service;
        }
    }

    /** Read-only state queries. Before Setworks finishes loading, every answer is "nothing". */
    public static ISetworksQuery query() {
        ISetworksQuery current = query;
        return current == null ? NO_QUERY : current;
    }

    private static final ISetworksQuery NO_QUERY = new ISetworksQuery() {
        @Override
        public int pieceCount(net.minecraft.entity.player.EntityPlayer player, String setId) {
            return 0;
        }

        @Override
        public boolean isActive(net.minecraft.entity.player.EntityPlayer player, String bonusId) {
            return false;
        }

        @Override
        public BonusState bonusState(net.minecraft.entity.player.EntityPlayer player, String bonusId) {
            return BonusState.MISSING_EQUIPMENT;
        }

        @Override
        public List<String> setsOf(net.minecraft.item.ItemStack stack) {
            return java.util.Collections.emptyList();
        }

        @Override
        public java.util.Map<String, String> satisfiedPieces(net.minecraft.entity.player.EntityPlayer player, String setId) {
            return java.util.Collections.emptyMap();
        }
    };

    /** Where listeners go once evaluation runs; before that they wait in {@link #earlyListeners}. */
    public interface ListenerRegistry {
        void add(BonusListener listener);

        void remove(BonusListener listener);
    }

    private static volatile ListenerRegistry listeners;
    private static final List<BonusListener> earlyListeners = new java.util.concurrent.CopyOnWriteArrayList<BonusListener>();

    /** Receive bonus transitions. May be called at any time; listeners added before load complete are kept. */
    public static void addBonusListener(BonusListener listener) {
        if (listener == null) {
            return;
        }
        ListenerRegistry current = listeners;
        if (current != null) {
            current.add(listener);
        } else if (!earlyListeners.contains(listener)) {
            earlyListeners.add(listener);
        }
    }

    public static void removeBonusListener(BonusListener listener) {
        ListenerRegistry current = listeners;
        if (current != null) {
            current.remove(listener);
        }
        earlyListeners.remove(listener);
    }

    /** Called once by Setworks itself. */
    public static void installListeners(ListenerRegistry registry) {
        if (listeners == null && registry != null) {
            listeners = registry;
        }
    }

    /** Called once by Setworks itself, right before {@link #installListeners}. */
    public static List<BonusListener> drainEarlyListeners() {
        List<BonusListener> copy = new java.util.ArrayList<BonusListener>(earlyListeners);
        earlyListeners.clear();
        return copy;
    }

    /** True once startup completed and definitions are final. */
    public static boolean isFrozen() {
        Registrar current = registrar;
        return current != null && current.isFrozen();
    }

    private static Registrar service() {
        Registrar current = registrar;
        if (current == null) {
            return NOT_LOADED;
        }
        return current;
    }

    /** Answers every call with an error until Setworks constructs; a caller never gets an exception. */
    private static final Registrar NOT_LOADED = new Registrar() {
        private RegistrationResult notLoaded() {
            return RegistrationResult.error("Setworks is not constructed yet; declare after:setworks in your mod dependencies");
        }

        @Override
        public RegistrationResult registerSet(SetDefinition set, DefinitionSource source, boolean replace) {
            return notLoaded();
        }

        @Override
        public RegistrationResult registerBonus(BonusDefinition bonus, DefinitionSource source, boolean replace) {
            return notLoaded();
        }

        @Override
        public RegistrationResult disableSet(String setId, DefinitionSource source) {
            return notLoaded();
        }

        @Override
        public RegistrationResult disableBonus(String bonusId, DefinitionSource source) {
            return notLoaded();
        }

        @Override
        public RegistrationResult modifyBonus(String bonusId, BonusModification modification, DefinitionSource source) {
            return notLoaded();
        }

        @Override
        public RegistrationResult addAlternatives(String setId, String pieceName, List<ItemFilter> filters, DefinitionSource source) {
            return notLoaded();
        }

        @Override
        public RegistrationResult registerLocation(ILocation location) {
            return notLoaded();
        }

        @Override
        public RegistrationResult registerEffectKind(IEffectKind kind) {
            return notLoaded();
        }

        @Override
        public RegistrationResult registerConditionKind(IConditionKind kind) {
            return notLoaded();
        }

        @Override
        public boolean isFrozen() {
            return false;
        }
    };
}
