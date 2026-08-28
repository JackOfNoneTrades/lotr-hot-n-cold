package wotrmc.fixture;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.ForgeEventFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fentanylsolutions.hotncold.Config;
import org.fentanylsolutions.hotncold.compat.LOTREquipmentControl;
import org.fentanylsolutions.hotncold.compat.LOTRSpawnControl;
import org.fentanylsolutions.hotncold.compat.LOTRSpawnReport;
import org.fentanylsolutions.hotncold.compat.WarOfTheRingSpawnCompat;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.registry.EntityRegistry;
import hotncold.fixture.entities.AddedTestAnimal;
import hotncold.fixture.entities.AllowedTestAnimal;
import hotncold.fixture.entities.BiomeBlockedTestAnimal;
import hotncold.fixture.entities.BlockedTestAnimal;
import hotncold.fixture.entities.ExplodingSpawnCheckAnimal;
import lotr.common.LOTRDimension;
import lotr.common.LOTRMod;
import lotr.common.entity.npc.LOTREntityGondorSoldier;
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
    private static final String ADDED_ENTITY_NAME = "hotncold_wotrmc_fixture.AddedTestAnimal";
    private static final String EXPLODING_SPAWN_CHECK_ENTITY_NAME = "hotncold_wotrmc_fixture.ExplodingSpawnCheckAnimal";
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
        EntityRegistry.registerModEntity(AddedTestAnimal.class, "AddedTestAnimal", 3, this, 64, 3, true);
        EntityRegistry
            .registerModEntity(ExplodingSpawnCheckAnimal.class, "ExplodingSpawnCheckAnimal", 4, this, 64, 3, true);

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
        Config.logBlockedSpawnAttempts = true;
        Config.blockedSpawnLogIntervalSeconds = 60;
        String[] configuredEntities = Config.blockedEntitiesInAllLOTRBiomes;
        Config.blockedEntitiesInAllLOTRBiomes = Arrays.copyOf(configuredEntities, configuredEntities.length + 3);
        Config.blockedEntitiesInAllLOTRBiomes[configuredEntities.length] = BLOCKED_ENTITY_NAME;
        Config.blockedEntitiesInAllLOTRBiomes[configuredEntities.length + 1] = EXPLODING_SPAWN_CHECK_ENTITY_NAME;
        Config.blockedEntitiesInAllLOTRBiomes[configuredEntities.length + 2] = "MoCreatures.Elephant";

        String[] configuredBiomeRules = Config.blockedEntityBiomeRules;
        Config.blockedEntityBiomeRules = Arrays.copyOf(configuredBiomeRules, configuredBiomeRules.length + 1);
        Config.blockedEntityBiomeRules[configuredBiomeRules.length] = BIOME_BLOCKED_ENTITY_NAME + ":"
            + testBiome.biomeName;

        EnumCreatureType[] creatureTypes = EnumCreatureType.values();
        String[] configuredAdditionRules = Config.addedEntityBiomeRules;
        Config.addedEntityBiomeRules = Arrays
            .copyOf(configuredAdditionRules, configuredAdditionRules.length + creatureTypes.length * 2);
        int ruleIndex = configuredAdditionRules.length;
        for (EnumCreatureType creatureType : creatureTypes) {
            String additionRule = ADDED_ENTITY_NAME + ":" + testBiome.biomeName + ":" + creatureType.name() + ":7:2:4";
            Config.addedEntityBiomeRules[ruleIndex++] = additionRule;
            Config.addedEntityBiomeRules[ruleIndex++] = additionRule;
        }

        Object gondorSoldierName = EntityList.classToStringMapping.get(LOTREntityGondorSoldier.class);
        Object gondorSwordName = Item.itemRegistry.getNameForObject(LOTRMod.swordGondor);
        if (!(gondorSoldierName instanceof String) || !(gondorSwordName instanceof String)) {
            throw new AssertionError(
                "Could not resolve genuine LOTR equipment fixture names: entity=" + gondorSoldierName
                    + ", item="
                    + gondorSwordName);
        }
        String[] configuredWeaponRules = Config.lotrNPCWeaponRules;
        Config.lotrNPCWeaponRules = Arrays.copyOf(configuredWeaponRules, configuredWeaponRules.length + 1);
        Config.lotrNPCWeaponRules[configuredWeaponRules.length] = gondorSoldierName + ";" + gondorSwordName + ";1";
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        boolean ruleReapplicationPassed = verifyRuleReapplication();
        List spawnEntries = testBiome.getSpawnableList(EnumCreatureType.creature);
        boolean fixtureRemoved = !spawnEntries.contains(fixtureEntry);
        boolean blockedRemoved = true;
        boolean allowedPreserved = true;
        boolean biomeBlockedRemoved = true;
        boolean otherBiomePreserved = true;
        boolean addedExactlyOnce = true;
        boolean absentFromOtherBiome = true;
        for (EnumCreatureType creatureType : EnumCreatureType.values()) {
            List categoryEntries = testBiome.getSpawnableList(creatureType);
            blockedRemoved &= !categoryEntries.contains(blockedEntries.get(creatureType));
            allowedPreserved &= categoryEntries.contains(allowedEntries.get(creatureType));
            biomeBlockedRemoved &= !categoryEntries.contains(biomeBlockedEntries.get(creatureType));
            otherBiomePreserved &= otherBiome.getSpawnableList(creatureType)
                .contains(otherBiomeEntries.get(creatureType));
            addedExactlyOnce &= countMatchingEntries(categoryEntries, AddedTestAnimal.class, 7, 2, 4) == 1;
            absentFromOtherBiome &= countMatchingEntries(
                otherBiome.getSpawnableList(creatureType),
                AddedTestAnimal.class,
                7,
                2,
                4) == 0;
        }
        boolean controlPreserved = spawnEntries.contains(controlEntry);
        int remainingWarOfTheRingEntries = WarOfTheRingSpawnCompat.countWarOfTheRingAnimalSpawns();
        boolean lateSpawnGuardPassed = verifyLateSpawnGuard();
        MinecraftServer server = MinecraftServer.getServer();
        boolean dumpCommandPassed = server.getCommandManager()
            .executeCommand(server, "hotncold spawns dump shire creature") == 1;
        boolean explainCommandPassed = server.getCommandManager()
            .executeCommand(server, "hotncold spawns explain shire " + BLOCKED_ENTITY_NAME) == 1;
        boolean explainReportPassed = verifyExplainReport();
        boolean exampleCommandPassed = server.getCommandManager()
            .executeCommand(server, "hotncold spawns example shire " + ADDED_ENTITY_NAME + " creature") == 1;
        boolean exampleReportPassed = verifyExampleReport();
        boolean reloadCommandPassed = server.getCommandManager()
            .executeCommand(server, "hotncold spawns reload") == 1
            && server.getCommandManager()
                .executeCommand(server, "hotncold spawns reload") == 1;
        boolean equipmentRulePassed = verifyEquipmentRule();

        if (!ruleReapplicationPassed || !fixtureRemoved
            || !blockedRemoved
            || !allowedPreserved
            || !biomeBlockedRemoved
            || !otherBiomePreserved
            || !addedExactlyOnce
            || !absentFromOtherBiome
            || !controlPreserved
            || !lateSpawnGuardPassed
            || !dumpCommandPassed
            || !explainCommandPassed
            || !explainReportPassed
            || !exampleCommandPassed
            || !exampleReportPassed
            || !reloadCommandPassed
            || !equipmentRulePassed
            || remainingWarOfTheRingEntries != 0) {
            throw new AssertionError(
                "Spawn cleanup integration check failed: ruleReapplicationPassed=" + ruleReapplicationPassed
                    + ", fixtureRemoved="
                    + fixtureRemoved
                    + ", blockedRemoved="
                    + blockedRemoved
                    + ", allowedPreserved="
                    + allowedPreserved
                    + ", biomeBlockedRemoved="
                    + biomeBlockedRemoved
                    + ", otherBiomePreserved="
                    + otherBiomePreserved
                    + ", addedExactlyOnce="
                    + addedExactlyOnce
                    + ", absentFromOtherBiome="
                    + absentFromOtherBiome
                    + ", controlPreserved="
                    + controlPreserved
                    + ", lateSpawnGuardPassed="
                    + lateSpawnGuardPassed
                    + ", dumpCommandPassed="
                    + dumpCommandPassed
                    + ", explainCommandPassed="
                    + explainCommandPassed
                    + ", explainReportPassed="
                    + explainReportPassed
                    + ", exampleCommandPassed="
                    + exampleCommandPassed
                    + ", exampleReportPassed="
                    + exampleReportPassed
                    + ", reloadCommandPassed="
                    + reloadCommandPassed
                    + ", equipmentRulePassed="
                    + equipmentRulePassed
                    + ", remainingWarOfTheRingEntries="
                    + remainingWarOfTheRingEntries);
        }

        LOG.info(
            "SERVER_FIXTURE_PASSED: additions and blocks changed only their targets; duplicates and controls handled");
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static boolean verifyRuleReapplication() {
        boolean configuredRemoveAll = Config.removeAllWarOfTheRingAnimalSpawns;
        String[] configuredGlobalBlocks = Config.blockedEntitiesInAllLOTRBiomes;
        String[] configuredBiomeBlocks = Config.blockedEntityBiomeRules;
        String[] configuredAdditions = Config.addedEntityBiomeRules;
        BiomeGenBase.SpawnListEntry unrelatedLateEntry = new BiomeGenBase.SpawnListEntry(
            AllowedTestAnimal.class,
            3,
            1,
            1);
        testBiome.getSpawnableList(EnumCreatureType.monster)
            .add(unrelatedLateEntry);

        boolean alternateRulesApplied;
        LOTRSpawnControl.SpawnRuleResult repeatedResult = null;
        try {
            Config.removeAllWarOfTheRingAnimalSpawns = false;
            Config.blockedEntitiesInAllLOTRBiomes = new String[0];
            Config.blockedEntityBiomeRules = new String[0];
            Config.addedEntityBiomeRules = new String[0];
            LOTRSpawnControl.applyConfiguredSpawnRules();

            alternateRulesApplied = testBiome.getSpawnableList(EnumCreatureType.creature)
                .contains(fixtureEntry) && WarOfTheRingSpawnCompat.countWarOfTheRingAnimalSpawns() > 0
                && testBiome.getSpawnableList(EnumCreatureType.monster)
                    .contains(unrelatedLateEntry);
            for (EnumCreatureType creatureType : EnumCreatureType.values()) {
                List entries = testBiome.getSpawnableList(creatureType);
                alternateRulesApplied &= entries.contains(blockedEntries.get(creatureType))
                    && entries.contains(biomeBlockedEntries.get(creatureType))
                    && countMatchingEntries(entries, AddedTestAnimal.class, 7, 2, 4) == 0;
            }
        } finally {
            Config.removeAllWarOfTheRingAnimalSpawns = configuredRemoveAll;
            Config.blockedEntitiesInAllLOTRBiomes = configuredGlobalBlocks;
            Config.blockedEntityBiomeRules = configuredBiomeBlocks;
            Config.addedEntityBiomeRules = configuredAdditions;
            LOTRSpawnControl.applyConfiguredSpawnRules();
            repeatedResult = LOTRSpawnControl.applyConfiguredSpawnRules();
        }

        return alternateRulesApplied && testBiome.getSpawnableList(EnumCreatureType.monster)
            .contains(unrelatedLateEntry)
            && repeatedResult != null
            && repeatedResult.describeStartup()
                .contains("LOTR spawn summary:")
            && repeatedResult.describeStartup()
                .contains("duplicate addition target(s)");
    }

    private static boolean verifyExplainReport() {
        List<String> globalBlock = LOTRSpawnReport.createSpawnExplanation(testBiome.biomeName, BLOCKED_ENTITY_NAME);
        List<String> biomeBlock = LOTRSpawnReport
            .createSpawnExplanation(testBiome.biomeName, BIOME_BLOCKED_ENTITY_NAME);
        List<String> configuredAddition = LOTRSpawnReport
            .createSpawnExplanation(testBiome.biomeName, ADDED_ENTITY_NAME);
        List<String> absentAddition = LOTRSpawnReport.createSpawnExplanation(otherBiome.biomeName, ADDED_ENTITY_NAME);
        String registeredWarOfTheRingEntity = findRegisteredWarOfTheRingEntity();
        List<String> warOfTheRingBlock = registeredWarOfTheRingEntity == null ? null
            : LOTRSpawnReport.createSpawnExplanation(testBiome.biomeName, registeredWarOfTheRingEntity);

        return containsLine(globalBlock, "Status: BLOCKED")
            && containsLine(globalBlock, "blockedEntitiesInAllLOTRBiomes")
            && containsLine(biomeBlock, "Status: BLOCKED")
            && containsLine(biomeBlock, "blockedEntityBiomeRules")
            && containsLine(configuredAddition, "Status: PRESENT")
            && containsLine(configuredAddition, "Matching addedEntityBiomeRules rule")
            && containsLine(absentAddition, "Status: ABSENT")
            && warOfTheRingBlock != null
            && containsLine(warOfTheRingBlock, "Status: BLOCKED")
            && containsLine(warOfTheRingBlock, "removeAllWarOfTheRingAnimalSpawns=true");
    }

    private static boolean verifyExampleReport() {
        List<String> examples = LOTRSpawnReport
            .createRuleExamples(testBiome.biomeName, ADDED_ENTITY_NAME, EnumCreatureType.creature.name());
        return containsLine(examples, "2 matching biome variant(s)")
            && containsLine(examples, "blockedEntitiesInAllLOTRBiomes: " + ADDED_ENTITY_NAME)
            && containsLine(examples, "addedEntityBiomeRules: " + ADDED_ENTITY_NAME + ":shire:creature:10:1:3");
    }

    private static boolean verifyEquipmentRule() {
        WorldServer world = DimensionManager.getWorld(LOTRDimension.MIDDLE_EARTH.dimensionID);
        if (world == null) {
            return false;
        }

        LOTREntityGondorSoldier soldier = new LOTREntityGondorSoldier(world);
        if (!LOTREquipmentControl.applyConfiguredWeapon(soldier)) {
            return false;
        }
        return soldier.npcItemsInv.getMeleeWeapon()
            .getItem() == LOTRMod.swordGondor
            && soldier.npcItemsInv.getMeleeWeaponMounted()
                .getItem() == LOTRMod.swordGondor
            && soldier.npcItemsInv.getIdleItem()
                .getItem() == LOTRMod.swordGondor
            && soldier.npcItemsInv.getIdleItemMounted()
                .getItem() == LOTRMod.swordGondor
            && soldier.getEquipmentInSlot(0)
                .getItem() == LOTRMod.swordGondor;
    }

    private static String findRegisteredWarOfTheRingEntity() {
        for (Object value : EntityList.stringToClassMapping.entrySet()) {
            Map.Entry entry = (Map.Entry) value;
            if (entry.getKey() instanceof String && entry.getValue() instanceof Class
                && ((Class) entry.getValue()).getName()
                    .startsWith("wotrmc.common.entities.")) {
                return (String) entry.getKey();
            }
        }
        return null;
    }

    private static boolean containsLine(List<String> lines, String expectedText) {
        for (String line : lines) {
            if (line.contains(expectedText)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static boolean verifyLateSpawnGuard() {
        WorldServer world = DimensionManager.getWorld(LOTRDimension.MIDDLE_EARTH.dimensionID);
        if (world == null) {
            return false;
        }

        world.getChunkFromChunkCoords(0, 0);
        int x = 8;
        int y = world.getTopSolidOrLiquidBlock(x, 8) + 1;
        int z = 8;
        BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
        BiomeGenBase.SpawnListEntry lateEntry = new BiomeGenBase.SpawnListEntry(
            ExplodingSpawnCheckAnimal.class,
            10,
            1,
            1);
        biome.getSpawnableList(EnumCreatureType.creature)
            .add(lateEntry);

        ExplodingSpawnCheckAnimal.resetSpawnCheckCalled();
        ExplodingSpawnCheckAnimal naturalEntity = new ExplodingSpawnCheckAnimal(world);
        naturalEntity.setLocationAndAngles(x + 0.5D, y, z + 0.5D, 0F, 0F);
        Event.Result naturalResult = ForgeEventFactory.canEntitySpawn(naturalEntity, world, x, y, z);
        boolean naturalDeniedBeforeEntityCheck = naturalResult == Event.Result.DENY
            && !ExplodingSpawnCheckAnimal.wasSpawnCheckCalled();

        AllowedTestAnimal allowedEntity = new AllowedTestAnimal(world);
        allowedEntity.setLocationAndAngles(x + 0.5D, y, z + 0.5D, 0F, 0F);
        boolean unblockedNaturalSpawnAllowed = ForgeEventFactory.canEntitySpawn(allowedEntity, world, x, y, z)
            != Event.Result.DENY;

        ExplodingSpawnCheckAnimal worldGenEntity = new ExplodingSpawnCheckAnimal(world);
        worldGenEntity.setLocationAndAngles(x + 1.5D, y, z + 0.5D, 0F, 0F);
        boolean worldGenDenied = !LOTRSpawnControl.spawnWorldGenEntityUnlessBlocked(world, worldGenEntity)
            && !world.loadedEntityList.contains(worldGenEntity);

        ExplodingSpawnCheckAnimal directEntity = new ExplodingSpawnCheckAnimal(world);
        directEntity.setLocationAndAngles(x + 2.5D, y, z + 0.5D, 0F, 0F);
        boolean directSpawnAllowed = world.spawnEntityInWorld(directEntity)
            && world.loadedEntityList.contains(directEntity);
        directEntity.setDead();

        Entity realElephant = EntityList.createEntityByName("MoCreatures.Elephant", world);
        boolean realElephantDenied = false;
        if (realElephant instanceof EntityLiving) {
            realElephant.setLocationAndAngles(x + 3.5D, y, z + 0.5D, 0F, 0F);
            realElephantDenied = ForgeEventFactory.canEntitySpawn((EntityLiving) realElephant, world, x + 3, y, z)
                == Event.Result.DENY;
        }

        return biome instanceof LOTRBiome && biome.getSpawnableList(EnumCreatureType.creature)
            .contains(lateEntry)
            && naturalDeniedBeforeEntityCheck
            && unblockedNaturalSpawnAllowed
            && worldGenDenied
            && directSpawnAllowed
            && realElephantDenied;
    }

    private static int countMatchingEntries(List entries, Class entityClass, int weight, int minimumGroupSize,
        int maximumGroupSize) {
        int matches = 0;
        for (Object value : entries) {
            if (!(value instanceof BiomeGenBase.SpawnListEntry)) {
                continue;
            }
            BiomeGenBase.SpawnListEntry entry = (BiomeGenBase.SpawnListEntry) value;
            if (entry.entityClass == entityClass && entry.itemWeight == weight
                && entry.minGroupCount == minimumGroupSize
                && entry.maxGroupCount == maximumGroupSize) {
                matches++;
            }
        }
        return matches;
    }
}
