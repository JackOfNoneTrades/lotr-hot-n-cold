package org.fentanylsolutions.hotncold.mixins.late.streams;

import net.minecraft.world.IBlockAccess;

import org.fentanylsolutions.hotncold.compat.StreamsCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import scala.collection.Seq;
import streams.world.gen.structure.RiverComponent;
import streams.world.gen.structure.RiverUpstreamComponent;

@Mixin(value = { RiverUpstreamComponent.class }, remap = false)
public class MixinRiverUpstreamComponent {

    @Inject(
        method = { "isValid(Lscala/collection/Seq;Lnet/minecraft/world/IBlockAccess;)Z" },
        at = { @At("HEAD") },
        cancellable = true)
    private void hotncold$restrictUpstreamBiome(Seq<RiverComponent> uncommitted, IBlockAccess worldAccess,
        CallbackInfoReturnable<Boolean> cir) {
        RiverComponent component = (RiverComponent) (Object) this;
        if (!StreamsCompat.isAllowedComponent(worldAccess, component.paddedBox(), false)) {
            cir.setReturnValue(false);
        }
    }
}
