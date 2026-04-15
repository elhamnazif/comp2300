package com.group8.comp2300.data.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JwtExpiryTest {
    @Test
    fun extractsExpirationFromJwtPayload() {
        val token = buildToken("""{"exp":1710000000}""")

        assertEquals(1_710_000_000_000, extractJwtExpiration(token))
    }

    @Test
    fun returnsNullForMalformedToken() {
        assertNull(extractJwtExpiration("invalid"))
    }

    private fun buildToken(payloadJson: String): String {
        val header = encode("""{"alg":"none","typ":"JWT"}""")
        val payload = encode(payloadJson)
        return "$header.$payload."
    }

    private fun encode(value: String): String =
        java.util.Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.toByteArray())
}
