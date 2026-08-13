package org.fentanylsolutions.hotncold.mixins.late.wotrmc;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import wotrmc.common.achievements.WOTRMCAchievement;

@Mixin(value = WOTRMCAchievement.class, remap = false)
public abstract class MixinWOTRMCAchievement {

    @Inject(method = "export", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private static void hotncold$allowDedicatedServers(CallbackInfo ci) {
        ci.cancel();
    }
}
