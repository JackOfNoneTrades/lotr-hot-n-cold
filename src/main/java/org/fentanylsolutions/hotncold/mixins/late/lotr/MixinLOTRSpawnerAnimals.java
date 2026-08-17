package org.fentanylsolutions.hotncold.mixins.late.lotr;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

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
}
