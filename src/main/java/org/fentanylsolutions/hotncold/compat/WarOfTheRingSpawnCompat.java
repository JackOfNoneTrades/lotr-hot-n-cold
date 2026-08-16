package org.fentanylsolutions.hotncold.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.biome.BiomeGenBase;

import org.fentanylsolutions.hotncold.Config;
import org.fentanylsolutions.hotncold.HotNCold;
import org.fentanylsolutions.hotncold.util.BiomeUtil;

import lotr.common.world.biome.LOTRBiome;

public final class WarOfTheRingSpawnCompat {

    private static final String WOTR_ENTITY_PACKAGE = "wotrmc.common.entities.";
    private static final String WOTR_BIOME_CLASS = "wotrmc.common.map.WOTRMCBiomes";

    private WarOfTheRingSpawnCompat() {}

    public static void removeAnimalSpawnsIfConfigured() {
        if (!Config.removeAllWarOfTheRingAnimalSpawns) {
            return;
        }

        int removedEntries = 0;
        int changedBiomes = 0;

        for (BiomeGenBase biome : getAllLOTRSpawnBiomes()) {
            int removedFromBiome = removeWarOfTheRingEntries(biome.getSpawnableList(EnumCreatureType.creature));
            if (removedFromBiome > 0) {
                removedEntries += removedFromBiome;
                changedBiomes++;
            }
        }

        HotNCold.LOG.info(
            "Removed {} War of the Ring natural animal spawn entries from {} LOTR biomes",
            removedEntries,
            changedBiomes);
    }

    public static int countWarOfTheRingAnimalSpawns() {
        int count = 0;
        for (BiomeGenBase biome : getAllLOTRSpawnBiomes()) {
            for (Object value : biome.getSpawnableList(EnumCreatureType.creature)) {
                if (value instanceof BiomeGenBase.SpawnListEntry
                    && isWarOfTheRingEntity(((BiomeGenBase.SpawnListEntry) value).entityClass)) {
                    count++;
                }
            }
        }
        return count;
    }

    static Set<BiomeGenBase> getAllLOTRSpawnBiomes() {
        Set<BiomeGenBase> biomes = Collections.newSetFromMap(new IdentityHashMap<BiomeGenBase, Boolean>());
        for (BiomeGenBase biome : BiomeGenBase.getBiomeGenArray()) {
            if (biome instanceof LOTRBiome) {
                biomes.add(biome);
            }
        }
        biomes.addAll(BiomeUtil.getAllLOTRBiomes());
        addStaticBiomeFields(biomes, LOTRBiome.class);

        try {
            addStaticBiomeFields(biomes, Class.forName(WOTR_BIOME_CLASS));
        } catch (ClassNotFoundException ignored) {
            // War of the Ring is optional and this method is harmless when it is absent.
        }

        return biomes;
    }

    private static void addStaticBiomeFields(Set<BiomeGenBase> biomes, Class biomeHolderClass) {
        for (Field field : biomeHolderClass.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || !BiomeGenBase.class.isAssignableFrom(field.getType())) {
                continue;
            }

            try {
                field.setAccessible(true);
                BiomeGenBase biome = (BiomeGenBase) field.get(null);
                if (biome != null) {
                    biomes.add(biome);
                }
            } catch (IllegalAccessException e) {
                HotNCold.LOG
                    .warn("Could not inspect biome field {}.{}", biomeHolderClass.getName(), field.getName(), e);
            }
        }
    }

    static int removeWarOfTheRingEntries(List spawnEntries) {
        int removedEntries = 0;
        Iterator iterator = spawnEntries.iterator();

        while (iterator.hasNext()) {
            Object value = iterator.next();
            if (!(value instanceof BiomeGenBase.SpawnListEntry)) {
                continue;
            }

            BiomeGenBase.SpawnListEntry entry = (BiomeGenBase.SpawnListEntry) value;
            if (isWarOfTheRingEntity(entry.entityClass)) {
                iterator.remove();
                removedEntries++;
            }
        }

        return removedEntries;
    }

    static boolean isWarOfTheRingEntity(Class entityClass) {
        return entityClass != null && entityClass.getName()
            .startsWith(WOTR_ENTITY_PACKAGE);
    }
}
