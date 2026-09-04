package org.fentanylsolutions.hotncold.mixins.late.wildcaves3;

import net.minecraft.world.World;

import org.fentanylsolutions.hotncold.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import lotr.common.LOTRDimension;
import wildCaves.WorldGenWildCaves;

@Mixin(value = { WorldGenWildCaves.class }, remap = false)
public class MixinWorldGenWildCaves {

    private static final int MINIMUM_SURFACE_DEPTH = 8;

    @Redirect(
        method = { "generate(Ljava/util/Random;IILnet/minecraft/world/World;)V" },
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isAirBlock(III)Z", remap = true),
        require = 1,
        remap = false)
    private boolean hotncold$isUndergroundAir(World world, int x, int y, int z) {
        if (!world.isAirBlock(x, y, z)) {
            return false;
        } else {
            return Config.enableWildCavesMiddleEarth
                && world.provider.dimensionId == LOTRDimension.MIDDLE_EARTH.dimensionID
                    ? !world.canBlockSeeTheSky(x, y, z) && y < world.getHeightValue(x, z) - 8
                    : true;
        }
    }
}
