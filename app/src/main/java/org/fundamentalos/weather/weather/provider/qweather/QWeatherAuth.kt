package org.fundamentalos.weather.weather.provider.qweather

import android.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec

class QWeatherAuth {
    @Volatile
    private var cache: Cached? = null

    fun createJwt(credentials: QWeatherCredentials): String {
        val now = System.currentTimeMillis() / 1000L
        cache?.let {
            if (it.credentialsKey == credentials.cacheKey() && it.expiresAt > now + REFRESH_MARGIN_SECONDS) {
                return it.jwt
            }
        }

        val iat = now - CLOCK_SKEW_SECONDS
        val exp = iat + TOKEN_LIFETIME_SECONDS
        val jwt = buildJwt(credentials, iat, exp)
        cache = Cached(credentials.cacheKey(), jwt, exp)
        return jwt
    }

    private fun buildJwt(credentials: QWeatherCredentials, iat: Long, exp: Long): String {
        val header = JSON.encodeToString(JwtHeader(alg = "EdDSA", kid = credentials.keyId))
        val payload = JSON.encodeToString(
            JwtPayload(sub = credentials.projectId, iat = iat, exp = exp),
        )
        val signingInput = buildString {
            append(base64Url(header.toByteArray(StandardCharsets.UTF_8)))
            append('.')
            append(base64Url(payload.toByteArray(StandardCharsets.UTF_8)))
        }
        val signature = Signature.getInstance("Ed25519").run {
            initSign(parsePrivateKey(credentials.privateKeyPem))
            update(signingInput.toByteArray(StandardCharsets.UTF_8))
            sign()
        }
        return "$signingInput.${base64Url(signature)}"
    }

    private fun parsePrivateKey(privateKeyPem: String): PrivateKey {
        val content = privateKeyPem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
        val bytes = Base64.decode(content, Base64.DEFAULT)
        val spec = PKCS8EncodedKeySpec(bytes)
        return KeyFactory.getInstance("Ed25519").generatePrivate(spec)
    }

    private fun base64Url(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun QWeatherCredentials.cacheKey(): String =
        "$projectId|$keyId|${privateKeyPem.hashCode()}"

    @Serializable
    private data class JwtHeader(val alg: String, val kid: String)

    @Serializable
    private data class JwtPayload(val sub: String, val iat: Long, val exp: Long)

    private data class Cached(val credentialsKey: String, val jwt: String, val expiresAt: Long)

    companion object {
        private const val CLOCK_SKEW_SECONDS = 30L
        private const val TOKEN_LIFETIME_SECONDS = 3600L
        private const val REFRESH_MARGIN_SECONDS = 300L
        private val JSON = Json { encodeDefaults = true }
    }
}
