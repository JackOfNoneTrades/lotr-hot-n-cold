package org.fentanylsolutions.hotncold.compat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
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

    @Test
    public void explainsEveryBlockingRuleAndMatchingAddition() {
        Set<BiomeGenBase> biomes = singletonBiome(BiomeGenBase.plains);
        Set<Class> globallyBlockedClasses = Collections.<Class>singleton(EntityPig.class);
        Map<BiomeGenBase, Set<Class>> blockedClassesByBiome = new IdentityHashMap<>();
        blockedClassesByBiome.put(BiomeGenBase.plains, Collections.<Class>singleton(EntityPig.class));
        List<LOTRSpawnControl.SpawnAddition> additions = LOTRSpawnControl
            .resolveBiomeSpawnAdditions(new String[] { "Pig:Plains:creature:7:2:4" }, biomes);

        List<String> lines = LOTRSpawnReport.createSpawnExplanation(
            "pLaInS",
            "Pig",
            biomes,
            false,
            globallyBlockedClasses,
            blockedClassesByBiome,
            additions);

        assertContains(lines, "Status: BLOCKED");
        assertContains(lines, "blockedEntitiesInAllLOTRBiomes");
        assertContains(lines, "blockedEntityBiomeRules");
        assertContains(lines, "runtime spawn-list entry exists");
        assertContains(lines, "Matching addedEntityBiomeRules rule: creature, weight 7, group 2-4");
    }

    @Test
    public void distinguishesPresentAndAbsentEntities() {
        Set<BiomeGenBase> biomes = singletonBiome(BiomeGenBase.plains);

        List<String> present = LOTRSpawnReport.createSpawnExplanation(
            "Plains",
            "Pig",
            biomes,
            false,
            Collections.<Class>emptySet(),
            Collections.<BiomeGenBase, Set<Class>>emptyMap(),
            Collections.<LOTRSpawnControl.SpawnAddition>emptyList());
        List<String> absent = LOTRSpawnReport.createSpawnExplanation(
            "Plains",
            "Villager",
            biomes,
            false,
            Collections.<Class>emptySet(),
            Collections.<BiomeGenBase, Set<Class>>emptyMap(),
            Collections.<LOTRSpawnControl.SpawnAddition>emptyList());

        assertContains(present, "Status: PRESENT");
        assertContains(present, "creature - weight");
        assertContains(absent, "Status: ABSENT");
    }

    @Test
    public void explainsInvalidBiomeAndEntityTokens() {
        Set<BiomeGenBase> biomes = singletonBiome(BiomeGenBase.plains);

        List<String> missingEntity = LOTRSpawnReport.createSpawnExplanation(
            "Plains",
            "missingEntity",
            biomes,
            false,
            Collections.<Class>emptySet(),
            Collections.<BiomeGenBase, Set<Class>>emptyMap(),
            new ArrayList<LOTRSpawnControl.SpawnAddition>());
        List<String> missingBiome = LOTRSpawnReport.createSpawnExplanation(
            "missingBiome",
            "Villager",
            biomes,
            false,
            Collections.<Class>emptySet(),
            Collections.<BiomeGenBase, Set<Class>>emptyMap(),
            new ArrayList<LOTRSpawnControl.SpawnAddition>());

        assertContains(missingEntity, "Entity 'missingEntity' was not found");
        assertContains(missingBiome, "LOTR biome 'missingBiome' was not found");
    }

    private static Set<BiomeGenBase> singletonBiome(BiomeGenBase biome) {
        Set<BiomeGenBase> biomes = Collections.newSetFromMap(new IdentityHashMap<BiomeGenBase, Boolean>());
        biomes.add(biome);
        return biomes;
    }

    private static void assertContains(List<String> lines, String expectedText) {
        assertTrue(
            "Expected report to contain '" + expectedText + "' but was " + lines,
            lines.stream()
                .anyMatch(line -> line.contains(expectedText)));
    }
}
