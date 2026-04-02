package com.group8.comp2300.util

internal expect fun pbkdf2HmacSha256(password: ByteArray, salt: ByteArray, iterations: Int): ByteArray
