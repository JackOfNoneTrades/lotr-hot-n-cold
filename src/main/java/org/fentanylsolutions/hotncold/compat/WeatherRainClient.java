package org.fentanylsolutions.hotncold.compat;

import net.minecraft.client.Minecraft;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public final class WeatherRainClient {

    public static void initialize() {
        cpw.mods.fml.common.FMLCommonHandler.instance()
            .bus()
            .register(new WeatherRainClient());
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase == TickEvent.Phase.END && !mc.isGamePaused()) {
            WeatherRainDelay.advance(mc.theWorld);
            if (WeatherRainDelay.isEnabled(mc.theWorld)) {
                mc.theWorld.getWorldInfo()
                    .setRaining(mc.theWorld.isRaining());
            }
        }
    }
}
