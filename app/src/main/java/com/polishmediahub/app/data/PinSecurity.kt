package com.polishmediahub.app.data

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * One-way, salted PBKDF2 hashing for household profile PIN codes.
 *
 * PINs are never persisted in clear text: [hash] produces a
 * `pbkdf2:<iterations>:<saltHex>:<digestHex>` string that is stored in the Room database and,
 * transitively, inside the Cloudflare KV ZIP backups produced by CloudProfileSyncWorker.
 * A per-PIN random salt defeats precomputed rainbow tables, and a deliberately slow,
 * high-iteration PBKDF2-HMAC-SHA256 stretch makes offline brute force of the tiny
 * 4-digit PIN keyspace expensive even when an attacker obtains the hash from a backup.
 * The hash stays portable across devices on restore (unlike a device-bound Keystore
 * ciphertext). [verify] transparently falls back to the legacy single-pass salted SHA-256
 * format and to a plaintext comparison for profiles saved before this hardening.
 */
object PinSecurity {

    private const val PBKDF2_PREFIX = "pbkdf2"
    private const val LEGACY_SHA256_PREFIX = "sha256"
    private const val SALT_BYTES = 16
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256

    fun isHashed(value: String?): Boolean =
        value != null &&
            (value.startsWith("$PBKDF2_PREFIX:") || value.startsWith("$LEGACY_SHA256_PREFIX:"))

    /**
     * True when [value] holds a non-empty PIN that is not yet in the current PBKDF2 format
     * (i.e. a legacy plaintext or legacy salted-SHA-256 value) and should be re-hashed after
     * the next successful verification.
     */
    fun needsUpgrade(value: String?): Boolean =
        !value.isNullOrBlank() && !value.startsWith("$PBKDF2_PREFIX:")

    fun hash(pin: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        return "$PBKDF2_PREFIX:$ITERATIONS:${salt.toHex()}:${pbkdf2(pin, salt, ITERATIONS).toHex()}"
    }

    fun verify(pin: String, stored: String?): Boolean {
        if (stored.isNullOrBlank()) return false
        val parts = stored.split(":")
        return when {
            stored.startsWith("$PBKDF2_PREFIX:") && parts.size == 4 -> {
                val iterations = parts[1].toIntOrNull() ?: return false
                val salt = parts[2].fromHex() ?: return false
                val expected = parts[3].fromHex() ?: return false
                MessageDigest.isEqual(pbkdf2(pin, salt, iterations), expected)
            }
            stored.startsWith("$LEGACY_SHA256_PREFIX:") && parts.size == 3 -> {
                val salt = parts[1].fromHex() ?: return false
                val expected = parts[2].fromHex() ?: return false
                MessageDigest.isEqual(legacySha256(pin, salt), expected)
            }
            else -> pin == stored
        }
    }

    private fun pbkdf2(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun legacySha256(pin: String, salt: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").apply {
            update(salt)
            update(pin.toByteArray(Charsets.UTF_8))
        }.digest()

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it) }

    private fun String.fromHex(): ByteArray? {
        if (length % 2 != 0) return null
        return runCatching {
            ByteArray(length / 2) { substring(it * 2, it * 2 + 2).toInt(16).toByte() }
        }.getOrNull()
    }
}
