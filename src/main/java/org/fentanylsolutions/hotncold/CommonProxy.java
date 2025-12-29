package org.fentanylsolutions.hotncold;

import org.fentanylsolutions.hotncold.util.MobUtil;

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

    }

    public void postInit(FMLPostInitializationEvent event) {
        if (Config.printMobs) {
            MobUtil.printMobNames();
        }
        HotNCold.rebuildMobLists();
    }

    public void serverStarting(FMLServerStartingEvent event) {}
}
