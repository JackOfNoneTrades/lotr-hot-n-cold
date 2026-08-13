package org.fentanylsolutions.hotncold.mixins.late.wotrmc;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import wotrmc.common.achievements.WOTRMCServerAchievement;

@Mixin(value = WOTRMCServerAchievement.class, remap = false)
public abstract class MixinWOTRMCServerAchievement {

    @Inject(method = "export", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private static void hotncold$allowDedicatedServers(CallbackInfo ci) {
        ci.cancel();
    }
}
