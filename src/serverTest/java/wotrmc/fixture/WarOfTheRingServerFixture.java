package wotrmc.fixture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.SpawnerAnimals;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.ForgeEventFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fentanylsolutions.hotncold.Config;
import org.fentanylsolutions.hotncold.compat.LOTREquipmentControl;
import org.fentanylsolutions.hotncold.compat.LOTREquipmentReport;
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
import lotr.common.entity.npc.LOTREntityGondorArcher;
import lotr.common.entity.npc.LOTREntityGondorSoldier;
import lotr.common.entity.npc.LOTREntityRohanMan;
import lotr.common.world.biome.LOTRBiome;
import lotr.common.world.spawning.LOTRSpawnerAnimals;
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
        Config.customizeHiredLOTREquipment = false;
        Config.customizeNamedLOTREquipment = false;
        Config.replaceExistingLOTREquipment = true;
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
        Object rohanSwordName = Item.itemRegistry.getNameForObject(LOTRMod.swordRohan);
        Object gondorSwordName = Item.itemRegistry.getNameForObject(LOTRMod.swordGondor);
        if (!(gondorSoldierName instanceof String) || !(rohanSwordName instanceof String)
            || !(gondorSwordName instanceof String)) {
            throw new AssertionError(
                "Could not resolve genuine LOTR equipment fixture names: entity=" + gondorSoldierName
                    + ", item="
                    + rohanSwordName
                    + ", fallbackItem="
                    + gondorSwordName);
        }
        String[] configuredWeaponRules = Config.lotrNPCWeaponRules;
        Config.lotrNPCWeaponRules = Arrays.copyOf(configuredWeaponRules, configuredWeaponRules.length + 3);
        Config.lotrNPCWeaponRules[configuredWeaponRules.length] = gondorSoldierName + ";" + rohanSwordName + ";1";
        Config.lotrNPCWeaponRules[configuredWeaponRules.length + 1] = "faction:GONDOR;" + rohanSwordName + ";1";
        Config.lotrNPCWeaponRules[configuredWeaponRules.length + 2] = "all;" + gondorSwordName + ";1";

        String[] armorSlots = { "boots", "leggings", "chest", "helmet" };
        Item[] rohanArmor = { LOTRMod.bootsRohan, LOTRMod.legsRohan, LOTRMod.bodyRohan, LOTRMod.helmetRohan };
        String[] configuredArmorRules = Config.lotrNPCArmorRules;
        Config.lotrNPCArmorRules = Arrays
            .copyOf(configuredArmorRules, configuredArmorRules.length + armorSlots.length + 4);
        for (int armorIndex = 0; armorIndex < armorSlots.length; armorIndex++) {
            Object itemName = Item.itemRegistry.getNameForObject(rohanArmor[armorIndex]);
            if (!(itemName instanceof String)) {
                throw new AssertionError(
                    "Could not resolve genuine LOTR " + armorSlots[armorIndex] + " fixture item: " + itemName);
            }
            Config.lotrNPCArmorRules[configuredArmorRules.length
                + armorIndex] = gondorSoldierName + ";" + armorSlots[armorIndex] + ";" + itemName + ";1";
        }
        Object gondorArcherName = EntityList.classToStringMapping.get(LOTREntityGondorArcher.class);
        if (!(gondorArcherName instanceof String)) {
            throw new AssertionError("Could not resolve genuine LOTR archer fixture entity: " + gondorArcherName);
        }
        Config.lotrNPCArmorRules[configuredArmorRules.length + armorSlots.length] = gondorArcherName + ";chest;empty;1";
        Object rohanHelmetName = Item.itemRegistry.getNameForObject(LOTRMod.helmetRohan);
        Object rohanBodyName = Item.itemRegistry.getNameForObject(LOTRMod.bodyRohan);
        Config.lotrNPCArmorRules[configuredArmorRules.length + armorSlots.length + 1] = "faction:GONDOR;helmet;"
            + rohanHelmetName
            + ";1";
        Config.lotrNPCArmorRules[configuredArmorRules.length + armorSlots.length + 2] = "faction:GONDOR;chest;"
            + rohanBodyName
            + ";1";
        Object gondorHelmetName = Item.itemRegistry.getNameForObject(LOTRMod.helmetGondor);
        Config.lotrNPCArmorRules[configuredArmorRules.length + armorSlots.length + 3] = "all;helmet;" + gondorHelmetName
            + ";1";
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
        boolean equipmentRulePassed = verifyAutomaticEquipmentRule();
        String gondorSoldierName = (String) EntityList.classToStringMapping.get(LOTREntityGondorSoldier.class);
        List<String> equipmentReport = LOTREquipmentReport.createEquipmentExplanation(gondorSoldierName);
        boolean equipmentExplainCommandPassed = server.getCommandManager()
            .executeCommand(server, "hotncold equipment explain " + gondorSoldierName) == 1;
        List<String> factionEquipmentReport = LOTREquipmentReport.createEquipmentExplanation("faction:GONDOR");
        boolean factionEquipmentExplainCommandPassed = server.getCommandManager()
            .executeCommand(server, "hotncold equipment explain faction:GONDOR") == 1;
        List<String> allEquipmentReport = LOTREquipmentReport.createEquipmentExplanation("all");
        boolean allEquipmentExplainCommandPassed = server.getCommandManager()
            .executeCommand(server, "hotncold equipment explain all") == 1;
        String gondorArcherName = (String) EntityList.classToStringMapping.get(LOTREntityGondorArcher.class);
        List<String> inheritedEquipmentReport = LOTREquipmentReport.createEquipmentExplanation(
            gondorArcherName,
            DimensionManager.getWorld(LOTRDimension.MIDDLE_EARTH.dimensionID));
        String rohanManName = (String) EntityList.classToStringMapping.get(LOTREntityRohanMan.class);
        List<String> allFallbackReport = LOTREquipmentReport.createEquipmentExplanation(
            rohanManName,
            DimensionManager.getWorld(LOTRDimension.MIDDLE_EARTH.dimensionID));
        boolean equipmentExplainReportPassed = containsLine(equipmentReport, "swordRohan")
            && containsLine(equipmentReport, "Helmet choices")
            && containsLine(equipmentReport, "Hired NPCs: protected")
            && containsLine(equipmentReport, "Existing NPCs are not changed")
            && containsLine(factionEquipmentReport, "faction:GONDOR")
            && containsLine(factionEquipmentReport, "Exact NPC rules take priority")
            && containsLine(allEquipmentReport, "Equipment rules for all LOTR NPCs")
            && containsLine(allEquipmentReport, "swordGondor")
            && containsLine(inheritedEquipmentReport, "Weapon (from faction:GONDOR)")
            && containsLine(inheritedEquipmentReport, "Chest choices")
            && containsLine(inheritedEquipmentReport, "Helmet (from faction:GONDOR)")
            && containsLine(allFallbackReport, "Weapon (from all)")
            && containsLine(allFallbackReport, "Helmet (from all)");
        boolean equipmentReloadCommandPassed = server.getCommandManager()
            .executeCommand(server, "hotncold equipment reload") == 1
            && server.getCommandManager()
                .executeCommand(server, "hotncold equipment reload") == 1;

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
            || !equipmentExplainCommandPassed
            || !factionEquipmentExplainCommandPassed
            || !allEquipmentExplainCommandPassed
            || !equipmentExplainReportPassed
            || !equipmentReloadCommandPassed
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
                    + ", equipmentExplainCommandPassed="
                    + equipmentExplainCommandPassed
                    + ", factionEquipmentExplainCommandPassed="
                    + factionEquipmentExplainCommandPassed
                    + ", allEquipmentExplainCommandPassed="
                    + allEquipmentExplainCommandPassed
                    + ", equipmentExplainReportPassed="
                    + equipmentExplainReportPassed
                    + ", equipmentReloadCommandPassed="
                    + equipmentReloadCommandPassed
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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static boolean verifyAutomaticEquipmentRule() {
        WorldServer world = DimensionManager.getWorld(LOTRDimension.MIDDLE_EARTH.dimensionID);
        if (world == null) {
            return false;
        }

        int[] spawnLocation = findCreatureSpawnLocation(world);
        if (spawnLocation == null) {
            return false;
        }

        List creatureSpawns = testBiome.getSpawnableList(EnumCreatureType.creature);
        List originalSpawns = new ArrayList(creatureSpawns);
        Set<Entity> existingEntities = Collections.newSetFromMap(new IdentityHashMap<Entity, Boolean>());
        existingEntities.addAll(world.loadedEntityList);
        LOTREntityGondorSoldier spawnedSoldier = null;
        try {
            creatureSpawns.clear();
            creatureSpawns.add(new BiomeGenBase.SpawnListEntry(LOTREntityGondorSoldier.class, 1, 1, 1));
            LOTRSpawnerAnimals.worldGenSpawnAnimals(
                world,
                (LOTRBiome) testBiome,
                null,
                spawnLocation[0],
                spawnLocation[1],
                new SingleWorldGenSpawnRandom());

            for (Object value : world.loadedEntityList) {
                if (value instanceof LOTREntityGondorSoldier && !existingEntities.contains(value)) {
                    spawnedSoldier = (LOTREntityGondorSoldier) value;
                    break;
                }
            }
            return spawnedSoldier != null && spawnedSoldier.npcItemsInv.getMeleeWeapon()
                .getItem() == LOTRMod.swordRohan
                && spawnedSoldier.npcItemsInv.getMeleeWeaponMounted()
                    .getItem() == LOTRMod.swordRohan
                && spawnedSoldier.npcItemsInv.getIdleItem()
                    .getItem() == LOTRMod.swordRohan
                && spawnedSoldier.npcItemsInv.getIdleItemMounted()
                    .getItem() == LOTRMod.swordRohan
                && spawnedSoldier.getEquipmentInSlot(0)
                    .getItem() == LOTRMod.swordRohan
                && spawnedSoldier.getEquipmentInSlot(1)
                    .getItem() == LOTRMod.bootsRohan
                && spawnedSoldier.getEquipmentInSlot(2)
                    .getItem() == LOTRMod.legsRohan
                && spawnedSoldier.getEquipmentInSlot(3)
                    .getItem() == LOTRMod.bodyRohan
                && spawnedSoldier.getEquipmentInSlot(4)
                    .getItem() == LOTRMod.helmetRohan
                && verifyEmptyArmorChoice(world)
                && verifyAllEquipmentFallback(world)
                && verifyFillEmptyMode(world)
                && verifyProtectedEquipmentNPCs(world);
        } finally {
            creatureSpawns.clear();
            creatureSpawns.addAll(originalSpawns);
            if (spawnedSoldier != null) {
                spawnedSoldier.setDead();
            }
        }
    }

    private static boolean verifyEmptyArmorChoice(WorldServer world) {
        LOTREntityGondorArcher archer = new LOTREntityGondorArcher(world);
        archer.onSpawnWithEgg(null);
        if (archer.getEquipmentInSlot(3) == null) {
            return false;
        }
        boolean factionWeaponApplied = LOTREquipmentControl.applyConfiguredWeapon(archer);
        int appliedArmorSlots = LOTREquipmentControl.applyConfiguredArmor(archer);
        return factionWeaponApplied && appliedArmorSlots == 2
            && archer.npcItemsInv.getMeleeWeapon()
                .getItem() == LOTRMod.swordRohan
            && archer.getEquipmentInSlot(3) == null
            && archer.getEquipmentInSlot(4)
                .getItem() == LOTRMod.helmetRohan;
    }

    private static boolean verifyAllEquipmentFallback(WorldServer world) {
        LOTREntityRohanMan rohanMan = new LOTREntityRohanMan(world);
        rohanMan.onSpawnWithEgg(null);
        boolean weaponApplied = LOTREquipmentControl.applyConfiguredWeapon(rohanMan);
        int appliedArmorSlots = LOTREquipmentControl.applyConfiguredArmor(rohanMan);
        return weaponApplied && appliedArmorSlots == 1
            && rohanMan.npcItemsInv.getMeleeWeapon()
                .getItem() == LOTRMod.swordGondor
            && rohanMan.getEquipmentInSlot(4)
                .getItem() == LOTRMod.helmetGondor;
    }

    private static boolean verifyFillEmptyMode(WorldServer world) {
        LOTREntityGondorSoldier soldier = new LOTREntityGondorSoldier(world);
        soldier.onSpawnWithEgg(null);
        if (soldier.npcItemsInv.getMeleeWeapon() == null || soldier.getEquipmentInSlot(1) == null
            || soldier.getEquipmentInSlot(2) == null
            || soldier.getEquipmentInSlot(3) == null
            || soldier.getEquipmentInSlot(4) == null) {
            return false;
        }

        Item originalWeapon = soldier.npcItemsInv.getMeleeWeapon()
            .getItem();
        Item originalBoots = soldier.getEquipmentInSlot(1)
            .getItem();
        Item originalLeggings = soldier.getEquipmentInSlot(2)
            .getItem();
        Item originalChest = soldier.getEquipmentInSlot(3)
            .getItem();
        soldier.setCurrentItemOrArmor(4, null);

        boolean configuredReplace = Config.replaceExistingLOTREquipment;
        try {
            Config.replaceExistingLOTREquipment = false;
            boolean weaponSkipped = !LOTREquipmentControl.applyConfiguredWeapon(soldier);
            int appliedArmorSlots = LOTREquipmentControl.applyConfiguredArmor(soldier);
            return weaponSkipped && appliedArmorSlots == 1
                && soldier.npcItemsInv.getMeleeWeapon()
                    .getItem() == originalWeapon
                && soldier.getEquipmentInSlot(1)
                    .getItem() == originalBoots
                && soldier.getEquipmentInSlot(2)
                    .getItem() == originalLeggings
                && soldier.getEquipmentInSlot(3)
                    .getItem() == originalChest
                && soldier.getEquipmentInSlot(4)
                    .getItem() == LOTRMod.helmetRohan;
        } finally {
            Config.replaceExistingLOTREquipment = configuredReplace;
        }
    }

    private static boolean verifyProtectedEquipmentNPCs(WorldServer world) {
        boolean configuredHired = Config.customizeHiredLOTREquipment;
        boolean configuredNamed = Config.customizeNamedLOTREquipment;
        try {
            Config.customizeHiredLOTREquipment = false;
            Config.customizeNamedLOTREquipment = false;

            LOTREntityGondorSoldier hiredSoldier = new LOTREntityGondorSoldier(world);
            hiredSoldier.onSpawnWithEgg(null);
            Item hiredOriginalWeapon = hiredSoldier.npcItemsInv.getMeleeWeapon()
                .getItem();
            hiredSoldier.hiredNPCInfo.isActive = true;
            boolean hiredProtected = !LOTREquipmentControl.applyConfiguredWeapon(hiredSoldier)
                && LOTREquipmentControl.applyConfiguredArmor(hiredSoldier) == 0
                && hiredSoldier.npcItemsInv.getMeleeWeapon()
                    .getItem() == hiredOriginalWeapon;

            Config.customizeHiredLOTREquipment = true;
            boolean hiredOptInWorks = LOTREquipmentControl.applyConfiguredWeapon(hiredSoldier)
                && LOTREquipmentControl.applyConfiguredArmor(hiredSoldier) == 4
                && hiredSoldier.npcItemsInv.getMeleeWeapon()
                    .getItem() == LOTRMod.swordRohan;

            LOTREntityGondorSoldier namedSoldier = new LOTREntityGondorSoldier(world);
            namedSoldier.onSpawnWithEgg(null);
            Item namedOriginalWeapon = namedSoldier.npcItemsInv.getMeleeWeapon()
                .getItem();
            NBTTagCompound namedData = new NBTTagCompound();
            namedSoldier.writeToNBT(namedData);
            namedData.setString("CustomName", "Hot N Cold Fixture");
            namedSoldier.readFromNBT(namedData);
            boolean namedProtected = namedSoldier.hasCustomNameTag()
                && !LOTREquipmentControl.applyConfiguredWeapon(namedSoldier)
                && LOTREquipmentControl.applyConfiguredArmor(namedSoldier) == 0
                && namedSoldier.npcItemsInv.getMeleeWeapon()
                    .getItem() == namedOriginalWeapon;

            Config.customizeNamedLOTREquipment = true;
            boolean namedOptInWorks = LOTREquipmentControl.applyConfiguredWeapon(namedSoldier)
                && LOTREquipmentControl.applyConfiguredArmor(namedSoldier) == 4
                && namedSoldier.npcItemsInv.getMeleeWeapon()
                    .getItem() == LOTRMod.swordRohan;
            return hiredProtected && hiredOptInWorks && namedProtected && namedOptInWorks;
        } finally {
            Config.customizeHiredLOTREquipment = configuredHired;
            Config.customizeNamedLOTREquipment = configuredNamed;
        }
    }

    private static int[] findCreatureSpawnLocation(WorldServer world) {
        for (int x = -32; x <= 32; x++) {
            for (int z = -32; z <= 32; z++) {
                int y = world.getTopSolidOrLiquidBlock(x, z);
                if (SpawnerAnimals.canCreatureTypeSpawnAtLocation(EnumCreatureType.creature, world, x, y, z)) {
                    return new int[] { x, z };
                }
            }
        }
        return null;
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

    private static final class SingleWorldGenSpawnRandom extends Random {

        private static final long serialVersionUID = 1L;
        private int floatCalls;

        @Override
        public int nextInt(int bound) {
            return 0;
        }

        @Override
        public float nextFloat() {
            return floatCalls++ < 2 ? 0F : 0.999999F;
        }
    }
}
