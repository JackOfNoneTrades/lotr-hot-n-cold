package org.fentanylsolutions.hotncold.mixins.late.enviromine;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.biome.BiomeGenBase;

import org.fentanylsolutions.hotncold.HotNCold;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import enviromine.handlers.compat.EM_StatusManager_LOTR;

@Mixin(value = EM_StatusManager_LOTR.class, remap = false)
public class MixinEMStatusManagerLOTR {

    @Inject(method = "findLOTRBiome", at = @At("HEAD"), cancellable = true, remap = false)
    private static void hotncold$useProviderAwareBiomeLookup(EntityLivingBase entity, int blockX, int blockZ,
        CallbackInfoReturnable<BiomeGenBase> cir) {
        if (entity == null || entity.worldObj == null) {
            cir.setReturnValue(null);
            return;
        }
        cir.setReturnValue(HotNCold.getWorldBiomeForCoords(entity.worldObj, blockX, blockZ));
    }
}
