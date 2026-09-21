package org.fentanylsolutions.hotncold.mixins.late.weather2;

import net.minecraft.world.World;

import org.fentanylsolutions.hotncold.compat.WeatherRainDelay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = weather2.weathersystem.storm.StormObject.class, remap = false)
public abstract class MixinStormObject {

    @Redirect(
        method = "tickProgression",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isRaining()Z", remap = true),
        require = 5)
    private boolean hotncold$preserveStormSimulation(World world) {
        return WeatherRainDelay.isEnabled(world) ? WeatherRainDelay.rawRainStrength(world, 1) > 0.2F
            : world.isRaining();
    }
}
