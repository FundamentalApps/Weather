package org.fundamentalos.weather.api.exception

class CaulumApiClientException: Exception {
    constructor(message: String): super(message)
    constructor(cause: Throwable): super(cause)
    constructor(message: String, cause: Throwable): super(message, cause)
}