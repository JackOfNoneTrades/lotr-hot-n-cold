package org.fentanylsolutions.hotncold.compat;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.biome.BiomeGenBase;

import org.fentanylsolutions.hotncold.Config;
import org.fentanylsolutions.hotncold.HotNCold;

public final class LOTRSpawnControl {

    private LOTRSpawnControl() {}

    public static void removeGloballyBlockedEntities() {
        Set<Class> blockedEntityClasses = resolveBlockedEntityClasses(Config.blockedEntitiesInAllLOTRBiomes);
        if (blockedEntityClasses.isEmpty()) {
            return;
        }

        int removedEntries = 0;
        int changedBiomes = 0;

        for (BiomeGenBase biome : WarOfTheRingSpawnCompat.getAllLOTRSpawnBiomes()) {
            int removedFromBiome = 0;
            for (EnumCreatureType creatureType : EnumCreatureType.values()) {
                removedFromBiome += removeBlockedEntries(biome.getSpawnableList(creatureType), blockedEntityClasses);
            }
            if (removedFromBiome > 0) {
                removedEntries += removedFromBiome;
                changedBiomes++;
            }
        }

        HotNCold.LOG.info(
            "Removed {} globally blocked natural spawn entries from {} LOTR biomes",
            removedEntries,
            changedBiomes);
    }

    public static void removeBiomeBlockedEntities() {
        Set<BiomeGenBase> lotrBiomes = WarOfTheRingSpawnCompat.getAllLOTRSpawnBiomes();
        Map<BiomeGenBase, Set<Class>> blockedClassesByBiome = resolveBiomeBlockedEntityClasses(
            Config.blockedEntityBiomeRules,
            lotrBiomes);
        if (blockedClassesByBiome.isEmpty()) {
            return;
        }

        int removedEntries = 0;
        int changedBiomes = 0;

        for (Map.Entry<BiomeGenBase, Set<Class>> rule : blockedClassesByBiome.entrySet()) {
            int removedFromBiome = 0;
            for (EnumCreatureType creatureType : EnumCreatureType.values()) {
                removedFromBiome += removeBlockedEntries(
                    rule.getKey()
                        .getSpawnableList(creatureType),
                    rule.getValue());
            }
            if (removedFromBiome > 0) {
                removedEntries += removedFromBiome;
                changedBiomes++;
            }
        }

        HotNCold.LOG
            .info("Removed {} biome-specific natural spawn entries from {} LOTR biomes", removedEntries, changedBiomes);
    }

    static Set<Class> resolveBlockedEntityClasses(String[] entityNames) {
        Set<Class> entityClasses = new LinkedHashSet<>();

        for (String configuredName : entityNames) {
            String entityName = configuredName == null ? "" : configuredName.trim();
            if (entityName.isEmpty()) {
                continue;
            }

            Object entityClass = EntityList.stringToClassMapping.get(entityName);
            if (!(entityClass instanceof Class)) {
                HotNCold.LOG.warn(
                    "Blocked LOTR spawn entity '{}' was not found; names are exact and case-sensitive. "
                        + "Enable printMobs to list valid names",
                    entityName);
                continue;
            }

            if (!entityClasses.add((Class) entityClass)) {
                HotNCold.LOG
                    .warn("Blocked LOTR spawn entity '{}' is listed more than once; ignoring duplicate", entityName);
            }
        }

        return entityClasses;
    }

    static Map<BiomeGenBase, Set<Class>> resolveBiomeBlockedEntityClasses(String[] configuredRules,
        Set<BiomeGenBase> lotrBiomes) {
        Map<BiomeGenBase, Set<Class>> blockedClassesByBiome = new IdentityHashMap<>();

        for (String configuredRule : configuredRules) {
            String rule = configuredRule == null ? "" : configuredRule.trim();
            if (rule.isEmpty()) {
                continue;
            }

            int separatorIndex = rule.lastIndexOf(':');
            if (separatorIndex <= 0 || separatorIndex >= rule.length() - 1) {
                HotNCold.LOG.warn(
                    "Invalid LOTR spawn block rule '{}'; expected entityName:biomeName or entityName:biomeId",
                    rule);
                continue;
            }

            String entityName = rule.substring(0, separatorIndex)
                .trim();
            String biomeToken = rule.substring(separatorIndex + 1)
                .trim();
            Object mappedEntityClass = EntityList.stringToClassMapping.get(entityName);
            if (!(mappedEntityClass instanceof Class)) {
                HotNCold.LOG.warn(
                    "Blocked LOTR spawn entity '{}' in rule '{}' was not found; names are exact and case-sensitive. "
                        + "Enable printMobs to list valid names",
                    entityName,
                    rule);
                continue;
            }

            Set<BiomeGenBase> matchingBiomes = resolveLOTRSpawnBiomes(biomeToken, lotrBiomes);
            if (matchingBiomes.isEmpty()) {
                HotNCold.LOG.warn(
                    "LOTR biome '{}' in spawn block rule '{}' was not found; enable printBiomes to list valid names",
                    biomeToken,
                    rule);
                continue;
            }

            boolean added = false;
            for (BiomeGenBase biome : matchingBiomes) {
                Set<Class> blockedClasses = blockedClassesByBiome.get(biome);
                if (blockedClasses == null) {
                    blockedClasses = new LinkedHashSet<>();
                    blockedClassesByBiome.put(biome, blockedClasses);
                }
                added |= blockedClasses.add((Class) mappedEntityClass);
            }
            if (!added) {
                HotNCold.LOG.warn("LOTR spawn block rule '{}' is listed more than once; ignoring duplicate", rule);
            }
        }

        return blockedClassesByBiome;
    }

    static Set<BiomeGenBase> resolveLOTRSpawnBiomes(String biomeToken, Set<BiomeGenBase> lotrBiomes) {
        Set<BiomeGenBase> matchingBiomes = Collections.newSetFromMap(new IdentityHashMap<BiomeGenBase, Boolean>());

        Integer biomeId = parseInteger(biomeToken);
        if (biomeId != null) {
            for (BiomeGenBase biome : lotrBiomes) {
                if (biome.biomeID == biomeId) {
                    matchingBiomes.add(biome);
                }
            }
            return matchingBiomes;
        }

        for (BiomeGenBase biome : lotrBiomes) {
            if (biome.biomeName != null && biome.biomeName.equals(biomeToken)) {
                matchingBiomes.add(biome);
            }
        }
        if (!matchingBiomes.isEmpty()) {
            return matchingBiomes;
        }

        for (BiomeGenBase biome : lotrBiomes) {
            if (biome.biomeName != null && biome.biomeName.equalsIgnoreCase(biomeToken)) {
                matchingBiomes.add(biome);
            }
        }
        return matchingBiomes;
    }

    private static Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static int removeBlockedEntries(List spawnEntries, Set<Class> blockedEntityClasses) {
        int removedEntries = 0;
        Iterator iterator = spawnEntries.iterator();

        while (iterator.hasNext()) {
            Object value = iterator.next();
            if (!(value instanceof BiomeGenBase.SpawnListEntry)) {
                continue;
            }

            BiomeGenBase.SpawnListEntry entry = (BiomeGenBase.SpawnListEntry) value;
            if (blockedEntityClasses.contains(entry.entityClass)) {
                iterator.remove();
                removedEntries++;
            }
        }

        return removedEntries;
    }
}
