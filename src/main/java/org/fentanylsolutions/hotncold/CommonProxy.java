package org.fentanylsolutions.hotncold;

import net.minecraftforge.common.MinecraftForge;

import org.fentanylsolutions.hotncold.command.CommandHotNCold;
import org.fentanylsolutions.hotncold.compat.EnviroMineCompat;
import org.fentanylsolutions.hotncold.compat.LOTREquipmentControl;
import org.fentanylsolutions.hotncold.compat.LOTREquipmentSpawns;
import org.fentanylsolutions.hotncold.compat.LOTRNPCShieldSync;
import org.fentanylsolutions.hotncold.compat.LOTRSpawnControl;
import org.fentanylsolutions.hotncold.compat.LOTRSpawnGuard;
import org.fentanylsolutions.hotncold.compat.StreamsCompat;
import org.fentanylsolutions.hotncold.compat.WildCavesCompat;
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
        if (Config.disableLightningExplosionSound) {
            HotNCold.LOG
                .info("Lightning explosion sound disabled; thunder, actual explosions and strike effects unchanged");
        }
    }

    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(LOTRSpawnGuard.INSTANCE);
        LOTRNPCShieldSync.init();
        LOTREquipmentSpawns.init();
    }

    public void receiveNPCShield(int entityId, int dimension, String shieldName) {}

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
        HotNCold.LOG.info(
            "Middle-earth worldgen compatibility: Streams installed={}, enabled={}; Greg Caves installed={}, enabled={}; Wild Caves installed={}, enabled={}",
            Loader.isModLoaded("streams"),
            Config.enableStreamsMiddleEarth,
            Loader.isModLoaded("gregcaves"),
            Config.enableGregCavesMiddleEarth,
            Loader.isModLoaded("wildcaves3"),
            Config.enableWildCavesMiddleEarth);
        if (Loader.isModLoaded("streams") && Loader.isModLoaded("farseek")) {
            StreamsCompat.initialize();
        }
        if (Loader.isModLoaded("wildcaves3") && Config.enableWildCavesMiddleEarth) {
            WildCavesCompat.initialize();
        }
    }

    public void serverStarting(FMLServerStartingEvent event) {
        if (Config.printMobs) {
            MobUtil.printMobNames();
        }
        HotNCold.LOG.info(
            LOTRSpawnControl.applyConfiguredSpawnRules()
                .describeStartup());
        HotNCold.LOG.info(
            LOTREquipmentControl.prepareConfiguredNPCGroups()
                .describeStartup());
        HotNCold.LOG.info(
            LOTREquipmentControl.prepareConfiguredWeaponRules()
                .describeStartup());
        HotNCold.LOG.info(
            LOTREquipmentControl.prepareConfiguredRangedWeaponRules()
                .describeStartup());
        HotNCold.LOG.info(
            LOTREquipmentControl.prepareConfiguredShieldRules()
                .describeStartup());
        HotNCold.LOG.info(
            LOTREquipmentControl.prepareConfiguredArmorRules()
                .describeStartup());
        event.registerServerCommand(new CommandHotNCold());
    }
}
