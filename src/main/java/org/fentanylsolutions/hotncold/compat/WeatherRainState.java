package org.fentanylsolutions.hotncold.compat;

/** One state per world, so integrated-server and client clocks cannot overwrite one another. */
public final class WeatherRainState {

    public int wetTicks;
    public float previousServerRain;
    public boolean synchronizedWeather;
    public boolean serverEnabled;
    public int serverDelay;
    public float serverRain;
    public int serverWetTicks;

    public void advance(float rain, int delayTicks) {
        wetTicks = rain <= 0 ? 0 : Math.min(wetTicks + 1, delayTicks);
    }
}
