package com.mahghuuuls.setworks;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Mod entry point. Optional integrations are declared with inclusive minimums and no
 * upper bound; nothing is required beyond Forge.
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

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("{} {} loading", Tags.MOD_NAME, Tags.VERSION);
    }
}
