package hotncold.production;

import net.minecraft.init.Blocks;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import org.fentanylsolutions.hotncold.Config;
import org.fentanylsolutions.hotncold.compat.WeatherRainDelay;
import org.fentanylsolutions.hotncold.compat.WeatherRainSync;

import cpw.mods.fml.common.Loader;

/** Uses actual server rain queries and Campfire Backport's rain-extinguishing predicate. */
public final class WeatherRainServerChecks {

    public static WeatherRainSync.RainMessage leadIn;
    public static WeatherRainSync.RainMessage raining;

    public static void run() {
        WorldServer world = DimensionManager.getWorld(0);
        float originalRain = WeatherRainDelay.rawRainStrength(world, 1);
        boolean enabled = Config.enableWeather2VanillaRain;
        int delay = Config.weather2RainDelaySeconds;
        try {
            Config.enableWeather2VanillaRain = true;
            Config.weather2RainDelaySeconds = 120;
            setRain(world, 1);
            WeatherRainDelay.state(world).wetTicks = 0;
            ProductionFixture.require(!world.isRaining(), "Server reports rain at the start of the lead-in");
            boolean overcast = weather2.config.ConfigMisc.overcastMode;
            int syncRate = weather2.config.ConfigMisc.tickerRateSyncWeatherCheckVanilla;
            try {
                weather2.config.ConfigMisc.overcastMode = true;
                weather2.config.ConfigMisc.tickerRateSyncWeatherCheckVanilla = 1;
                weather2.weathersystem.WeatherManagerServer manager = new weather2.weathersystem.WeatherManagerServer(
                    0);
                manager.tick();
                ProductionFixture
                    .require(manager.isVanillaRainActiveOnServer, "Delay altered Weather 2's storm simulation input");
            } finally {
                weather2.config.ConfigMisc.overcastMode = overcast;
                weather2.config.ConfigMisc.tickerRateSyncWeatherCheckVanilla = syncRate;
            }
            connor135246.campfirebackport.common.tileentity.TileEntityCampfire campfire = null;
            if (Loader.isModLoaded("campfirebackport")) {
                world.getChunkFromBlockCoords(8, 8);
                world.getChunkFromBlockCoords(8, 8)
                    .getBiomeArray()[8 * 16 + 8] = (byte) net.minecraft.world.biome.BiomeGenBase.plains.biomeID;
                world.setBlock(8, 249, 8, Blocks.stone);
                world.setBlock(
                    8,
                    250,
                    8,
                    connor135246.campfirebackport.common.blocks.CampfireBackportBlocks.getBlockFromLitAndType(true, 0),
                    2,
                    3);
                campfire = (connor135246.campfirebackport.common.tileentity.TileEntityCampfire) world
                    .getTileEntity(8, 250, 8);
                ProductionFixture.require(!campfire.isBeingRainedOn(), "Campfire can be extinguished during lead-in");
            }
            for (int tick = 0; tick < 1200; tick++) WeatherRainDelay.advance(world);
            leadIn = roundTrip(new WeatherRainSync.RainMessage(world));
            for (int tick = 1200; tick < 2399; tick++) WeatherRainDelay.advance(world);
            ProductionFixture.require(!world.isRaining(), "Server rain started before 120 seconds");
            if (campfire != null)
                ProductionFixture.require(!campfire.isBeingRainedOn(), "Campfire got rain before deadline");
            WeatherRainDelay.advance(world);
            ProductionFixture
                .require(world.isRaining() && world.getRainStrength(1) == 1, "Server rain did not start at deadline");
            if (campfire != null) {
                ProductionFixture.require(campfire.isBeingRainedOn(), "Campfire rain exposure did not resume");
                world.setBlockToAir(8, 250, 8);
            }
            raining = roundTrip(new WeatherRainSync.RainMessage(world));
            setRain(world, 0);
            WeatherRainDelay.advance(world);
            setRain(world, 1);
            ProductionFixture.require(!world.isRaining(), "Clear weather did not reset server delay");
            Config.weather2RainDelaySeconds = 0;
            ProductionFixture.require(world.isRaining(), "Zero server delay ignored");
            Config.weather2RainDelaySeconds = 120;
            Config.enableWeather2VanillaRain = false;
            ProductionFixture.require(world.isRaining(), "Disabled integration still delays server rain");
            ProductionFixture.LOG.info(
                "PRODUCTION_SERVER_RAIN_PASSED: dry lead-in, two-minute boundary, campfire exposure={}, storm simulation, reset, disabled/zero delay, serialized join phases",
                campfire != null);
        } finally {
            Config.enableWeather2VanillaRain = enabled;
            Config.weather2RainDelaySeconds = delay;
            setRain(world, originalRain);
            WeatherRainDelay.state(world).wetTicks = 0;
        }
    }

    private static void setRain(WorldServer world, float strength) {
        cpw.mods.fml.relauncher.ReflectionHelper
            .setPrivateValue(net.minecraft.world.World.class, world, strength, "rainingStrength", "field_73004_o");
        cpw.mods.fml.relauncher.ReflectionHelper
            .setPrivateValue(net.minecraft.world.World.class, world, strength, "prevRainingStrength", "field_73003_n");
    }

    private static WeatherRainSync.RainMessage roundTrip(WeatherRainSync.RainMessage message) {
        io.netty.buffer.ByteBuf buffer = io.netty.buffer.Unpooled.buffer();
        try {
            message.toBytes(buffer);
            WeatherRainSync.RainMessage result = new WeatherRainSync.RainMessage();
            result.fromBytes(buffer);
            return result;
        } finally {
            buffer.release();
        }
    }
}
