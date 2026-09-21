package org.fentanylsolutions.hotncold.compat;

import net.minecraft.world.World;

import org.fentanylsolutions.hotncold.Config;

/** Separates overcast weather from precipitation, on both sides of the connection. */
public final class WeatherRainDelay {

    private static boolean installed;

    public static void initialize() {
        installed = true;
        WeatherRainSync.initialize();
    }

    public static WeatherRainState state(World world) {
        return ((WeatherRainAccess) world).hotncold$rainState();
    }

    public static boolean isEnabled(World world) {
        if (!installed || world == null) return false;
        WeatherRainState state = state(world);
        return world.isRemote && state.synchronizedWeather ? state.serverEnabled : Config.enableWeather2VanillaRain;
    }

    public static float rawRainStrength(World world, float partialTicks) {
        return ((WeatherRainAccess) world).hotncold$rawRain(partialTicks);
    }

    public static boolean hasLocalStorms(World world) {
        return installed && world.isRemote
            && weather2.util.WeatherUtilConfig.listDimensionsWeather.contains(world.provider.dimensionId);
    }

    private static float sourceRain(World world, float partialTicks) {
        WeatherRainState state = state(world);
        if (world.isRemote && state.synchronizedWeather && !hasLocalStorms(world)) return state.serverRain;
        return rawRainStrength(world, partialTicks);
    }

    private static int delayTicks(World world) {
        WeatherRainState state = state(world);
        return (world.isRemote && state.synchronizedWeather ? state.serverDelay : Config.weather2RainDelaySeconds) * 20;
    }

    public static boolean shouldDelay(World world) {
        if (!isEnabled(world) || sourceRain(world, 1) <= 0) return false;
        WeatherRainState state = state(world);
        // Native/global rainfall follows the server's phase, including when joining an active rain.
        int elapsed = world.isRemote && state.synchronizedWeather && (!hasLocalStorms(world) || state.serverRain > 0)
            ? state.serverWetTicks
            : state.wetTicks;
        return elapsed < delayTicks(world);
    }

    public static float rainStrength(World world, float partialTicks) {
        if (shouldDelay(world)) return 0;
        float rain = sourceRain(world, partialTicks);
        return hasLocalStorms(world) ? normalize(rain) : rain;
    }

    private static float normalize(float rain) {
        // Weather 2's ordinary rain tops out at 0.3, which is too faint for native precipitation.
        return net.minecraft.util.MathHelper.clamp_float(rain / 0.3F, 0, 1);
    }

    public static float skyRainStrength(World world, float partialTicks) {
        if (!isEnabled(world)) return world.getRainStrength(partialTicks);
        float rain = sourceRain(world, partialTicks);
        return hasLocalStorms(world) ? normalize(rain) : rain;
    }

    public static float skyThunderStrength(World world, float partialTicks) {
        if (!isEnabled(world)) return world.getWeightedThunderStrength(partialTicks);
        float thunder = ((WeatherRainAccess) world).hotncold$rawThunder(partialTicks) * sourceRain(world, partialTicks);
        // Rendering only: a half-strength thunder tint darkens full overcast by roughly 30%.
        return hasLocalStorms(world) ? Math.max(thunder, skyRainStrength(world, partialTicks) * 0.5F) : thunder;
    }

    public static void advance(World world) {
        if (world == null) return;
        WeatherRainState state = state(world);
        state.advance(isEnabled(world) ? sourceRain(world, 1) : 0, delayTicks(world));
        if (world.isRemote && state.synchronizedWeather && state.serverRain > 0) {
            state.serverWetTicks = Math.min(state.serverWetTicks + 1, delayTicks(world));
        }
    }
}
