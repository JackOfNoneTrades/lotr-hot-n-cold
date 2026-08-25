package org.fentanylsolutions.hotncold;

import net.minecraftforge.common.MinecraftForge;

import org.fentanylsolutions.hotncold.command.CommandHotNCold;
import org.fentanylsolutions.hotncold.compat.EnviroMineCompat;
import org.fentanylsolutions.hotncold.compat.LOTRSpawnControl;
import org.fentanylsolutions.hotncold.compat.LOTRSpawnGuard;
import org.fentanylsolutions.hotncold.util.BiomeUtil;
import org.fentanylsolutions.hotncold.util.MobUtil;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());

        HotNCold.LOG.info("I am Hot N Cold at version " + Tags.VERSION);
    }

    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(LOTRSpawnGuard.INSTANCE);
    }

    public void postInit(FMLPostInitializationEvent event) {
        HotNCold.rebuildMobLists();
        if (Config.printBiomes) {
            BiomeUtil.printBiomeNames();
        }
        HotNCold.rebuildBiomeLists();
        Config.populateGeneratedEnviromineBiomeTemperaturesIfNeeded();
        HotNCold.rebuildEnviromineBiomeTemperatureOverrides();
        if (Loader.isModLoaded("enviromine")) {
            EnviroMineCompat.applyBiomeTemperatureOverrides("CommonProxy.postInit");
        }
    }

    public void serverStarting(FMLServerStartingEvent event) {
        if (Config.printMobs) {
            MobUtil.printMobNames();
        }
        HotNCold.LOG.info(
            LOTRSpawnControl.applyConfiguredSpawnRules()
                .describeStartup());
        event.registerServerCommand(new CommandHotNCold());
    }
}
