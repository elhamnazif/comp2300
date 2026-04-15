package com.group8.comp2300.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HashUtilTest {
    @Test
    fun verifyPinHashMatchesCurrentFormat() {
        val result = hashPinSecure("1234")

        assertTrue(
            verifyPinHash(
                pin = "1234",
                storedHash = result.hash,
                salt = result.salt,
                iterations = result.iterations,
                version = result.version,
            ),
        )
    }

    @Test
    fun verifyPinHashAcceptsLegacyPlaintextRows() {
        assertTrue(
            verifyPinHash(
                pin = "1234",
                storedHash = "1234",
                salt = "",
                iterations = 0,
                version = 1,
            ),
        )
    }

    @Test
    fun verifyPinHashRejectsMalformedCurrentRows() {
        assertFalse(
            verifyPinHash(
                pin = "1234",
                storedHash = "abcd",
                salt = "legacy",
                iterations = 0,
                version = CurrentPinHashVersion,
            ),
        )
    }
}
