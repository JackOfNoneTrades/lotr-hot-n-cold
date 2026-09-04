package org.fentanylsolutions.hotncold.mixins.late.wildcaves3;

import java.util.Random;

import net.minecraft.world.World;

import org.fentanylsolutions.hotncold.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import lotr.common.LOTRDimension;
import wildCaves.generation.structureGen.DecorationHelper;

@Mixin(value = { DecorationHelper.class }, remap = false)
public class MixinDecorationHelper {

    @Inject(
        method = { "generateFloodedCaves(Lnet/minecraft/world/World;Ljava/util/Random;III)V" },
        at = { @At("HEAD") },
        cancellable = true,
        require = 1,
        remap = false)
    private static void hotncold$disableFloodedMiddleEarthCaves(World world, Random random, int x, int y, int z,
        CallbackInfo ci) {
        if (Config.enableWildCavesMiddleEarth && world.provider.dimensionId == LOTRDimension.MIDDLE_EARTH.dimensionID) {
            ci.cancel();
        }
    }
}
