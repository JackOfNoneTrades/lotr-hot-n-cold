package org.fentanylsolutions.hotncold.mixins.late.gregcaves;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.MapGenBase;

import org.fentanylsolutions.hotncold.Config;
import org.fentanylsolutions.hotncold.HotNCold;
import org.fentanylsolutions.hotncold.compat.GregCavesCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import lotr.common.LOTRDimension;
import lotr.common.world.LOTRChunkProvider;
import lotr.common.world.mapgen.LOTRMapGenCaves;

@Mixin(value = LOTRChunkProvider.class, remap = false)
public class MixinLOTRChunkProvider {

    @Unique
    private MapGenBase hotncold$gregCavesGenerator;
    @Unique
    private boolean hotncold$attemptedGregCavesCreation;

    @Redirect(
        method = "provideChunk(II)Lnet/minecraft/world/chunk/Chunk;",
        require = 1,
        remap = true,
        at = @At(
            value = "INVOKE",
            remap = true,
            target = "Llotr/common/world/mapgen/LOTRMapGenCaves;func_151539_a(Lnet/minecraft/world/chunk/IChunkProvider;Lnet/minecraft/world/World;II[Lnet/minecraft/block/Block;)V"))
    private void hotncold$generateGregCaves(LOTRMapGenCaves lotrGenerator, IChunkProvider provider, World world,
        int chunkX, int chunkZ, Block[] blocks) {
        if (Config.enableGregCavesMiddleEarth && world.provider.dimensionId == LOTRDimension.MIDDLE_EARTH.dimensionID) {
            if (!hotncold$attemptedGregCavesCreation) {
                hotncold$attemptedGregCavesCreation = true;
                hotncold$gregCavesGenerator = GregCavesCompat.createGenerator();
            }
            if (hotncold$gregCavesGenerator != null) {
                int before = Config.logWorldgenCompatibility ? hotncold$countAir(blocks) : 0;
                hotncold$gregCavesGenerator.func_151539_a(provider, world, chunkX, chunkZ, blocks);
                if (Config.logWorldgenCompatibility) {
                    HotNCold.LOG.info(
                        "[worldgen][GregCaves] chunk=({},{}), carvedDelta={}",
                        chunkX,
                        chunkZ,
                        hotncold$countAir(blocks) - before);
                }
                return;
            }
        }
        lotrGenerator.func_151539_a(provider, world, chunkX, chunkZ, blocks);
    }

    @Unique
    private static int hotncold$countAir(Block[] blocks) {
        int count = 0;
        for (Block block : blocks) {
            if (block == null || block == Blocks.air) {
                count++;
            }
        }
        return count;
    }
}
