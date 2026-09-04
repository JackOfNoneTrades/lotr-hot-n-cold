package org.fentanylsolutions.hotncold.mixins.late.streams;

import net.minecraft.world.IBlockAccess;

import org.fentanylsolutions.hotncold.compat.StreamsCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import streams.world.gen.structure.RiverComponent;
import streams.world.gen.structure.RiverMouthComponent;

@Mixin(value = { RiverMouthComponent.class }, remap = false)
public class MixinRiverMouthComponent {

    @Inject(method = { "isValid(Lnet/minecraft/world/IBlockAccess;)Z" }, at = { @At("HEAD") }, cancellable = true)
    private void hotncold$restrictMouthBiome(IBlockAccess worldAccess, CallbackInfoReturnable<Boolean> cir) {
        RiverComponent component = (RiverComponent) (Object) this;
        if (!StreamsCompat.isAllowedComponent(worldAccess, component.paddedBox(), true)) {
            cir.setReturnValue(false);
        }
    }
}
