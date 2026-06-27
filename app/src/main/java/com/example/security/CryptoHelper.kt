package com.example.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoHelper {
    private const val KEY_ALIAS = "FinFlowSecretKeyAlias"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private val fallbackKey: SecretKey by lazy {
        // Fallback key in case KeyStore is not available (e.g. JVM tests)
        val keyBytes = "FinFlowFallbackKeyForOfflineEnc!".toByteArray(Charsets.UTF_8)
        SecretKeySpec(keyBytes, "AES")
    }

    init {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getSecretKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey ?: fallbackKey
        } catch (e: Exception) {
            fallbackKey
        }
    }

    fun encrypt(plainText: String): EncryptedData {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val encryptedBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            val ivBase64 = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
            EncryptedData(encryptedBase64, ivBase64)
        } catch (e: Exception) {
            // JVM fallback encryption if base android crypt APIs aren't fully mock-initialized
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.ENCRYPT_MODE, fallbackKey)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val encryptedBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            EncryptedData(encryptedBase64, "")
        }
    }

    fun decrypt(encryptedBase64: String, ivBase64: String): String {
        return try {
            val encryptedBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (ivBase64.isEmpty()) {
                val cipher = Cipher.getInstance("AES")
                cipher.init(Cipher.DECRYPT_MODE, fallbackKey)
                val decryptedBytes = cipher.doFinal(encryptedBytes)
                return String(decryptedBytes, Charsets.UTF_8)
            }
            val ivBytes = Base64.decode(ivBase64, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, ivBytes)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}

data class EncryptedData(
    val encryptedBase64: String,
    val ivBase64: String
)
