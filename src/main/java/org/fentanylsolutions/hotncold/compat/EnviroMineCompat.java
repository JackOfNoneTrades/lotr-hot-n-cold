package org.fentanylsolutions.hotncold.compat;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.config.Configuration;

import org.fentanylsolutions.hotncold.HotNCold;
import org.fentanylsolutions.hotncold.util.BiomeUtil;

import enviromine.core.EM_ConfigHandler;
import enviromine.core.EM_Settings;
import enviromine.trackers.properties.BiomeProperties;
import enviromine.utils.EnviroUtils;
import lotr.common.world.biome.LOTRBiome;

public final class EnviroMineCompat {

    private static final String LOTR_CUSTOM_CONFIG_FILE_NAME = "lotr.cfg";
    private static final String BIOME_CATEGORY_PREFIX = "biomes.";
    private static final String BIOME_ID_KEY = "01.Biome ID";
    private static final String ALLOW_CONFIG_OVERRIDE_KEY = "02.Allow Config Override";
    private static final String WATER_QUALITY_KEY = "03.Water Quality";
    private static final String AMBIENT_TEMPERATURE_KEY = "04.Ambient Temperature";
    private static final String TEMP_RATE_KEY = "05.Temp Rate";
    private static final String SANITY_RATE_KEY = "06.Sanity Rate";
    private static final String DEHYDRATE_RATE_KEY = "07.Dehydrate Rate";
    private static final String AIR_QUALITY_RATE_KEY = "08.Air Quality Rate";
    private static final String WATER_QUALITY_COMMENT = "Water Quality: dirty, salty, cold, clean";
    private static final String AMBIENT_TEMPERATURE_COMMENT = "Biome temperature in celsius (Player body temp is offset by + 12C)";
    private static final Set<Integer> loggedCollisionIds = new HashSet<>();

    private EnviroMineCompat() {}

    public static void applyBiomeTemperatureOverrides(String source) {
        if (EM_Settings.biomeProperties == null) {
            return;
        }

        ensureCustomBiomeConfigEntries();
        Map<Integer, BiomeProperties> overrides = loadCustomBiomeTemperatureOverrides();
        String overrideSource = LOTR_CUSTOM_CONFIG_FILE_NAME;
        if (overrides.isEmpty()) {
            overrides = buildFallbackBiomeTemperatureOverrides(HotNCold.MODID);
            overrideSource = "hotncold.cfg";
        }
        if (overrides.isEmpty()) {
            return;
        }

        int appliedCount = 0;
        for (Map.Entry<Integer, BiomeProperties> entry : overrides.entrySet()) {
            BiomeGenBase biome = BiomeUtil.getLOTRBiome(entry.getKey());
            if (biome == null) {
                HotNCold.LOG.warn(
                    "LOTR biome id {} vanished before EnviroMine override application from {}, skipping",
                    entry.getKey(),
                    source);
                continue;
            }

            logBiomeIdCollisionIfNeeded(biome);
            EM_Settings.biomeProperties.put(biome.biomeID, entry.getValue());
            appliedCount++;
        }

        if (appliedCount > 0) {
            HotNCold.LOG.info(
                "Applied {} EnviroMine LOTR biome temperature overrides from {} using {}",
                appliedCount,
                source,
                overrideSource);
        }
    }

    private static void ensureCustomBiomeConfigEntries() {
        File configFile = getLotrCustomConfigFile();
        if (configFile == null) {
            return;
        }

        File parent = configFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            HotNCold.LOG.warn("Failed to create EnviroMine custom config directory at {}", parent.getPath());
            return;
        }

        Map<Integer, BiomeProperties> defaults = buildFallbackBiomeTemperatureOverrides(configFile.getName());
        if (defaults.isEmpty()) {
            return;
        }

        Configuration configuration = new Configuration(configFile, true);
        configuration.load();

        Set<String> existingCategories = new HashSet<>(configuration.getCategoryNames());
        int generatedCount = 0;

        for (Map.Entry<Integer, BiomeProperties> entry : defaults.entrySet()) {
            BiomeGenBase biome = BiomeUtil.getLOTRBiome(entry.getKey());
            if (biome == null) {
                continue;
            }

            String category = getBiomeCategoryName(biome);
            boolean hadCategory = existingCategories.contains(category);
            entry.getValue()
                .SaveProperty(configuration, category);
            if (!hadCategory) {
                existingCategories.add(category);
                generatedCount++;
            }
        }

        if (configuration.hasChanged()) {
            configuration.save();
        }

        if (generatedCount > 0) {
            HotNCold.LOG.info(
                "Generated {} LOTR biome entries in EnviroMine custom config {}",
                generatedCount,
                configFile.getPath());
        }
    }

    private static Map<Integer, BiomeProperties> loadCustomBiomeTemperatureOverrides() {
        File configFile = getLotrCustomConfigFile();
        if (configFile == null || !configFile.exists()) {
            return Collections.emptyMap();
        }

        Configuration configuration = new Configuration(configFile, true);
        configuration.load();

        List<String> categories = new ArrayList<>(configuration.getCategoryNames());
        Collections.sort(categories);

        Map<Integer, BiomeProperties> overrides = new LinkedHashMap<>();
        for (String category : categories) {
            if (!category.startsWith(BIOME_CATEGORY_PREFIX)) {
                continue;
            }

            int biomeId = configuration.get(category, BIOME_ID_KEY, -1)
                .getInt(-1);
            BiomeGenBase biome = BiomeUtil.getLOTRBiome(biomeId);
            if (biome == null) {
                continue;
            }

            BiomeProperties existing = EM_Settings.biomeProperties == null ? null
                : EM_Settings.biomeProperties.get(biomeId);
            boolean biomeOverride = configuration.get(category, ALLOW_CONFIG_OVERRIDE_KEY, true)
                .getBoolean(true);
            String waterQuality = configuration
                .get(category, WATER_QUALITY_KEY, getWaterQuality(biome, existing), WATER_QUALITY_COMMENT)
                .getString();
            float ambientTemp = (float) configuration
                .get(
                    category,
                    AMBIENT_TEMPERATURE_KEY,
                    getDefaultAmbientTemperature(biome),
                    AMBIENT_TEMPERATURE_COMMENT)
                .getDouble(getDefaultAmbientTemperature(biome));
            float tempRate = (float) configuration
                .get(category, TEMP_RATE_KEY, existing != null ? existing.tempRate : 0D)
                .getDouble(existing != null ? existing.tempRate : 0D);
            float sanityRate = (float) configuration
                .get(category, SANITY_RATE_KEY, existing != null ? existing.sanityRate : 0D)
                .getDouble(existing != null ? existing.sanityRate : 0D);
            float dehydrateRate = (float) configuration
                .get(category, DEHYDRATE_RATE_KEY, existing != null ? existing.dehydrateRate : 0D)
                .getDouble(existing != null ? existing.dehydrateRate : 0D);
            float airRate = (float) configuration
                .get(category, AIR_QUALITY_RATE_KEY, existing != null ? existing.airRate : 0D)
                .getDouble(existing != null ? existing.airRate : 0D);

            overrides.put(
                biomeId,
                new BiomeProperties(
                    biomeId,
                    biomeOverride,
                    normalizeWaterQuality(biome, existing, waterQuality),
                    ambientTemp,
                    tempRate,
                    sanityRate,
                    dehydrateRate,
                    airRate,
                    configFile.getName()));
        }

        if (configuration.hasChanged()) {
            configuration.save();
        }

        return overrides;
    }

    private static Map<Integer, BiomeProperties> buildFallbackBiomeTemperatureOverrides(String loadedFrom) {
        Map<Integer, BiomeProperties> overrides = new LinkedHashMap<>();
        if (HotNCold.enviromineBiomeTemperatures == null || HotNCold.enviromineBiomeTemperatures.isEmpty()) {
            return overrides;
        }

        List<Map.Entry<Integer, Float>> entries = new ArrayList<>(HotNCold.enviromineBiomeTemperatures.entrySet());
        entries.sort(Comparator.comparing(entry -> {
            BiomeGenBase biome = BiomeUtil.getLOTRBiome(entry.getKey());
            return biome != null ? biome.biomeName : Integer.toString(entry.getKey());
        }));

        for (Map.Entry<Integer, Float> entry : entries) {
            BiomeGenBase biome = BiomeUtil.getLOTRBiome(entry.getKey());
            if (biome == null) {
                continue;
            }

            BiomeProperties existing = EM_Settings.biomeProperties == null ? null
                : EM_Settings.biomeProperties.get(entry.getKey());
            overrides.put(
                biome.biomeID,
                new BiomeProperties(
                    biome.biomeID,
                    true,
                    getWaterQuality(biome, existing),
                    entry.getValue(),
                    existing != null ? existing.tempRate : 0F,
                    existing != null ? existing.sanityRate : 0F,
                    existing != null ? existing.dehydrateRate : 0F,
                    existing != null ? existing.airRate : 0F,
                    loadedFrom));
        }

        return overrides;
    }

    private static File getLotrCustomConfigFile() {
        String loadedProfile = EM_ConfigHandler.loadedProfile;
        if (loadedProfile == null || loadedProfile.isEmpty()) {
            loadedProfile = EM_ConfigHandler.defaultProfile;
        }
        if (loadedProfile == null || loadedProfile.isEmpty()) {
            return null;
        }

        return new File(loadedProfile + EM_ConfigHandler.customPath + LOTR_CUSTOM_CONFIG_FILE_NAME);
    }

    private static String getBiomeCategoryName(BiomeGenBase biome) {
        return BIOME_CATEGORY_PREFIX + biome.biomeName;
    }

    private static float getDefaultAmbientTemperature(BiomeGenBase biome) {
        Float configured = HotNCold.enviromineBiomeTemperatures == null ? null
            : HotNCold.enviromineBiomeTemperatures.get(biome.biomeID);
        if (configured != null) {
            return configured;
        }

        return BiomeUtil.getEnviromineDefaultAmbientTemperature(biome);
    }

    private static String getWaterQuality(BiomeGenBase biome, BiomeProperties existing) {
        if (existing != null && existing.waterQuality != null && existing.getWaterQualityId() != -1) {
            return existing.waterQuality;
        }
        return EnviroUtils.getBiomeWater(biome);
    }

    private static String normalizeWaterQuality(BiomeGenBase biome, BiomeProperties existing,
        String configuredWaterQuality) {
        if (configuredWaterQuality == null || configuredWaterQuality.trim()
            .isEmpty()) {
            return getWaterQuality(biome, existing);
        }

        return configuredWaterQuality;
    }

    private static void logBiomeIdCollisionIfNeeded(BiomeGenBase lotrBiome) {
        BiomeGenBase biomeArrayEntry = BiomeUtil.getBiomeArrayEntry(lotrBiome.biomeID);
        if (biomeArrayEntry == null || biomeArrayEntry instanceof LOTRBiome
            || !loggedCollisionIds.add(lotrBiome.biomeID)) {
            return;
        }

        HotNCold.LOG.warn(
            "EnviroMine biome id collision detected: LOTR biome '{}' (id {}) shares its biome ID with '{}' ({}) from the global biome array. EnviroMine keys biome properties by biome ID, so this can cause the wrong ambient temperature to be used.",
            lotrBiome.biomeName,
            lotrBiome.biomeID,
            biomeArrayEntry.biomeName,
            biomeArrayEntry.getClass()
                .getName());
    }
}
