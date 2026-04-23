package com.group8.comp2300.util

import kotlin.random.Random

const val PinHashIterations = 100_000
private const val SaltSize = 16

data class PinHashResult(val hash: String, val salt: String, val iterations: Int)

/** Hash a PIN with PBKDF2-HMAC-SHA256 and a per-entry random salt. */
fun hashPinSecure(pin: String, salt: ByteArray = Random.nextBytes(SaltSize)): PinHashResult {
    val hashBytes = pbkdf2HmacSha256(pin.encodeToByteArray(), salt, PinHashIterations)
    return PinHashResult(
        hash = hashBytes.toHexString(),
        salt = salt.toHexString(),
        iterations = PinHashIterations,
    )
}

/** Re-derive and compare using constant-time equality. */
fun verifyPinHash(pin: String, storedHash: String, salt: String, iterations: Int): Boolean {
    if (iterations <= 0 || !salt.isHexEncoded() || !storedHash.isHexEncoded()) {
        return false
    }

    val saltBytes = salt.hexToByteArray() ?: return false
    val derived = pbkdf2HmacSha256(pin.encodeToByteArray(), saltBytes, iterations)
    return constantTimeEquals(derived.toHexString(), storedHash)
}

private fun ByteArray.toHexString(): String = joinToString("") {
    ((it.toInt() and 0xFF) shr 4).toString(16) + (it.toInt() and 0x0F).toString(16)
}

private fun String.hexToByteArray(): ByteArray? {
    if (!isHexEncoded()) return null
    val len = length / 2
    return ByteArray(len) { i ->
        ((this[2 * i].digitToInt(16) shl 4) + this[2 * i + 1].digitToInt(16)).toByte()
    }
}

private fun String.isHexEncoded(): Boolean =
    isNotBlank() && length % 2 == 0 && all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
