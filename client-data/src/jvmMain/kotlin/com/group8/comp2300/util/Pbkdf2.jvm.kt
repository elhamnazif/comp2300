package com.group8.comp2300.util

import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

internal actual fun pbkdf2HmacSha256(password: ByteArray, salt: ByteArray, iterations: Int): ByteArray {
    val chars = CharArray(password.size) { i -> password[i].toInt().toChar() }
    val spec = PBEKeySpec(chars, salt, iterations, 256)
    chars.fill('\u0000')
    return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
}
