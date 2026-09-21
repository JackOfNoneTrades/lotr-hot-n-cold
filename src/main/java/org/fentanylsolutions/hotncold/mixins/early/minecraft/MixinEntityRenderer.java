package org.fentanylsolutions.hotncold.mixins.early.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;

import org.fentanylsolutions.hotncold.compat.WeatherRainDelay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

    @Inject(method = "renderRainSnow", at = @At("HEAD"), cancellable = true, require = 1)
    private void hotncold$delayPrecipitation(float partialTicks, CallbackInfo ci) {
        if (WeatherRainDelay.shouldDelay(Minecraft.getMinecraft().theWorld)) {
            ci.cancel();
        }
    }

    @Inject(method = "addRainParticles", at = @At("HEAD"), cancellable = true, require = 1)
    private void hotncold$delayRainSoundsAndSplashes(CallbackInfo ci) {
        if (WeatherRainDelay.shouldDelay(Minecraft.getMinecraft().theWorld)) {
            ci.cancel();
        }
    }
}
