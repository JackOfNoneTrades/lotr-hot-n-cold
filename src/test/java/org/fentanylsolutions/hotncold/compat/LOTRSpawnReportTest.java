package org.fentanylsolutions.hotncold.compat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.world.biome.BiomeGenBase;

import org.junit.Test;

public class LOTRSpawnReportTest {

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void reportsEffectiveEntriesForTheSelectedCategory() {
        Set<BiomeGenBase> biomes = Collections.newSetFromMap(new IdentityHashMap<BiomeGenBase, Boolean>());
        biomes.add(BiomeGenBase.plains);
        List monsterEntries = BiomeGenBase.plains.getSpawnableList(EnumCreatureType.monster);
        BiomeGenBase.SpawnListEntry testEntry = new BiomeGenBase.SpawnListEntry(EntityPig.class, 7, 2, 4);
        monsterEntries.add(testEntry);

        try {
            List<String> lines = LOTRSpawnReport.createBiomeDump("pLaInS", "MoNsTeR", biomes);

            assertTrue(
                lines.get(0)
                    .contains("Found 1 LOTR biome variant"));
            assertTrue(
                lines.stream()
                    .anyMatch(line -> line.contains("Pig - weight 7, group 2-4")));
            assertFalse(
                lines.stream()
                    .anyMatch(line -> line.startsWith("  creature")));
        } finally {
            monsterEntries.remove(testEntry);
        }
    }

    @Test
    public void explainsInvalidBiomeAndCategoryTokens() {
        Set<BiomeGenBase> biomes = Collections.newSetFromMap(new IdentityHashMap<BiomeGenBase, Boolean>());
        biomes.add(BiomeGenBase.plains);

        List<String> missingBiome = LOTRSpawnReport.createBiomeDump("missingBiome", null, biomes);
        List<String> missingCategory = LOTRSpawnReport.createBiomeDump("Plains", "missingCategory", biomes);

        assertTrue(
            missingBiome.get(0)
                .contains("was not found"));
        assertTrue(
            missingCategory.get(0)
                .contains("Available: monster, creature, ambient, waterCreature"));
    }
}
