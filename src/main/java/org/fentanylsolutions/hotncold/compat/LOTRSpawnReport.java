package org.fentanylsolutions.hotncold.compat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.biome.BiomeGenBase;

public final class LOTRSpawnReport {

    private LOTRSpawnReport() {}

    public static List<String> createBiomeDump(String biomeToken, String categoryToken) {
        return createBiomeDump(biomeToken, categoryToken, WarOfTheRingSpawnCompat.getAllLOTRSpawnBiomes());
    }

    public static List<String> createSpawnExplanation(String biomeToken, String entityToken) {
        return createSpawnExplanation(
            biomeToken,
            entityToken,
            LOTRSpawnControl.getPreparedLOTRBiomes(),
            LOTRSpawnControl.removesAllWarOfTheRingAnimals(),
            LOTRSpawnControl.getGloballyBlockedClasses(),
            LOTRSpawnControl.getBlockedClassesByBiome(),
            LOTRSpawnControl.getConfiguredAdditions());
    }

    public static List<String> createRuleExamples(String biomeToken, String entityToken, String categoryToken) {
        return createRuleExamples(
            biomeToken,
            entityToken,
            categoryToken,
            WarOfTheRingSpawnCompat.getAllLOTRSpawnBiomes());
    }

    static List<String> createBiomeDump(String biomeToken, String categoryToken, Set<BiomeGenBase> lotrBiomes) {
        EnumCreatureType selectedType = null;
        if (categoryToken != null) {
            selectedType = LOTRSpawnControl.resolveCreatureType(categoryToken);
            if (selectedType == null) {
                return Collections.singletonList(
                    "Unknown spawn category '" + categoryToken
                        + "'. Available: "
                        + String.join(", ", getCreatureTypeNames()));
            }
        }

        Set<BiomeGenBase> matchingBiomes = LOTRSpawnControl.resolveLOTRSpawnBiomes(biomeToken, lotrBiomes);
        if (matchingBiomes.isEmpty()) {
            return Collections.singletonList(
                "LOTR biome '" + biomeToken + "' was not found. Enable printBiomes to list valid names and IDs.");
        }

        List<BiomeGenBase> sortedBiomes = getSortedBiomes(matchingBiomes);

        List<String> lines = new ArrayList<>();
        lines.add("Found " + sortedBiomes.size() + " LOTR biome variant(s) matching '" + biomeToken + "'.");
        for (BiomeGenBase biome : sortedBiomes) {
            lines.add(
                "Biome " + biome.biomeName
                    + " (ID "
                    + biome.biomeID
                    + ", "
                    + biome.getClass()
                        .getSimpleName()
                    + ")");
            if (selectedType == null) {
                for (EnumCreatureType creatureType : EnumCreatureType.values()) {
                    appendCategory(lines, biome, creatureType);
                }
            } else {
                appendCategory(lines, biome, selectedType);
            }
        }
        return lines;
    }

    static List<String> createSpawnExplanation(String biomeToken, String entityToken, Set<BiomeGenBase> lotrBiomes,
        boolean removeAllWarOfTheRingAnimals, Set<Class> globallyBlockedClasses,
        Map<BiomeGenBase, Set<Class>> blockedClassesByBiome, List<LOTRSpawnControl.SpawnAddition> configuredAdditions) {
        Object mappedEntityClass = EntityList.stringToClassMapping.get(entityToken);
        if (!(mappedEntityClass instanceof Class)) {
            return Collections.singletonList(
                "Entity '" + entityToken
                    + "' was not found. Names are exact and case-sensitive; enable printMobs to list valid names.");
        }
        Class entityClass = (Class) mappedEntityClass;

        Set<BiomeGenBase> matchingBiomes = LOTRSpawnControl.resolveLOTRSpawnBiomes(biomeToken, lotrBiomes);
        if (matchingBiomes.isEmpty()) {
            return Collections.singletonList(
                "LOTR biome '" + biomeToken + "' was not found. Enable printBiomes to list valid names and IDs.");
        }

        List<BiomeGenBase> sortedBiomes = getSortedBiomes(matchingBiomes);
        List<String> lines = new ArrayList<>();
        lines.add("Entity " + getEntityName(entityClass) + " (" + entityClass.getName() + ")");
        lines.add("Found " + sortedBiomes.size() + " LOTR biome variant(s) matching '" + biomeToken + "'.");
        for (BiomeGenBase biome : sortedBiomes) {
            appendExplanation(
                lines,
                biome,
                entityClass,
                lotrBiomes,
                removeAllWarOfTheRingAnimals,
                globallyBlockedClasses,
                blockedClassesByBiome,
                configuredAdditions);
        }
        return lines;
    }

    static List<String> createRuleExamples(String biomeToken, String entityToken, String categoryToken,
        Set<BiomeGenBase> lotrBiomes) {
        Object mappedEntityClass = EntityList.stringToClassMapping.get(entityToken);
        if (!(mappedEntityClass instanceof Class)) {
            return Collections.singletonList(
                "Entity '" + entityToken
                    + "' was not found. Names are exact and case-sensitive; enable printMobs to list valid names.");
        }
        Class entityClass = (Class) mappedEntityClass;

        Set<BiomeGenBase> matchingBiomes = LOTRSpawnControl.resolveLOTRSpawnBiomes(biomeToken, lotrBiomes);
        if (matchingBiomes.isEmpty()) {
            return Collections.singletonList(
                "LOTR biome '" + biomeToken + "' was not found. Enable printBiomes to list valid names and IDs.");
        }

        String selectedCategory = categoryToken == null ? EnumCreatureType.creature.name() : categoryToken;
        EnumCreatureType creatureType = LOTRSpawnControl.resolveCreatureType(selectedCategory);
        if (creatureType == null) {
            return Collections.singletonList(
                "Unknown spawn category '" + selectedCategory
                    + "'. Available: "
                    + String.join(", ", getCreatureTypeNames()));
        }

        List<BiomeGenBase> sortedBiomes = getSortedBiomes(matchingBiomes);
        String ruleEntityName = getEntityName(entityClass);
        String ruleBiomeToken = getCanonicalRuleBiomeToken(biomeToken, sortedBiomes.get(0));
        List<String> lines = new ArrayList<>();
        lines.add(
            "Ready-to-copy rules for " + ruleEntityName
                + " in "
                + ruleBiomeToken
                + " ("
                + sortedBiomes.size()
                + " matching biome variant(s)):");
        lines.add("  blockedEntitiesInAllLOTRBiomes: " + ruleEntityName);
        lines.add("  blockedEntityBiomeRules: " + ruleEntityName + ":" + ruleBiomeToken);
        if (EntityLiving.class.isAssignableFrom(entityClass)) {
            long supported = sortedBiomes.stream()
                .filter(biome -> biome.getSpawnableList(creatureType) != null)
                .count();
            if (supported == 0) {
                lines.add(
                    "  Addition unavailable: category '" + creatureType.name() + "' is unsupported by these biomes.");
                return lines;
            }
            lines.add(
                "  addedEntityBiomeRules: " + ruleEntityName
                    + ":"
                    + ruleBiomeToken
                    + ":"
                    + creatureType.name()
                    + ":10:1:3");
            lines.add("  Addition defaults shown: weight 10, group 1-3; adjust them before use.");
            if (supported < sortedBiomes.size()) {
                lines.add(
                    "  This category is supported by " + supported
                        + " of "
                        + sortedBiomes.size()
                        + " matching biome variants; unsupported variants will be skipped.");
            }
        } else {
            lines.add("  This entity is not living and cannot be added as a natural spawn.");
        }
        return lines;
    }

    public static String[] getBiomeNamesAndIds() {
        Set<String> values = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (BiomeGenBase biome : WarOfTheRingSpawnCompat.getAllLOTRSpawnBiomes()) {
            values.add(biome.biomeName);
            values.add(Integer.toString(biome.biomeID));
        }
        return values.toArray(new String[0]);
    }

    public static String[] getCreatureTypeNames() {
        Set<String> names = new LinkedHashSet<>();
        for (EnumCreatureType creatureType : EnumCreatureType.values()) {
            names.add(creatureType.name());
        }
        return names.toArray(new String[0]);
    }

    public static String[] getEntityNames() {
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Object value : EntityList.stringToClassMapping.keySet()) {
            if (value instanceof String) {
                names.add((String) value);
            }
        }
        return names.toArray(new String[0]);
    }

    private static void appendExplanation(List<String> lines, BiomeGenBase biome, Class entityClass,
        Set<BiomeGenBase> lotrBiomes, boolean removeAllWarOfTheRingAnimals, Set<Class> globallyBlockedClasses,
        Map<BiomeGenBase, Set<Class>> blockedClassesByBiome, List<LOTRSpawnControl.SpawnAddition> configuredAdditions) {
        lines.add(
            "Biome " + biome.biomeName
                + " (ID "
                + biome.biomeID
                + ", "
                + biome.getClass()
                    .getSimpleName()
                + ")");

        boolean warOfTheRingBlocked = removeAllWarOfTheRingAnimals
            && WarOfTheRingSpawnCompat.isWarOfTheRingAnimal(entityClass);
        boolean globallyBlocked = globallyBlockedClasses.contains(entityClass);
        Set<Class> biomeBlockedClasses = blockedClassesByBiome.get(biome);
        boolean biomeBlocked = biomeBlockedClasses != null && biomeBlockedClasses.contains(entityClass);
        boolean blocked = LOTRSpawnControl.isSpawnBlocked(
            entityClass,
            biome,
            lotrBiomes,
            removeAllWarOfTheRingAnimals,
            globallyBlockedClasses,
            blockedClassesByBiome);
        List<CategorizedSpawnEntry> entries = getEntityEntries(biome, entityClass);
        Set<String> additionDescriptions = getAdditionDescriptions(biome, entityClass, configuredAdditions);

        if (blocked) {
            lines.add("  Status: BLOCKED");
            if (warOfTheRingBlocked) {
                lines.add("    Reason: removeAllWarOfTheRingAnimalSpawns=true");
            }
            if (globallyBlocked) {
                lines.add("    Reason: blockedEntitiesInAllLOTRBiomes");
            }
            if (biomeBlocked) {
                lines.add("    Reason: blockedEntityBiomeRules matches this biome");
            }
            if (!entries.isEmpty()) {
                lines.add("    A runtime spawn-list entry exists, but the final spawn guard still rejects it.");
                appendEntries(lines, entries);
            }
        } else if (!entries.isEmpty()) {
            lines.add("  Status: PRESENT - listed for natural spawning");
            appendEntries(lines, entries);
        } else {
            lines.add("  Status: ABSENT - no natural spawn-list entry is present");
        }

        for (String description : additionDescriptions) {
            lines.add("    Matching addedEntityBiomeRules rule: " + description);
        }
    }

    private static void appendEntries(List<String> lines, List<CategorizedSpawnEntry> entries) {
        for (CategorizedSpawnEntry categorizedEntry : entries) {
            BiomeGenBase.SpawnListEntry entry = categorizedEntry.entry;
            lines.add(
                "    " + categorizedEntry.creatureType.name()
                    + " - weight "
                    + entry.itemWeight
                    + ", group "
                    + entry.minGroupCount
                    + "-"
                    + entry.maxGroupCount);
        }
    }

    private static List<CategorizedSpawnEntry> getEntityEntries(BiomeGenBase biome, Class entityClass) {
        List<CategorizedSpawnEntry> entries = new ArrayList<>();
        for (EnumCreatureType creatureType : EnumCreatureType.values()) {
            List spawnEntries = biome.getSpawnableList(creatureType);
            if (spawnEntries == null) {
                continue;
            }
            for (Object value : spawnEntries) {
                if (value instanceof BiomeGenBase.SpawnListEntry
                    && ((BiomeGenBase.SpawnListEntry) value).entityClass == entityClass) {
                    entries.add(new CategorizedSpawnEntry(creatureType, (BiomeGenBase.SpawnListEntry) value));
                }
            }
        }
        entries.sort(
            Comparator.comparing((CategorizedSpawnEntry value) -> value.creatureType.name())
                .thenComparingInt(value -> value.entry.itemWeight)
                .thenComparingInt(value -> value.entry.minGroupCount)
                .thenComparingInt(value -> value.entry.maxGroupCount));
        return entries;
    }

    private static Set<String> getAdditionDescriptions(BiomeGenBase biome, Class entityClass,
        List<LOTRSpawnControl.SpawnAddition> configuredAdditions) {
        Set<String> descriptions = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (LOTRSpawnControl.SpawnAddition addition : configuredAdditions) {
            if (addition.biome == biome && addition.entityClass == entityClass) {
                descriptions.add(
                    addition.creatureType.name() + ", weight "
                        + addition.weight
                        + ", group "
                        + addition.minimumGroupSize
                        + "-"
                        + addition.maximumGroupSize
                        + (biome.getSpawnableList(addition.creatureType) == null
                            ? " (ignored: category unsupported by this biome)"
                            : ""));
            }
        }
        return descriptions;
    }

    private static List<BiomeGenBase> getSortedBiomes(Set<BiomeGenBase> biomes) {
        List<BiomeGenBase> sortedBiomes = new ArrayList<>(biomes);
        sortedBiomes.sort(
            Comparator.comparing((BiomeGenBase biome) -> biome.biomeName)
                .thenComparingInt(biome -> biome.biomeID)
                .thenComparing(
                    biome -> biome.getClass()
                        .getName()));
        return sortedBiomes;
    }

    private static String getCanonicalRuleBiomeToken(String requestedToken, BiomeGenBase matchingBiome) {
        try {
            Integer.parseInt(requestedToken);
            return Integer.toString(matchingBiome.biomeID);
        } catch (NumberFormatException ignored) {
            return matchingBiome.biomeName;
        }
    }

    private static void appendCategory(List<String> lines, BiomeGenBase biome, EnumCreatureType creatureType) {
        List spawnEntries = biome.getSpawnableList(creatureType);
        if (spawnEntries == null) {
            lines.add("  " + creatureType.name() + " (unsupported by this biome)");
            return;
        }
        List<BiomeGenBase.SpawnListEntry> entries = getSortedEntries(biome, spawnEntries);
        lines.add("  " + creatureType.name() + " (" + entries.size() + ")");
        for (BiomeGenBase.SpawnListEntry entry : entries) {
            lines.add(
                "    " + getEntityName(entry.entityClass)
                    + " - weight "
                    + entry.itemWeight
                    + ", group "
                    + entry.minGroupCount
                    + "-"
                    + entry.maxGroupCount);
        }
    }

    private static List<BiomeGenBase.SpawnListEntry> getSortedEntries(BiomeGenBase biome, List spawnEntries) {
        List<BiomeGenBase.SpawnListEntry> entries = new ArrayList<>();
        for (Object value : spawnEntries) {
            if (value instanceof BiomeGenBase.SpawnListEntry
                && !LOTRSpawnControl.isSpawnBlocked(((BiomeGenBase.SpawnListEntry) value).entityClass, biome)) {
                entries.add((BiomeGenBase.SpawnListEntry) value);
            }
        }
        entries.sort(
            Comparator.comparing((BiomeGenBase.SpawnListEntry entry) -> getEntityName(entry.entityClass))
                .thenComparingInt(entry -> entry.itemWeight)
                .thenComparingInt(entry -> entry.minGroupCount)
                .thenComparingInt(entry -> entry.maxGroupCount));
        return entries;
    }

    private static String getEntityName(Class entityClass) {
        Object registeredName = EntityList.classToStringMapping.get(entityClass);
        if (registeredName instanceof String) {
            return (String) registeredName;
        }
        return entityClass == null ? "<unknown>" : entityClass.getName();
    }

    private static final class CategorizedSpawnEntry {

        private final EnumCreatureType creatureType;
        private final BiomeGenBase.SpawnListEntry entry;

        private CategorizedSpawnEntry(EnumCreatureType creatureType, BiomeGenBase.SpawnListEntry entry) {
            this.creatureType = creatureType;
            this.entry = entry;
        }
    }
}
