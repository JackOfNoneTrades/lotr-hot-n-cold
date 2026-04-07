package org.fentanylsolutions.hotncold.mixins.late.enviromine;

import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.chunk.Chunk;

import org.fentanylsolutions.hotncold.HotNCold;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import enviromine.handlers.EM_StatusManager;

@Mixin(value = EM_StatusManager.class, remap = false)
public class MixinEMStatusManager {

    @Redirect(
        method = "getSurroundingData",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/Chunk;getBiomeGenForWorldCoords(IILnet/minecraft/world/biome/WorldChunkManager;)Lnet/minecraft/world/biome/BiomeGenBase;"))
    private static BiomeGenBase hotncold$useProviderAwareBiomeLookup(Chunk chunk, int localX, int localZ,
        WorldChunkManager worldChunkManager) {
        if (chunk == null) {
            return null;
        }

        int blockX = (chunk.xPosition << 4) + (localX & 15);
        int blockZ = (chunk.zPosition << 4) + (localZ & 15);
        return HotNCold.getWorldBiomeForCoords(chunk.worldObj, blockX, blockZ);
    }
}
