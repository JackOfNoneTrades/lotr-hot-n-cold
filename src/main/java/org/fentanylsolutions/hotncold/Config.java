package org.fentanylsolutions.hotncold;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import org.fentanylsolutions.hotncold.util.BiomeUtil;

public class Config {

    private static File loadedConfigFile;
    private static boolean shouldPopulateGeneratedEnviromineBiomeTemperatures;

    public static boolean printMobs = false;
    public static boolean printBiomes = false;
    public static boolean autoPopulateEnviromineBiomeTemperatures = false;
    public static boolean removeAllWarOfTheRingAnimalSpawns = false;

    public static String[] mobsImmuneToFrost = {};
    public static String[] mobsImmuneToHeat = {};
    public static String[] blockedEntitiesInAllLOTRBiomes = {};
    public static String[] blockedEntityBiomeRules = {};
    public static String[] addedEntityBiomeRules = {};

    public static String[] frostBiomes = {};
    public static String[] heatBiomes = {};
    public static String[] enviromineBiomeTemperatures = {};

    public static void synchronizeConfiguration(File configFile) {
        loadedConfigFile = configFile;
        Configuration configuration = new Configuration(configFile);
        boolean enviromineBiomeTemperaturesKeyMissing = !configuration
            .hasKey(Configuration.CATEGORY_GENERAL, "enviromineBiomeTemperatures");

        autoPopulateEnviromineBiomeTemperatures = configuration.getBoolean(
            "autoPopulateEnviromineBiomeTemperatures",
            Configuration.CATEGORY_GENERAL,
            autoPopulateEnviromineBiomeTemperatures,
            "If true, hotncold will auto-fill enviromineBiomeTemperatures with generated defaults on first run. Off by default so EnviroMine's lotr.cfg stays the canonical source of truth; turn this on if you want to seed enviromineBiomeTemperatures with hotncold's defaults and edit from there.");

        shouldPopulateGeneratedEnviromineBiomeTemperatures = enviromineBiomeTemperaturesKeyMissing
            && autoPopulateEnviromineBiomeTemperatures;

        printMobs = configuration.getBoolean("printMobs", Configuration.CATEGORY_GENERAL, printMobs, "Print mob names");
        printBiomes = configuration
            .getBoolean("printBiomes", Configuration.CATEGORY_GENERAL, printBiomes, "Print biome names");

        removeAllWarOfTheRingAnimalSpawns = configuration.getBoolean(
            "removeAllWarOfTheRingAnimalSpawns",
            Configuration.CATEGORY_GENERAL,
            removeAllWarOfTheRingAnimalSpawns,
            "Remove every natural animal spawn added to LOTR biomes by War of the Ring. This does not remove "
                + "existing entities or disable spawn eggs, commands, breeding, mounts, or scripted spawns.");

        blockedEntitiesInAllLOTRBiomes = configuration
            .get(
                Configuration.CATEGORY_GENERAL,
                "blockedEntitiesInAllLOTRBiomes",
                blockedEntitiesInAllLOTRBiomes,
                "Registered entity names that must not spawn naturally in any LOTR biome. Names are exact and "
                    + "case-sensitive; enable printMobs to list valid names. This does not remove existing entities "
                    + "or disable spawn eggs, commands, breeding, mounts, or scripted spawns.")
            .getStringList();

        blockedEntityBiomeRules = configuration
            .get(
                Configuration.CATEGORY_GENERAL,
                "blockedEntityBiomeRules",
                blockedEntityBiomeRules,
                "Natural spawn blocks in the format entityName:biomeName or entityName:biomeId. Entity names are "
                    + "exact and case-sensitive; enable printMobs and printBiomes to list valid names. This does not "
                    + "remove existing entities or disable spawn eggs, commands, breeding, mounts, or scripted spawns.")
            .getStringList();

        addedEntityBiomeRules = configuration.get(
            Configuration.CATEGORY_GENERAL,
            "addedEntityBiomeRules",
            addedEntityBiomeRules,
            "Natural spawn additions in the format "
                + "entityName:biomeName:category:weight:minGroup:maxGroup. Biome IDs are also accepted. "
                + "Categories are creature, monster, waterCreature, ambient, and LOTRAmbient when available. "
                + "Entity names are exact and case-sensitive; enable printMobs and printBiomes to list valid names.")
            .getStringList();

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
