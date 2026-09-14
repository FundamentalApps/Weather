package org.fundamentalos.weather.ipc;

oneway interface IWeatherCallback {
    void onWeatherChanged(in android.os.Bundle snapshot);
}
