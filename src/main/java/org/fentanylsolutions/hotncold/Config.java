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
    public static boolean logBlockedSpawnAttempts = false;
    public static boolean replaceExistingLOTREquipment = true;
    public static boolean customizeHiredLOTREquipment = false;
    public static boolean customizeNamedLOTREquipment = false;
    public static int blockedSpawnLogIntervalSeconds = 60;

    public static String[] mobsImmuneToFrost = {};
    public static String[] mobsImmuneToHeat = {};
    public static String[] blockedEntitiesInAllLOTRBiomes = {};
    public static String[] blockedEntityBiomeRules = {};
    public static String[] addedEntityBiomeRules = {};
    public static String[] lotrNPCWeaponRules = {};
    public static String[] lotrNPCArmorRules = {};

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

        readSpawnConfiguration(configuration);
        readNPCEquipmentConfiguration(configuration);

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

    public static boolean reloadSpawnConfiguration() {
        if (loadedConfigFile == null) {
            return false;
        }

        Configuration configuration = new Configuration(loadedConfigFile);
        readSpawnConfiguration(configuration);
        if (configuration.hasChanged()) {
            configuration.save();
        }
        return true;
    }

    public static boolean reloadNPCEquipmentConfiguration() {
        if (loadedConfigFile == null) {
            return false;
        }

        Configuration configuration = new Configuration(loadedConfigFile);
        readNPCEquipmentConfiguration(configuration);
        if (configuration.hasChanged()) {
            configuration.save();
        }
        return true;
    }

    private static void readSpawnConfiguration(Configuration configuration) {
        logBlockedSpawnAttempts = configuration.getBoolean(
            "logBlockedSpawnAttempts",
            Configuration.CATEGORY_GENERAL,
            logBlockedSpawnAttempts,
            "Log an aggregated summary of blocked natural spawn attempts. Disabled by default. At most one summary "
                + "line is written per blockedSpawnLogIntervalSeconds.");

        blockedSpawnLogIntervalSeconds = configuration.getInt(
            "blockedSpawnLogIntervalSeconds",
            Configuration.CATEGORY_GENERAL,
            blockedSpawnLogIntervalSeconds,
            1,
            3600,
            "Minimum number of seconds between aggregated blocked natural spawn summaries.");

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
    }

    private static void readNPCEquipmentConfiguration(Configuration configuration) {
        customizeHiredLOTREquipment = configuration.getBoolean(
            "customizeHiredLOTREquipment",
            Configuration.CATEGORY_GENERAL,
            customizeHiredLOTREquipment,
            "If true, configured equipment may be applied to hired LOTR NPCs. Disabled by default to preserve gear "
                + "owned or managed by players.");

        customizeNamedLOTREquipment = configuration.getBoolean(
            "customizeNamedLOTREquipment",
            Configuration.CATEGORY_GENERAL,
            customizeNamedLOTREquipment,
            "If true, configured equipment may be applied to LOTR NPCs with a custom name tag. Disabled by default "
                + "to preserve deliberately customized NPCs. LOTR's ordinary generated NPC names are unaffected.");

        replaceExistingLOTREquipment = configuration.getBoolean(
            "replaceExistingLOTREquipment",
            Configuration.CATEGORY_GENERAL,
            replaceExistingLOTREquipment,
            "If true, configured LOTR NPC equipment replaces normal gear. If false, rules apply only to equipment "
                + "slots that are empty after LOTR initializes the NPC. Existing NPCs are never changed.");

        lotrNPCWeaponRules = configuration
            .get(
                Configuration.CATEGORY_GENERAL,
                "lotrNPCWeaponRules",
                lotrNPCWeaponRules,
                "Weighted weapon choices in the format target;itemName;weight. A target can be an exact LOTR NPC "
                    + "name or faction:FACTION_CODE, such as faction:GONDOR. Use empty as an item name for a weighted "
                    + "empty-hand chance. Exact NPC rules override faction rules. Entity and item names are exact and "
                    + "case-sensitive. Example: "
                    + "LOTR.GondorSoldier;lotr:swordGondor;10")
            .getStringList();

        lotrNPCArmorRules = configuration
            .get(
                Configuration.CATEGORY_GENERAL,
                "lotrNPCArmorRules",
                lotrNPCArmorRules,
                "Weighted armor choices in the format target;slot;itemName;weight. A target can be an exact LOTR NPC "
                    + "name or faction:FACTION_CODE, such as faction:GONDOR. Slots are boots, leggings, chest, and "
                    + "helmet. Use empty for a weighted empty slot chance. Exact NPC rules override faction rules for "
                    + "the same slot. Example: "
                    + "LOTR.GondorSoldier;helmet;lotr:helmetGondor;10")
            .getStringList();
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
