package com.example.comunamadsistemacitas.data.remote

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Guarda el JWT cifrado con una clave AES-GCM no exportable del Android Keystore. */
class MarketplaceSessionStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "marketplace_secure_session", Context.MODE_PRIVATE
    )

    fun guardarToken(token: String) {
        require(token.isNotBlank()) { "El token de sesión está vacío." }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, obtenerOCrearClave())
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        val stored = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        check(preferences.edit().putString(KEY_TOKEN, stored).commit()) {
            "No se pudo guardar la sesión cifrada."
        }
    }

    fun obtenerToken(): String? {
        val stored = preferences.getString(KEY_TOKEN, null) ?: return null
        return try {
            val separator = stored.indexOf(':')
            require(separator > 0 && separator < stored.length - 1)
            val iv = Base64.decode(stored.substring(0, separator), Base64.NO_WRAP)
            val encrypted = Base64.decode(stored.substring(separator + 1), Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, obtenerOCrearClave(), GCMParameterSpec(128, iv))
            cipher.doFinal(encrypted).toString(Charsets.UTF_8).takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            cerrarSesion()
            null
        }
    }

    fun cerrarSesion() {
        preferences.edit().remove(KEY_TOKEN).apply()
    }

    private fun obtenerOCrearClave(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private companion object {
        const val KEY_ALIAS = "unamad_marketplace_session_aes"
        const val KEY_TOKEN = "encrypted_access_token"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
