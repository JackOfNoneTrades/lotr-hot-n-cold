package org.fentanylsolutions.hotncold.compat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.biome.BiomeGenBase;

public final class LOTRSpawnReport {

    private LOTRSpawnReport() {}

    public static List<String> createBiomeDump(String biomeToken, String categoryToken) {
        return createBiomeDump(biomeToken, categoryToken, WarOfTheRingSpawnCompat.getAllLOTRSpawnBiomes());
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

        List<BiomeGenBase> sortedBiomes = new ArrayList<>(matchingBiomes);
        sortedBiomes.sort(
            Comparator.comparing((BiomeGenBase biome) -> biome.biomeName)
                .thenComparingInt(biome -> biome.biomeID)
                .thenComparing(
                    biome -> biome.getClass()
                        .getName()));

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

    private static void appendCategory(List<String> lines, BiomeGenBase biome, EnumCreatureType creatureType) {
        List<BiomeGenBase.SpawnListEntry> entries = getSortedEntries(biome, creatureType);
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

    private static List<BiomeGenBase.SpawnListEntry> getSortedEntries(BiomeGenBase biome,
        EnumCreatureType creatureType) {
        List<BiomeGenBase.SpawnListEntry> entries = new ArrayList<>();
        for (Object value : biome.getSpawnableList(creatureType)) {
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
}
