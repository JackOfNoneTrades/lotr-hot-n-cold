package org.fentanylsolutions.hotncold.mixins.late.weather2;

import net.minecraft.client.multiplayer.WorldClient;

import org.fentanylsolutions.hotncold.compat.WeatherRainDelay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = weather2.EventHandlerFML.class, remap = false)
public abstract class MixinEventHandlerFML {

    @Redirect(
        method = "tickRenderScreen",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/WorldClient;setRainStrength(F)V",
            remap = true),
        require = 1)
    private void hotncold$separateOvercastFromRain(WorldClient world, float strength) {
        if (!WeatherRainDelay.isEnabled(world)) {
            world.setRainStrength(strength);
            return;
        }
        if (WeatherRainDelay.hasLocalStorms(world)) world.setRainStrength(strength);
        world.getWorldInfo()
            .setRaining(world.isRaining());
    }
}
