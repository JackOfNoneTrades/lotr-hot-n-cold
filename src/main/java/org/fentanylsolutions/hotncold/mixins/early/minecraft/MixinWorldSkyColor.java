package org.fentanylsolutions.hotncold.mixins.early.minecraft;

import net.minecraft.world.World;

import org.fentanylsolutions.hotncold.compat.WeatherRainDelay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(World.class)
public abstract class MixinWorldSkyColor {

    @Redirect(
        method = { "getSkyColorBody", "getSunBrightnessBody" },
        remap = false,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getWeightedThunderStrength(F)F", remap = true),
        require = 1)
    private float hotncold$darkenStormSky(World world, float partialTicks) {
        return WeatherRainDelay.skyThunderStrength(world, partialTicks);
    }

    @Redirect(
        method = { "getSkyColorBody", "getSunBrightnessBody" },
        remap = false,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getRainStrength(F)F", remap = true),
        require = 1)
    private float hotncold$greyStormSky(World world, float partialTicks) {
        return WeatherRainDelay.skyRainStrength(world, partialTicks);
    }
}
