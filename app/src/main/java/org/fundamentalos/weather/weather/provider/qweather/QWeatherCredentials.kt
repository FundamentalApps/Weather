package org.fundamentalos.weather.weather.provider.qweather

data class QWeatherCredentials(
    val host: String,
    val projectId: String,
    val keyId: String,
    val privateKeyPem: String,
) {
    val configured: Boolean
        get() = host.isNotBlank() &&
                projectId.isNotBlank() &&
                keyId.isNotBlank() &&
                privateKeyPem.isNotBlank()
}
