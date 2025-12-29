package org.fentanylsolutions.hotncold;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    public static boolean printMobs = false;
    public static boolean printBiomes = false;

    public static String[] mobsImmuneToFrost = {};
    public static String[] mobsImmuneToHeat = {};

    public static String[] frostBiomes = {};
    public static String[] heatBiomes = {};

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        printMobs = configuration.getBoolean("printMobs", Configuration.CATEGORY_GENERAL, printMobs, "Print mob names");
        printBiomes = configuration
            .getBoolean("printBiomes", Configuration.CATEGORY_GENERAL, printBiomes, "Print biome names");

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

        frostBiomes = configuration
            .get(
                Configuration.CATEGORY_GENERAL,
                "frostBiomes",
                frostBiomes,
                "List of biomes that should apply the frost mechanic.")
            .getStringList();

        heatBiomes = configuration
            .get(
                Configuration.CATEGORY_GENERAL,
                "heatBiomes",
                heatBiomes,
                "List of biomes that should apply the heat mechanic.")
            .getStringList();

        if (configuration.hasChanged()) {
            configuration.save();
            HotNCold.rebuildMobLists();
            HotNCold.rebuildBiomeLists();
        }
    }
}
