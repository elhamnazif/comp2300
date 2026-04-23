package com.group8.comp2300.util

import kotlinx.cinterop.*
import platform.CoreCrypto.CCKeyDerivationPBKDF
import platform.CoreCrypto.kCCPBKDF2
import platform.CoreCrypto.kCCPRFHmacAlgSHA256

@OptIn(ExperimentalForeignApi::class)
internal actual fun pbkdf2HmacSha256(password: ByteArray, salt: ByteArray, iterations: Int): ByteArray {
    val derivedKey = ByteArray(32)
    val passwordString = password.decodeToString()
    memScoped {
        val saltPtr = salt.usePinned { it.addressOf(0).reinterpret<UByteVarOf<UByte>>() }
        val dkPtr = derivedKey.usePinned { it.addressOf(0).reinterpret<UByteVarOf<UByte>>() }
        CCKeyDerivationPBKDF(
            kCCPBKDF2,
            passwordString,
            password.size.toULong(),
            saltPtr,
            salt.size.toULong(),
            kCCPRFHmacAlgSHA256,
            iterations.toUInt(),
            dkPtr,
            derivedKey.size.toULong(),
        )
    }
    return derivedKey
}
