package org.fentanylsolutions.hotncold.compat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.biome.BiomeGenBase;

import org.fentanylsolutions.hotncold.Config;
import org.fentanylsolutions.hotncold.HotNCold;

public final class LOTRSpawnControl {

    private LOTRSpawnControl() {}

    public static void addBiomeEntitySpawns() {
        List<SpawnAddition> additions = resolveBiomeSpawnAdditions(
            Config.addedEntityBiomeRules,
            WarOfTheRingSpawnCompat.getAllLOTRSpawnBiomes());
        if (additions.isEmpty()) {
            return;
        }

        int addedEntries = 0;
        Set<BiomeGenBase> changedBiomes = Collections.newSetFromMap(new IdentityHashMap<BiomeGenBase, Boolean>());
        for (SpawnAddition addition : additions) {
            List spawnEntries = addition.biome.getSpawnableList(addition.creatureType);
            if (addSpawnEntryIfAbsent(
                spawnEntries,
                addition.entityClass,
                addition.weight,
                addition.minimumGroupSize,
                addition.maximumGroupSize)) {
                addedEntries++;
                changedBiomes.add(addition.biome);
            } else {
                HotNCold.LOG.warn(
                    "LOTR biome '{}' ({}) already has a {} spawn entry for {}; ignoring duplicate addition",
                    addition.biome.biomeName,
                    addition.biome.biomeID,
                    addition.creatureType.name(),
                    EntityList.classToStringMapping.get(addition.entityClass));
            }
        }

        HotNCold.LOG.info("Added {} natural spawn entries to {} LOTR biomes", addedEntries, changedBiomes.size());
    }

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

    @SuppressWarnings("unchecked")
    static List<SpawnAddition> resolveBiomeSpawnAdditions(String[] configuredRules, Set<BiomeGenBase> lotrBiomes) {
        List<SpawnAddition> additions = new ArrayList<>();

        for (String configuredRule : configuredRules) {
            String rule = configuredRule == null ? "" : configuredRule.trim();
            if (rule.isEmpty()) {
                continue;
            }

            String[] fields = splitSpawnAdditionRule(rule);
            if (fields == null) {
                HotNCold.LOG.warn(
                    "Invalid LOTR spawn addition '{}'; expected "
                        + "entityName:biomeName:category:weight:minGroup:maxGroup",
                    rule);
                continue;
            }

            Object mappedEntityClass = EntityList.stringToClassMapping.get(fields[0]);
            if (!(mappedEntityClass instanceof Class)) {
                HotNCold.LOG.warn(
                    "LOTR spawn addition entity '{}' in rule '{}' was not found; names are exact and case-sensitive. "
                        + "Enable printMobs to list valid names",
                    fields[0],
                    rule);
                continue;
            }
            if (!EntityLiving.class.isAssignableFrom((Class) mappedEntityClass)) {
                HotNCold.LOG.warn(
                    "LOTR spawn addition entity '{}' in rule '{}' is not a living entity and cannot spawn naturally",
                    fields[0],
                    rule);
                continue;
            }

            Set<BiomeGenBase> matchingBiomes = resolveLOTRSpawnBiomes(fields[1], lotrBiomes);
            if (matchingBiomes.isEmpty()) {
                HotNCold.LOG.warn(
                    "LOTR biome '{}' in spawn addition rule '{}' was not found; enable printBiomes to list valid names",
                    fields[1],
                    rule);
                continue;
            }

            EnumCreatureType creatureType = resolveCreatureType(fields[2]);
            if (creatureType == null) {
                HotNCold.LOG.warn(
                    "Spawn category '{}' in LOTR spawn addition rule '{}' was not found; available categories are {}",
                    fields[2],
                    rule,
                    availableCreatureTypes());
                continue;
            }

            Integer weight = parsePositiveInteger(fields[3], "weight", rule);
            Integer minimumGroupSize = parsePositiveInteger(fields[4], "minimum group size", rule);
            Integer maximumGroupSize = parsePositiveInteger(fields[5], "maximum group size", rule);
            if (weight == null || minimumGroupSize == null || maximumGroupSize == null) {
                continue;
            }
            if (maximumGroupSize < minimumGroupSize) {
                HotNCold.LOG.warn(
                    "Invalid LOTR spawn addition '{}'; maximum group size must be at least the minimum group size",
                    rule);
                continue;
            }

            for (BiomeGenBase biome : matchingBiomes) {
                additions.add(
                    new SpawnAddition(
                        biome,
                        creatureType,
                        (Class<? extends EntityLiving>) mappedEntityClass,
                        weight,
                        minimumGroupSize,
                        maximumGroupSize));
            }
        }

        return additions;
    }

    static String[] splitSpawnAdditionRule(String rule) {
        String[] fields = new String[6];
        int fieldEnd = rule.length();

        for (int fieldIndex = fields.length - 1; fieldIndex >= 1; fieldIndex--) {
            int separatorIndex = rule.lastIndexOf(':', fieldEnd - 1);
            if (separatorIndex < 0) {
                return null;
            }
            fields[fieldIndex] = rule.substring(separatorIndex + 1, fieldEnd)
                .trim();
            fieldEnd = separatorIndex;
        }
        fields[0] = rule.substring(0, fieldEnd)
            .trim();

        for (String field : fields) {
            if (field.isEmpty()) {
                return null;
            }
        }
        return fields;
    }

    static EnumCreatureType resolveCreatureType(String configuredName) {
        for (EnumCreatureType creatureType : EnumCreatureType.values()) {
            if (creatureType.name()
                .equals(configuredName)) {
                return creatureType;
            }
        }
        for (EnumCreatureType creatureType : EnumCreatureType.values()) {
            if (creatureType.name()
                .equalsIgnoreCase(configuredName)) {
                return creatureType;
            }
        }
        return null;
    }

    private static String availableCreatureTypes() {
        StringBuilder result = new StringBuilder();
        for (EnumCreatureType creatureType : EnumCreatureType.values()) {
            if (result.length() > 0) {
                result.append(", ");
            }
            result.append(creatureType.name());
        }
        return result.toString();
    }

    private static Integer parsePositiveInteger(String configuredValue, String fieldName, String rule) {
        Integer value = parseInteger(configuredValue);
        if (value == null || value <= 0) {
            HotNCold.LOG.warn(
                "Invalid {} '{}' in LOTR spawn addition '{}'; the value must be a positive whole number",
                fieldName,
                configuredValue,
                rule);
            return null;
        }
        return value;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    static boolean addSpawnEntryIfAbsent(List spawnEntries, Class<? extends EntityLiving> entityClass, int weight,
        int minimumGroupSize, int maximumGroupSize) {
        for (Object value : spawnEntries) {
            if (value instanceof BiomeGenBase.SpawnListEntry
                && ((BiomeGenBase.SpawnListEntry) value).entityClass == entityClass) {
                return false;
            }
        }

        spawnEntries.add(new BiomeGenBase.SpawnListEntry(entityClass, weight, minimumGroupSize, maximumGroupSize));
        return true;
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

    static final class SpawnAddition {

        final BiomeGenBase biome;
        final EnumCreatureType creatureType;
        final Class<? extends EntityLiving> entityClass;
        final int weight;
        final int minimumGroupSize;
        final int maximumGroupSize;

        private SpawnAddition(BiomeGenBase biome, EnumCreatureType creatureType,
            Class<? extends EntityLiving> entityClass, int weight, int minimumGroupSize, int maximumGroupSize) {
            this.biome = biome;
            this.creatureType = creatureType;
            this.entityClass = entityClass;
            this.weight = weight;
            this.minimumGroupSize = minimumGroupSize;
            this.maximumGroupSize = maximumGroupSize;
        }
    }
}
