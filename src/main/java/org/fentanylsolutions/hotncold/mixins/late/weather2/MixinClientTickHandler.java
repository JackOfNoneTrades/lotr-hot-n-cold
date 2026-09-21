package org.fentanylsolutions.hotncold.mixins.late.weather2;

import net.minecraft.client.Minecraft;

import org.fentanylsolutions.hotncold.compat.WeatherRainDelay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import weather2.ClientTickHandler;
import weather2.config.ConfigMisc;

@Mixin(value = ClientTickHandler.class, remap = false)
public abstract class MixinClientTickHandler {

    @Redirect(
        method = "onTickInGame",
        at = @At(value = "FIELD", target = "Lweather2/config/ConfigMisc;Misc_proxyRenderOverrideEnabled:Z"),
        require = 1)
    private boolean hotncold$keepNativeCameraRenderer() {
        // LOTR installs its own renderer each tick. Weather 2's replacement would fight it and reset the camera.
        // The false branch also removes an existing Weather 2 proxy once; all weather ticking still runs.
        return !WeatherRainDelay.isEnabled(Minecraft.getMinecraft().theWorld)
            && ConfigMisc.Misc_proxyRenderOverrideEnabled;
    }
}
