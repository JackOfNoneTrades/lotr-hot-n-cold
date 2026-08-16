package org.fentanylsolutions.hotncold.compat;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
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
