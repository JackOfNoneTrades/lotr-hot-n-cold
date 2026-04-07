package org.fentanylsolutions.hotncold;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import org.fentanylsolutions.hotncold.util.BiomeUtil;

public class Config {

    private static File loadedConfigFile;
    private static boolean shouldPopulateGeneratedEnviromineBiomeTemperatures;

    public static boolean printMobs = false;
    public static boolean printBiomes = false;

    public static String[] mobsImmuneToFrost = {};
    public static String[] mobsImmuneToHeat = {};

    public static String[] frostBiomes = {};
    public static String[] heatBiomes = {};
    public static String[] enviromineBiomeTemperatures = {};

    public static void synchronizeConfiguration(File configFile) {
        loadedConfigFile = configFile;
        Configuration configuration = new Configuration(configFile);
        shouldPopulateGeneratedEnviromineBiomeTemperatures = !configuration
            .hasKey(Configuration.CATEGORY_GENERAL, "enviromineBiomeTemperatures");

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

        enviromineBiomeTemperatures = configuration
            .get(
                Configuration.CATEGORY_GENERAL,
                "enviromineBiomeTemperatures",
                enviromineBiomeTemperatures,
                "List of LOTR biome ambient temperature overrides for EnviroMine in the format biomeName:temperatureC "
                    + "or biomeId:temperatureC. Defaults are generated from LOTR biome temperatures using EnviroMine's "
                    + "vanilla ambient temperature conversion. Example: shire:26.5")
            .getStringList();

        if (configuration.hasChanged()) {
            configuration.save();
            HotNCold.rebuildMobLists();
            HotNCold.rebuildBiomeLists();
            HotNCold.rebuildEnviromineBiomeTemperatureOverrides();
        }
    }

    public static void populateGeneratedEnviromineBiomeTemperaturesIfNeeded() {
        if (!shouldPopulateGeneratedEnviromineBiomeTemperatures || loadedConfigFile == null) {
            return;
        }

        String[] generatedDefaults = BiomeUtil.getDefaultEnviromineBiomeTemperatures();
        if (generatedDefaults.length == 0) {
            HotNCold.LOG
                .warn("Failed to generate default EnviroMine LOTR biome temperatures, leaving config entry empty");
            return;
        }

        Configuration configuration = new Configuration(loadedConfigFile);
        configuration.load();
        configuration
            .get(
                Configuration.CATEGORY_GENERAL,
                "enviromineBiomeTemperatures",
                generatedDefaults,
                "List of LOTR biome ambient temperature overrides for EnviroMine in the format biomeName:temperatureC "
                    + "or biomeId:temperatureC. Defaults are generated from LOTR biome temperatures using EnviroMine's "
                    + "vanilla ambient temperature conversion. Example: shire:26.5")
            .set(generatedDefaults);

        if (configuration.hasChanged()) {
            configuration.save();
        }

        enviromineBiomeTemperatures = generatedDefaults;
        shouldPopulateGeneratedEnviromineBiomeTemperatures = false;
        HotNCold.LOG
            .info("Generated {} default EnviroMine LOTR biome temperature entries", enviromineBiomeTemperatures.length);
    }
}
