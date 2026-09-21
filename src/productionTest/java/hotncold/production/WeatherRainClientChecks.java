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

    public boolean checkStableRenderer(Minecraft minecraft) {
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
            WeatherRainDelay.advance(null);
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
            ProductionFixture.require(world.getRainStrength(1) == 1, "Delay changed sky-darkening strength");
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

            world.rain = 0;
            WeatherRainDelay.advance(world);
            world.rain = 1;
            ProductionFixture.require(WeatherRainDelay.shouldDelay(world), "Clear weather did not reset delay");
            Config.weather2RainDelaySeconds = 0;
            ProductionFixture.require(!WeatherRainDelay.shouldDelay(world), "Zero delay setting ignored");
            Config.weather2RainDelaySeconds = 120;
            WeatherRainDelay.advance(new ControlledWorld(mc));
            ProductionFixture.require(WeatherRainDelay.shouldDelay(world), "World change did not reset delay");
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
            WeatherRainDelay.advance(null);
            GL11.glPopAttrib();
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
        private float rain = 1;
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
        }

        @Override
        public BiomeGenBase getBiomeGenForCoords(int x, int z) {
            return biome == null ? LOTRBiome.shire : biome;
        }

        @Override
        public float getRainStrength(float partialTicks) {
            return rain;
        }

        @Override
        public int getPrecipitationHeight(int x, int z) {
            surfaceQueries++;
            return (int) Minecraft.getMinecraft().renderViewEntity.posY - 2;
        }

        @Override
        public Block getBlock(int x, int y, int z) {
            return Blocks.stone;
        }
    }
}
