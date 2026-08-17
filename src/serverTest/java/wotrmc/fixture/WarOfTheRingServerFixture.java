package wotrmc.fixture;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.world.biome.BiomeGenBase;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fentanylsolutions.hotncold.Config;
import org.fentanylsolutions.hotncold.compat.WarOfTheRingSpawnCompat;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.registry.EntityRegistry;
import hotncold.fixture.entities.AllowedTestAnimal;
import hotncold.fixture.entities.BiomeBlockedTestAnimal;
import hotncold.fixture.entities.BlockedTestAnimal;
import lotr.common.world.biome.LOTRBiome;
import wotrmc.common.entities.ServerTestAnimal;

@Mod(
    modid = "hotncold_wotrmc_fixture",
    name = "War of the Ring server test fixture",
    version = "test-only",
    dependencies = "required-after:lotr;after:wotrmc;before:hotncold")
public final class WarOfTheRingServerFixture {

    private static final String BLOCKED_ENTITY_NAME = "hotncold_wotrmc_fixture.BlockedTestAnimal";
    private static final String BIOME_BLOCKED_ENTITY_NAME = "hotncold_wotrmc_fixture.BiomeBlockedTestAnimal";
    private static final Logger LOG = LogManager.getLogger("WOTR server fixture");
    private static BiomeGenBase testBiome;
    private static BiomeGenBase otherBiome;
    private static BiomeGenBase.SpawnListEntry fixtureEntry;
    private static final Map<EnumCreatureType, BiomeGenBase.SpawnListEntry> blockedEntries = new IdentityHashMap<>();
    private static final Map<EnumCreatureType, BiomeGenBase.SpawnListEntry> allowedEntries = new IdentityHashMap<>();
    private static final Map<EnumCreatureType, BiomeGenBase.SpawnListEntry> biomeBlockedEntries = new IdentityHashMap<>();
    private static final Map<EnumCreatureType, BiomeGenBase.SpawnListEntry> otherBiomeEntries = new IdentityHashMap<>();
    private static BiomeGenBase.SpawnListEntry controlEntry;

    @Mod.EventHandler
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void init(FMLInitializationEvent event) {
        EntityRegistry.registerModEntity(BlockedTestAnimal.class, "BlockedTestAnimal", 0, this, 64, 3, true);
        EntityRegistry.registerModEntity(AllowedTestAnimal.class, "AllowedTestAnimal", 1, this, 64, 3, true);
        EntityRegistry.registerModEntity(BiomeBlockedTestAnimal.class, "BiomeBlockedTestAnimal", 2, this, 64, 3, true);

        testBiome = LOTRBiome.shire;
        otherBiome = LOTRBiome.mordor;
        List spawnEntries = testBiome.getSpawnableList(EnumCreatureType.creature);
        fixtureEntry = new BiomeGenBase.SpawnListEntry(
            (Class<? extends EntityLiving>) (Class) ServerTestAnimal.class,
            10,
            1,
            3);
        controlEntry = new BiomeGenBase.SpawnListEntry(EntityCow.class, 8, 1, 2);
        spawnEntries.add(fixtureEntry);
        spawnEntries.add(controlEntry);

        for (EnumCreatureType creatureType : EnumCreatureType.values()) {
            BiomeGenBase.SpawnListEntry blockedEntry = new BiomeGenBase.SpawnListEntry(
                BlockedTestAnimal.class,
                10,
                1,
                3);
            BiomeGenBase.SpawnListEntry allowedEntry = new BiomeGenBase.SpawnListEntry(
                AllowedTestAnimal.class,
                10,
                1,
                3);
            BiomeGenBase.SpawnListEntry biomeBlockedEntry = new BiomeGenBase.SpawnListEntry(
                BiomeBlockedTestAnimal.class,
                10,
                1,
                3);
            BiomeGenBase.SpawnListEntry otherBiomeEntry = new BiomeGenBase.SpawnListEntry(
                BiomeBlockedTestAnimal.class,
                10,
                1,
                3);
            testBiome.getSpawnableList(creatureType)
                .add(blockedEntry);
            testBiome.getSpawnableList(creatureType)
                .add(allowedEntry);
            testBiome.getSpawnableList(creatureType)
                .add(biomeBlockedEntry);
            otherBiome.getSpawnableList(creatureType)
                .add(otherBiomeEntry);
            blockedEntries.put(creatureType, blockedEntry);
            allowedEntries.put(creatureType, allowedEntry);
            biomeBlockedEntries.put(creatureType, biomeBlockedEntry);
            otherBiomeEntries.put(creatureType, otherBiomeEntry);
        }
        LOG.info(
            "SERVER_FIXTURE_REGISTERED: added all-WOTR, global, biome-specific and control entries in every category");
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        Config.removeAllWarOfTheRingAnimalSpawns = true;
        String[] configuredEntities = Config.blockedEntitiesInAllLOTRBiomes;
        Config.blockedEntitiesInAllLOTRBiomes = Arrays.copyOf(configuredEntities, configuredEntities.length + 1);
        Config.blockedEntitiesInAllLOTRBiomes[configuredEntities.length] = BLOCKED_ENTITY_NAME;

        String[] configuredBiomeRules = Config.blockedEntityBiomeRules;
        Config.blockedEntityBiomeRules = Arrays.copyOf(configuredBiomeRules, configuredBiomeRules.length + 1);
        Config.blockedEntityBiomeRules[configuredBiomeRules.length] = BIOME_BLOCKED_ENTITY_NAME + ":"
            + testBiome.biomeName;
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        List spawnEntries = testBiome.getSpawnableList(EnumCreatureType.creature);
        boolean fixtureRemoved = !spawnEntries.contains(fixtureEntry);
        boolean blockedRemoved = true;
        boolean allowedPreserved = true;
        boolean biomeBlockedRemoved = true;
        boolean otherBiomePreserved = true;
        for (EnumCreatureType creatureType : EnumCreatureType.values()) {
            List categoryEntries = testBiome.getSpawnableList(creatureType);
            blockedRemoved &= !categoryEntries.contains(blockedEntries.get(creatureType));
            allowedPreserved &= categoryEntries.contains(allowedEntries.get(creatureType));
            biomeBlockedRemoved &= !categoryEntries.contains(biomeBlockedEntries.get(creatureType));
            otherBiomePreserved &= otherBiome.getSpawnableList(creatureType)
                .contains(otherBiomeEntries.get(creatureType));
        }
        boolean controlPreserved = spawnEntries.contains(controlEntry);
        int remainingWarOfTheRingEntries = WarOfTheRingSpawnCompat.countWarOfTheRingAnimalSpawns();

        if (!fixtureRemoved || !blockedRemoved
            || !allowedPreserved
            || !biomeBlockedRemoved
            || !otherBiomePreserved
            || !controlPreserved
            || remainingWarOfTheRingEntries != 0) {
            throw new AssertionError(
                "Spawn cleanup integration check failed: fixtureRemoved=" + fixtureRemoved
                    + ", blockedRemoved="
                    + blockedRemoved
                    + ", allowedPreserved="
                    + allowedPreserved
                    + ", biomeBlockedRemoved="
                    + biomeBlockedRemoved
                    + ", otherBiomePreserved="
                    + otherBiomePreserved
                    + ", controlPreserved="
                    + controlPreserved
                    + ", remainingWarOfTheRingEntries="
                    + remainingWarOfTheRingEntries);
        }

        LOG.info(
            "SERVER_FIXTURE_PASSED: global and biome-specific blocks removed only their targets; controls preserved");
    }
}
