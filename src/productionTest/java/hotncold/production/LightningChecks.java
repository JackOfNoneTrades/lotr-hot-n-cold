package hotncold.production;

import java.io.File;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.Explosion;
import net.minecraft.world.IWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.config.Configuration;

import org.fentanylsolutions.hotncold.Config;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import lotr.common.LOTRConfig;
import lotr.common.LOTRDimension;

/** Observes actual transformed lightning and explosion calls, never the mixin handler directly. */
public final class LightningChecks {

    public static volatile boolean startupMuted;
    public static volatile int networkStage;

    private LightningChecks() {}

    public static void run() {
        startupMuted = Config.disableLightningExplosionSound;
        Configuration configuration = new Configuration(new File("config/hotncold.cfg"));
        ProductionFixture.require(
            startupMuted == configuration.get("general", "disableLightningExplosionSound", false)
                .getBoolean(),
            "Lightning sound setting was not loaded from the startup config");
        boolean originalGrief = LOTRConfig.disableLightningGrief;
        try {
            for (int dimension : new int[] { 0, LOTRDimension.MIDDLE_EARTH.dimensionID }) {
                WorldServer world = DimensionManager.getWorld(dimension);
                ProductionFixture.require(world != null, "Lightning test dimension unavailable: " + dimension);
                for (boolean disableGrief : new boolean[] { false, true }) {
                    LOTRConfig.disableLightningGrief = disableGrief;
                    List<String> vanilla = strike(world, false, disableGrief);
                    List<String> muted = strike(world, true, disableGrief);
                    List<String> restored = strike(world, false, disableGrief);
                    ProductionFixture.require(vanilla.equals(muted), "Muting changed lightning mechanics or thunder");
                    ProductionFixture.require(vanilla.equals(restored), "Turning mute off did not restore behavior");
                }
            }
        } finally {
            Config.disableLightningExplosionSound = startupMuted;
            LOTRConfig.disableLightningGrief = originalGrief;
        }
        ProductionFixture.LOG.info(
            "PRODUCTION_LIGHTNING_PASSED: startupMuted={}; Overworld and Middle-earth; off/on/off; thunder, actual explosions, damage, fire, RNG and bolt lifetime unchanged; LOTR grief switch respected",
            startupMuted);
        FMLCommonHandler.instance()
            .bus()
            .register(new LightningChecks());
    }

    private static List<String> strike(WorldServer world, boolean muted, boolean disableGrief) {
        Config.disableLightningExplosionSound = muted;
        EnumDifficulty difficulty = world.difficultySetting;
        boolean fireTick = world.getGameRules()
            .getGameRuleBooleanValue("doFireTick");
        world.difficultySetting = EnumDifficulty.NORMAL;
        world.getGameRules()
            .setOrCreateGameRule("doFireTick", "true");
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                world.getChunkFromChunkCoords(x, z);
            }
        }
        for (int x = 5; x <= 11; x++) {
            for (int z = 5; z <= 11; z++) {
                world.setBlock(x, 199, z, Blocks.stone);
                for (int y = 200; y <= 203; y++) {
                    world.setBlockToAir(x, y, z);
                }
            }
        }
        EntityCow cow = new EntityCow(world);
        cow.setPosition(8.5, 200, 8.5);
        ProductionFixture.require(world.spawnEntityInWorld(cow), "Could not create lightning damage control");
        try (Sounds sounds = new Sounds(world)) {
            EntityLightningBolt bolt = new EntityLightningBolt(world, 8.5, 200, 8.5);
            Random random = ReflectionHelper.getPrivateValue(Entity.class, bolt, "rand", "field_70146_Z");
            random.setSeed(894321L);
            ReflectionHelper.setPrivateValue(EntityLightningBolt.class, bolt, 2, "boltLivingTime", "field_70263_c");
            bolt.boltVertex = 42;
            List<String> state = new ArrayList<>();
            for (int tick = 0; !bolt.isDead && tick < 200; tick++) {
                bolt.onUpdate();
                state.add(bolt.boltVertex + ":" + bolt.isDead);
            }
            ProductionFixture.require(bolt.isDead, "Lightning did not complete its normal lifetime");
            ProductionFixture.require(sounds.thunder.size() == 1, "Lightning thunder was removed or duplicated");
            ProductionFixture.require(sounds.explosions == (muted ? 0 : 1), "Wrong lightning explosion sound count");
            ProductionFixture.require(cow.getHealth() < cow.getMaxHealth(), "Lightning stopped damaging entities");
            state.addAll(sounds.thunder);
            state.add("health=" + cow.getHealth() + ",burning=" + cow.isBurning() + ",rng=" + random.nextLong());
            ProductionFixture.require(
                (world.getBlock(8, 200, 8) == Blocks.fire) == !disableGrief,
                "Lightning fire no longer follows LOTR's grief setting");
            // Constructor fire placement is random too; compare the deterministic central strike block only.
            state.add("fire=" + (world.getBlock(8, 200, 8) == Blocks.fire));
            int before = sounds.explosions;
            Explosion explosion = new Explosion(world, null, 8.5, 205, 8.5, 1);
            explosion.isSmoking = false;
            explosion.doExplosionB(false);
            ProductionFixture.require(sounds.explosions == before + 1, "Actual explosion sound was muted");
            return state;
        } finally {
            world.removePlayerEntityDangerously(cow);
            world.difficultySetting = difficulty;
            world.getGameRules()
                .setOrCreateGameRule("doFireTick", Boolean.toString(fireTick));
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !ProductionFixture.serverChecksPassed
            || (networkStage != 1 && networkStage != 3)) {
            return;
        }
        MinecraftServer server = MinecraftServer.getServer();
        if (server.getConfigurationManager().playerEntityList.isEmpty()) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        WorldServer world = (WorldServer) player.worldObj;
        if (networkStage == 1) {
            world.getWorldInfo()
                .setRaining(false);
            world.getWorldInfo()
                .setThundering(false);
            ProductionFixture.require(
                world.addWeatherEffect(new EntityLightningBolt(world, player.posX + 4, player.posY + 2, player.posZ)),
                "Weather bolt rejected");
            networkStage = 2;
        } else {
            Explosion explosion = new Explosion(world, null, player.posX + 4, player.posY + 2, player.posZ, 1);
            explosion.isSmoking = false;
            explosion.doExplosionB(false);
            networkStage = 4;
        }
    }

    private static final class Sounds implements AutoCloseable {

        private final World world;
        private final IWorldAccess listener;
        private final List<String> thunder = new ArrayList<>();
        private int explosions;

        private Sounds(World world) {
            this.world = world;
            listener = (IWorldAccess) Proxy.newProxyInstance(
                IWorldAccess.class.getClassLoader(),
                new Class<?>[] { IWorldAccess.class },
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        if ("equals".equals(method.getName())) {
                            return proxy == args[0];
                        }
                        if ("hashCode".equals(method.getName())) {
                            return System.identityHashCode(proxy);
                        }
                        return "Lightning sound observer";
                    }
                    // Signature, not method spelling: this listener works under both MCP and production SRG names.
                    if (args != null && args.length == 6 && args[0] instanceof String && args[1] instanceof Double) {
                        if ("ambient.weather.thunder".equals(args[0])) {
                            thunder.add(java.util.Arrays.toString(args));
                        } else if ("random.explode".equals(args[0])) {
                            explosions++;
                        }
                    }
                    return null;
                });
            world.addWorldAccess(listener);
        }

        @Override
        public void close() {
            world.removeWorldAccess(listener);
        }
    }
}
