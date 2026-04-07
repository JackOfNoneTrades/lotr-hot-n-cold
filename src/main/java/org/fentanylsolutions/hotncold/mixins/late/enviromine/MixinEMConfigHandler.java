package org.fentanylsolutions.hotncold.mixins.late.enviromine;

import org.fentanylsolutions.hotncold.compat.EnviroMineCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import enviromine.core.EM_ConfigHandler;

@Mixin(value = EM_ConfigHandler.class, remap = false)
public class MixinEMConfigHandler {

    @Inject(method = "initProfile", at = @At("RETURN"))
    private static void hotncold$reapplyEnviromineBiomeOverrides(CallbackInfo ci) {
        EnviroMineCompat.applyBiomeTemperatureOverrides("EM_ConfigHandler.initProfile");
    }
}
