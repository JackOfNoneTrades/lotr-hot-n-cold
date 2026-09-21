package org.fentanylsolutions.hotncold.compat;

/** Access to weather-cycle values, before the precipitation delay is applied. */
public interface WeatherRainAccess {

    float hotncold$rawRain(float partialTicks);

    float hotncold$rawThunder(float partialTicks);

    WeatherRainState hotncold$rainState();
}
