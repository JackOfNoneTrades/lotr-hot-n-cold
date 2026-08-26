package org.fentanylsolutions.hotncold.compat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import org.fentanylsolutions.hotncold.Config;
import org.fentanylsolutions.hotncold.HotNCold;

public final class LOTRSpawnControl {

    private static Set<BiomeGenBase> cachedLOTRBiomes = Collections.emptySet();
    private static Set<Class> cachedGloballyBlockedClasses = Collections.emptySet();
    private static Map<BiomeGenBase, Set<Class>> cachedBlockedClassesByBiome = Collections.emptyMap();
    private static List<SpawnAddition> cachedConfiguredAdditions = Collections.emptyList();
    private static final SpawnListJournal APPLIED_CHANGES = new SpawnListJournal();
    private static final BlockedSpawnAttemptLog BLOCKED_ATTEMPT_LOG = new BlockedSpawnAttemptLog();
    private static boolean cachedRemoveAllWarOfTheRingAnimals;

    private LOTRSpawnControl() {}

    public static SpawnRuleResult applyConfiguredSpawnRules() {
        int undoneChanges = APPLIED_CHANGES.undo();
        BLOCKED_ATTEMPT_LOG.reset();
        prepareSpawnBlockRules();
        int addedEntries = addBiomeEntitySpawns();
        int rejectedAdditionTargets = cachedConfiguredAdditions.size() - addedEntries;
        int removedWarOfTheRingEntries = WarOfTheRingSpawnCompat.removeAnimalSpawnsIfConfigured(APPLIED_CHANGES);
        int removedGloballyBlockedEntries = removeGloballyBlockedEntities();
        int removedBiomeBlockedEntries = removeBiomeBlockedEntities();
        return new SpawnRuleResult(
            undoneChanges,
            addedEntries,
            removedWarOfTheRingEntries,
            removedGloballyBlockedEntries,
            removedBiomeBlockedEntries,
            rejectedAdditionTargets);
    }

    public static SpawnRuleResult reloadConfiguredSpawnRules() {
        if (!Config.reloadSpawnConfiguration()) {
            return null;
        }
        return applyConfiguredSpawnRules();
    }

    public static void prepareSpawnBlockRules() {
        Set<BiomeGenBase> lotrBiomes = WarOfTheRingSpawnCompat.getAllLOTRSpawnBiomes();
        Set<Class> globallyBlockedClasses = resolveBlockedEntityClasses(Config.blockedEntitiesInAllLOTRBiomes);
        Map<BiomeGenBase, Set<Class>> blockedClassesByBiome = resolveBiomeBlockedEntityClasses(
            Config.blockedEntityBiomeRules,
            lotrBiomes);

        cachedLOTRBiomes = lotrBiomes;
        cachedGloballyBlockedClasses = globallyBlockedClasses;
        cachedBlockedClassesByBiome = blockedClassesByBiome;
        cachedRemoveAllWarOfTheRingAnimals = Config.removeAllWarOfTheRingAnimalSpawns;
    }

    public static int addBiomeEntitySpawns() {
        List<SpawnAddition> additions = resolveBiomeSpawnAdditions(
            Config.addedEntityBiomeRules,
            WarOfTheRingSpawnCompat.getAllLOTRSpawnBiomes());
        cachedConfiguredAdditions = Collections.unmodifiableList(new ArrayList<>(additions));
        if (additions.isEmpty()) {
            return 0;
        }

        int addedEntries = 0;
        Set<BiomeGenBase> changedBiomes = Collections.newSetFromMap(new IdentityHashMap<BiomeGenBase, Boolean>());
        for (SpawnAddition addition : additions) {
            List spawnEntries = addition.biome.getSpawnableList(addition.creatureType);
            BiomeGenBase.SpawnListEntry addedEntry = addSpawnEntryIfAbsentAndReturn(
                spawnEntries,
                addition.entityClass,
                addition.weight,
                addition.minimumGroupSize,
                addition.maximumGroupSize);
            if (addedEntry != null) {
                APPLIED_CHANGES.recordAdded(spawnEntries, addedEntry);
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
        return addedEntries;
    }

    public static int removeGloballyBlockedEntities() {
        Set<Class> blockedEntityClasses = cachedGloballyBlockedClasses;
        if (blockedEntityClasses.isEmpty()) {
            return 0;
        }

        int removedEntries = 0;
        int changedBiomes = 0;

        for (BiomeGenBase biome : WarOfTheRingSpawnCompat.getAllLOTRSpawnBiomes()) {
            int removedFromBiome = 0;
            for (EnumCreatureType creatureType : EnumCreatureType.values()) {
                removedFromBiome += removeBlockedEntries(
                    biome.getSpawnableList(creatureType),
                    blockedEntityClasses,
                    APPLIED_CHANGES);
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
        return removedEntries;
    }

    public static int removeBiomeBlockedEntities() {
        Map<BiomeGenBase, Set<Class>> blockedClassesByBiome = cachedBlockedClassesByBiome;
        if (blockedClassesByBiome.isEmpty()) {
            return 0;
        }

        int removedEntries = 0;
        int changedBiomes = 0;

        for (Map.Entry<BiomeGenBase, Set<Class>> rule : blockedClassesByBiome.entrySet()) {
            int removedFromBiome = 0;
            for (EnumCreatureType creatureType : EnumCreatureType.values()) {
                removedFromBiome += removeBlockedEntries(
                    rule.getKey()
                        .getSpawnableList(creatureType),
                    rule.getValue(),
                    APPLIED_CHANGES);
            }
            if (removedFromBiome > 0) {
                removedEntries += removedFromBiome;
                changedBiomes++;
            }
        }

        HotNCold.LOG
            .info("Removed {} biome-specific natural spawn entries from {} LOTR biomes", removedEntries, changedBiomes);
        return removedEntries;
    }

    public static boolean isSpawnBlocked(Class entityClass, BiomeGenBase biome) {
        return isSpawnBlocked(
            entityClass,
            biome,
            cachedLOTRBiomes,
            cachedRemoveAllWarOfTheRingAnimals,
            cachedGloballyBlockedClasses,
            cachedBlockedClassesByBiome);
    }

    static Set<BiomeGenBase> getPreparedLOTRBiomes() {
        return cachedLOTRBiomes;
    }

    static boolean removesAllWarOfTheRingAnimals() {
        return cachedRemoveAllWarOfTheRingAnimals;
    }

    static Set<Class> getGloballyBlockedClasses() {
        return cachedGloballyBlockedClasses;
    }

    static Map<BiomeGenBase, Set<Class>> getBlockedClassesByBiome() {
        return cachedBlockedClassesByBiome;
    }

    static List<SpawnAddition> getConfiguredAdditions() {
        return cachedConfiguredAdditions;
    }

    static boolean isSpawnBlocked(Class entityClass, BiomeGenBase biome, Set<BiomeGenBase> lotrBiomes,
        boolean removeAllWarOfTheRingAnimals, Set<Class> globallyBlockedClasses,
        Map<BiomeGenBase, Set<Class>> blockedClassesByBiome) {
        if (entityClass == null || biome == null || !lotrBiomes.contains(biome)) {
            return false;
        }
        if (removeAllWarOfTheRingAnimals && WarOfTheRingSpawnCompat.isWarOfTheRingEntity(entityClass)) {
            return true;
        }
        if (globallyBlockedClasses.contains(entityClass)) {
            return true;
        }

        Set<Class> biomeBlockedClasses = blockedClassesByBiome.get(biome);
        return biomeBlockedClasses != null && biomeBlockedClasses.contains(entityClass);
    }

    public static boolean spawnWorldGenEntityUnlessBlocked(World world, Entity entity) {
        if (entity instanceof EntityLiving) {
            BiomeGenBase biome = world.getBiomeGenForCoords(
                net.minecraft.util.MathHelper.floor_double(entity.posX),
                net.minecraft.util.MathHelper.floor_double(entity.posZ));
            if (isSpawnBlocked(entity.getClass(), biome)) {
                recordBlockedSpawnAttempt(entity.getClass(), biome, "LOTR world-gen");
                return false;
            }
        }
        return world.spawnEntityInWorld(entity);
    }

    public static void recordBlockedSpawnAttempt(Class entityClass, BiomeGenBase biome, String spawnPath) {
        if (!Config.logBlockedSpawnAttempts || entityClass == null || biome == null) {
            return;
        }

        Object registeredName = EntityList.classToStringMapping.get(entityClass);
        String entityName = registeredName instanceof String ? (String) registeredName : entityClass.getName();
        String biomeName = biome.biomeName == null ? "<unnamed>" : biome.biomeName;
        String summary = BLOCKED_ATTEMPT_LOG.record(
            entityName,
            biomeName + " (ID " + biome.biomeID + ")",
            spawnPath,
            System.currentTimeMillis(),
            Config.blockedSpawnLogIntervalSeconds * 1000L);
        if (summary != null) {
            HotNCold.LOG.info(summary);
        }
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
        return addSpawnEntryIfAbsentAndReturn(spawnEntries, entityClass, weight, minimumGroupSize, maximumGroupSize)
            != null;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static BiomeGenBase.SpawnListEntry addSpawnEntryIfAbsentAndReturn(List spawnEntries,
        Class<? extends EntityLiving> entityClass, int weight, int minimumGroupSize, int maximumGroupSize) {
        for (Object value : spawnEntries) {
            if (value instanceof BiomeGenBase.SpawnListEntry
                && ((BiomeGenBase.SpawnListEntry) value).entityClass == entityClass) {
                return null;
            }
        }

        BiomeGenBase.SpawnListEntry entry = new BiomeGenBase.SpawnListEntry(
            entityClass,
            weight,
            minimumGroupSize,
            maximumGroupSize);
        spawnEntries.add(entry);
        return entry;
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
        return removeBlockedEntries(spawnEntries, blockedEntityClasses, null);
    }

    private static int removeBlockedEntries(List spawnEntries, Set<Class> blockedEntityClasses,
        SpawnListJournal journal) {
        int removedEntries = 0;
        Iterator iterator = spawnEntries.iterator();
        int entryIndex = 0;

        while (iterator.hasNext()) {
            Object value = iterator.next();
            if (!(value instanceof BiomeGenBase.SpawnListEntry)) {
                entryIndex++;
                continue;
            }

            BiomeGenBase.SpawnListEntry entry = (BiomeGenBase.SpawnListEntry) value;
            if (blockedEntityClasses.contains(entry.entityClass)) {
                if (journal != null) {
                    journal.recordRemoved(spawnEntries, entry, entryIndex);
                }
                iterator.remove();
                removedEntries++;
            } else {
                entryIndex++;
            }
        }

        return removedEntries;
    }

    public static final class SpawnRuleResult {

        private final int undoneChanges;
        private final int addedEntries;
        private final int removedWarOfTheRingEntries;
        private final int removedGloballyBlockedEntries;
        private final int removedBiomeBlockedEntries;
        private final int rejectedAdditionTargets;

        SpawnRuleResult(int undoneChanges, int addedEntries, int removedWarOfTheRingEntries,
            int removedGloballyBlockedEntries, int removedBiomeBlockedEntries, int rejectedAdditionTargets) {
            this.undoneChanges = undoneChanges;
            this.addedEntries = addedEntries;
            this.removedWarOfTheRingEntries = removedWarOfTheRingEntries;
            this.removedGloballyBlockedEntries = removedGloballyBlockedEntries;
            this.removedBiomeBlockedEntries = removedBiomeBlockedEntries;
            this.rejectedAdditionTargets = rejectedAdditionTargets;
        }

        public String describeStartup() {
            int totalRemovedEntries = removedWarOfTheRingEntries + removedGloballyBlockedEntries
                + removedBiomeBlockedEntries;
            return "LOTR spawn summary: added " + addedEntries
                + "; removed "
                + totalRemovedEntries
                + " total ("
                + removedWarOfTheRingEntries
                + " War of the Ring, "
                + removedGloballyBlockedEntries
                + " globally blocked, "
                + removedBiomeBlockedEntries
                + " biome-blocked); rejected "
                + rejectedAdditionTargets
                + " duplicate addition target(s).";
        }

        public String describeReload() {
            return "Reloaded LOTR spawn rules: undid " + undoneChanges
                + " previous change(s); added "
                + addedEntries
                + ", removed "
                + removedWarOfTheRingEntries
                + " War of the Ring, "
                + removedGloballyBlockedEntries
                + " globally blocked, and "
                + removedBiomeBlockedEntries
                + " biome-blocked spawn entry/entries; rejected "
                + rejectedAdditionTargets
                + " duplicate addition target(s).";
        }
    }

    static final class SpawnAddition {

        final BiomeGenBase biome;
        final EnumCreatureType creatureType;
        final Class<? extends EntityLiving> entityClass;
        final int weight;
        final int minimumGroupSize;
        final int maximumGroupSize;

        SpawnAddition(BiomeGenBase biome, EnumCreatureType creatureType, Class<? extends EntityLiving> entityClass,
            int weight, int minimumGroupSize, int maximumGroupSize) {
            this.biome = biome;
            this.creatureType = creatureType;
            this.entityClass = entityClass;
            this.weight = weight;
            this.minimumGroupSize = minimumGroupSize;
            this.maximumGroupSize = maximumGroupSize;
        }
    }
}
