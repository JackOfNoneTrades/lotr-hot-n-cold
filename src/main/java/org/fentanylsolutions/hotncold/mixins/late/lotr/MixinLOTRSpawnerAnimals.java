package org.fentanylsolutions.hotncold.mixins.late.lotr;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.world.World;

import org.fentanylsolutions.hotncold.compat.LOTREquipmentControl;
import org.fentanylsolutions.hotncold.compat.LOTRSpawnControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import lotr.common.world.spawning.LOTRSpawnerAnimals;

@Mixin(value = LOTRSpawnerAnimals.class, remap = false)
public abstract class MixinLOTRSpawnerAnimals {

    @Redirect(
        method = "worldGenSpawnAnimals",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;spawnEntityInWorld(Lnet/minecraft/entity/Entity;)Z",
            remap = true),
        require = 1,
        remap = false)
    private static boolean hotncold$blockConfiguredWorldGenSpawns(World world, Entity entity) {
        return LOTRSpawnControl.spawnWorldGenEntityUnlessBlocked(world, entity);
    }

    @Redirect(
        method = "performSpawning",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/EntityLiving;onSpawnWithEgg(Lnet/minecraft/entity/IEntityLivingData;)Lnet/minecraft/entity/IEntityLivingData;",
            remap = true),
        require = 1,
        remap = false)
    private static IEntityLivingData hotncold$applyEquipmentAfterNaturalSpawn(EntityLiving entity,
        IEntityLivingData livingData) {
        return LOTREquipmentControl.finishNaturalSpawn(entity, livingData);
    }

    @Redirect(
        method = "worldGenSpawnAnimals",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/EntityLiving;onSpawnWithEgg(Lnet/minecraft/entity/IEntityLivingData;)Lnet/minecraft/entity/IEntityLivingData;",
            remap = true),
        require = 1,
        remap = false)
    private static IEntityLivingData hotncold$applyEquipmentAfterWorldGenSpawn(EntityLiving entity,
        IEntityLivingData livingData) {
        return LOTREquipmentControl.finishNaturalSpawn(entity, livingData);
    }
}
