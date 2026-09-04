package hotncold.production;

import java.util.Map;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.event.ForgeEventFactory;

import org.fentanylsolutions.hotncold.Config;
import org.fentanylsolutions.hotncold.compat.LOTRSpawnControl;
import org.fentanylsolutions.hotncold.compat.LOTRSpawnReport;

import cpw.mods.fml.common.eventhandler.Event;
import lotr.common.entity.npc.LOTREntityNPC;
import lotr.common.world.biome.LOTRBiome;

public final class SpawnBehaviorChecks {

    private SpawnBehaviorChecks() {}

    public static void run(WorldServer world) throws Exception {
        Config.removeAllWarOfTheRingAnimalSpawns = true;
        LOTRSpawnControl.applyConfiguredSpawnRules();
        int npcCount = 0;
        int blockedNPCs = 0;
        for (Object value : EntityList.stringToClassMapping.entrySet()) {
            Map.Entry entry = (Map.Entry) value;
            Class type = (Class) entry.getValue();
            if (type.getName()
                .startsWith("wotrmc.common.entities.") && LOTREntityNPC.class.isAssignableFrom(type)) {
                npcCount++;
                if (LOTRSpawnControl.isSpawnBlocked(type, LOTRBiome.shire)) {
                    blockedNPCs++;
                }
            }
        }
        ProductionFixture.LOG
            .info("PRODUCTION_WOTR_NPC_CHECK: registered={}, incorrectlyBlocked={}", npcCount, blockedNPCs);

        Class animal = Class.forName("wotrmc.common.entities.WOTRMCEntityDeer");
        String name = (String) EntityList.classToStringMapping.get(animal);
        ProductionFixture.require(name != null, "Real WOTR deer must be registered");
        Config.addedEntityBiomeRules = new String[] { name + ":shire:creature:88:1:3" };
        LOTRSpawnControl.applyConfiguredSpawnRules();
        int count = 0;
        for (Object value : LOTRBiome.shire.getSpawnableList(EnumCreatureType.creature)) {
            BiomeGenBase.SpawnListEntry entry = (BiomeGenBase.SpawnListEntry) value;
            if (entry.entityClass == animal && entry.itemWeight == 88
                && entry.minGroupCount == 1
                && entry.maxGroupCount == 3) {
                count++;
            }
        }
        boolean blocked = LOTRSpawnControl.isSpawnBlocked(animal, LOTRBiome.shire);
        ProductionFixture.LOG.info(
            "PRODUCTION_WOTR_READD_CHECK: entity={}, entries={}, guardBlocked={}, report={}",
            name,
            count,
            blocked,
            LOTRSpawnReport.createSpawnExplanation("shire", name));
        ProductionFixture.require(npcCount > 0 && blockedNPCs == 0, "Animal cleanup must preserve WOTR NPC spawning");
        ProductionFixture
            .require(count == 0 && blocked, "Broad animal block must override conflicting additions as documented");
        ProductionFixture.require(
            LOTRSpawnControl.isSpawnBlocked(animal, LOTRBiome.mordor),
            "Broad animal block must also apply outside the addition's biome");
        Config.removeAllWarOfTheRingAnimalSpawns = false;
        LOTRSpawnControl.applyConfiguredSpawnRules();
        ProductionFixture.require(
            !LOTRSpawnControl.isSpawnBlocked(animal, LOTRBiome.shire),
            "Disabling broad cleanup must allow the animal again");
        // Explicit denial remains stronger than an addition.
        Config.blockedEntityBiomeRules = new String[] { name + ":shire" };
        LOTRSpawnControl.applyConfiguredSpawnRules();
        ProductionFixture
            .require(LOTRSpawnControl.isSpawnBlocked(animal, LOTRBiome.shire), "Explicit per-biome block must win");
        Config.blockedEntityBiomeRules = new String[0];
        LOTRSpawnControl.applyConfiguredSpawnRules();
        LOTRSpawnControl.applyConfiguredSpawnRules();
        ProductionFixture.require(
            !LOTRSpawnControl.isSpawnBlocked(animal, LOTRBiome.shire),
            "Reload must restore the explicit addition");

        Config.removeAllWarOfTheRingAnimalSpawns = true;
        LOTRSpawnControl.applyConfiguredSpawnRules();
        Class mumak = Class.forName("wotrmc.common.entities.WOTRMCEntityMumakilBigger");
        EntityLiving entity = (EntityLiving) mumak.getConstructor(net.minecraft.world.World.class)
            .newInstance(world);
        int y = world.getTopSolidOrLiquidBlock(8, 8) + 1;
        ProductionFixture.require(
            ForgeEventFactory.canEntitySpawn(entity, world, 8, y, 8) == Event.Result.DENY,
            "Original report's WOTR MoCreatures subclass must be denied before its broken spawn check");
        ProductionFixture.LOG.info(
            "PRODUCTION_WOTR_SPAWN_PASSED: NPCs preserved, block/add conflicts explained, reload, original Mumakil crash guard");
    }
}
