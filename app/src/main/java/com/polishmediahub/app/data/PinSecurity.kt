package com.polishmediahub.app.data

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * One-way, salted SHA-256 hashing for household profile PIN codes.
 *
 * PINs are never persisted in clear text: [hash] produces a `sha256:<saltHex>:<digestHex>`
 * string that is stored in the Room database and, transitively, inside the Cloudflare KV
 * ZIP backups produced by CloudProfileSyncWorker. A per-PIN random salt defeats rainbow
 * tables, and a one-way hash keeps the value portable across devices on restore (unlike a
 * device-bound Keystore ciphertext). [verify] transparently falls back to a plaintext
 * comparison for legacy profiles saved before this hardening.
 */
object PinSecurity {

    private const val PREFIX = "sha256"
    private const val SALT_BYTES = 16

    fun isHashed(value: String?): Boolean =
        value != null && value.startsWith("$PREFIX:") && value.count { it == ':' } == 2

    fun hash(pin: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        return "$PREFIX:${salt.toHex()}:${digest(pin, salt).toHex()}"
    }

    fun verify(pin: String, stored: String?): Boolean {
        if (stored.isNullOrBlank()) return false
        if (!isHashed(stored)) return pin == stored
        val parts = stored.split(":")
        val salt = parts[1].fromHex() ?: return false
        val expected = parts[2].fromHex() ?: return false
        return MessageDigest.isEqual(digest(pin, salt), expected)
    }

    private fun digest(pin: String, salt: ByteArray): ByteArray =
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
