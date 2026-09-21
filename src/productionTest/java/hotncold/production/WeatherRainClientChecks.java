package hotncold.production;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;

import org.fentanylsolutions.hotncold.Config;
import org.fentanylsolutions.hotncold.compat.WeatherRainDelay;
import org.lwjgl.opengl.GL11;

import lotr.client.render.LOTRWeatherRenderer;
import lotr.common.LOTRDimension;
import lotr.common.world.biome.LOTRBiome;
import weather2.client.SceneEnhancer;
import weather2.config.ConfigMisc;

/** Exercises the actual Weather 2 -> vanilla -> LOTR rendering chain in an obfuscated client. */
public final class WeatherRainClientChecks extends GuiScreen {

    public boolean finished;

    private EntityRenderer stableRenderer;
    private int stableTicks;
    private int settleTicks;

    public boolean checkStableRenderer(Minecraft minecraft) {
        // Let server config snapshots sent during the off/on test round trip before camera assertions.
        if (++settleTicks <= 40) return false;
        ProductionFixture.require(
            WeatherRainDelay.state(minecraft.theWorld).synchronizedWeather,
            "Actual login did not deliver the server rain phase over the network");
        ProductionFixture.require(
            minecraft.entityRenderer instanceof lotr.client.LOTREntityRenderer,
            "Weather 2 replaced LOTR's gameplay renderer");
        if (stableRenderer == null) {
            stableRenderer = minecraft.entityRenderer;
        }
        ProductionFixture.require(
            minecraft.entityRenderer == stableRenderer,
            "Gameplay renderer was recreated between ticks, resetting the camera");
        if (++stableTicks == 100) {
            ProductionFixture.LOG.info(
                "PRODUCTION_WEATHER_CAMERA_PASSED: same LOTR renderer retained across 100 gameplay ticks with Weather 2's renderer override setting enabled");
        }
        return stableTicks >= 100;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (finished) {
            return;
        }
        WorldClient originalWorld = mc.theWorld;
        EntityRenderer originalRenderer = mc.entityRenderer;
        TextureManager originalTextures = mc.renderEngine;
        int delay = Config.weather2RainDelaySeconds;
        boolean enabled = Config.enableWeather2VanillaRain;
        boolean particles = ConfigMisc.Particle_RainSnow;
        boolean proxy = ConfigMisc.Misc_proxyRenderOverrideEnabled;
        ControlledWorld world = new ControlledWorld(mc);
        TrackingTextures textures = new TrackingTextures(mc, originalTextures);
        ExposedRenderer renderer = new ExposedRenderer(mc);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            mc.theWorld = world;
            mc.entityRenderer = renderer;
            mc.renderEngine = textures;
            Config.enableWeather2VanillaRain = true;
            Config.weather2RainDelaySeconds = 120;
            ConfigMisc.Particle_RainSnow = true;
            ConfigMisc.Misc_proxyRenderOverrideEnabled = true;
            world.provider.setWeatherRenderer(new LOTRWeatherRenderer());
            checkSkyColors(world);
            checkWetness(world);
            WeatherRainDelay.state(world).wetTicks = 0;
            renderer.rain();
            ProductionFixture.require(textures.seen.isEmpty(), "Rain appeared before dark-sky delay");
            Method splashes = null;
            for (String name : new String[] { "addRainParticles", "func_78484_h" }) {
                try {
                    splashes = EntityRenderer.class.getDeclaredMethod(name);
                    splashes.setAccessible(true);
                    break;
                } catch (NoSuchMethodException ignored) {}
            }
            ProductionFixture.require(splashes != null, "Could not resolve vanilla rain splash method");
            world.surfaceQueries = 0;
            splashes.invoke(renderer);
            ProductionFixture.require(world.surfaceQueries == 0, "Rain sounds/splashes ran during delay");
            for (int tick = 0; tick < 2399; tick++) {
                WeatherRainDelay.advance(world);
            }
            renderer.rain();
            ProductionFixture.require(textures.seen.isEmpty(), "Delay ended before 120 seconds");
            ProductionFixture.require(
                world.getRainStrength(1) == 0 && !world.isRaining() && WeatherRainDelay.skyRainStrength(world, 1) == 1,
                "Lead-in is wet or lost its dark sky");
            WeatherRainDelay.advance(world);
            renderer.rain();
            ProductionFixture.require(textures.has("rain.png"), "LOTR rain did not resume after 120 seconds");
            splashes.invoke(renderer);
            ProductionFixture.require(world.surfaceQueries > 0, "Rain splashes did not resume");

            world.biome = LOTRBiome.mordor;
            textures.seen.clear();
            renderer.rain();
            ProductionFixture.require(textures.has("ash.png"), "Mordor ash was not restored");
            for (LOTRBiome biome : LOTRDimension.MIDDLE_EARTH.biomeList) {
                if (biome != null && LOTRWeatherRenderer.isSandstormBiome(biome)) {
                    world.biome = biome;
                    break;
                }
            }
            textures.seen.clear();
            renderer.rain();
            ProductionFixture.require(textures.has("sandstorm.png"), "LOTR sandstorms were not restored");
            world.biome = BiomeGenBase.icePlains;
            textures.seen.clear();
            renderer.rain();
            ProductionFixture.require(textures.has("snow.png"), "LOTR snow was not restored");

            // LOTRWorldProvider always supplies its own renderer; use an actual Overworld provider here.
            world = new ControlledWorld(mc, 0);
            mc.theWorld = world;
            checkSkyColors(world);
            checkServerPhases(world);
            for (int tick = 0; tick < 2400; tick++) {
                WeatherRainDelay.advance(world);
            }
            world.biome = BiomeGenBase.plains;
            textures.seen.clear();
            renderer.rain();
            ProductionFixture
                .require(textures.seen.contains("minecraft:textures/environment/rain.png"), "Vanilla rain missing");
            world.biome = BiomeGenBase.desert;
            textures.seen.clear();
            renderer.rain();
            ProductionFixture
                .require(!textures.has("rain.png") && !textures.has("snow.png"), "Vanilla desert received rain");

            world.setRainStrength(0.3F);
            ProductionFixture
                .require(world.getRainStrength(1) == 1, "Weather 2 rain was not boosted to native intensity");
            world.setRainStrength(0);
            WeatherRainDelay.advance(world);
            world.setRainStrength(1);
            ProductionFixture.require(WeatherRainDelay.shouldDelay(world), "Clear weather did not reset delay");
            Config.weather2RainDelaySeconds = 0;
            ProductionFixture.require(!WeatherRainDelay.shouldDelay(world), "Zero delay setting ignored");
            Config.weather2RainDelaySeconds = 120;
            ProductionFixture
                .require(WeatherRainDelay.shouldDelay(new ControlledWorld(mc)), "New world inherited old delay");
            Config.enableWeather2VanillaRain = false;
            textures.seen.clear();
            world.biome = BiomeGenBase.plains;
            renderer.rain();
            ProductionFixture.require(
                textures.seen.isEmpty() && !WeatherRainDelay.shouldDelay(world),
                "Disabled integration did not restore Weather 2's particle renderer");

            Config.enableWeather2VanillaRain = true;
            // A canceled particle pass must not access the player or spawn any custom precipitation.
            net.minecraft.client.entity.EntityClientPlayerMP originalPlayer = mc.thePlayer;
            try {
                mc.thePlayer = null;
                new SceneEnhancer().tickParticlePrecipitation();
            } finally {
                mc.thePlayer = originalPlayer;
            }
            net.minecraft.world.World playerWorld = mc.thePlayer.worldObj;
            try {
                mc.thePlayer.worldObj = world;
                boolean wasEnabled = weather2.util.WeatherUtilConfig.listDimensionsWeather
                    .remove(Integer.valueOf(world.provider.dimensionId));
                try {
                    world.getWorldInfo()
                        .setRaining(true);
                    float strength = SceneEnhancer.getRainStrengthAndControlVisuals(mc.thePlayer, true);
                    ProductionFixture.require(
                        strength == 1 && world.getWorldInfo()
                            .isRaining(),
                        "Weather 2 erased native weather in an unmanaged dimension");
                } finally {
                    if (wasEnabled) {
                        weather2.util.WeatherUtilConfig.listDimensionsWeather.add(world.provider.dimensionId);
                    }
                }
            } finally {
                mc.thePlayer.worldObj = playerWorld;
            }
            ProductionFixture.LOG.info(
                "PRODUCTION_WEATHER_RAIN_PASSED: native rain/snow, LOTR ash/sand, dry desert, two-minute boundary, sky strength, splash delay, resets, zero delay, disabled setting, particle suppression, unmanaged dimension weather");
            finished = true;
        } catch (Exception e) {
            throw new RuntimeException("Weather rain rendering checks failed", e);
        } finally {
            mc.theWorld = originalWorld;
            mc.entityRenderer = originalRenderer;
            mc.renderEngine = originalTextures;
            Config.weather2RainDelaySeconds = delay;
            Config.enableWeather2VanillaRain = enabled;
            ConfigMisc.Particle_RainSnow = particles;
            ConfigMisc.Misc_proxyRenderOverrideEnabled = proxy;
            WeatherRainDelay.state(world).wetTicks = 0;
            GL11.glPopAttrib();
        }
    }

    private void checkServerPhases(ControlledWorld world) {
        boolean managed = weather2.util.WeatherUtilConfig.listDimensionsWeather.remove(Integer.valueOf(0));
        try {
            Config.weather2RainDelaySeconds = 0; // A different client setting must not bypass server mechanics.
            WeatherRainServerChecks.leadIn.apply(world);
            ProductionFixture.require(
                !world.isRaining() && WeatherRainDelay.skyRainStrength(world, 1) == 1,
                "Join during lead-in lost dry state or dark sky");
            for (int tick = 0; tick < 1199; tick++) WeatherRainDelay.advance(world);
            ProductionFixture.require(!world.isRaining(), "Joined client's remaining delay was shortened");
            WeatherRainDelay.advance(world);
            ProductionFixture.require(world.isRaining(), "Joined client did not finish remaining delay");
            WeatherRainDelay.state(world).wetTicks = 0;
            WeatherRainServerChecks.raining.apply(world);
            ProductionFixture.require(world.isRaining(), "Joining active rain incorrectly restarted delay");
            ProductionFixture.LOG.info(
                "PRODUCTION_RAIN_SYNC_PASSED: serialized server phases override client delay and preserve remaining time on join");
        } finally {
            if (managed) weather2.util.WeatherUtilConfig.listDimensionsWeather.add(0);
            WeatherRainDelay.state(world).synchronizedWeather = false;
            WeatherRainDelay.state(world).wetTicks = 0;
            Config.weather2RainDelaySeconds = 120;
        }
    }

    private void checkWetness(ControlledWorld world) throws Exception {
        world.biome = BiomeGenBase.plains;
        world.setRainStrength(1);
        WeatherRainDelay.state(world).wetTicks = 0;
        net.minecraft.entity.passive.EntityPig pig = new net.minecraft.entity.passive.EntityPig(world);
        pig.setPosition(mc.renderViewEntity.posX, mc.renderViewEntity.posY, mc.renderViewEntity.posZ);
        ProductionFixture.require(!pig.isWet(), "Exposed entity is wet before rainfall");
        ProductionFixture.require(
            !world.canLightningStrikeAt((int) pig.posX, (int) pig.posY, (int) pig.posZ),
            "Rain exposure API reports rain before rainfall");
        Method rainWetness = null;
        Object ripples = null;
        Method spawnRipples = null;
        if (cpw.mods.fml.common.Loader.isModLoaded("anextratouch")) {
            Class<?> helper = Class
                .forName("org.fentanylsolutions.anextratouch.handlers.client.effects.WetnessFluidHelper");
            rainWetness = helper.getDeclaredMethod("isRainingOn", net.minecraft.entity.EntityLivingBase.class);
            rainWetness.setAccessible(true);
            ProductionFixture
                .require(!(Boolean) rainWetness.invoke(null, pig), "An Extra Touch considers entity wet before rain");
            Class<?> manager = Class
                .forName("org.fentanylsolutions.anextratouch.handlers.client.effects.WaterRippleManager");
            java.lang.reflect.Constructor<?> constructor = manager.getDeclaredConstructor();
            constructor.setAccessible(true);
            ripples = constructor.newInstance();
            spawnRipples = manager.getDeclaredMethod("spawnRainRipples", Minecraft.class);
            spawnRipples.setAccessible(true);
            world.surfaceQueries = 0;
            spawnRipples.invoke(ripples, mc);
            ProductionFixture
                .require(world.surfaceQueries == 0, "An Extra Touch attempted rain ripples during lead-in");
        }
        for (int tick = 0; tick < 2400; tick++) WeatherRainDelay.advance(world);
        ProductionFixture.require(pig.isWet(), "Exposed entity did not become wet after rain started");
        if (rainWetness != null) {
            ProductionFixture
                .require((Boolean) rainWetness.invoke(null, pig), "An Extra Touch rain wetness did not resume");
            world.surfaceQueries = 0;
            spawnRipples.invoke(ripples, mc);
            ProductionFixture.require(world.surfaceQueries > 0, "An Extra Touch rain ripples did not resume");
            ProductionFixture.LOG
                .info("PRODUCTION_EXTRA_TOUCH_RAIN_PASSED: actual ripple and wet-entity handlers wait for rainfall");
        }
        world.biome = LOTRBiome.shire;
    }

    private void checkSkyColors(ControlledWorld world) {
        int dimension = world.provider.dimensionId;
        boolean alreadyManaged = weather2.util.WeatherUtilConfig.listDimensionsWeather.contains(dimension);
        if (!alreadyManaged) {
            weather2.util.WeatherUtilConfig.listDimensionsWeather.add(dimension);
        }
        BiomeGenBase originalBiome = world.biome;
        try {
            for (BiomeGenBase biome : new BiomeGenBase[] { LOTRBiome.shire, BiomeGenBase.plains,
                BiomeGenBase.desert }) {
                world.biome = biome;
                Config.enableWeather2VanillaRain = false;
                world.setRainStrength(0);
                Vec3 clear = world.getSkyColor(mc.renderViewEntity, 1);
                world.setRainStrength(1);
                Vec3 fullOvercast = world.getSkyColor(mc.renderViewEntity, 1);
                world.setThunderStrength(0.5F);
                Vec3 stormOvercast = world.getSkyColor(mc.renderViewEntity, 1);
                world.setThunderStrength(0);
                world.setRainStrength(0.3F);
                Vec3 weakOvercast = world.getSkyColor(mc.renderViewEntity, 1);
                Config.enableWeather2VanillaRain = true;
                WeatherRainDelay.state(world).wetTicks = 0;
                Vec3 patched = world.getSkyColor(mc.renderViewEntity, 1);
                ProductionFixture.require(
                    patched.distanceTo(stormOvercast) < 0.000001,
                    "Light Weather 2 rain did not produce the darker storm sky: " + biome.biomeName);
                ProductionFixture.require(
                    patched.xCoord + patched.yCoord + patched.zCoord
                        < (fullOvercast.xCoord + fullOvercast.yCoord + fullOvercast.zCoord) * 0.75,
                    "Storm sky is not noticeably darker than the previous overcast tint");
                ProductionFixture.require(
                    Math.abs(patched.zCoord - patched.xCoord) < Math.abs(weakOvercast.zCoord - weakOvercast.xCoord),
                    "Storm sky remained blue");
                ProductionFixture.require(
                    world.getRainStrength(1) == 0 && world.getWeightedThunderStrength(1) == 0
                        && WeatherRainDelay.shouldDelay(world),
                    "Dark sky exposed rain to other mods during the delay");
                world.setRainStrength(0);
                ProductionFixture.require(
                    world.getSkyColor(mc.renderViewEntity, 1)
                        .distanceTo(clear) < 0.000001,
                    "Clear weather did not restore the original sky colour");
                world.setRainStrength(0.3F);
                weather2.util.WeatherUtilConfig.listDimensionsWeather.remove(Integer.valueOf(dimension));
                ProductionFixture.require(
                    world.getSkyColor(mc.renderViewEntity, 1)
                        .distanceTo(weakOvercast) < 0.000001,
                    "Unmanaged dimension's native sky tint changed");
                weather2.util.WeatherUtilConfig.listDimensionsWeather.add(dimension);
            }
            ProductionFixture.LOG.info(
                "PRODUCTION_WEATHER_SKY_PASSED: dimension={}, actual sky colours grey/darker before precipitation, darker storm tint, clear sky restored, unmanaged weather unchanged",
                dimension);
        } finally {
            if (!alreadyManaged) {
                weather2.util.WeatherUtilConfig.listDimensionsWeather.remove(Integer.valueOf(dimension));
            }
            Config.enableWeather2VanillaRain = true;
            world.setRainStrength(1);
            world.biome = originalBiome;
        }
    }

    private static final class ExposedRenderer extends weather2.weathersystem.EntityRendererProxyWeather2Mini {

        private ExposedRenderer(Minecraft mc) {
            super(mc, mc.getResourceManager());
        }

        private void rain() {
            renderRainSnow(0);
        }
    }

    private static final class TrackingTextures extends TextureManager {

        private final TextureManager delegate;
        private final Set<String> seen = new HashSet<>();

        private TrackingTextures(Minecraft mc, TextureManager delegate) {
            super(mc.getResourceManager());
            this.delegate = delegate;
        }

        private boolean has(String file) {
            return seen.contains("lotr:weather/" + file) || seen.contains("minecraft:textures/environment/" + file);
        }

        @Override
        public void bindTexture(ResourceLocation texture) {
            seen.add(texture.toString());
            delegate.bindTexture(texture);
        }
    }

    private static final class ControlledWorld extends WorldClient {

        private BiomeGenBase biome = LOTRBiome.shire;
        private int surfaceQueries;

        private ControlledWorld(Minecraft mc) {
            this(mc, LOTRDimension.MIDDLE_EARTH.dimensionID);
        }

        private ControlledWorld(Minecraft mc, int dimension) {
            super(
                mc.getNetHandler(),
                new WorldSettings(1, WorldSettings.GameType.CREATIVE, false, false, WorldType.DEFAULT),
                dimension,
                EnumDifficulty.PEACEFUL,
                mc.mcProfiler);
            setRainStrength(1);
        }

        @Override
        public BiomeGenBase getBiomeGenForCoords(int x, int z) {
            return biome == null ? LOTRBiome.shire : biome;
        }

        @Override
        public float getCelestialAngle(float partialTicks) {
            return 0;
        }

        @Override
        public int getPrecipitationHeight(int x, int z) {
            surfaceQueries++;
            return (int) Minecraft.getMinecraft().renderViewEntity.posY - 2;
        }

        @Override
        public boolean canBlockSeeTheSky(int x, int y, int z) {
            return true;
        }

        @Override
        public Block getBlock(int x, int y, int z) {
            return Blocks.stone;
        }
    }
}
