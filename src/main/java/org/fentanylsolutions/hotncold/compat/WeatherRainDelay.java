package org.fentanylsolutions.hotncold.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;

import org.fentanylsolutions.hotncold.Config;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Delays only client precipitation; sky lighting and server weather continue normally. */
public final class WeatherRainDelay {

    private static boolean installed;
    private static World currentWorld;
    private static int wetTicks;

    public static void initialize() {
        installed = true;
        cpw.mods.fml.common.FMLCommonHandler.instance()
            .bus()
            .register(new WeatherRainDelay());
    }

    public static boolean isEnabled() {
        return installed && Config.enableWeather2VanillaRain;
    }

    public static boolean shouldDelay(World world) {
        if (!isEnabled() || world == null || Config.weather2RainDelaySeconds <= 0) {
            return false;
        }
        return world.getRainStrength(1) > 0
            && (world != currentWorld || wetTicks < Config.weather2RainDelaySeconds * 20);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.isGamePaused()) {
            return;
        }
        advance(mc.theWorld);
    }

    public static void advance(World world) {
        if (world != currentWorld) {
            currentWorld = world;
            wetTicks = 0;
        }
        if (!isEnabled() || world == null || world.getRainStrength(1) <= 0) {
            wetTicks = 0;
        } else if (wetTicks < Config.weather2RainDelaySeconds * 20) {
            wetTicks++;
        }
    }
}
