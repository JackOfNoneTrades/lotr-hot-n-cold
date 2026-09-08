package org.fentanylsolutions.hotncold.mixins.early.minecraft;

import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.world.World;

import org.fentanylsolutions.hotncold.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;

@Mixin(EntityLightningBolt.class)
public abstract class MixinEntityLightningBolt {

    @WrapWithCondition(
        method = "onUpdate",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSoundEffect(DDDLjava/lang/String;FF)V"),
        require = 2,
        expect = 2)
    private boolean hotncold$keepLightningSound(World world, double x, double y, double z, String sound, float volume,
        float pitch) {
        // Keep the original call and its arguments (including RNG consumption) unless this is the muted impact.
        return !Config.disableLightningExplosionSound || !"random.explode".equals(sound);
    }
}
