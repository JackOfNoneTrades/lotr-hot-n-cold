package org.fentanylsolutions.hotncold;

import java.util.ArrayList;
import java.util.HashMap;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fentanylsolutions.hotncold.util.BiomeUtil;
import org.fentanylsolutions.hotncold.util.MobUtil;
import org.fentanylsolutions.hotncold.util.Util;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(
    modid = HotNCold.MODID,
    version = Tags.VERSION,
    name = "Hot N Cold: LOTR Addon",
    acceptedMinecraftVersions = "[1.7.10]")
public class HotNCold {

    public static final String MODID = "hotncold";
    public static final Logger LOG = LogManager.getLogger(MODID);

    public static ArrayList<Class> mobsImmuneToFrost;
    public static ArrayList<Class> mobsImmuneToHeat;
    public static ArrayList<Integer> frostBiomes;
    public static ArrayList<Integer> heatBiomes;
    public static HashMap<Integer, Float> enviromineBiomeTemperatures;

    @SidedProxy(
        clientSide = "org.fentanylsolutions.hotncold.ClientProxy",
        serverSide = "org.fentanylsolutions.hotncold.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    // register server commands in this event handler (Remove if not needed)
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }

    public static void rebuildMobLists() {
        mobsImmuneToFrost = new ArrayList<>();
        for (String s : Config.mobsImmuneToFrost) {
            String class_ = MobUtil.getClassByName(s);
            if (class_ == null) {
                LOG.error("Failed to get mob class for name {}", s);
            } else {
                try {
                    Class c = Class.forName(class_);
                    mobsImmuneToFrost.add(c);
                } catch (ClassNotFoundException e) {
                    LOG.error("Failed to get class for classname {}", class_);
                }
            }
        }

        mobsImmuneToHeat = new ArrayList<>();
        for (String s : Config.mobsImmuneToHeat) {
            String class_ = MobUtil.getClassByName(s);
            if (class_ == null) {
                LOG.error("Failed to get mob class for name {}", s);
            } else {
                try {
                    Class c = Class.forName(class_);
                    mobsImmuneToHeat.add(c);
                } catch (ClassNotFoundException e) {
                    LOG.error("Failed to get class for classname {}", class_);
                }
            }
        }
    }

    public static void rebuildBiomeLists() {
        frostBiomes = new ArrayList<>();
        for (String s : Config.frostBiomes) {
            BiomeGenBase b;
            if (Util.isNumeric(s)) {
                b = BiomeUtil.getBiomeGenBase(Integer.parseInt(s));
            } else {
                b = BiomeUtil.getBiomeGenBase(s);
            }
            if (b == null) {
                LOG.warn("Biome {} not found, skipping", s);
                continue;
            }
            frostBiomes.add(b.biomeID);
        }
        heatBiomes = new ArrayList<>();
        for (String s : Config.heatBiomes) {
            BiomeGenBase b;
            if (Util.isNumeric(s)) {
                b = BiomeUtil.getBiomeGenBase(Integer.parseInt(s));
            } else {
                b = BiomeUtil.getBiomeGenBase(s);
            }
            if (b == null) {
                LOG.warn("Biome {} not found, skipping", s);
                continue;
            }
            heatBiomes.add(b.biomeID);
        }
    }

    public static void rebuildEnviromineBiomeTemperatureOverrides() {
        enviromineBiomeTemperatures = new HashMap<>();

        for (String entry : Config.enviromineBiomeTemperatures) {
            if (entry == null || entry.trim()
                .isEmpty()) {
                continue;
            }

            int separatorIndex = entry.lastIndexOf(':');
            if (separatorIndex <= 0 || separatorIndex >= entry.length() - 1) {
                LOG.warn(
                    "Invalid EnviroMine biome temperature override '{}', expected biomeName:temperatureC or biomeId:temperatureC",
                    entry);
                continue;
            }

            String biomeToken = entry.substring(0, separatorIndex)
                .trim();
            String temperatureToken = entry.substring(separatorIndex + 1)
                .trim();

            BiomeGenBase biome = BiomeUtil.getLOTRBiome(biomeToken);
            if (biome == null) {
                LOG.warn("LOTR biome '{}' not found for EnviroMine temperature override '{}'", biomeToken, entry);
                continue;
            }

            float temperature;
            try {
                temperature = Float.parseFloat(temperatureToken);
            } catch (NumberFormatException e) {
                LOG.warn("Invalid EnviroMine temperature '{}' in override '{}'", temperatureToken, entry);
                continue;
            }

            Float previous = enviromineBiomeTemperatures.put(biome.biomeID, temperature);
            if (previous != null) {
                LOG.warn(
                    "Duplicate EnviroMine temperature override for biome {} ({}), replacing {}C with {}C",
                    biome.biomeName,
                    biome.biomeID,
                    previous,
                    temperature);
            }
        }
    }

    public static BiomeGenBase getWorldBiomeForCoords(World world, int x, int z) {
        if (world == null) {
            return null;
        }

        return world.getBiomeGenForCoords(x, z);
    }
}
