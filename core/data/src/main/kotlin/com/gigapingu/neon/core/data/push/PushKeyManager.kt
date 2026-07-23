package com.gigapingu.neon.core.data.push

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The device's Web Push subscriber keys. [p256dhBase64]/[authBase64] are the
 * base64url (no padding) forms sent to Mastodon; the raw bytes + [privateKey]
 * are used locally to decrypt incoming pushes ([WebPushDecryptor]).
 */
class PushKeys(
    val p256dhBase64: String,
    val authBase64: String,
    val privateKey: ECPrivateKey,
    /** Uncompressed EC point: `0x04 || X(32) || Y(32)`, 65 bytes. */
    val publicKeyUncompressed: ByteArray,
    /** 16 random bytes. */
    val authSecret: ByteArray,
)

/**
 * Generates and persists the on-device P-256 ECDH keypair + 16-byte auth secret
 * used for RFC 8291 Web Push. Backed by EncryptedSharedPreferences so the private
 * key never sits in plaintext at rest. These values never leave the device except
 * for the public key + auth secret, which are handed to Mastodon at subscription time.
 */
@Singleton
class PushKeyManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    /** Lazily creates the keypair on first call, then returns the stored one. */
    @Synchronized
    fun getOrCreateKeys(): PushKeys {
        loadKeys()?.let { return it }
        return generateAndStore()
    }

    /** Wipes the stored keypair (on logout). A fresh one is generated on next use. */
    @Synchronized
    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun loadKeys(): PushKeys? {
        val privB64 = prefs.getString(KEY_PRIVATE, null) ?: return null
        val pubB64 = prefs.getString(KEY_PUBLIC, null) ?: return null
        val authB64 = prefs.getString(KEY_AUTH, null) ?: return null
        return runCatching {
            val privateKey = KeyFactory.getInstance("EC")
                .generatePrivate(PKCS8EncodedKeySpec(Base64.decode(privB64, Base64.DEFAULT))) as ECPrivateKey
            val publicUncompressed = decodeUrl(pubB64)
            PushKeys(
                p256dhBase64 = pubB64,
                authBase64 = authB64,
                privateKey = privateKey,
                publicKeyUncompressed = publicUncompressed,
                authSecret = decodeUrl(authB64),
            )
        }.getOrNull()
    }

    private fun generateAndStore(): PushKeys {
        val generator = KeyPairGenerator.getInstance("EC").apply {
            initialize(ECGenParameterSpec("secp256r1"))
        }
        val keyPair = generator.generateKeyPair()
        val privateKey = keyPair.private as ECPrivateKey
        val publicKey = keyPair.public as ECPublicKey
        val uncompressed = publicKey.uncompressedPoint()

        val authSecret = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val pubB64 = encodeUrl(uncompressed)
        val authB64 = encodeUrl(authSecret)

        prefs.edit()
            .putString(KEY_PRIVATE, Base64.encodeToString(privateKey.encoded, Base64.DEFAULT))
            .putString(KEY_PUBLIC, pubB64)
            .putString(KEY_AUTH, authB64)
            .apply()

        return PushKeys(
            p256dhBase64 = pubB64,
            authBase64 = authB64,
            privateKey = privateKey,
            publicKeyUncompressed = uncompressed,
            authSecret = authSecret,
        )
    }

    private companion object {
        const val PREFS_NAME = "neon_push_keys"
        const val KEY_PRIVATE = "private_pkcs8"
        const val KEY_PUBLIC = "public_point"
        const val KEY_AUTH = "auth_secret"

        val URL_FLAGS = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP

        fun encodeUrl(bytes: ByteArray): String = Base64.encodeToString(bytes, URL_FLAGS)
        fun decodeUrl(value: String): ByteArray = Base64.decode(value, URL_FLAGS)

        /** Encode an EC public key as an uncompressed point (`0x04 || X || Y`), each coord fixed to 32 bytes. */
        fun ECPublicKey.uncompressedPoint(): ByteArray {
            val x = w.affineX.toFixed32()
            val y = w.affineY.toFixed32()
            return ByteArray(65).apply {
                this[0] = 0x04
                x.copyInto(this, 1)
                y.copyInto(this, 33)
            }
        }

        /** BigInteger -> exactly 32 bytes, left-padded / sign-byte-trimmed. */
        fun BigInteger.toFixed32(): ByteArray {
            val raw = toByteArray()
            return when {
                raw.size == 32 -> raw
                raw.size == 33 && raw[0].toInt() == 0 -> raw.copyOfRange(1, 33)
                raw.size < 32 -> ByteArray(32).also { raw.copyInto(it, 32 - raw.size) }
                else -> raw.copyOfRange(raw.size - 32, raw.size)
            }
        }
    }
}
