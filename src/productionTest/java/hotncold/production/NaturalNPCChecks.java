package hotncold.production;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

import org.fentanylsolutions.hotncold.Config;
import org.fentanylsolutions.hotncold.compat.LOTREquipmentControl;

import com.mojang.authlib.GameProfile;

import lotr.common.LOTRConfig;
import lotr.common.LOTRMod;
import lotr.common.LOTRShields;
import lotr.common.entity.npc.LOTREntityGondorSoldier;
import lotr.common.world.biome.LOTRBiome;
import lotr.common.world.spawning.LOTRBiomeSpawnList;
import lotr.common.world.spawning.LOTRSpawnEntry;
import lotr.common.world.spawning.LOTRSpawnerNPCs;

/** Exercises the dedicated NPC spawning loop, not an NPC inserted in an animal list. */
public final class NaturalNPCChecks {

    private NaturalNPCChecks() {}

    public static void run(WorldServer world) {
        run(world, LOTRMod.helmetRohanMarshal);
    }

    public static void run(WorldServer world, Item configuredHelmet) {
        String itemName = (String) Item.itemRegistry.getNameForObject(configuredHelmet);
        Config.lotrNPCArmorRules = new String[] { "group:Gondor;helmet;" + itemName + ";100" };
        LOTREquipmentControl.prepareConfiguredArmorRules();
        Config.replaceExistingLOTREquipment = false;
        check(world, LOTRMod.helmetGondor, "fill-empty", false);
        Config.replaceExistingLOTREquipment = true;
        Config.lotrNPCWeaponRules = new String[] { "group:Gondor;lotr:item.swordRohan;100" };
        Config.lotrNPCRangedWeaponRules = new String[] { "group:Gondor;lotr:item.gondorBow;100" };
        Config.lotrNPCShieldRules = new String[] { "group:Gondor;ALIGNMENT_ROHAN;100" };
        LOTREquipmentControl.prepareConfiguredWeaponRules();
        LOTREquipmentControl.prepareConfiguredRangedWeaponRules();
        LOTREquipmentControl.prepareConfiguredShieldRules();
        check(world, configuredHelmet, "group-replacement", true);
        Config.lotrNPCArmorRules = new String[] { "group:Gondor;helmet;" + itemName + ";100",
            "lotr.GondorSoldier;helmet;lotr:item.helmetRanger;10" };
        LOTREquipmentControl.prepareConfiguredArmorRules();
        check(world, LOTRMod.helmetRanger, "exact-priority", true);
        Config.lotrNPCArmorRules = new String[] { "lotr.GondorSoldier;helmet;" + itemName + ";100" };
        LOTREquipmentControl.prepareConfiguredArmorRules();
        check(world, configuredHelmet, "exact-replacement", true);
        // Egg/command initialization is not a natural-spawner call and must remain unchanged.
        LOTREntityGondorSoldier manual = new LOTREntityGondorSoldier(world);
        manual.onSpawnWithEgg(null);
        ProductionFixture.require(
            manual.getEquipmentInSlot(4)
                .getItem() == LOTRMod.helmetGondor,
            "Manual spawn initialization was unexpectedly customized");
        ProductionFixture.LOG.info("PRODUCTION_MANUAL_EQUIPMENT_PASSED: direct spawn initialization unchanged");
        NBTTagCompound normalData = new NBTTagCompound();
        manual.writeToNBT(normalData);
        LOTREntityGondorSoldier normalRestored = new LOTREntityGondorSoldier(world);
        normalRestored.readFromNBT(normalData);
        ProductionFixture
            .require(normalRestored.npcShield == manual.npcShield, "Unconfigured NPC shield was changed on load");
        org.fentanylsolutions.hotncold.compat.LOTRNPCShieldSync.setConfiguredShield(manual, null);
        NBTTagCompound emptyData = new NBTTagCompound();
        manual.writeToNBT(emptyData);
        LOTREntityGondorSoldier emptyRestored = new LOTREntityGondorSoldier(world);
        emptyRestored.readFromNBT(emptyData);
        ProductionFixture.require(emptyRestored.npcShield == null, "Configured empty shield did not survive save/load");
        ProductionFixture.LOG
            .info("PRODUCTION_SHIELD_SAVE_PASSED: configured, empty, and unconfigured shields preserved");
    }

    @SuppressWarnings("unchecked")
    private static void check(WorldServer world, Item expectedHelmet, String stage, boolean checkOtherGear) {
        Map<LOTRBiome, LOTRBiomeSpawnList> originalLists = new IdentityHashMap<>();
        List<Object> originalEntities = new ArrayList<>(world.loadedEntityList);
        // A small flat test area gives the real spawn loop valid positions without changing its logic.
        for (int x = 64; x < 112; x++) {
            for (int z = 64; z < 112; z++) {
                world.setBlock(x, 80, z, Blocks.grass, 0, 2);
                for (int y = 81; y < 96; y++) {
                    world.setBlock(x, y, z, Blocks.air, 0, 2);
                }
                LOTRBiome biome = (LOTRBiome) world.getBiomeGenForCoords(x, z);
                if (!originalLists.containsKey(biome)) {
                    originalLists.put(biome, biome.npcSpawnList);
                    biome.npcSpawnList = new LOTRBiomeSpawnList("production-acceptance") {

                        @Override
                        public LOTRSpawnEntry.Instance getRandomSpawnEntry(Random random, World spawnWorld, int sx,
                            int sy, int sz) {
                            return new LOTRSpawnEntry.Instance(
                                new LOTRSpawnEntry(LOTREntityGondorSoldier.class, 1, 1, 1),
                                0,
                                false);
                        }
                    };
                }
            }
        }
        FakePlayer player = new FakePlayer(
            world,
            new GameProfile(UUID.fromString("1c72dcaf-2441-4d60-80b1-f4d599426681"), "SpawnAcceptance"));
        player.setPosition(32, 80, 32);
        world.playerEntities.add(player);
        Random originalRandom = world.rand;
        int originalInterval = LOTRConfig.mobSpawnInterval;
        List<LOTREntityGondorSoldier> spawned = new ArrayList<>();
        try {
            LOTRConfig.mobSpawnInterval = 0;
            world.rand = new Random(719233) {

                @Override
                public int nextInt(int bound) {
                    // WOTR Shire selects the base NPC list on one of 200 rolls.
                    if (bound == 200) {
                        return 0;
                    }
                    return bound >= 95 && bound % 16 == 15 ? 81 : bound / 2;
                }

                @Override
                public float nextFloat() {
                    return 0.99F;
                }
            };
            net.minecraft.world.ChunkPosition sample = LOTRSpawnerNPCs.getRandomSpawningPointInChunk(world, 4, 4);
            LOTREntityGondorSoldier probe = new LOTREntityGondorSoldier(world);
            probe.setPosition(72.5, 81, 72.5);
            ProductionFixture.LOG.info(
                "PRODUCTION_NPC_DIAGNOSTIC: point={},{},{}, canSpawn={}, event={}, loaded={}, cap={}, biome={}",
                sample.chunkPosX,
                sample.chunkPosY,
                sample.chunkPosZ,
                probe.getCanSpawnHere(),
                net.minecraftforge.event.ForgeEventFactory.canEntitySpawn(probe, world, 72.5F, 81F, 72.5F),
                world.loadedEntityList.size(),
                lotr.common.LOTRSpawnDamping.getNPCSpawnCap(world),
                world.getBiomeGenForCoords(72, 72)
                    .getClass()
                    .getName());
            for (int attempt = 0; attempt < 30 && spawned.size() < 5; attempt++) {
                LOTRSpawnerNPCs.performSpawning(world);
                for (Object value : world.loadedEntityList) {
                    if (value instanceof LOTREntityGondorSoldier && !originalEntities.contains(value)
                        && !spawned.contains(value)) {
                        spawned.add((LOTREntityGondorSoldier) value);
                    }
                }
            }
            ProductionFixture
                .require(spawned.size() >= 5, "NPC spawner did not produce five soldiers: " + spawned.size());
            int correct = 0;
            for (LOTREntityGondorSoldier soldier : spawned) {
                if (soldier.getEquipmentInSlot(4) != null && soldier.getEquipmentInSlot(4)
                    .getItem() == expectedHelmet) {
                    correct++;
                }
                ProductionFixture.require(
                    soldier.getEquipmentInSlot(3)
                        .getItem() == LOTRMod.bodyGondor,
                    "Helmet-only rule modified chest armor");
                if (checkOtherGear) {
                    ProductionFixture.require(
                        soldier.npcItemsInv.getMeleeWeapon()
                            .getItem() == LOTRMod.swordRohan,
                        "Natural spawner did not apply melee rule");
                    ProductionFixture.require(
                        soldier.npcItemsInv.getRangedWeapon()
                            .getItem() == LOTRMod.gondorBow,
                        "Natural spawner did not apply ranged rule");
                    ProductionFixture.require(
                        soldier.npcShield == LOTRShields.ALIGNMENT_ROHAN,
                        "Natural spawner did not apply shield rule");
                }
                NBTTagCompound saved = new NBTTagCompound();
                soldier.writeToNBT(saved);
                LOTREntityGondorSoldier restored = new LOTREntityGondorSoldier(world);
                restored.readFromNBT(saved);
                ProductionFixture.require(
                    restored.getEquipmentInSlot(4)
                        .getItem() == expectedHelmet,
                    "Configured helmet did not survive entity save/load");
                if (checkOtherGear) {
                    ProductionFixture.require(
                        restored.npcItemsInv.getMeleeWeapon()
                            .getItem() == LOTRMod.swordRohan
                            && restored.npcItemsInv.getRangedWeapon()
                                .getItem() == LOTRMod.gondorBow
                            && restored.npcShield == LOTRShields.ALIGNMENT_ROHAN,
                        "Configured weapons/shield did not survive entity save/load: melee="
                            + restored.npcItemsInv.getMeleeWeapon()
                            + ", ranged="
                            + restored.npcItemsInv.getRangedWeapon()
                            + ", shield="
                            + restored.npcShield);
                }
                if ("exact-replacement".equals(stage)) {
                    ProductionFixture.visibilityNPCData = saved;
                    ProductionFixture.visibilityHelmet = expectedHelmet;
                }
            }
            ProductionFixture.LOG.info(
                "PRODUCTION_NATURAL_NPC_CHECK: stage={}, soldiers={}, correctHelmets={}, expectedHelmet={}",
                stage,
                spawned.size(),
                correct,
                Item.itemRegistry.getNameForObject(expectedHelmet));
            ProductionFixture.require(correct == spawned.size(), "Natural NPC spawner bypasses equipment rules");
            ProductionFixture.LOG.info("PRODUCTION_NATURAL_NPC_PASSED: {} including entity save/load", stage);
        } finally {
            world.rand = originalRandom;
            LOTRConfig.mobSpawnInterval = originalInterval;
            world.playerEntities.remove(player);
            for (Map.Entry<LOTRBiome, LOTRBiomeSpawnList> entry : originalLists.entrySet()) {
                entry.getKey().npcSpawnList = entry.getValue();
            }
            for (LOTREntityGondorSoldier soldier : spawned) {
                world.removePlayerEntityDangerously(soldier);
            }
        }
    }
}
