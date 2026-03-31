package com.group8.comp2300.util

import io.ktor.utils.io.core.toByteArray
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import net.sergeych.sprintf.format
import platform.CoreCrypto.CC_SHA256

@OptIn(ExperimentalForeignApi::class)
actual fun sha256(input: String): String {
    val inputBytes = input.toByteArray()
    val digest = UByteArray(32)
    CC_SHA256(inputBytes.refTo(0), inputBytes.size.toUInt(), digest.refTo(0))
    return digest.joinToString("") { "%02x".format(it) }
}
