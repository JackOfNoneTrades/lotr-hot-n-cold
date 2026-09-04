package org.fentanylsolutions.hotncold.mixins.late.wildcaves3;

import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

import org.fentanylsolutions.hotncold.Config;
import org.fentanylsolutions.hotncold.compat.WildCavesCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import lotr.common.LOTRDimension;
import lotr.common.world.LOTRChunkProvider;

@Mixin(value = { LOTRChunkProvider.class }, remap = false)
public class MixinLOTRChunkProvider {

    @Shadow
    private World worldObj;
    @Shadow
    private Random rand;

    @Inject(
        method = { "populate(Lnet/minecraft/world/chunk/IChunkProvider;II)V" },
        require = 1,
        remap = true,
        at = { @At(
            value = "INVOKE",
            remap = true,
            target = "Llotr/common/world/biome/LOTRBiome;decorate(Lnet/minecraft/world/World;Ljava/util/Random;II)V",
            shift = Shift.AFTER) })
    private void hotncold$generateWildCaves(IChunkProvider provider, int chunkX, int chunkZ, CallbackInfo ci) {
        if (Config.enableWildCavesMiddleEarth
            && this.worldObj.provider.dimensionId == LOTRDimension.MIDDLE_EARTH.dimensionID) {
            WildCavesCompat.generate(this.worldObj, this.rand, chunkX << 4, chunkZ << 4);
        }
    }
}
