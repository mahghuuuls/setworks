package com.mahghuuuls.setworks;

import com.mahghuuuls.setworks.api.BonusModification;
import com.mahghuuuls.setworks.api.DefinitionSource;
import com.mahghuuuls.setworks.api.ILocation;
import com.mahghuuuls.setworks.api.ItemFilter;
import com.mahghuuuls.setworks.api.RegistrationResult;
import com.mahghuuuls.setworks.api.SetworksApi;
import com.mahghuuuls.setworks.api.definition.BonusDefinition;
import com.mahghuuuls.setworks.api.definition.SetDefinition;
import com.mahghuuuls.setworks.config.SetworksConfig;
import com.mahghuuuls.setworks.core.definition.DefinitionRegistry;
import com.mahghuuuls.setworks.core.definition.ValidationContext;
import com.mahghuuuls.setworks.core.diagnostic.DiagnosticLog;
import com.mahghuuuls.setworks.core.diagnostic.Timings;
import com.mahghuuuls.setworks.core.eval.ConditionEvaluator;
import com.mahghuuuls.setworks.api.BonusListener;
import com.mahghuuuls.setworks.core.eval.Evaluator;
import com.mahghuuuls.setworks.core.eval.ListenerDispatch;
import com.mahghuuuls.setworks.core.eval.PlayerStateTracker;
import com.mahghuuuls.setworks.core.eval.QueryService;
import com.mahghuuuls.setworks.core.location.LocationRegistry;
import com.mahghuuuls.setworks.core.match.CompiledDefinitions;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

import java.util.List;

/**
 * The mod's live objects, created once at pre-initialization. Implements the public API's
 * registrar by forwarding to the registries, and supplies the validation context from the game.
 */
public final class SetworksRuntime implements SetworksApi.Registrar {

    private final DefinitionRegistry registry = new DefinitionRegistry();
    private final LocationRegistry locations = new LocationRegistry();
    private volatile SetworksConfig config = SetworksConfig.defaults();
    private volatile DiagnosticLog diagnostic = new DiagnosticLog(SetworksMod.LOGGER, false);

    /** Created at mod construction so the API accepts registrations from any mod's pre-init. */
    public SetworksRuntime() {
    }

    /** Applies the loaded configuration; called once at pre-initialization before anything reads it. */
    public void applyConfig(SetworksConfig loaded) {
        this.config = loaded;
        this.diagnostic = new DiagnosticLog(SetworksMod.LOGGER, loaded.diagnosticLog());
    }

    public SetworksConfig config() {
        return config;
    }

    public DefinitionRegistry registry() {
        return registry;
    }

    public LocationRegistry locations() {
        return locations;
    }

    public DiagnosticLog diagnostic() {
        return diagnostic;
    }

    private final Timings timings = new Timings();
    private volatile CompiledDefinitions compiled = CompiledDefinitions.empty();
    private volatile PlayerStateTracker tracker;

    public CompiledDefinitions compiled() {
        return compiled;
    }

    /** The tracker, or {@code null} before load complete. */
    public PlayerStateTracker tracker() {
        return tracker;
    }

    public Timings timings() {
        return timings;
    }

    /**
     * Compiles the frozen definitions and starts evaluation: called once at load complete after the
     * freeze. Registers the tracker on the Forge bus and installs the query service.
     */
    public void startEvaluation() {
        compiled = new CompiledDefinitions(registry.definitions());
        Evaluator evaluator = new Evaluator(compiled, ConditionEvaluator.NONE, config.conditionIntervalTicks());
        tracker = new PlayerStateTracker(evaluator, locations, diagnostic, timings);
        ListenerDispatch dispatch = new ListenerDispatch(compiled, SetworksMod.LOGGER);
        for (BonusListener listener : SetworksApi.drainEarlyListeners()) {
            dispatch.add(listener);
        }
        SetworksApi.installListeners(dispatch);
        tracker.addSink(dispatch);
        MinecraftForge.EVENT_BUS.register(tracker);
        SetworksApi.installQuery(new QueryService(tracker, compiled));
    }

    /** The context validation uses against the real game. Effect and condition kinds are registered by later work. */
    public ValidationContext validationContext() {
        return new ValidationContext() {
            @Override
            public boolean itemExists(String itemId) {
                ResourceLocation key = tryParse(itemId);
                return key != null && ForgeRegistries.ITEMS.containsKey(key);
            }

            @Override
            public boolean oreHasEntries(String oreName) {
                return OreDictionary.doesOreNameExist(oreName) && !OreDictionary.getOres(oreName, false).isEmpty();
            }

            @Override
            public boolean locationExists(String locationId) {
                return locations.exists(locationId);
            }

            @Override
            public boolean effectKindExists(String kindId) {
                return false;
            }

            @Override
            public boolean conditionKindExists(String kindId) {
                return false;
            }

            @Override
            public boolean exactNbtParses(String text) {
                try {
                    net.minecraft.nbt.JsonToNBT.getTagFromJson(text);
                    return true;
                } catch (net.minecraft.nbt.NBTException e) {
                    return false;
                }
            }

        };
    }

    private static ResourceLocation tryParse(String id) {
        try {
            return new ResourceLocation(id);
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ---- SetworksApi.Registrar ----

    @Override
    public RegistrationResult registerSet(SetDefinition set, DefinitionSource source, boolean replace) {
        return logged(registry.registerSet(set, source, replace), source);
    }

    @Override
    public RegistrationResult registerBonus(BonusDefinition bonus, DefinitionSource source, boolean replace) {
        return logged(registry.registerBonus(bonus, source, replace), source);
    }

    @Override
    public RegistrationResult disableSet(String setId, DefinitionSource source) {
        return logged(registry.disableSet(setId, source), source);
    }

    @Override
    public RegistrationResult disableBonus(String bonusId, DefinitionSource source) {
        return logged(registry.disableBonus(bonusId, source), source);
    }

    @Override
    public RegistrationResult modifyBonus(String bonusId, BonusModification modification, DefinitionSource source) {
        return logged(registry.modifyBonus(bonusId, modification, source), source);
    }

    @Override
    public RegistrationResult addAlternatives(String setId, String pieceName, List<ItemFilter> filters, DefinitionSource source) {
        return logged(registry.addAlternatives(setId, pieceName, filters, source), source);
    }

    @Override
    public RegistrationResult registerLocation(ILocation location) {
        return logged(locations.register(location), location == null ? null : location.source());
    }

    @Override
    public boolean isFrozen() {
        return registry.isFrozen();
    }

    private static RegistrationResult logged(RegistrationResult result, DefinitionSource source) {
        if (!result.isOk()) {
            SetworksMod.LOGGER.warn("registration rejected ({}): {}", source == null ? "unknown source" : source.describe(), result.message());
        }
        return result;
    }
}
