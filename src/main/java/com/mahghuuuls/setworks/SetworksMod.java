package com.mahghuuuls.setworks;

import com.mahghuuuls.setworks.api.SetworksApi;
import com.mahghuuuls.setworks.command.SetworksCommand;
import com.mahghuuuls.setworks.config.SetworksConfig;
import com.mahghuuuls.setworks.api.definition.SetDefinition;
import com.mahghuuuls.setworks.core.definition.FrozenDefinitions;
import com.mahghuuuls.setworks.core.definition.ValidationReport;
import com.mahghuuuls.setworks.core.discovery.DiscoveryCapabilities;
import com.mahghuuuls.setworks.core.location.VanillaLocations;
import com.mahghuuuls.setworks.examples.ExampleSets;
import com.mahghuuuls.setworks.network.SetworksNetwork;
import com.mahghuuuls.setworks.proxy.CommonProxy;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Mod entry point. Optional integrations are declared with inclusive minimums and no upper bound;
 * nothing is required beyond Forge. Definitions are collected from pre-initialization to load
 * complete, then frozen and validated once.
 */
@Mod(
        modid = Tags.MOD_ID,
        name = Tags.MOD_NAME,
        version = Tags.VERSION,
        acceptedMinecraftVersions = "[1.12.2]",
        dependencies = "after:crafttweaker@[4.1.20,);after:baubles@[1.5.2,);after:jei"
)
public class SetworksMod {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    @SidedProxy(clientSide = "com.mahghuuuls.setworks.proxy.ClientProxy", serverSide = "com.mahghuuuls.setworks.proxy.CommonProxy")
    public static CommonProxy proxy;

    private static SetworksRuntime runtime;

    public static SetworksRuntime runtime() {
        return runtime;
    }

    @Mod.EventHandler
    public void construct(FMLConstructionEvent event) {
        runtime = new SetworksRuntime();
        SetworksApi.install(runtime);
        VanillaLocations.registerAll(runtime.locations());
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        SetworksConfig config = SetworksConfig.load(event.getSuggestedConfigurationFile(), LOGGER);
        runtime.applyConfig(config);
        runtime.registerBuiltinKinds();
        SetworksNetwork.register();
        DiscoveryCapabilities.register();
        if (config.enableExampleSets()) {
            ExampleSets.register();
        }
        proxy.preInit(event.getSuggestedConfigurationFile());
        LOGGER.info("{} {} loading (strictValidation={}, conditionIntervalTicks={}, enableExampleSets={}, diagnosticLog={})",
                Tags.MOD_NAME, Tags.VERSION, config.strictValidation(), config.conditionIntervalTicks(),
                config.enableExampleSets(), config.diagnosticLog());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new DiscoveryCapabilities.Events());
        if (Loader.isModLoaded("baubles")) {
            // The Baubles classes are loaded only here, so a game without Baubles never touches them.
            com.mahghuuuls.setworks.compat.baubles.BaublesLocations.register(LOGGER);
            if (Boolean.getBoolean(ExampleSets.INVALID_EXAMPLE_PROPERTY)) {
                MinecraftForge.EVENT_BUS.register(com.mahghuuuls.setworks.compat.baubles.DevBaubleFixture.handler());
            }
        }
        proxy.init();
    }

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        if (Loader.isModLoaded("crafttweaker")) {
            // The bridge class is loaded only here, so a game without CraftTweaker never touches it.
            com.mahghuuuls.setworks.compat.crafttweaker.CraftTweakerBridge.loadScriptsAndApply(LOGGER);
        }
        runtime.locations().freeze();
        runtime.effectKinds().freeze();
        runtime.conditionKinds().freeze();
        ValidationReport report = runtime.registry().freeze(runtime.validationContext());
        FrozenDefinitions definitions = runtime.registry().definitions();
        int pieces = 0;
        java.util.Set<String> sources = new java.util.HashSet<String>();
        for (FrozenDefinitions.Entry<SetDefinition> entry : definitions.allSets()) {
            if (entry.enabled()) {
                pieces += entry.definition().pieces().size();
            }
            sources.add(entry.source().describe());
        }
        for (ValidationReport.Error error : report.errors()) {
            LOGGER.error("definition error: {}", error.describe());
        }
        for (String failure : report.scriptFailures()) {
            LOGGER.error("script failure: {}", failure);
        }
        if (!report.disabledSets().isEmpty()) {
            LOGGER.warn("disabled sets: {}", report.disabledSets());
        }
        runtime.diagnostic().loadSummary(definitions.enabledSetCount(), definitions.allSets().size(),
                definitions.enabledBonusCount(), definitions.allBonuses().size(), pieces,
                report.errors().size(), report.scriptFailures().size(), runtime.locations().all().size(), sources.size());
        if (report.hasProblems() && runtime.config().strictValidation()) {
            StringBuilder message = new StringBuilder("Setworks strict validation failed: ")
                    .append(report.errors().size()).append(" definition error(s) and ")
                    .append(report.scriptFailures().size())
                    .append(" script failure(s). Turn strictValidation off to start with the invalid sets disabled.");
            int shown = 0;
            for (String line : report.lines()) {
                if (shown++ == 5) {
                    message.append("\n  ...");
                    break;
                }
                message.append("\n  ").append(line);
            }
            throw new IllegalStateException(message.toString());
        }
        runtime.startEvaluation();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new SetworksCommand());
    }
}
