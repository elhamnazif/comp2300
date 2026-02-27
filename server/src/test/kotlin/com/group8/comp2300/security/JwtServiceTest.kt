package com.group8.comp2300.security

import com.auth0.jwt.JWT
import com.auth0.jwt.exceptions.InvalidClaimException
import kotlin.test.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

class JwtServiceTest {

    private val jwtService: JwtService = JwtServiceImpl(
        secret = "test-secret-key-for-unit-testing",
        issuer = "test-issuer",
        audience = "test-audience",
    )

    @Test
    fun `generateAccessToken creates valid JWT with correct claims`() {
        val userId = "user_123"

        val token = jwtService.generateAccessToken(userId)

        assertNotNull(token)
        assertTrue(token.isNotEmpty())

        val decoded = JWT.decode(token)
        assertEquals(userId, decoded.subject)
        assertEquals("test-issuer", decoded.issuer)
        assertEquals("test-audience", decoded.audience.first())
        assertEquals("access", decoded.getClaim("type").asString())
        assertNotNull(decoded.id)
        assertNotNull(decoded.expiresAt)
    }

    @Test
    fun `generateRefreshToken creates valid JWT with refresh type`() {
        val userId = "user_456"

        val token = jwtService.generateRefreshToken(userId)

        assertNotNull(token)
        assertTrue(token.isNotEmpty())

        val decoded = JWT.decode(token)
        assertEquals(userId, decoded.subject)
        assertEquals("test-issuer", decoded.issuer)
        assertEquals("refresh", decoded.getClaim("type").asString())
        assertNotNull(decoded.id)
        assertNotNull(decoded.expiresAt)
    }

    @Test
    fun `verifier accepts valid access token`() {
        val userId = "user_789"
        val token = jwtService.generateAccessToken(userId)

        // Should not throw
        val verifiedToken = jwtService.verifier.verify(token)

        assertEquals(userId, verifiedToken.subject)
    }

    @Test
    fun `verifier rejects token with wrong issuer`() {
        val wrongIssuerService = JwtServiceImpl(
            secret = "test-secret-key-for-unit-testing",
            issuer = "wrong-issuer",
            audience = "test-audience",
        )
        val token = wrongIssuerService.generateAccessToken("user_123")

        assertFailsWith<InvalidClaimException> {
            jwtService.verifier.verify(token)
        }
    }

    @Test
    fun `refreshVerifier accepts valid refresh token`() {
        val userId = "user_refresh"
        val token = jwtService.generateRefreshToken(userId)

        // Should not throw
        val verifiedToken = jwtService.refreshVerifier.verify(token)

        assertEquals(userId, verifiedToken.subject)
    }

    @Test
    fun `tokens contain correct user ID as subject`() {
        val userId = "unique_user_id_12345"

        val accessToken = jwtService.generateAccessToken(userId)
        val refreshToken = jwtService.generateRefreshToken(userId)

        val accessDecoded = JWT.decode(accessToken)
        val refreshDecoded = JWT.decode(refreshToken)

        assertEquals(userId, accessDecoded.subject)
        assertEquals(userId, refreshDecoded.subject)
    }

    @Test
    fun `accessTokenExpiration is 15 minutes`() {
        assertEquals(15.minutes, JwtServiceImpl.ACCESS_TOKEN_EXPIRATION)
    }

    @Test
    fun `refreshTokenExpiration is 30 days`() {
        assertEquals(30.days, JwtServiceImpl.REFRESH_TOKEN_EXPIRATION)
    }

    @Test
    fun `access and refresh tokens are different`() {
        val userId = "user_same"

        val accessToken = jwtService.generateAccessToken(userId)
        val refreshToken = jwtService.generateRefreshToken(userId)

        // Tokens should be different (different types, different expiration)
        assertNotEquals(accessToken, refreshToken)
    }

    @Test
    fun `each token has unique JWT ID`() {
        val userId = "user_jti"

        val token1 = jwtService.generateAccessToken(userId)
        val token2 = jwtService.generateAccessToken(userId)

        val decoded1 = JWT.decode(token1)
        val decoded2 = JWT.decode(token2)

        // Even for same user, each token should have unique ID
        assertNotNull(decoded1.id)
        assertNotNull(decoded2.id)
        assertTrue(decoded1.id != decoded2.id)
    }

    private inline fun <reified T : Throwable> assertFailsWith(block: () -> Unit) {
        try {
            block()
            throw AssertionError("Expected ${T::class.simpleName} but no exception was thrown")
        } catch (e: Throwable) {
            if (e !is T) {
                throw AssertionError("Expected ${T::class.simpleName} but got ${e::class.simpleName}")
            }
        }
    }
}
