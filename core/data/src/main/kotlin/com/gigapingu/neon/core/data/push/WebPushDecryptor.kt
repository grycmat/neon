package com.gigapingu.neon.core.data.push

import android.util.Base64
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decrypts a Mastodon Web Push payload delivered (still encrypted) via FCM.
 *
 * Supports both the modern `aes128gcm` scheme (RFC 8188 + RFC 8291, used when the
 * subscription is registered with `standard=true` against Mastodon >= 4.4) and the
 * legacy `aesgcm` draft scheme (older instances), using the on-device keypair from
 * [PushKeyManager]. Pure crypto — no state, no Firebase, no network.
 */
@Singleton
class WebPushDecryptor @Inject constructor() {

    /**
     * @param bodyBase64 relay's `data.body` — standard base64 of the raw encrypted body.
     * @param contentEncoding relay's `data.contentEncoding` (`aes128gcm` or `aesgcm`).
     * @param encryption relay's `data.encryption` — legacy `Encryption` header (aesgcm only).
     * @param cryptoKey relay's `data.cryptoKey` — legacy `Crypto-Key` header (aesgcm only).
     * @return decrypted UTF-8 JSON bytes.
     */
    fun decrypt(
        bodyBase64: String,
        contentEncoding: String,
        encryption: String?,
        cryptoKey: String?,
        keys: PushKeys,
    ): ByteArray {
        val body = Base64.decode(bodyBase64, Base64.DEFAULT)
        return when (contentEncoding.trim()) {
            "aes128gcm" -> decryptAes128Gcm(body, keys)
            "aesgcm" -> decryptLegacyAesGcm(body, encryption.orEmpty(), cryptoKey.orEmpty(), keys)
            else -> error("Unsupported Content-Encoding: $contentEncoding")
        }
    }

    // --- aes128gcm (RFC 8188 header + RFC 8291 key derivation) --------------------------

    private fun decryptAes128Gcm(body: ByteArray, keys: PushKeys): ByteArray {
        val buf = ByteBuffer.wrap(body)
        val salt = ByteArray(16).also { buf.get(it) }
        buf.int // record size — single record, ignored
        val idLen = buf.get().toInt() and 0xff
        val asPublic = ByteArray(idLen).also { buf.get(it) } // server public key, uncompressed
        val ciphertext = ByteArray(buf.remaining()).also { buf.get(it) }

        val uaPublic = keys.publicKeyUncompressed
        val sharedSecret = ecdh(keys.privateKey, importPublicKey(asPublic, keys.privateKey.params))

        // RFC 8291 §3.4: IKM = HKDF(auth, ecdh, "WebPush: info"||0x00||ua||as, 32)
        val prkKey = hmacSha256(keys.authSecret, sharedSecret)
        val keyInfo = concat(ASCII("WebPush: info"), byteArrayOf(0), uaPublic, asPublic)
        val ikm = hkdfExpand(prkKey, keyInfo, 32)

        // RFC 8188 §2.2
        val prk = hmacSha256(salt, ikm)
        val cek = hkdfExpand(prk, infoFor("aes128gcm"), 16)
        val nonce = hkdfExpand(prk, infoFor("nonce"), 12)

        val plaintext = aesGcmDecrypt(cek, nonce, ciphertext)
        return stripAes128GcmPadding(plaintext)
    }

    /** RFC 8188 record padding: `data || 0x02 || 0x00*` (single, last record). */
    private fun stripAes128GcmPadding(plaintext: ByteArray): ByteArray {
        var end = plaintext.size - 1
        while (end >= 0 && plaintext[end].toInt() == 0) end--
        // plaintext[end] is now the delimiter (0x02 last / 0x01 non-last); drop it.
        return if (end <= 0) ByteArray(0) else plaintext.copyOfRange(0, end)
    }

    // --- legacy aesgcm (draft-ietf-webpush-encryption-04) -------------------------------

    private fun decryptLegacyAesGcm(
        ciphertext: ByteArray,
        encryptionHeader: String,
        cryptoKeyHeader: String,
        keys: PushKeys,
    ): ByteArray {
        val salt = decodeUrl(param(encryptionHeader, "salt"))
        val asPublic = decodeUrl(param(cryptoKeyHeader, "dh"))
        val uaPublic = keys.publicKeyUncompressed

        val sharedSecret = ecdh(keys.privateKey, importPublicKey(asPublic, keys.privateKey.params))

        val ikm = hkdfExpand(
            hmacSha256(keys.authSecret, sharedSecret),
            concat(ASCII("Content-Encoding: auth"), byteArrayOf(0)),
            32,
        )

        val context = concat(
            ASCII("P-256"), byteArrayOf(0),
            twoByteLen(uaPublic.size), uaPublic,
            twoByteLen(asPublic.size), asPublic,
        )
        val prk = hmacSha256(salt, ikm)
        val cek = hkdfExpand(prk, concat(ASCII("Content-Encoding: aesgcm"), byteArrayOf(0), context), 16)
        val nonce = hkdfExpand(prk, concat(ASCII("Content-Encoding: nonce"), byteArrayOf(0), context), 12)

        val plaintext = aesGcmDecrypt(cek, nonce, ciphertext)
        return stripLegacyPadding(plaintext)
    }

    /** Legacy record padding: `padLen(2 BE) || 0x00*padLen || data`. */
    private fun stripLegacyPadding(plaintext: ByteArray): ByteArray {
        if (plaintext.size < 2) return ByteArray(0)
        val padLen = ((plaintext[0].toInt() and 0xff) shl 8) or (plaintext[1].toInt() and 0xff)
        val start = 2 + padLen
        return if (start > plaintext.size) ByteArray(0) else plaintext.copyOfRange(start, plaintext.size)
    }

    // --- primitives ---------------------------------------------------------------------

    private fun ecdh(privateKey: ECPrivateKey, publicKey: ECPublicKey): ByteArray =
        KeyAgreement.getInstance("ECDH").apply {
            init(privateKey)
            doPhase(publicKey, true)
        }.generateSecret()

    private fun importPublicKey(uncompressed: ByteArray, params: ECParameterSpec): ECPublicKey {
        val x = BigInteger(1, uncompressed.copyOfRange(1, 33))
        val y = BigInteger(1, uncompressed.copyOfRange(33, 65))
        return KeyFactory.getInstance("EC")
            .generatePublic(ECPublicKeySpec(ECPoint(x, y), params)) as ECPublicKey
    }

    private fun aesGcmDecrypt(key: ByteArray, nonce: ByteArray, ciphertext: ByteArray): ByteArray =
        Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        }.doFinal(ciphertext)

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray =
        Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(key, "HmacSHA256")) }.doFinal(data)

    /** HKDF-Expand for a single output block (all our outputs are <= 32 bytes). */
    private fun hkdfExpand(prk: ByteArray, info: ByteArray, length: Int): ByteArray =
        hmacSha256(prk, concat(info, byteArrayOf(0x01))).copyOf(length)

    private fun infoFor(name: String): ByteArray = concat(ASCII("Content-Encoding: $name"), byteArrayOf(0))

    private fun concat(vararg arrays: ByteArray): ByteArray {
        val out = ByteArray(arrays.sumOf { it.size })
        var pos = 0
        for (a in arrays) {
            a.copyInto(out, pos)
            pos += a.size
        }
        return out
    }

    private fun twoByteLen(value: Int): ByteArray =
        byteArrayOf(((value shr 8) and 0xff).toByte(), (value and 0xff).toByte())

    private fun ASCII(s: String): ByteArray = s.toByteArray(Charsets.US_ASCII)

    private fun decodeUrl(value: String): ByteArray =
        Base64.decode(value.trim(), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

    /** Pulls `name=value` out of a `;`-separated header string (e.g. `dh=...;p256ecdsa=...`). */
    private fun param(header: String, name: String): String {
        for (part in header.split(';')) {
            val trimmed = part.trim()
            if (trimmed.startsWith("$name=")) return trimmed.substring(name.length + 1)
        }
        // Header may be the bare value with no `name=` prefix.
        return header.trim()
    }
}
