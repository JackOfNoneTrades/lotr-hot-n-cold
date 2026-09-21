package org.fentanylsolutions.hotncold.mixins.late.lotr;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.BiomeGenBase;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import lotr.client.render.LOTRSkyRenderer;
import lotr.common.LOTRDimension;
import lotr.common.world.biome.LOTRBiome;

@Mixin(value = LOTRSkyRenderer.class, remap = false)
public abstract class MixinLOTRSkyRenderer {

    @Shadow
    @Final
    private static ResourceLocation earendilTexture;

    // After the celestial matrix is popped, but before the below-horizon sky is drawn.
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/WorldClient;getHorizon()D",
            remap = true),
        require = 1)
    private void hotncold$renderEarendilInDarkBiomes(float partialTicks, WorldClient world, Minecraft mc,
        CallbackInfo ci) {
        if (world.provider.dimensionId != LOTRDimension.MIDDLE_EARTH.dimensionID || !world.provider.isSurfaceWorld()) {
            return;
        }
        BiomeGenBase biome = world.getBiomeGenForCoords(
            MathHelper.floor_double(mc.renderViewEntity.posX),
            MathHelper.floor_double(mc.renderViewEntity.posZ));
        if (!(biome instanceof LOTRBiome) || ((LOTRBiome) biome).hasSky()) {
            return;
        }

        // Match Legacy's normal Eärendil pass, including its dawn/dusk window and rain fading.
        float celestialAngle = world.getCelestialAngle(partialTicks);
        float offset = celestialAngle - 0.5F;
        float distance = Math.abs(offset);
        float min = 0.15F;
        float max = 0.3F;
        if (distance < min || distance > max) {
            return;
        }
        float midpoint = (min + max) / 2.0F;
        float halfWidth = max - midpoint;
        float brightness = MathHelper.cos((distance - midpoint) / halfWidth * (float) Math.PI / 2.0F);
        brightness *= brightness;
        brightness *= 1.0F - world.getRainStrength(partialTicks);

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT | GL11.GL_TEXTURE_BIT);
        GL11.glPushMatrix();
        try {
            GL11.glDisable(GL11.GL_FOG);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(celestialAngle * 360.0F, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(Math.signum(offset) * 18.0F, 1.0F, 0.0F, 0.0F);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, brightness);
            mc.renderEngine.bindTexture(earendilTexture);
            Tessellator tessellator = Tessellator.instance;
            double radius = 1.5;
            tessellator.startDrawingQuads();
            tessellator.addVertexWithUV(-radius, 100.0, -radius, 0.0, 0.0);
            tessellator.addVertexWithUV(radius, 100.0, -radius, 1.0, 0.0);
            tessellator.addVertexWithUV(radius, 100.0, radius, 1.0, 1.0);
            tessellator.addVertexWithUV(-radius, 100.0, radius, 0.0, 1.0);
            tessellator.draw();
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }
}
