package hotncold.production;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.server.MinecraftServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

@Mod(modid = "hotncold_production_fixture", version = "test-only", dependencies = "required-after:lotr;after:hotncold")
public final class ProductionFixture {

    public static final Logger LOG = LogManager.getLogger("Hot N Cold production acceptance");
    public static volatile boolean serverChecksPassed;
    public static net.minecraft.nbt.NBTTagCompound visibilityNPCData;
    public static net.minecraft.item.Item visibilityHelmet;
    public static volatile int visibleEntityId;
    public static volatile int visibilityStage;
    public static volatile String visibilityLabel = "preview";
    public static final Set<String> visibilityLabels = new HashSet<>();
    private static final Queue<VisibilityCheck> visibilityChecks = new ArrayDeque<>();

    public static void addVisibilityCheck(lotr.common.entity.npc.LOTREntityNPC npc, net.minecraft.item.Item helmet,
        String label) {
        net.minecraft.nbt.NBTTagCompound data = new net.minecraft.nbt.NBTTagCompound();
        npc.writeToNBT(data);
        data.setString("id", net.minecraft.entity.EntityList.getEntityString(npc));
        visibilityHelmet = helmet;
        visibilityLabels.add(label);
        if (visibilityNPCData == null) {
            visibilityNPCData = data;
            visibilityLabel = label;
        } else {
            visibilityChecks.add(new VisibilityCheck(data, label));
        }
    }

    private static final class VisibilityCheck {

        private final net.minecraft.nbt.NBTTagCompound data;
        private final String label;

        private VisibilityCheck(net.minecraft.nbt.NBTTagCompound data, String label) {
            this.data = data;
            this.label = label;
        }
    }

    @SidedProxy(
        clientSide = "hotncold.production.ProductionClient",
        serverSide = "hotncold.production.ProductionFixture$ServerProxy")
    public static ServerProxy proxy;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        require(
            Boolean.FALSE.equals(Launch.blackboard.get("fml.deobfuscatedEnvironment")),
            "Acceptance tests must run on obfuscated Minecraft, never Gradle's development runtime");
        LOG.info("PRODUCTION_RUNTIME_CONFIRMED: obfuscated Minecraft; profile={}", profile());
        if (profile().startsWith("null-")) {
            org.fentanylsolutions.hotncold.compat.NullableSpawnChecks.init(profile());
        }
        FMLCommonHandler.instance()
            .bus()
            .register(this);
        proxy.init();
    }

    @SubscribeEvent
    public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !serverChecksPassed
            || visibilityNPCData == null
            || MinecraftServer.getServer()
                .isDedicatedServer()) {
            return;
        }
        MinecraftServer server = MinecraftServer.getServer();
        if (server.getConfigurationManager().playerEntityList.isEmpty()) {
            return;
        }
        net.minecraft.entity.player.EntityPlayerMP player = (net.minecraft.entity.player.EntityPlayerMP) server
            .getConfigurationManager().playerEntityList.get(0);
        if (visibilityStage == 0) {
            // Restore an NPC from an actual spawn source next to the connected player.
            lotr.common.entity.npc.LOTREntityNPC npc;
            if (visibilityNPCData.hasKey("id")) {
                npc = (lotr.common.entity.npc.LOTREntityNPC) net.minecraft.entity.EntityList
                    .createEntityFromNBT(visibilityNPCData, player.worldObj);
            } else {
                npc = new lotr.common.entity.npc.LOTREntityGondorSoldier(player.worldObj);
                npc.readFromNBT(visibilityNPCData);
            }
            npc.dimension = player.dimension;
            npc.setPosition(player.posX + 3, player.posY, player.posZ + 3);
            npc.isNPCPersistent = true;
            require(player.worldObj.spawnEntityInWorld(npc), "Could not spawn saved NPC for client sync test");
            visibleEntityId = npc.getEntityId();
            visibilityStage = 1;
            LOG.info(
                "PRODUCTION_SERVER_VISIBLE_NPC: id={}, dimension={}, shield={}, helmet={}",
                visibleEntityId,
                npc.dimension,
                npc.npcShield,
                npc.getEquipmentInSlot(4));
        } else if (visibilityStage == 2) {
            lotr.common.entity.npc.LOTREntityNPC npc = (lotr.common.entity.npc.LOTREntityNPC) player.worldObj
                .getEntityByID(visibleEntityId);
            org.fentanylsolutions.hotncold.compat.LOTRNPCShieldSync.setConfiguredShield(npc, null);
            LOG.info(
                "PRODUCTION_SERVER_EMPTY_SHIELD: trackingPlayers={}",
                ((net.minecraft.world.WorldServer) npc.worldObj).getEntityTracker()
                    .getTrackingPlayers(npc)
                    .size());
            visibilityStage = 3;
        } else if (visibilityStage == 4 && !visibilityChecks.isEmpty()) {
            net.minecraft.entity.Entity old = player.worldObj.getEntityByID(visibleEntityId);
            if (old != null) {
                old.setDead();
            }
            VisibilityCheck next = visibilityChecks.remove();
            visibilityNPCData = next.data;
            visibilityLabel = next.label;
            visibilityStage = 0;
        }
    }

    @Mod.EventHandler
    public void started(FMLServerStartedEvent event) throws Exception {
        if (profile().startsWith("null-")) {
            org.fentanylsolutions.hotncold.compat.NullableSpawnChecks.run();
            completeServerChecks();
            return;
        }
        if (cpw.mods.fml.common.Loader.isModLoaded("wotrmc") && !"baseline".equals(profile())
            && !"compat".equals(profile())
            && !"streams".equals(profile())) {
            org.fentanylsolutions.hotncold.core.RestrictionChecks.run();
        }
        if ("terrain".equals(profile()) || "baseline".equals(profile()) || "compat".equals(profile())) {
            TerrainChecks.run();
        } else if ("worldgen".equals(profile()) || "combined".equals(profile())) {
            WorldgenChecks.run();
            if ("combined".equals(profile())) {
                FriendConfigChecks.run();
            }
        } else if ("streams".equals(profile())) {
            StreamsChecks.run();
        } else if ("friend".equals(profile())) {
            FriendConfigChecks.run();
        } else if ("sources".equals(profile())) {
            SpawnSourceChecks.start();
            return;
        } else if ("lightning".equals(profile())) {
            LightningChecks.run();
        } else if ("full".equals(profile())) {
            org.fentanylsolutions.hotncold.Config.lotrNPCGroupMembers = new String[] { "Gondor;lotr.GondorSoldier" };
            org.fentanylsolutions.hotncold.compat.LOTREquipmentControl.prepareConfiguredNPCGroups();
            net.minecraft.world.WorldServer world = net.minecraftforge.common.DimensionManager
                .getWorld(lotr.common.LOTRDimension.MIDDLE_EARTH.dimensionID);
            NaturalNPCChecks.run(world);
            SpawnBehaviorChecks.run(world);
        }
        completeServerChecks();
    }

    public static void completeServerChecks() throws Exception {
        serverChecksPassed = true;
        if (MinecraftServer.getServer()
            .isDedicatedServer()) {
            passed();
            MinecraftServer.getServer()
                .initiateShutdown();
        }
    }

    public static String profile() {
        return System.getProperty("hotncold.fixture.profile", "full");
    }

    public static void require(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }

    public static void passed() throws Exception {
        String result = "PRODUCTION_ACCEPTANCE_PASSED profile=" + profile();
        Files.write(new File("production-result.txt").toPath(), Arrays.asList(result), StandardCharsets.UTF_8);
        LOG.info(result);
    }

    public static class ServerProxy {

        public void init() {}
    }
}
