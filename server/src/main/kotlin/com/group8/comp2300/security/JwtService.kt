package com.group8.comp2300.security

import com.auth0.jwt.JWTVerifier
import kotlin.time.Duration

interface JwtService {
    fun generateAccessToken(userId: String): String
    fun generateRefreshToken(userId: String): String
    val verifier: JWTVerifier
    val refreshVerifier: JWTVerifier
    val accessTokenExpiration: Duration
    val refreshTokenExpiration: Duration
}
