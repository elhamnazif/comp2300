package com.group8.comp2300.config

object JwtConfig {
    val isDevelopment =
        System.getenv("ENV")?.lowercase() == "development" ||
            System.getProperty("ENV")?.lowercase() == "development" ||
            // Allow test mode by checking if we're running in a test context
            System.getProperty("ktor.testing") != null

    val devAuthBypass: Boolean =
        isDevelopment &&
            (System.getenv("DEV_AUTH_BYPASS")?.lowercase() != "false")

    val realm: String = System.getenv("JWT_REALM") ?: "comp2300"

    val secret: String = System.getenv("JWT_SECRET") ?: if (isDevelopment) {
        "dev-secret-key-change-in-production"
    } else {
        // Default to development mode if no environment is set
        "dev-secret-key-change-in-production"
    }

    val issuer: String = System.getenv("JWT_ISSUER") ?: "http://0.0.0.0:8080"
    val audience: String = System.getenv("JWT_AUDIENCE") ?: "http://0.0.0.0:8080"

    const val DEV_USER_ID = "dev-user-001"
}
