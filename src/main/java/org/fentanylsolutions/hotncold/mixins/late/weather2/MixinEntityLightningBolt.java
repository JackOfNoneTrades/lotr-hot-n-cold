package org.fentanylsolutions.hotncold.mixins.late.weather2;

import net.minecraft.world.World;

import org.fentanylsolutions.hotncold.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;

import weather2.entity.EntityLightningBolt;

@Mixin(value = EntityLightningBolt.class, remap = false)
public abstract class MixinEntityLightningBolt {

    @WrapWithCondition(
        method = "onUpdate",
        remap = true,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSound(DDDLjava/lang/String;FFZ)V"),
        require = 2,
        expect = 2)
    private boolean hotncold$keepLightningSound(World world, double x, double y, double z, String sound, float volume,
        float pitch, boolean distanceDelay) {
        // Weather 2 emits these on the client. Preserve thunder, sound arguments and RNG consumption.
        return !Config.disableLightningExplosionSound || !"random.explode".equals(sound);
    }
}
