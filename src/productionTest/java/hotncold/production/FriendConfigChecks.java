package hotncold.production;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.item.Item;
import net.minecraft.world.SpawnerAnimals;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.DimensionManager;

import org.fentanylsolutions.hotncold.Config;
import org.fentanylsolutions.hotncold.compat.LOTREquipmentControl;
import org.fentanylsolutions.hotncold.compat.LOTREquipmentReport;
import org.fentanylsolutions.hotncold.compat.LOTRSpawnReport;

import lotr.common.LOTRDimension;
import lotr.common.LOTRMod;
import lotr.common.entity.npc.LOTREntityGondorSoldier;
import lotr.common.world.biome.LOTRBiome;
import lotr.common.world.spawning.LOTRSpawnerAnimals;

public final class FriendConfigChecks {

    private FriendConfigChecks() {}

    public static void run() throws Exception {
        ProductionFixture
            .require(Config.lotrNPCGroupMembers.length == 27, "Expected all 27 group members from friend's file");
        ProductionFixture
            .require(!Config.replaceExistingLOTREquipment, "Expected original fill-empty mode from friend's file");
        for (String member : Config.lotrNPCGroupMembers) {
            String name = member.split(";")[1].trim();
            ProductionFixture
                .require(EntityList.stringToClassMapping.containsKey(name), "Unregistered group member: " + name);
        }
        ProductionFixture.LOG.info("FRIEND_GROUP_NAMES_PASSED: all 27 names registered");
        ProductionFixture.LOG
            .info("FRIEND_ORIGINAL_REPORT: {}", LOTREquipmentReport.createEquipmentExplanation("group:Gondor"));
        Item customHelmet = (Item) Item.itemRegistry.getObject("historyitems:breehelmet");
        if (cpw.mods.fml.common.Loader.isModLoaded("historyitems")
            || Boolean.getBoolean("hotncold.fixture.requireHistoryItems")) {
            ProductionFixture.require(customHelmet != null, "Required History Items Bree helmet is not registered");
            ProductionFixture.LOG.info(
                "FRIEND_CUSTOM_HELMET_REGISTERED: historyitems:breehelmet, class={}",
                customHelmet.getClass()
                    .getName());
        }
        if (customHelmet == null) {
            ProductionFixture.LOG.warn(
                "PRODUCTION_TEST_UNAVAILABLE: historyitems:breehelmet; mod is not installed. Known LOTR helmet tested separately.");
        }
        Item helmet = customHelmet == null ? LOTRMod.helmetRohanMarshal : customHelmet;
        String itemName = (String) Item.itemRegistry.getNameForObject(helmet);
        WorldServer world = DimensionManager.getWorld(LOTRDimension.MIDDLE_EARTH.dimensionID);
        Config.lotrNPCArmorRules = new String[] { "group:Gondor;helmet;" + itemName + ";100" };
        LOTREquipmentControl.prepareConfiguredArmorRules();
        LOTREntityGondorSoldier preserved = spawnSoldier(world);
        ProductionFixture.require(
            preserved.getEquipmentInSlot(4)
                .getItem() == LOTRMod.helmetGondor,
            "Fill-empty mode must preserve Gondor helmet");
        ProductionFixture.LOG.info("FRIEND_FILL_EMPTY_PASSED: real LOTR spawner preserved existing helmet");
        Config.replaceExistingLOTREquipment = true;
        LOTREntityGondorSoldier replaced = spawnSoldier(world);
        ProductionFixture.require(
            replaced.getEquipmentInSlot(4)
                .getItem() == helmet,
            "Group rule must replace helmet when enabled");
        ProductionFixture.require(
            replaced.getEquipmentInSlot(3)
                .getItem() == LOTRMod.bodyGondor,
            "Helmet rule changed chest armor");
        ProductionFixture.LOG.info("FRIEND_GROUP_REPLACEMENT_PASSED: real LOTR spawner equipped {}", itemName);
        Config.lotrNPCArmorRules = new String[] { "group:Gondor;helmet;" + itemName + ";100",
            "lotr.GondorSoldier;helmet;lotr:item.helmetRanger;10" };
        LOTREquipmentControl.prepareConfiguredArmorRules();
        LOTREntityGondorSoldier exact = spawnSoldier(world);
        ProductionFixture.require(
            exact.getEquipmentInSlot(4)
                .getItem() == LOTRMod.helmetRanger,
            "Exact NPC rule must take priority over group");
        ProductionFixture.LOG.info("FRIEND_EXACT_PRIORITY_PASSED: exact rule overrides group");
        ProductionFixture.LOG.info(
            "FRIEND_KANGAROO_REPORT: {}",
            LOTRSpawnReport.createSpawnExplanation("shire", "lotr.wotra_kangarooBlack"));
        NaturalNPCChecks.run(world, helmet);
        SpawnBehaviorChecks.run(world);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static LOTREntityGondorSoldier spawnSoldier(WorldServer world) {
        int sx = 0;
        int sz = 0;
        boolean found = false;
        for (int x = -32; x < 32 && !found; x++) {
            for (int z = -32; z < 32 && !found; z++) {
                if (SpawnerAnimals.canCreatureTypeSpawnAtLocation(
                    EnumCreatureType.creature,
                    world,
                    x,
                    world.getTopSolidOrLiquidBlock(x, z),
                    z)) {
                    sx = x;
                    sz = z;
                    found = true;
                }
            }
        }
        ProductionFixture.require(found, "No valid spawn location");
        List entries = LOTRBiome.shire.getSpawnableList(EnumCreatureType.creature);
        List savedEntries = new ArrayList(entries);
        List previousEntities = new ArrayList(world.loadedEntityList);
        try {
            entries.clear();
            entries.add(new BiomeGenBase.SpawnListEntry(LOTREntityGondorSoldier.class, 1, 1, 1));
            LOTRSpawnerAnimals.worldGenSpawnAnimals(world, LOTRBiome.shire, null, sx, sz, new Random() {

                private int calls;

                @Override
                public int nextInt(int bound) {
                    return 0;
                }

                @Override
                public float nextFloat() {
                    return calls++ < 2 ? 0F : 0.999999F;
                }
            });
            for (Object entity : world.loadedEntityList) {
                if (entity instanceof LOTREntityGondorSoldier && !previousEntities.contains(entity)) {
                    LOTREntityGondorSoldier soldier = (LOTREntityGondorSoldier) entity;
                    soldier.setDead();
                    return soldier;
                }
            }
            throw new AssertionError("Real LOTR spawner produced no Gondor soldier");
        } finally {
            entries.clear();
            entries.addAll(savedEntries);
        }
    }
}
