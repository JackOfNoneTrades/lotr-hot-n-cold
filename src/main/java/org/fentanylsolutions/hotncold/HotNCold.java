package org.fentanylsolutions.hotncold;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fentanylsolutions.hotncold.util.MobUtil;

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
}
