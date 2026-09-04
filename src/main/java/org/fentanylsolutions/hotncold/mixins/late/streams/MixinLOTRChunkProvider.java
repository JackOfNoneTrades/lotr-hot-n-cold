package org.fentanylsolutions.hotncold.mixins.late.streams;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;

import org.fentanylsolutions.hotncold.compat.StreamsCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import lotr.common.world.LOTRChunkProvider;
import lotr.common.world.LOTRChunkProvider.ChunkFlags;
import lotr.common.world.biome.variant.LOTRBiomeVariant;

@Mixin(value = { LOTRChunkProvider.class }, remap = false)
public abstract class MixinLOTRChunkProvider {

    @Shadow
    private World worldObj;
    @Shadow
    private Random rand;

    @Shadow
    private void replaceBlocksForBiome(int chunkX, int chunkZ, Block[] blocks, byte[] metadata, BiomeGenBase[] biomes,
        LOTRBiomeVariant[] variants, ChunkFlags chunkFlags) {}

    @Redirect(
        method = { "provideChunk(II)Lnet/minecraft/world/chunk/Chunk;" },
        require = 1,
        remap = true,
        at = @At(
            value = "INVOKE",
            remap = false,
            target = "Llotr/common/world/LOTRChunkProvider;replaceBlocksForBiome(II[Lnet/minecraft/block/Block;[B[Lnet/minecraft/world/biome/BiomeGenBase;[Llotr/common/world/biome/variant/LOTRBiomeVariant;Llotr/common/world/LOTRChunkProvider$ChunkFlags;)V"))
    private void hotncold$generateStreamsBeforeBiomeSurface(LOTRChunkProvider instance, int chunkX, int chunkZ,
        Block[] blocks, byte[] metadata, BiomeGenBase[] biomes, LOTRBiomeVariant[] variants, ChunkFlags chunkFlags) {
        StreamsCompat.generateTerrain(this.worldObj, instance, chunkX, chunkZ, blocks, metadata);
        this.replaceBlocksForBiome(chunkX, chunkZ, blocks, metadata, biomes, variants, chunkFlags);
    }

    @Inject(
        method = { "populate(Lnet/minecraft/world/chunk/IChunkProvider;II)V" },
        require = 1,
        remap = true,
        at = { @At(
            value = "INVOKE",
            remap = true,
            target = "Lnet/minecraft/world/gen/structure/MapGenStructure;generateStructuresInChunk(Lnet/minecraft/world/World;Ljava/util/Random;II)Z",
            ordinal = 0) })
    private void hotncold$populateStreams(IChunkProvider provider, int chunkX, int chunkZ, CallbackInfo ci) {
        StreamsCompat.populate(this.worldObj, this.rand, chunkX, chunkZ);
    }
}
