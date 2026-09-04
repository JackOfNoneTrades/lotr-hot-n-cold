package org.fentanylsolutions.hotncold.mixins.late.lotr;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.IEntityLivingData;

import org.fentanylsolutions.hotncold.compat.LOTREquipmentControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import lotr.common.world.spawning.LOTRSpawnerNPCs;

@Mixin(value = LOTRSpawnerNPCs.class, remap = false)
public abstract class MixinLOTRSpawnerNPCs {

    @Redirect(
        method = "performSpawning",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/EntityLiving;onSpawnWithEgg(Lnet/minecraft/entity/IEntityLivingData;)Lnet/minecraft/entity/IEntityLivingData;",
            remap = true),
        require = 1,
        remap = false)
    private static IEntityLivingData hotncold$applyEquipmentAfterNaturalNPCSpawn(EntityLiving entity,
        IEntityLivingData livingData) {
        return LOTREquipmentControl.finishNaturalSpawn(entity, livingData);
    }
}
