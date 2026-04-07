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

    private static final Set<Integer> loggedCollisionIds = new HashSet<>();

    private EnviroMineCompat() {}

    public static void applyBiomeTemperatureOverrides(String source) {
        if (EM_Settings.biomeProperties == null) {
            return;
        }
        if (HotNCold.enviromineBiomeTemperatures == null || HotNCold.enviromineBiomeTemperatures.isEmpty()) {
            return;
        }

        int appliedCount = 0;
        for (Map.Entry<Integer, Float> entry : HotNCold.enviromineBiomeTemperatures.entrySet()) {
            BiomeGenBase biome = BiomeUtil.getLOTRBiome(entry.getKey());
            if (biome == null) {
                HotNCold.LOG.warn(
                    "LOTR biome id {} vanished before EnviroMine override application from {}, skipping",
                    entry.getKey(),
                    source);
                continue;
            }

            logBiomeIdCollisionIfNeeded(biome);
            BiomeProperties existing = EM_Settings.biomeProperties.get(entry.getKey());
            BiomeProperties override = new BiomeProperties(
                biome.biomeID,
                true,
                getWaterQuality(biome, existing),
                entry.getValue(),
                existing != null ? existing.tempRate : 0F,
                existing != null ? existing.sanityRate : 0F,
                existing != null ? existing.dehydrateRate : 0F,
                existing != null ? existing.airRate : 0F,
                HotNCold.MODID);

            EM_Settings.biomeProperties.put(biome.biomeID, override);
            appliedCount++;
        }

        if (appliedCount > 0) {
            HotNCold.LOG.info("Applied {} EnviroMine LOTR biome temperature overrides from {}", appliedCount, source);
        }
    }

    private static String getWaterQuality(BiomeGenBase biome, BiomeProperties existing) {
        if (existing != null && existing.waterQuality != null && existing.getWaterQualityId() != -1) {
            return existing.waterQuality;
        }
        return EnviroUtils.getBiomeWater(biome);
    }

    private static void logBiomeIdCollisionIfNeeded(BiomeGenBase lotrBiome) {
        BiomeGenBase biomeArrayEntry = BiomeUtil.getBiomeArrayEntry(lotrBiome.biomeID);
        if (biomeArrayEntry == null || biomeArrayEntry instanceof LOTRBiome || !loggedCollisionIds.add(lotrBiome.biomeID)) {
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
