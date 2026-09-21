package org.fentanylsolutions.hotncold.mixins.early.minecraft;

import net.minecraft.world.World;

import org.fentanylsolutions.hotncold.compat.WeatherRainAccess;
import org.fentanylsolutions.hotncold.compat.WeatherRainDelay;
import org.fentanylsolutions.hotncold.compat.WeatherRainState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class MixinWorldRain implements WeatherRainAccess {

    @Shadow
    protected float prevRainingStrength;
    @Shadow
    protected float rainingStrength;
    @Shadow
    protected float prevThunderingStrength;
    @Shadow
    protected float thunderingStrength;
    @Unique
    private WeatherRainState hotncold$weather;

    @Override
    public float hotncold$rawRain(float partialTicks) {
        return prevRainingStrength + (rainingStrength - prevRainingStrength) * partialTicks;
    }

    @Override
    public float hotncold$rawThunder(float partialTicks) {
        return prevThunderingStrength + (thunderingStrength - prevThunderingStrength) * partialTicks;
    }

    @Override
    public WeatherRainState hotncold$rainState() {
        if (hotncold$weather == null) hotncold$weather = new WeatherRainState();
        return hotncold$weather;
    }

    @Redirect(
        method = "getSunBrightnessFactor",
        remap = false,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getRainStrength(F)F", remap = true),
        require = 1)
    private float hotncold$keepOvercastLight(World world, float partialTicks) {
        return WeatherRainDelay.skyRainStrength(world, partialTicks);
    }

    @Redirect(
        method = "getSunBrightnessFactor",
        remap = false,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getWeightedThunderStrength(F)F", remap = true),
        require = 1)
    private float hotncold$keepOvercastThunderLight(World world, float partialTicks) {
        return WeatherRainDelay.skyThunderStrength(world, partialTicks);
    }

    @Inject(method = "getRainStrength", at = @At("HEAD"), cancellable = true, require = 1)
    private void hotncold$delayWetWeather(float partialTicks, CallbackInfoReturnable<Float> cir) {
        World world = (World) (Object) this;
        if (WeatherRainDelay.isEnabled(world)) {
            cir.setReturnValue(WeatherRainDelay.rainStrength(world, partialTicks));
        }
    }
}
