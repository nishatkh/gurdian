package com.example.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object KeystoreCryptoManager {
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "guardian_ai_aes_gcm_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private fun getOrCreateKey(): SecretKey? {
        return try {
            val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            val existing = ks.getKey(KEY_ALIAS, null) as? SecretKey
            if (existing != null) return existing

            val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGen.init(spec)
            keyGen.generateKey()
        } catch (_: Exception) {
            null
        }
    }

    fun encrypt(plainText: String): String {
        if (plainText.isBlank()) return ""
        val key = getOrCreateKey() ?: return "PLAIN:${Base64.encodeToString(plainText.toByteArray(), Base64.NO_WRAP)}"
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(1 + iv.size + encrypted.size)
            combined[0] = iv.size.toByte()
            System.arraycopy(iv, 0, combined, 1, iv.size)
            System.arraycopy(encrypted, 0, combined, 1 + iv.size, encrypted.size)
            "KS:${Base64.encodeToString(combined, Base64.NO_WRAP)}"
        } catch (_: Exception) {
            "PLAIN:${Base64.encodeToString(plainText.toByteArray(), Base64.NO_WRAP)}"
        }
    }

    fun decrypt(cipherText: String): String {
        if (cipherText.isBlank()) return ""
        if (cipherText.startsWith("PLAIN:")) {
            return String(Base64.decode(cipherText.removePrefix("PLAIN:"), Base64.NO_WRAP), Charsets.UTF_8)
        }
        if (!cipherText.startsWith("KS:")) return cipherText
        val key = getOrCreateKey() ?: return ""
        return try {
            val raw = Base64.decode(cipherText.removePrefix("KS:"), Base64.NO_WRAP)
            val ivLen = raw[0].toInt()
            val iv = raw.copyOfRange(1, 1 + ivLen)
            val encrypted = raw.copyOfRange(1 + ivLen, raw.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    fun maskImei(imei: String?): String {
        val digits = imei.orEmpty().filter { it.isDigit() }
        if (digits.length < 4) return "Not saved"
        return "•••• •••• •••• ${digits.takeLast(4)}"
    }

    fun isValidLuhnImei(imei: String): Boolean {
        val digits = imei.filter { it.isDigit() }
        if (digits.length != 15) return false
        var sum = 0
        for (i in digits.indices) {
            var d = digits[i] - '0'
            if (i % 2 == 1) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
        }
        return sum % 10 == 0
    }
}
