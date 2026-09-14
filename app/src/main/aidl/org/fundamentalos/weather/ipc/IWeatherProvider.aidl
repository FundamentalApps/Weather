package org.fundamentalos.weather.ipc;

import org.fundamentalos.weather.ipc.IWeatherCallback;

interface IWeatherProvider {
    // May return null before the first refresh has produced a value.
    android.os.Bundle getCurrent();

    // Registering pushes the current value back once immediately (if there is one).
    void registerCallback(IWeatherCallback cb);

    void unregisterCallback(IWeatherCallback cb);
}
