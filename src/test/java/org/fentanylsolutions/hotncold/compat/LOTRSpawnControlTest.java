package org.fentanylsolutions.hotncold.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.world.biome.BiomeGenBase;

import org.junit.Test;

public class LOTRSpawnControlTest {

    @Test
    public void resolvesExactRegisteredEntityNamesAndSkipsInvalidNames() {
        Set<Class> resolved = LOTRSpawnControl
            .resolveBlockedEntityClasses(new String[] { " Cow ", "Cow", "cow", "missing.test.entity", "", null });

        assertEquals(1, resolved.size());
        assertTrue(resolved.contains(EntityCow.class));
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void removesOnlyConfiguredEntityClasses() {
        BiomeGenBase.SpawnListEntry blockedEntry = new BiomeGenBase.SpawnListEntry(EntityCow.class, 10, 1, 3);
        BiomeGenBase.SpawnListEntry allowedEntry = new BiomeGenBase.SpawnListEntry(EntityPig.class, 8, 2, 4);
        Object unknownEntry = new Object();
        List entries = new ArrayList();
        entries.add(blockedEntry);
        entries.add(allowedEntry);
        entries.add(unknownEntry);

        assertEquals(1, LOTRSpawnControl.removeBlockedEntries(entries, Collections.<Class>singleton(EntityCow.class)));
        assertEquals(2, entries.size());
        assertSame(allowedEntry, entries.get(0));
        assertSame(unknownEntry, entries.get(1));
        assertFalse(entries.contains(blockedEntry));
    }

    @Test
    public void resolvesEntityRulesByBiomeNameOrId() {
        Set<BiomeGenBase> lotrBiomes = Collections.newSetFromMap(new IdentityHashMap<BiomeGenBase, Boolean>());
        lotrBiomes.add(BiomeGenBase.plains);
        lotrBiomes.add(BiomeGenBase.desert);

        Map<BiomeGenBase, Set<Class>> resolved = LOTRSpawnControl.resolveBiomeBlockedEntityClasses(
            new String[] { "Cow:pLaInS", "Pig:" + BiomeGenBase.desert.biomeID, "Cow:missingBiome",
                "missing.test.entity:Plains", "invalidRule" },
            lotrBiomes);

        assertEquals(2, resolved.size());
        assertEquals(Collections.<Class>singleton(EntityCow.class), resolved.get(BiomeGenBase.plains));
        assertEquals(Collections.<Class>singleton(EntityPig.class), resolved.get(BiomeGenBase.desert));
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void leavesTheSameEntityAvailableOutsideTheTargetBiome() {
        BiomeGenBase.SpawnListEntry targetEntry = new BiomeGenBase.SpawnListEntry(EntityCow.class, 10, 1, 3);
        BiomeGenBase.SpawnListEntry otherBiomeEntry = new BiomeGenBase.SpawnListEntry(EntityCow.class, 10, 1, 3);
        List targetBiomeEntries = new ArrayList();
        List otherBiomeEntries = new ArrayList();
        targetBiomeEntries.add(targetEntry);
        otherBiomeEntries.add(otherBiomeEntry);

        Set<Class> blockedClasses = new LinkedHashSet<>();
        blockedClasses.add(EntityCow.class);
        assertEquals(1, LOTRSpawnControl.removeBlockedEntries(targetBiomeEntries, blockedClasses));

        assertTrue(targetBiomeEntries.isEmpty());
        assertSame(otherBiomeEntry, otherBiomeEntries.get(0));
    }
}
