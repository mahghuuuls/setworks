package com.mahghuuuls.setworks;

import com.mahghuuuls.setworks.api.BonusListener;
import com.mahghuuuls.setworks.api.BonusModification;
import com.mahghuuuls.setworks.api.DefinitionSource;
import com.mahghuuuls.setworks.api.IConditionKind;
import com.mahghuuuls.setworks.api.IEffectKind;
import com.mahghuuuls.setworks.api.ILocation;
import com.mahghuuuls.setworks.api.ItemFilter;
import com.mahghuuuls.setworks.api.RegistrationResult;
import com.mahghuuuls.setworks.api.SetworksApi;
import com.mahghuuuls.setworks.api.definition.BonusDefinition;
import com.mahghuuuls.setworks.api.definition.ConditionSpec;
import com.mahghuuuls.setworks.api.definition.EffectSpec;
import com.mahghuuuls.setworks.api.definition.SetDefinition;
import com.mahghuuuls.setworks.command.InspectReport;
import com.mahghuuuls.setworks.config.SetworksConfig;
import com.mahghuuuls.setworks.core.definition.DefinitionRegistry;
import com.mahghuuuls.setworks.core.definition.ValidationContext;
import com.mahghuuuls.setworks.core.diagnostic.DiagnosticLog;
import com.mahghuuuls.setworks.core.diagnostic.Timings;
import com.mahghuuuls.setworks.core.effect.AttributeEffectKind;
import com.mahghuuuls.setworks.core.effect.EffectKindRegistry;
import com.mahghuuuls.setworks.core.effect.EffectLedger;
import com.mahghuuuls.setworks.core.effect.EffectManager;
import com.mahghuuuls.setworks.core.effect.PotionEffectKind;
import com.mahghuuuls.setworks.core.eval.ConditionKindRegistry;
import com.mahghuuuls.setworks.core.eval.EffectSink;
import com.mahghuuuls.setworks.core.eval.Evaluator;
import com.mahghuuuls.setworks.core.eval.KindConditionEvaluator;
import com.mahghuuuls.setworks.core.eval.condition.HealthBelowKind;
import com.mahghuuuls.setworks.core.eval.condition.SneakingKind;
import com.mahghuuuls.setworks.core.eval.ListenerDispatch;
import com.mahghuuuls.setworks.core.eval.PlayerStateTracker;
import com.mahghuuuls.setworks.core.eval.QueryService;
import com.mahghuuuls.setworks.core.location.LocationRegistry;
import com.mahghuuuls.setworks.core.match.CompiledDefinitions;
import com.mahghuuuls.setworks.network.DescriptionGenerator;
import com.mahghuuuls.setworks.network.ServerStateSink;
import com.mahghuuuls.setworks.network.SnapshotBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
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
    private final EffectKindRegistry effectKinds = new EffectKindRegistry();
    private final ConditionKindRegistry conditionKinds = new ConditionKindRegistry();
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

    public EffectKindRegistry effectKinds() {
        return effectKinds;
    }

    public ConditionKindRegistry conditionKinds() {
        return conditionKinds;
    }

    /** The built-in effect and condition kinds; called once at pre-initialization. */
    public void registerBuiltinKinds() {
        effectKinds.register(new AttributeEffectKind());
        effectKinds.register(new PotionEffectKind());
        conditionKinds.register(new HealthBelowKind());
        conditionKinds.register(new SneakingKind());
    }

    public DiagnosticLog diagnostic() {
        return diagnostic;
    }

    private final Timings timings = new Timings();
    private volatile CompiledDefinitions compiled = CompiledDefinitions.empty();
    private volatile PlayerStateTracker tracker;
    private volatile EffectManager effects;
    private volatile KindConditionEvaluator conditions;

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

    /** The effect manager, or {@code null} before load complete. */
    public EffectManager effects() {
        return effects;
    }

    /** The per-bonus effect and condition lines for the inspect command; nothing before load complete. */
    public InspectReport.BonusDetails bonusDetails() {
        final EffectManager currentEffects = effects;
        final KindConditionEvaluator currentConditions = conditions;
        if (currentEffects == null || currentConditions == null) {
            return InspectReport.NO_DETAILS;
        }
        return new InspectReport.BonusDetails() {
            @Override
            public List<String> effectLines(java.util.UUID player, BonusDefinition bonus) {
                net.minecraft.server.MinecraftServer server = net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance();
                net.minecraft.entity.player.EntityPlayerMP entity = server == null ? null : server.getPlayerList().getPlayerByUUID(player);
                return currentEffects.effectLines(player, bonus, entity);
            }

            @Override
            public List<String> conditionLines(BonusDefinition bonus) {
                return currentConditions.conditionLines(bonus);
            }
        };
    }

    /**
     * Compiles the frozen definitions and starts evaluation: called once at load complete after the
     * freeze. Registers the tracker on the Forge bus and installs the query service.
     */
    public void startEvaluation() {
        compiled = new CompiledDefinitions(registry.definitions());
        conditions = new KindConditionEvaluator(compiled, conditionKinds, SetworksMod.LOGGER);
        Evaluator evaluator = new Evaluator(compiled, conditions, config.conditionIntervalTicks());
        tracker = new PlayerStateTracker(evaluator, locations, diagnostic, timings);
        // Effects first, then listeners and events: a listener sees the modifier already in place.
        effects = new EffectManager(new EffectLedger(registry.definitions()), effectKinds, SetworksMod.LOGGER, diagnostic);
        tracker.addSink(new EffectSink(effects));
        MinecraftForge.EVENT_BUS.register(effects);
        ListenerDispatch dispatch = new ListenerDispatch(compiled, SetworksMod.LOGGER);
        for (BonusListener listener : SetworksApi.drainEarlyListeners()) {
            dispatch.add(listener);
        }
        SetworksApi.installListeners(dispatch);
        tracker.addSink(dispatch);
        ServerStateSink stateSink = new ServerStateSink(registry.definitions(), snapshotBuilder(), diagnostic);
        stateSink.prepare();
        tracker.setStateSink(stateSink);
        tracker.setDiscoverySink(stateSink);
        MinecraftForge.EVENT_BUS.register(tracker);
        SetworksApi.installQuery(new QueryService(tracker, compiled));
    }

    /** Builds display snapshots against the real registries: item translation keys, potion names, addon describe texts. */
    private SnapshotBuilder snapshotBuilder() {
        DescriptionGenerator descriptions = new DescriptionGenerator(new DescriptionGenerator.PotionNames() {
            @Override
            public String translationKey(String potionId) {
                ResourceLocation key = tryParse(potionId);
                Potion potion = key == null ? null : ForgeRegistries.POTIONS.getValue(key);
                return potion == null ? null : potion.getName();
            }
        }, new DescriptionGenerator.Kinds() {
            @Override
            public IEffectKind kind(String kindId) {
                return effectKinds.get(kindId);
            }
        });
        return new SnapshotBuilder(descriptions, new SnapshotBuilder.Labels() {
            @Override
            public String itemLabel(String itemId, int metadata) {
                ResourceLocation key = tryParse(itemId);
                Item item = key == null ? null : ForgeRegistries.ITEMS.getValue(key);
                if (item == null) {
                    return null;
                }
                try {
                    return new ItemStack(item, 1, metadata).getTranslationKey() + ".name";
                } catch (RuntimeException e) {
                    return null;
                }
            }
        });
    }

    /** The context validation uses against the real game. */
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
                return effectKinds.exists(kindId);
            }

            @Override
            public String effectProblem(EffectSpec effect) {
                IEffectKind kind = effectKinds.get(effect.kindId());
                if (kind == null) {
                    return null;
                }
                try {
                    return kind.validate(effect.params());
                } catch (RuntimeException e) {
                    return "validation threw " + e;
                }
            }

            @Override
            public boolean conditionKindExists(String kindId) {
                return conditionKinds.exists(kindId);
            }

            @Override
            public String conditionProblem(ConditionSpec condition) {
                IConditionKind kind = conditionKinds.get(condition.kindId());
                if (kind == null) {
                    return null;
                }
                try {
                    return kind.validate(condition.params());
                } catch (RuntimeException e) {
                    return "validation threw " + e;
                }
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
    public RegistrationResult registerEffectKind(IEffectKind kind) {
        return logged(effectKinds.register(kind), null);
    }

    @Override
    public RegistrationResult registerConditionKind(IConditionKind kind) {
        return logged(conditionKinds.register(kind), null);
    }

    @Override
    public void reportScriptFailure(String description) {
        registry.reportScriptFailure(description);
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
