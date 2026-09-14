package org.fundamentalos.weather.ipc;

import org.fundamentalos.weather.ipc.WeatherSnapshot;

oneway interface IWeatherCallback {
    void onWeatherChanged(in WeatherSnapshot snapshot);
}
