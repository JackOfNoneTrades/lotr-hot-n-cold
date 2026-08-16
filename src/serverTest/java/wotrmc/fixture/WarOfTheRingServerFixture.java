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
    private static final Logger LOG = LogManager.getLogger("WOTR server fixture");
    private static BiomeGenBase testBiome;
    private static BiomeGenBase.SpawnListEntry fixtureEntry;
    private static final Map<EnumCreatureType, BiomeGenBase.SpawnListEntry> blockedEntries = new IdentityHashMap<>();
    private static final Map<EnumCreatureType, BiomeGenBase.SpawnListEntry> allowedEntries = new IdentityHashMap<>();
    private static BiomeGenBase.SpawnListEntry controlEntry;

    @Mod.EventHandler
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void init(FMLInitializationEvent event) {
        EntityRegistry.registerModEntity(BlockedTestAnimal.class, "BlockedTestAnimal", 0, this, 64, 3, true);
        EntityRegistry.registerModEntity(AllowedTestAnimal.class, "AllowedTestAnimal", 1, this, 64, 3, true);

        testBiome = LOTRBiome.shire;
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
            testBiome.getSpawnableList(creatureType)
                .add(blockedEntry);
            testBiome.getSpawnableList(creatureType)
                .add(allowedEntry);
            blockedEntries.put(creatureType, blockedEntry);
            allowedEntries.put(creatureType, allowedEntry);
        }
        LOG.info(
            "SERVER_FIXTURE_REGISTERED: added all-WOTR, selected and unlisted entries in every category, and a control spawn");
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        Config.removeAllWarOfTheRingAnimalSpawns = true;
        String[] configuredEntities = Config.blockedEntitiesInAllLOTRBiomes;
        Config.blockedEntitiesInAllLOTRBiomes = Arrays.copyOf(configuredEntities, configuredEntities.length + 1);
        Config.blockedEntitiesInAllLOTRBiomes[configuredEntities.length] = BLOCKED_ENTITY_NAME;
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        List spawnEntries = testBiome.getSpawnableList(EnumCreatureType.creature);
        boolean fixtureRemoved = !spawnEntries.contains(fixtureEntry);
        boolean blockedRemoved = true;
        boolean allowedPreserved = true;
        for (EnumCreatureType creatureType : EnumCreatureType.values()) {
            List categoryEntries = testBiome.getSpawnableList(creatureType);
            blockedRemoved &= !categoryEntries.contains(blockedEntries.get(creatureType));
            allowedPreserved &= categoryEntries.contains(allowedEntries.get(creatureType));
        }
        boolean controlPreserved = spawnEntries.contains(controlEntry);
        int remainingWarOfTheRingEntries = WarOfTheRingSpawnCompat.countWarOfTheRingAnimalSpawns();

        if (!fixtureRemoved || !blockedRemoved
            || !allowedPreserved
            || !controlPreserved
            || remainingWarOfTheRingEntries != 0) {
            throw new AssertionError(
                "Spawn cleanup integration check failed: fixtureRemoved=" + fixtureRemoved
                    + ", blockedRemoved="
                    + blockedRemoved
                    + ", allowedPreserved="
                    + allowedPreserved
                    + ", controlPreserved="
                    + controlPreserved
                    + ", remainingWarOfTheRingEntries="
                    + remainingWarOfTheRingEntries);
        }

        LOG.info("SERVER_FIXTURE_PASSED: all-WOTR and selected-entity spawns removed; unselected spawns preserved");
    }
}
