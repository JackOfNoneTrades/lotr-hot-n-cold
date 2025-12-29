package org.fentanylsolutions.hotncold;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    public static boolean printMobs = false;

    public static String[] mobsImmuneToFrost = {};
    public static String[] mobsImmuneToHeat = {};

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        printMobs = configuration.getBoolean("printMobs", Configuration.CATEGORY_GENERAL, printMobs, "Print mob names");

        mobsImmuneToFrost = configuration
            .get(
                Configuration.CATEGORY_GENERAL,
                "mobsImmuneToFrost",
                mobsImmuneToFrost,
                "List of mobs that should be immune to frost.")
            .getStringList();

        mobsImmuneToHeat = configuration
            .get(
                Configuration.CATEGORY_GENERAL,
                "mobsImmuneToHeat",
                mobsImmuneToHeat,
                "List of mobs that should be immune to heat.")
            .getStringList();

        if (configuration.hasChanged()) {
            configuration.save();
            HotNCold.rebuildMobLists();
        }
    }
}
