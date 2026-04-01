package com.group8.comp2300.util

import kotlin.random.Random

private const val Pbkdf2Iterations = 100_000
private const val SaltSize = 16

data class PinHashResult(val hash: String, val salt: String, val iterations: Int, val version: Int = 2)

/** Hash a PIN with PBKDF2-HMAC-SHA256 and a per-entry random salt. */
fun hashPinSecure(pin: String, salt: ByteArray = Random.nextBytes(SaltSize)): PinHashResult {
    val hashBytes = pbkdf2HmacSha256(pin.encodeToByteArray(), salt, Pbkdf2Iterations)
    return PinHashResult(
        hash = hashBytes.toHexString(),
        salt = salt.toHexString(),
        iterations = Pbkdf2Iterations,
    )
}

/** Re-derive and compare using constant-time equality. */
fun verifyPinHash(pin: String, storedHash: String, salt: String, iterations: Int): Boolean {
    val saltBytes = salt.hexToByteArray()
    val derived = pbkdf2HmacSha256(pin.encodeToByteArray(), saltBytes, iterations)
    return constantTimeEquals(derived.toHexString(), storedHash)
}

private fun ByteArray.toHexString(): String = joinToString("") {
    ((it.toInt() and 0xFF) shr 4).toString(16) + (it.toInt() and 0x0F).toString(16)
}

private fun String.hexToByteArray(): ByteArray {
    val len = length / 2
    return ByteArray(len) { i ->
        ((this[2 * i].digitToInt(16) shl 4) + this[2 * i + 1].digitToInt(16)).toByte()
    }
}
