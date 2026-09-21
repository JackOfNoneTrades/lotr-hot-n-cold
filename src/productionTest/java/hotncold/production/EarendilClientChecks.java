package hotncold.production;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import lotr.client.render.LOTRSkyRenderer;
import lotr.common.LOTRDimension;
import lotr.common.world.LOTRWorldProvider;
import lotr.common.world.biome.LOTRBiome;

/** Exercises the transformed release renderer with real GL and textures, controlling biome, time and rain. */
public final class EarendilClientChecks extends GuiScreen {

    private static final int SIZE = 256;
    public boolean rendered;

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (rendered) {
            return;
        }
        TextureManager original = mc.renderEngine;
        TrackingTextures textures = new TrackingTextures(mc, original);
        ControlledWorld world = new ControlledWorld(mc);
        LOTRSkyRenderer renderer = new LOTRSkyRenderer((LOTRWorldProvider) world.provider);
        mc.renderEngine = textures;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(-4, 4, -4, 4, 1, 300);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        try {
            int biomes = 0;
            int darkBiomes = 0;
            for (LOTRBiome biome : LOTRDimension.MIDDLE_EARTH.biomeList) {
                if (biome == null) {
                    continue;
                }
                biomes++;
                if (!biome.hasSky()) {
                    darkBiomes++;
                }
                for (float angle : new float[] { 0.225F, 0.775F, 0.0F, 0.5F }) {
                    for (float rain : new float[] { 0.0F, 0.5F, 1.0F }) {
                        world.biome = LOTRBiome.shire;
                        world.angle = angle;
                        world.rain = rain;
                        render(renderer, world, textures);
                        int referenceCount = textures.earendil;
                        float referenceAlpha = textures.alpha;
                        ProductionFixture.require(
                            referenceCount == (angle == 0.225F || angle == 0.775F ? 1 : 0),
                            "Normal Shire Eärendil pass did not match the expected time window");
                        world.biome = biome;
                        render(renderer, world, textures);
                        ProductionFixture.require(
                            textures.earendil == referenceCount,
                            "Eärendil count differs from Shire: " + biome.biomeName + ", angle=" + angle);
                        ProductionFixture.require(
                            referenceCount == 0 || Math.abs(textures.alpha - referenceAlpha) < 0.000001F,
                            "Eärendil fading differs from Shire: " + biome.biomeName);
                        ProductionFixture.require(
                            textures.otherCelestials == (biome.hasSky() ? 2 : 0),
                            "Sun/moon biome visibility changed: " + biome.biomeName);
                    }
                }
            }
            ProductionFixture.require(darkBiomes >= 6, "Did not exercise the Mordor biome family");
            world.angle = 0.775F;
            world.rain = 0;
            world.biome = LOTRBiome.shire;
            render(renderer, world, textures);
            int[] shire = pixels("earendil-shire.png");
            world.biome = LOTRBiome.mordor;
            render(renderer, world, textures);
            int[] mordor = pixels("earendil-mordor.png");
            ProductionFixture
                .require(Arrays.equals(shire, mordor), "Mordor Eärendil pixels differ from normal rendering");
            int lit = 0;
            for (int pixel : mordor) {
                if ((pixel & 0xFFFFFF) != 0) {
                    lit++;
                }
            }
            ProductionFixture.require(lit > 20, "Eärendil did not produce visible pixels");
            world.rain = 1;
            render(renderer, world, textures);
            ProductionFixture.require(!Arrays.equals(mordor, pixels("earendil-rain.png")), "Rain did not dim Eärendil");
            world.provider.dimensionId = LOTRDimension.UTUMNO.dimensionID;
            render(renderer, world, textures);
            ProductionFixture.require(textures.earendil == 0, "Extra Eärendil pass leaked outside Middle-earth");
            ProductionFixture.require(GL11.glGetError() == GL11.GL_NO_ERROR, "Sky rendering generated an OpenGL error");
            ProductionFixture.LOG.info(
                "PRODUCTION_EARENDIL_PASSED: {} biomes ({} dark), dawn/dusk/day/night, dry/half/full rain, sun/moon restrictions, dimension isolation, matching visible pixels",
                biomes,
                darkBiomes);
            rendered = true;
        } catch (Exception e) {
            throw new RuntimeException("Eärendil rendering checks failed", e);
        } finally {
            mc.renderEngine = original;
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopAttrib();
        }
    }

    private void render(LOTRSkyRenderer renderer, ControlledWorld world, TrackingTextures textures) {
        textures.earendil = textures.otherCelestials = 0;
        textures.alpha = 0;
        GL11.glViewport(0, 0, SIZE, SIZE);
        GL11.glClearColor(0, 0, 0, 1);
        GL11.glDepthMask(true);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glLoadIdentity();
        // Look directly at the star; the narrow view excludes the sun and moon.
        GL11.glRotatef(-90 - world.angle * 360 - Math.signum(world.angle - 0.5F) * 18, 1, 0, 0);
        GL11.glRotatef(90, 0, 1, 0);
        FloatBuffer before = BufferUtils.createFloatBuffer(16);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, before);
        renderer.render(0, world, mc);
        FloatBuffer after = BufferUtils.createFloatBuffer(16);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, after);
        ProductionFixture.require(before.equals(after), "Sky renderer leaked its matrix");
    }

    private int[] pixels(String name) throws Exception {
        ByteBuffer bytes = BufferUtils.createByteBuffer(SIZE * SIZE * 4);
        GL11.glReadPixels(0, 0, SIZE, SIZE, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, bytes);
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
        int[] pixels = new int[SIZE * SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int offset = (y * SIZE + x) * 4;
                int rgb = (bytes.get(offset) & 255) << 16 | (bytes.get(offset + 1) & 255) << 8
                    | (bytes.get(offset + 2) & 255);
                pixels[y * SIZE + x] = rgb;
                image.setRGB(x, SIZE - 1 - y, rgb);
            }
        }
        File directory = new File(mc.mcDataDir, "screenshots");
        directory.mkdirs();
        ImageIO.write(image, "png", new File(directory, name));
        return pixels;
    }

    private static final class TrackingTextures extends TextureManager {

        private final TextureManager delegate;
        private int earendil;
        private int otherCelestials;
        private float alpha;

        private TrackingTextures(Minecraft mc, TextureManager delegate) {
            super(mc.getResourceManager());
            this.delegate = delegate;
        }

        @Override
        public void bindTexture(ResourceLocation texture) {
            if ("lotr:sky/earendil.png".equals(texture.toString())) {
                earendil++;
                FloatBuffer color = BufferUtils.createFloatBuffer(16);
                GL11.glGetFloat(GL11.GL_CURRENT_COLOR, color);
                alpha = color.get(3);
            } else
                if ("lotr:sky/sun.png".equals(texture.toString()) || "lotr:sky/moon.png".equals(texture.toString())) {
                    otherCelestials++;
                }
            delegate.bindTexture(texture);
        }
    }

    private static final class ControlledWorld extends WorldClient {

        private LOTRBiome biome = LOTRBiome.shire;
        private float angle;
        private float rain;

        private ControlledWorld(Minecraft mc) {
            super(
                mc.getNetHandler(),
                new WorldSettings(1, WorldSettings.GameType.CREATIVE, false, false, WorldType.DEFAULT),
                LOTRDimension.MIDDLE_EARTH.dimensionID,
                EnumDifficulty.PEACEFUL,
                mc.mcProfiler);
        }

        @Override
        public BiomeGenBase getBiomeGenForCoords(int x, int z) {
            return biome == null ? LOTRBiome.shire : biome;
        }

        @Override
        public float getCelestialAngle(float partialTicks) {
            return angle;
        }

        @Override
        public float getRainStrength(float partialTicks) {
            return rain;
        }

        @Override
        public float getStarBrightness(float partialTicks) {
            return 0;
        }

        @Override
        public Vec3 getSkyColor(Entity entity, float partialTicks) {
            return Vec3.createVectorHelper(0, 0, 0);
        }

        @Override
        public double getHorizon() {
            return -1000;
        }
    }
}
