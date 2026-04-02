package com.group8.comp2300.config

object JwtConfig {
    val realm: String = System.getenv("JWT_REALM") ?: "comp2300"

    val secret: String = System.getenv("JWT_SECRET")
        ?: if (Environment.isDevelopment) {
            "dev-secret-key-change-in-production"
        } else {
            throw IllegalStateException("JWT_SECRET must be set in production")
        }

    val issuer: String = System.getenv("JWT_ISSUER") ?: "http://0.0.0.0:8080"
    val audience: String = System.getenv("JWT_AUDIENCE") ?: "http://0.0.0.0:8080"
}
