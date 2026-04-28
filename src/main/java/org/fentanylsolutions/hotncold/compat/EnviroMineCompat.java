package org.fentanylsolutions.hotncold.compat;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.world.biome.BiomeGenBase;

import org.fentanylsolutions.hotncold.HotNCold;
import org.fentanylsolutions.hotncold.util.BiomeUtil;

import enviromine.core.EM_Settings;
import enviromine.trackers.properties.BiomeProperties;
import enviromine.utils.EnviroUtils;
import lotr.common.world.biome.LOTRBiome;

public final class EnviroMineCompat {

    private static final float DEFAULT_TEMPERATURE_EPSILON = 0.001F;
    private static final Set<Integer> loggedCollisionIds = new HashSet<>();

    private EnviroMineCompat() {}

    public static void applyBiomeTemperatureOverrides(String source) {
        if (EM_Settings.biomeProperties == null || HotNCold.enviromineBiomeTemperatures == null
            || HotNCold.enviromineBiomeTemperatures.isEmpty()) {
            return;
        }

        int applied = 0;
        int missingProperties = 0;
        int respectedLotrCfg = 0;
        for (Map.Entry<Integer, Float> entry : HotNCold.enviromineBiomeTemperatures.entrySet()) {
            BiomeGenBase biome = BiomeUtil.getLOTRBiome(entry.getKey());
            if (biome == null) {
                continue;
            }

            logBiomeIdCollisionIfNeeded(biome);

            BiomeProperties properties = EM_Settings.biomeProperties.get(biome.biomeID);
            if (properties == null) {
                missingProperties++;
                continue;
            }

            float continuationDefault = EnviroUtils.getBiomeTemp(biome);
            if (Math.abs(properties.ambientTemp - continuationDefault) > DEFAULT_TEMPERATURE_EPSILON) {
                respectedLotrCfg++;
                continue;
            }

            properties.ambientTemp = entry.getValue();
            applied++;
        }

        if (applied > 0) {
            HotNCold.LOG
                .info("Applied {} EnviroMine LOTR biome ambient temperature overrides from {}", applied, source);
        }
        if (respectedLotrCfg > 0) {
            HotNCold.LOG.info(
                "Skipped {} EnviroMine LOTR biome ambient temperature overrides from {} because lotr.cfg already has user-customized values",
                respectedLotrCfg,
                source);
        }
        if (missingProperties > 0) {
            HotNCold.LOG.warn(
                "Skipped {} EnviroMine LOTR biome ambient temperature overrides from {} (no BiomeProperties registered for those biomes yet)",
                missingProperties,
                source);
        }
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
