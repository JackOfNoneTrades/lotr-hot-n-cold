package org.fentanylsolutions.hotncold.mixins.late.weather2;

import net.minecraft.entity.player.EntityPlayer;

import org.fentanylsolutions.hotncold.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import weather2.client.SceneEnhancer;
import weather2.util.WeatherUtilConfig;

@Mixin(value = SceneEnhancer.class, remap = false)
public abstract class MixinSceneEnhancer {

    @Inject(method = "tickParticlePrecipitation", at = @At("HEAD"), cancellable = true, require = 1)
    private void hotncold$replacePrecipitationParticles(CallbackInfo ci) {
        if (Config.enableWeather2VanillaRain) {
            ci.cancel();
        }
    }

    @Inject(
        method = "getRainStrengthAndControlVisuals(Lnet/minecraft/entity/player/EntityPlayer;Z)F",
        at = @At("HEAD"),
        cancellable = true,
        require = 1)
    private static void hotncold$preserveOtherDimensionsWeather(EntityPlayer player, boolean overcast,
        CallbackInfoReturnable<Float> cir) {
        if (Config.enableWeather2VanillaRain && player != null
            && !WeatherUtilConfig.listDimensionsWeather.contains(player.worldObj.provider.dimensionId)) {
            // Weather 2 has no storm simulation here: retain the dimension's own weather.
            cir.setReturnValue(player.worldObj.getRainStrength(1));
        }
    }
}
