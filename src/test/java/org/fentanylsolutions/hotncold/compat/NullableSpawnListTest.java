package org.fentanylsolutions.hotncold.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.world.biome.BiomeGenBase;

import org.junit.Test;

public class NullableSpawnListTest {

    @Test
    public void missingListsHaveNothingToRemoveOrAdd() {
        assertEquals(0, LOTRSpawnControl.removeBlockedEntries(null, Collections.<Class>singleton(EntityPig.class)));
        assertEquals(0, WarOfTheRingSpawnCompat.removeWarOfTheRingEntries(null));
        assertFalse(LOTRSpawnControl.addSpawnEntryIfAbsent(null, EntityPig.class, 10, 1, 3));
    }

    @Test
    public void emptySupportedListsAreStillWritable() {
        BiomeGenBase biome = biome(false);
        List entries = biome.getSpawnableList(EnumCreatureType.ambient);
        entries.clear();
        assertTrue(entries.isEmpty());
        assertTrue(LOTRSpawnControl.addSpawnEntryIfAbsent(entries, EntityPig.class, 10, 1, 3));
        assertEquals(1, LOTRSpawnControl.removeBlockedEntries(entries, Collections.<Class>singleton(EntityPig.class)));
    }

    @Test
    public void dumpsDistinguishUnsupportedCategoriesFromEmptyOnes() {
        Set<BiomeGenBase> biomes = Collections.singleton(biome(true));
        assertTrue(
            LOTRSpawnReport.createBiomeDump("nullable", null, biomes)
                .toString()
                .contains("ambient (unsupported by this biome)"));
        assertTrue(
            LOTRSpawnReport.createBiomeDump("nullable", "ambient", biomes)
                .toString()
                .contains("unsupported by this biome"));
        assertTrue(
            LOTRSpawnReport.createBiomeDump("nullable", "ambient", Collections.singleton(biome(false)))
                .toString()
                .contains("ambient (0)"));
    }

    @Test
    public void explanationsStillReportSupportedEntriesAndBlocks() {
        Set<BiomeGenBase> biomes = Collections.singleton(biome(true));
        String present = LOTRSpawnReport
            .createSpawnExplanation(
                "nullable",
                "Pig",
                biomes,
                false,
                Collections.<Class>emptySet(),
                Collections.emptyMap(),
                Collections.emptyList())
            .toString();
        assertTrue(present.contains("Status: PRESENT"));
        String blocked = LOTRSpawnReport
            .createSpawnExplanation(
                "nullable",
                "Pig",
                biomes,
                false,
                Collections.<Class>singleton(EntityPig.class),
                Collections.emptyMap(),
                Collections.emptyList())
            .toString();
        assertTrue(blocked.contains("Status: BLOCKED"));
    }

    @Test
    public void examplesDoNotSuggestUnsupportedAdditions() {
        String report = LOTRSpawnReport
            .createRuleExamples("nullable", "Pig", "ambient", Collections.singleton(biome(true)))
            .toString();
        assertTrue(report.contains("blockedEntityBiomeRules: Pig:nullable"));
        assertTrue(report.contains("unsupported"));
        assertFalse(report.contains("addedEntityBiomeRules:"));
    }

    @Test
    public void examplesExplainPartiallySupportedBiomeVariants() {
        Set<BiomeGenBase> biomes = new HashSet<>(Arrays.asList(biome(true), biome(false)));
        String report = LOTRSpawnReport.createRuleExamples("nullable", "Pig", "ambient", biomes)
            .toString();
        assertTrue(report.contains("addedEntityBiomeRules: Pig:nullable:ambient:10:1:3"));
        assertTrue(report.contains("supported by 1 of 2"));
    }

    private static BiomeGenBase biome(final boolean missingAmbient) {
        BiomeGenBase biome = new BiomeGenBase(0, false) {

            @Override
            public List<SpawnListEntry> getSpawnableList(EnumCreatureType type) {
                return missingAmbient && type == EnumCreatureType.ambient ? null : super.getSpawnableList(type);
            }
        }.setBiomeName("nullable");
        if (!missingAmbient) {
            biome.getSpawnableList(EnumCreatureType.ambient)
                .clear();
        }
        return biome;
    }
}
