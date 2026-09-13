package org.fundamentalos.weather.weather.provider.qweather

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class QWeatherCredentialStore(context: Context) {
    private val preferences = context.getSharedPreferences("qweather_credentials", Context.MODE_PRIVATE)

    fun getCredentials(): QWeatherCredentials? {
        val host = preferences.getString(KEY_HOST, null) ?: return null
        val projectId = preferences.getString(KEY_PROJECT_ID, null) ?: return null
        val keyId = preferences.getString(KEY_KEY_ID, null) ?: return null
        val encryptedPrivateKey = preferences.getString(KEY_PRIVATE_KEY, null) ?: return null
        val privateKeyPem = decrypt(encryptedPrivateKey)
        return QWeatherCredentials(
            host = host,
            projectId = projectId,
            keyId = keyId,
            privateKeyPem = privateKeyPem,
        )
    }

    fun save(credentials: QWeatherCredentials) {
        require(credentials.configured) { "QWeather credentials are incomplete" }
        val normalizedPrivateKey = normalizePrivateKeyInput(credentials.privateKeyPem)
        preferences.edit()
            .putString(KEY_HOST, credentials.host.trim())
            .putString(KEY_PROJECT_ID, credentials.projectId.trim())
            .putString(KEY_KEY_ID, credentials.keyId.trim())
            .putString(KEY_PRIVATE_KEY, encrypt(normalizedPrivateKey))
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    fun hasCredentials(): Boolean = getCredentials()?.configured == true

    fun fingerprint(): String? {
        val pem = getCredentials()?.privateKeyPem ?: return null
        val digest = MessageDigest.getInstance("SHA-256").digest(pem.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString(":") { "%02x".format(it) }
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val content = Base64.encodeToString(encrypted, Base64.NO_WRAP)
        return "$iv:$content"
    }

    private fun decrypt(value: String): String {
        val parts = value.split(":")
        require(parts.size == 2) { "Invalid encrypted QWeather private key" }
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    companion object {
        private const val KEY_HOST = "host"
        private const val KEY_PROJECT_ID = "project_id"
        private const val KEY_KEY_ID = "key_id"
        private const val KEY_PRIVATE_KEY = "private_key"
        private const val KEY_ALIAS = "weather_qweather_private_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
    }
}

fun normalizePrivateKeyInput(input: String): String {
    val trimmed = input.trim()
    if (trimmed.contains("BEGIN") && trimmed.contains("PRIVATE KEY")) {
        return trimmed
    }

    val decoded = String(Base64.decode(trimmed, Base64.DEFAULT), StandardCharsets.UTF_8).trim()
    require(decoded.contains("BEGIN") && decoded.contains("PRIVATE KEY")) {
        "Private key must be PEM text or base64-encoded PEM text"
    }
    return decoded
}
