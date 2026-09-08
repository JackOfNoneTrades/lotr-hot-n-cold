package org.fentanylsolutions.hotncold.compat;

import static hotncold.production.ProductionFixture.require;

import java.io.File;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.util.EnumHelper;

import org.fentanylsolutions.hotncold.Config;

import cpw.mods.fml.common.Loader;
import hotncold.production.ProductionFixture;
import lotr.common.world.biome.LOTRBiome;

/** Real startup and reload checks with an extra mod-supplied category absent from LOTR biomes. */
public final class NullableSpawnChecks {

    private static EnumCreatureType unsupported;

    private NullableSpawnChecks() {}

    public static void init(String profile) {
        unsupported = EnumHelper
            .addCreatureType("HOTNCOLD_TEST_UNSUPPORTED", EntityAnimal.class, 1, Material.air, true, true);
        require(LOTRBiome.shire.getSpawnableList(unsupported) == null, "Expected an unsupported LOTR spawn category");
        configure(
            "null-wotr".equals(profile),
            "null-block".equals(profile) ? new String[] { "Chicken" } : new String[0],
            "null-biome".equals(profile) ? new String[] { "Chicken:shire" } : new String[0],
            "null-add".equals(profile) ? new String[] { "Villager:shire:" + unsupported.name() + ":10:1:3" }
                : new String[0],
            false);
        ProductionFixture.LOG
            .info("PRODUCTION_NULL_LIST_SETUP: profile={}, unsupported={}", profile, unsupported.name());
    }

    public static void run() {
        Set<BiomeGenBase> biomes = WarOfTheRingSpawnCompat.getAllLOTRSpawnBiomes();
        Set<BiomeGenBase> shires = LOTRSpawnControl.resolveLOTRSpawnBiomes("shire", biomes);
        require(!shires.isEmpty(), "No real Shire biomes found");
        for (BiomeGenBase biome : shires) {
            require(biome.getSpawnableList(unsupported) == null, "Unsupported list was fabricated");
            if ("null-block".equals(ProductionFixture.profile()) || "null-biome".equals(ProductionFixture.profile())) {
                require(
                    LOTRSpawnControl.isSpawnBlocked(EntityChicken.class, biome),
                    "Startup block did not take effect");
                require(count(biome, EntityChicken.class) == 0, "Startup left blocked chickens in spawn lists");
            }
        }
        if ("null-wotr".equals(ProductionFixture.profile())) {
            require(
                WarOfTheRingSpawnCompat.countWarOfTheRingAnimalSpawns() == 0,
                "Startup WOTR cleanup did not take effect");
        }
        ProductionFixture.LOG.info("PRODUCTION_NULL_STARTUP_PASSED: {}", ProductionFixture.profile());

        reload(false, new String[0], new String[0], new String[0]);
        Map<List, List> baseline = snapshot(biomes);
        int animalCount = WarOfTheRingSpawnCompat.countWarOfTheRingAnimalSpawns();
        EnumCreatureType ambient = LOTRBiome.creatureType_LOTRAmbient;
        require(ambient.ordinal() >= 4, "Expected LOTR's genuinely extra creature category");
        String[] additions = { "Villager:shire:" + unsupported.name() + ":10:1:3",
            "Villager:shire:" + ambient.name() + ":17:1:2", "Chicken:shire:creature:7:1:2" };

        // Unsupported additions must not prevent later valid additions, nor create detached lists.
        reload(false, new String[0], new String[0], additions);
        Map<List, List> added = snapshot(biomes);
        for (BiomeGenBase biome : shires) {
            require(biome.getSpawnableList(unsupported) == null, "Addition fabricated an unsupported list");
            require(
                contains(biome.getSpawnableList(ambient), EntityVillager.class),
                "Valid LOTR ambient addition was skipped");
        }
        String dump = LOTRSpawnReport.createBiomeDump("shire", null)
            .toString();
        require(
            dump.contains("unsupported by this biome") && dump.contains("Villager - weight 17"),
            "Dump lost unsupported or real entries");
        String explain = LOTRSpawnReport.createSpawnExplanation("shire", "Villager")
            .toString();
        require(
            explain.contains("Status: PRESENT") && explain.contains("ignored: category unsupported"),
            "Explanation hid a rejected addition");
        require(
            !LOTRSpawnReport.createRuleExamples("shire", "Villager", unsupported.name())
                .toString()
                .contains("addedEntityBiomeRules:"),
            "Example suggested an unsupported addition");
        commands(unsupported.name());
        reload(false, new String[0], new String[0], additions);
        requireSameEntries(added, "Repeated addition reload duplicated or reordered entries");

        // Global blocking traverses all categories, including a real added LOTR ambient entry.
        reload(false, new String[] { "Villager", "Chicken" }, new String[0], additions);
        for (BiomeGenBase biome : biomes) {
            require(
                count(biome, EntityVillager.class) == 0 && count(biome, EntityChicken.class) == 0,
                "Global block missed a supported list");
            require(LOTRSpawnControl.isSpawnBlocked(EntityChicken.class, biome), "Global spawn guard is inactive");
        }
        require(
            !LOTRSpawnControl.isSpawnBlocked(EntityChicken.class, BiomeGenBase.plains),
            "Global LOTR block leaked into vanilla biomes");
        require(
            LOTRSpawnReport.createSpawnExplanation("shire", "Chicken")
                .toString()
                .contains("Status: BLOCKED"),
            "Blocked explanation failed");
        commands(null);

        reload(false, new String[0], new String[] { "Villager:shire", "Pig:shire" }, additions);
        for (BiomeGenBase biome : shires) {
            require(
                count(biome, EntityVillager.class) == 0 && count(biome, EntityPig.class) == 0,
                "Biome block missed entries");
            require(LOTRSpawnControl.isSpawnBlocked(EntityPig.class, biome), "Biome spawn guard is inactive");
        }
        require(
            !LOTRSpawnControl.isSpawnBlocked(EntityPig.class, LOTRBiome.gondor),
            "Biome block leaked outside the Shire");

        reload(true, new String[0], new String[0], new String[0]);
        require(
            WarOfTheRingSpawnCompat.countWarOfTheRingAnimalSpawns() == 0,
            "WOTR count/cleanup failed with extra category");
        if (Loader.isModLoaded("wotrmc")) {
            require(animalCount > 0, "WOTR installed but test found no original animal spawns");
            int npcs = 0;
            for (Object key : EntityList.classToStringMapping.keySet()) {
                if (key instanceof Class && WarOfTheRingSpawnCompat.isWarOfTheRingEntity((Class) key)
                    && lotr.common.entity.npc.LOTREntityNPC.class.isAssignableFrom((Class) key)) {
                    require(
                        !LOTRSpawnControl.isSpawnBlocked((Class) key, LOTRBiome.shire),
                        "WOTR cleanup blocked an NPC");
                    npcs++;
                }
            }
            require(npcs > 0, "No real WOTR NPC registrations found");
            ProductionFixture.LOG
                .info("PRODUCTION_NULL_WOTR_PASSED: originalAnimals={}, protectedNPCTypes={}", animalCount, npcs);
        }
        commands(null);

        // Clearing the config restores the same list objects and original entries, in order.
        reload(false, new String[0], new String[0], new String[0]);
        requireSameEntries(baseline, "Clearing rules failed to restore baseline spawn lists");
        for (Map.Entry<List, List> entry : baseline.entrySet()) {
            for (int i = 0; i < entry.getKey()
                .size(); i++) {
                require(
                    entry.getKey()
                        .get(i)
                        == entry.getValue()
                            .get(i),
                    "Clearing rules replaced an original entry object");
            }
        }
        require(
            !LOTRSpawnControl.isSpawnBlocked(EntityChicken.class, LOTRBiome.shire),
            "Clearing config left stale blocks");
        ProductionFixture.LOG.info(
            "PRODUCTION_NULL_LISTS_PASSED: startup, global/biome/WOTR blocks, supported/unsupported additions, reports, reload and restoration; biomes={}",
            biomes.size());
    }

    private static void commands(String category) {
        command("hotncold spawns dump shire" + (category == null ? "" : " " + category));
        command("hotncold spawns explain shire Chicken");
        command("hotncold spawns example shire Chicken" + (category == null ? "" : " " + category));
    }

    private static void command(String command) {
        MinecraftServer server = MinecraftServer.getServer();
        require(
            server.getCommandManager()
                .executeCommand(server, command) == 1,
            "Command failed: " + command);
    }

    private static void reload(boolean wotr, String[] global, String[] biomeRules, String[] additions) {
        configure(wotr, global, biomeRules, additions, true);
    }

    private static void configure(boolean wotr, String[] global, String[] biomeRules, String[] additions,
        boolean command) {
        Configuration config = new Configuration(new File("config/hotncold.cfg"));
        config.get("general", "removeAllWarOfTheRingAnimalSpawns", false)
            .set(wotr);
        config.get("general", "blockedEntitiesInAllLOTRBiomes", new String[0])
            .set(global);
        config.get("general", "blockedEntityBiomeRules", new String[0])
            .set(biomeRules);
        config.get("general", "addedEntityBiomeRules", new String[0])
            .set(additions);
        config.save();
        if (command) {
            command("hotncold spawns reload");
        } else {
            require(Config.reloadSpawnConfiguration(), "Could not read startup config");
        }
    }

    private static int count(BiomeGenBase biome, Class entity) {
        int count = 0;
        for (EnumCreatureType type : EnumCreatureType.values()) {
            List entries = biome.getSpawnableList(type);
            if (entries != null) {
                for (Object value : entries) {
                    if (value instanceof BiomeGenBase.SpawnListEntry
                        && ((BiomeGenBase.SpawnListEntry) value).entityClass == entity) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static boolean contains(List entries, Class entity) {
        for (Object value : entries) {
            if (value instanceof BiomeGenBase.SpawnListEntry
                && ((BiomeGenBase.SpawnListEntry) value).entityClass == entity) {
                return true;
            }
        }
        return false;
    }

    private static Map<List, List> snapshot(Set<BiomeGenBase> biomes) {
        Map<List, List> snapshot = new IdentityHashMap<>();
        for (BiomeGenBase biome : biomes) {
            for (EnumCreatureType type : EnumCreatureType.values()) {
                List entries = biome.getSpawnableList(type);
                if (entries != null) {
                    snapshot.put(entries, new ArrayList(entries));
                }
            }
        }
        return snapshot;
    }

    private static void requireSameEntries(Map<List, List> expected, String message) {
        for (Map.Entry<List, List> entry : expected.entrySet()) {
            List actual = entry.getKey();
            List original = entry.getValue();
            require(actual.size() == original.size(), message);
            for (int i = 0; i < actual.size(); i++) {
                Object left = actual.get(i);
                Object right = original.get(i);
                if (left == right) {
                    continue;
                }
                // Reapplied additions have new entry objects but must retain identical spawn parameters/order.
                require(
                    left instanceof BiomeGenBase.SpawnListEntry && right instanceof BiomeGenBase.SpawnListEntry,
                    message);
                BiomeGenBase.SpawnListEntry a = (BiomeGenBase.SpawnListEntry) left;
                BiomeGenBase.SpawnListEntry b = (BiomeGenBase.SpawnListEntry) right;
                require(
                    a.entityClass == b.entityClass && a.itemWeight == b.itemWeight
                        && a.minGroupCount == b.minGroupCount
                        && a.maxGroupCount == b.maxGroupCount,
                    message);
            }
        }
    }
}
