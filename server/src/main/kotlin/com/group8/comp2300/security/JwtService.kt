package com.group8.comp2300.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

interface JwtService {
    fun generateAccessToken(userId: String): String
    fun generateRefreshToken(userId: String): String
    val verifier: JWTVerifier
    val refreshVerifier: JWTVerifier
    val accessTokenExpiration: Duration
    val refreshTokenExpiration: Duration
}

class JwtServiceImpl(secret: String, private val issuer: String, private val audience: String) : JwtService {

    private val accessAlgorithm = Algorithm.HMAC256(secret)
    private val refreshAlgorithm = Algorithm.HMAC256(secret)

    override val accessTokenExpiration: Duration = ACCESS_TOKEN_EXPIRATION
    override val refreshTokenExpiration: Duration = REFRESH_TOKEN_EXPIRATION

    override fun generateAccessToken(userId: String): String = JWT.create()
        .withAudience(audience)
        .withIssuer(issuer)
        .withSubject(userId)
        .withJWTId(UUID.randomUUID().toString())
        .withExpiresAt(Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION.inWholeMilliseconds))
        .withClaim("type", "access")
        .sign(accessAlgorithm)

    override fun generateRefreshToken(userId: String): String = JWT.create()
        .withIssuer(issuer)
        .withSubject(userId)
        .withJWTId(UUID.randomUUID().toString())
        .withExpiresAt(Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION.inWholeMilliseconds))
        .withClaim("type", "refresh")
        .sign(refreshAlgorithm)

    override val verifier: JWTVerifier =
        JWT.require(accessAlgorithm)
            .withAudience(audience)
            .withIssuer(issuer)
            .build()

    override val refreshVerifier: JWTVerifier =
        JWT.require(refreshAlgorithm)
            .withIssuer(issuer)
            .build()

    companion object {
        val ACCESS_TOKEN_EXPIRATION: Duration = 15.minutes
        val REFRESH_TOKEN_EXPIRATION: Duration = 30.days
    }
}
