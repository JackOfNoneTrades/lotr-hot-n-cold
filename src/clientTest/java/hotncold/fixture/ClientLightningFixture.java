package hotncold.fixture;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Explosion;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import net.minecraftforge.client.event.sound.PlaySoundEvent17;
import net.minecraftforge.common.DimensionManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fentanylsolutions.hotncold.Config;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import lotr.common.LOTRConfig;
import lotr.common.LOTRDimension;

/** Opt-in development-client regression for the actual Wizardry command, including LOTR sound replacement. */
public final class ClientLightningFixture {

    private static final Logger LOG = LogManager.getLogger("Hot N Cold lightning client fixture");
    private volatile int stage;
    private int scenario;
    private int ticks;
    private int settled;
    private int thunder;
    private int explosions;
    private boolean flash;
    private boolean startupMuted;
    private String thunderResource;

    public boolean tick(Minecraft mc) {
        require(++ticks < 3000, "Lightning command fixture timed out at stage " + stage);
        if (stage == 0) {
            startupMuted = Config.disableLightningExplosionSound;
            require(startupMuted, "Use a copied config with disableLightningExplosionSound=true");
            require(LOTRConfig.newWeather, "Use LOTR New weather=true to exercise sound replacement");
            stage = 1;
        } else if (stage == 2 && mc.thePlayer.dimension == dimension() && ++settled >= 60) {
            settled = 0;
            thunder = 0;
            explosions = 0;
            flash = false;
            thunderResource = null;
            stage = 3;
        } else if (stage == 4) {
            flash |= mc.theWorld.lastLightningBolt > 0;
            if (++settled >= 60) {
                require(thunder == 1, "Expected exactly one thunder; got " + thunder);
                require(explosions == (muted() ? 0 : 1), "Wrong impact sound count: " + explosions);
                require(flash, "Lightning flash was lost");
                String expected = dimension() == 0 ? "minecraft:ambient.weather.thunder"
                    : "lotr:ambient.weather.thunder";
                require(expected.equals(thunderResource), "Unexpected thunder resource: " + thunderResource);
                settled = 0;
                stage = 5;
            }
        } else if (stage == 6 && ++settled >= 40) {
            require(explosions == (muted() ? 1 : 2), "Ordinary explosion sound was lost");
            LOG.info(
                "CLIENT_LIGHTNING_COMMAND_CASE_PASSED: dimension={}, muted={}, thunder={}, impactSounds={}, ordinaryExplosionSounds=1, flash={}",
                dimension(),
                muted(),
                thunderResource,
                explosions - 1,
                flash);
            settled = 0;
            if (++scenario == 5) {
                require(Config.disableLightningExplosionSound == startupMuted, "Startup mute flag was not restored");
                LOG.info(
                    "CLIENT_LIGHTNING_COMMAND_PASSED: actual /cast lightning_bolt; Overworld and Middle-earth; enabled/disabled/restored; LOTR replacement thunder and ordinary explosions preserved");
                return true;
            }
            stage = 1;
        }
        return false;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void sound(PlaySoundEvent17 event) {
        if (stage < 3 || event.result == null) {
            return;
        }
        String resource = event.result.getPositionedSoundLocation()
            .toString();
        LOG.info(
            "CLIENT_LIGHTNING_SOUND: stage={}, input={}, output={}, category={}, volume={}, pitch={}",
            stage,
            event.name,
            resource,
            event.category,
            event.result.getVolume(),
            event.result.getPitch());
        if ("ambient.weather.thunder".equals(event.name)) {
            thunder++;
            thunderResource = resource;
        } else if ("random.explode".equals(event.name)) {
            explosions++;
        }
    }

    @SubscribeEvent
    public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || (stage != 1 && stage != 3 && stage != 5)) {
            return;
        }
        MinecraftServer server = MinecraftServer.getServer();
        if (server.getConfigurationManager().playerEntityList.isEmpty()) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        if (stage == 1) {
            Config.disableLightningExplosionSound = muted();
            WorldServer target = DimensionManager.getWorld(dimension());
            require(target != null, "Target dimension is unavailable");
            prepare(target);
            if (player.dimension != dimension()) {
                server.getConfigurationManager()
                    .transferPlayerToDimension(player, dimension(), new Teleporter(target) {

                        @Override
                        public void placeInPortal(Entity entity, double x, double y, double z, float yaw) {
                            entity.setLocationAndAngles(0.5, 202, 0.5, 0, 60);
                        }
                    });
            }
            player.playerNetServerHandler.setPlayerLocation(0.5, 202, 0.5, 0, 60);
            LOG.info("CLIENT_LIGHTNING_COMMAND_PREPARED: dimension={}, muted={}", dimension(), muted());
            stage = 2;
        } else if (stage == 3) {
            WorldServer world = player.getServerForPlayer();
            player.rotationYaw = 0;
            player.rotationPitch = 60;
            int before = world.weatherEffects.size();
            int result = server.getCommandManager()
                .executeCommand(player, "cast lightning_bolt");
            require(result == 1, "Wizardry command failed: " + result);
            require(world.weatherEffects.size() == before + 1, "Wizardry did not add a real weather entity");
            Entity bolt = (Entity) world.weatherEffects.get(before);
            require(bolt instanceof EntityLightningBolt, "Wizardry did not create vanilla lightning");
            require(
                bolt.getEntityData()
                    .hasKey("summoningPlayer"),
                "Wizardry summoner marker absent");
            LOG.info("CLIENT_LIGHTNING_COMMAND_CAST: dimension={}, muted={}, bolt={}", dimension(), muted(), bolt);
            stage = 4;
        } else {
            new Explosion(player.worldObj, null, player.posX + 4, player.posY, player.posZ, 1).doExplosionB(false);
            stage = 6;
        }
    }

    private int dimension() {
        return scenario == 0 || scenario == 2 ? 0 : LOTRDimension.MIDDLE_EARTH.dimensionID;
    }

    private boolean muted() {
        return scenario < 2 || scenario == 4;
    }

    private static void prepare(WorldServer world) {
        world.getGameRules()
            .setOrCreateGameRule("doMobSpawning", "false");
        world.getGameRules()
            .setOrCreateGameRule("doFireTick", "false");
        world.getWorldInfo()
            .setRaining(false);
        world.getWorldInfo()
            .setThundering(false);
        world.getWorldInfo()
            .setRainTime(100000);
        world.getWorldInfo()
            .setThunderTime(100000);
        world.setRainStrength(0);
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                world.setBlock(x, 199, z, Blocks.stone);
                for (int y = 200; y <= 210; y++) {
                    world.setBlockToAir(x, y, z);
                }
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
