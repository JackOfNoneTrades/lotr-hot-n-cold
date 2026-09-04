package org.fentanylsolutions.hotncold.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.biome.BiomeGenBase;

import org.junit.Test;

import wotrmc.common.entities.TestWarOfTheRingAnimal;
import wotrmc.common.entities.TestWarOfTheRingNPC;

public class WarOfTheRingSpawnCompatTest {

    @Test
    public void identifiesOnlyWarOfTheRingEntityPackage() {
        assertTrue(WarOfTheRingSpawnCompat.isWarOfTheRingEntity(TestWarOfTheRingAnimal.class));
        assertFalse(WarOfTheRingSpawnCompat.isWarOfTheRingEntity(WarOfTheRingSpawnCompatTest.class));
        assertFalse(WarOfTheRingSpawnCompat.isWarOfTheRingEntity(null));
        assertTrue(WarOfTheRingSpawnCompat.isWarOfTheRingAnimal(TestWarOfTheRingAnimal.class));
        assertFalse(WarOfTheRingSpawnCompat.isWarOfTheRingAnimal(TestWarOfTheRingNPC.class));
        assertFalse(WarOfTheRingSpawnCompat.isWarOfTheRingAnimal(net.minecraft.entity.passive.EntityCow.class));
        assertFalse(WarOfTheRingSpawnCompat.isWarOfTheRingAnimal(null));
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void removesOnlyWarOfTheRingSpawnEntries() {
        BiomeGenBase.SpawnListEntry warOfTheRingEntry = new BiomeGenBase.SpawnListEntry(
            (Class) TestWarOfTheRingAnimal.class,
            10,
            1,
            3);
        BiomeGenBase.SpawnListEntry regularEntry = new BiomeGenBase.SpawnListEntry(
            (Class) WarOfTheRingSpawnCompatTest.class,
            8,
            2,
            4);
        Object unknownEntry = new Object();
        List entries = new ArrayList();
        entries.add(warOfTheRingEntry);
        entries.add(regularEntry);
        entries.add(unknownEntry);
        BiomeGenBase.SpawnListEntry npcEntry = new BiomeGenBase.SpawnListEntry(TestWarOfTheRingNPC.class, 8, 1, 2);
        entries.add(npcEntry);

        assertEquals(1, WarOfTheRingSpawnCompat.removeWarOfTheRingEntries(entries));
        assertEquals(3, entries.size());
        assertSame(regularEntry, entries.get(0));
        assertSame(unknownEntry, entries.get(1));
        assertSame(npcEntry, entries.get(2));
    }
}
