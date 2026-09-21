package org.fentanylsolutions.hotncold.mixins.late.weather2;

import net.minecraft.client.Minecraft;

import org.fentanylsolutions.hotncold.compat.WeatherRainDelay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import weather2.config.ConfigMisc;
import weather2.weathersystem.EntityRendererProxyWeather2Mini;

@Mixin(value = EntityRendererProxyWeather2Mini.class, remap = false)
public abstract class MixinEntityRendererProxyWeather2Mini {

    @Redirect(
        method = "renderRainSnow",
        remap = true,
        at = @At(value = "FIELD", target = "Lweather2/config/ConfigMisc;Particle_RainSnow:Z", remap = false),
        require = 1)
    private boolean hotncold$restoreNativePrecipitation() {
        // Let EntityRenderer call the world's renderer (including LOTR ash/sand), or vanilla rain/snow.
        return !WeatherRainDelay.isEnabled(Minecraft.getMinecraft().theWorld) && ConfigMisc.Particle_RainSnow;
    }
}
