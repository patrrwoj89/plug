package com.polishmediahub.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.polishmediahub.app.BuildConfig
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

private const val KEY_ALIAS = "plug_settings_master_key"
private const val AES_GCM = "AES/GCM/NoPadding"
private const val GCM_TAG_LENGTH = 128
private const val GCM_IV_LENGTH = 12

/**
 * Hardware-backed encryption helper for sensitive DataStore values.
 *
 * Generates a 256-bit AES key inside the Android Keystore and uses AES/GCM/NoPadding
 * with a random 12-byte IV for each encryption. The IV is prepended to the ciphertext
 * and the whole payload is Base64-encoded before being stored as a plain string in DataStore.
 */
@Singleton
class EncryptedSettingsManager @Inject constructor(
    private val context: Context
) {

    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    init {
        generateKeyIfNeeded()
    }

    fun encrypt(plaintext: String): String? = try {
        val cipher = Cipher.getInstance(AES_GCM).apply {
            init(Cipher.ENCRYPT_MODE, getKey())
        }
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
        Base64.encodeToString(combined, Base64.NO_WRAP or Base64.NO_PADDING)
    } catch (e: Exception) {
        if (BuildConfig.DEBUG) Log.w("EncryptedSettingsManager", "Encryption failed: ${e.message}", e)
        null
    }

    fun decrypt(ciphertext: String): String? = try {
        val combined = Base64.decode(ciphertext, Base64.NO_WRAP or Base64.NO_PADDING)
        if (combined.size < GCM_IV_LENGTH) return null
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val encrypted = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
        val cipher = Cipher.getInstance(AES_GCM).apply {
            init(Cipher.DECRYPT_MODE, getKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        }
        String(cipher.doFinal(encrypted), Charsets.UTF_8)
    } catch (e: Exception) {
        if (BuildConfig.DEBUG) Log.w("EncryptedSettingsManager", "Decryption failed: ${e.message}", e)
        null
    }

    private fun getKey(): SecretKey {
        val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return entry?.secretKey ?: generateKey()
    }

    private fun generateKeyIfNeeded() {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateKey()
        }
    }

    private fun generateKey(): SecretKey {
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
        }.generateKey()
    }
}
