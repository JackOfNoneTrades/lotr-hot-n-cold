package org.fentanylsolutions.hotncold.mixins.late.gregcaves;

import java.util.Arrays;

import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.gen.MapGenCaves;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import lotr.common.LOTRDimension;
import mods.tesseract.gregcaves.world.MapGenGregCaves;

@Mixin(value = MapGenGregCaves.class, remap = false)
public abstract class MixinMapGenGregCaves extends MapGenCaves {

    @Redirect(
        method = "generateNoiseCavesNoise(II)V",
        require = 1,
        remap = false,
        at = @At(
            value = "INVOKE",
            remap = true,
            target = "Lnet/minecraft/world/biome/WorldChunkManager;getBiomesForGeneration([Lnet/minecraft/world/biome/BiomeGenBase;IIII)[Lnet/minecraft/world/biome/BiomeGenBase;"))
    private BiomeGenBase[] hotncold$useVanillaCaveDensityProfile(WorldChunkManager manager, BiomeGenBase[] biomes,
        int x, int z, int width, int height) {
        if (worldObj != null && worldObj.provider.dimensionId == LOTRDimension.MIDDLE_EARTH.dimensionID) {
            // Preserve test4's cave-density profile, without replacing the world's actual LOTR biomes.
            int size = width * height;
            if (biomes == null || biomes.length < size) {
                biomes = new BiomeGenBase[size];
            }
            Arrays.fill(biomes, 0, size, BiomeGenBase.plains);
            return biomes;
        }
        return manager.getBiomesForGeneration(biomes, x, z, width, height);
    }
}
