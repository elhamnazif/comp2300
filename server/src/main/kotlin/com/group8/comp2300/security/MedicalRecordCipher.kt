package com.group8.comp2300.security

import com.group8.comp2300.config.Environment
import com.group8.comp2300.config.JwtConfig
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

interface MedicalRecordCipher {
    fun encrypt(plaintext: ByteArray): ByteArray
    fun decrypt(ciphertext: ByteArray): ByteArray
}

object MedicalRecordEncryptionConfig {
    private const val KEY_ENV_NAME = "MEDICAL_RECORD_ENCRYPTION_KEY"
    private const val KEY_SIZE_BYTES = 32

    val keyBytes: ByteArray by lazy {
        val configuredKey = Environment.value(KEY_ENV_NAME)
        when {
            configuredKey != null -> decodeConfiguredKey(configuredKey)

            Environment.isDevelopment -> deriveDevelopmentKey()

            else -> throw IllegalStateException(
                "$KEY_ENV_NAME must be set to a base64-encoded 256-bit key in production",
            )
        }
    }

    private fun decodeConfiguredKey(base64Key: String): ByteArray {
        val decoded = try {
            Base64.getDecoder().decode(base64Key)
        } catch (_: IllegalArgumentException) {
            throw IllegalStateException("$KEY_ENV_NAME must be valid base64")
        }

        require(decoded.size == KEY_SIZE_BYTES) {
            "$KEY_ENV_NAME must decode to exactly $KEY_SIZE_BYTES bytes"
        }

        return decoded
    }

    private fun deriveDevelopmentKey(): ByteArray = MessageDigest.getInstance("SHA-256")
        .digest("medical-records:${JwtConfig.secret}".toByteArray())
}

class AesGcmMedicalRecordCipher(keyBytes: ByteArray) : MedicalRecordCipher {
    private val secretKey = SecretKeySpec(keyBytes.copyOf(), KEY_ALGORITHM)
    private val secureRandom = SecureRandom()

    init {
        require(keyBytes.size == KEY_SIZE_BYTES) { "AES-256 key must be $KEY_SIZE_BYTES bytes" }
    }

    override fun encrypt(plaintext: ByteArray): ByteArray {
        val iv = ByteArray(IV_SIZE_BYTES).also(secureRandom::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_SIZE_BITS, iv))
        val encryptedPayload = cipher.doFinal(plaintext)

        return HEADER + iv + encryptedPayload
    }

    override fun decrypt(ciphertext: ByteArray): ByteArray {
        require(ciphertext.size > HEADER.size + IV_SIZE_BYTES) { "Encrypted payload is truncated" }
        require(ciphertext.copyOfRange(0, HEADER.size).contentEquals(HEADER)) {
            "Unsupported encrypted medical record format"
        }

        val ivStart = HEADER.size
        val payloadStart = ivStart + IV_SIZE_BYTES
        val iv = ciphertext.copyOfRange(ivStart, payloadStart)
        val encryptedPayload = ciphertext.copyOfRange(payloadStart, ciphertext.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_SIZE_BITS, iv))
        return cipher.doFinal(encryptedPayload)
    }

    private companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_ALGORITHM = "AES"
        private const val KEY_SIZE_BYTES = 32
        private const val IV_SIZE_BYTES = 12
        private const val TAG_SIZE_BITS = 128
        private val HEADER = byteArrayOf('M'.code.toByte(), 'R'.code.toByte(), 1)
    }
}
