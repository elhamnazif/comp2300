package com.group8.comp2300.config

object Environment {
    val isDevelopment: Boolean =
        System.getenv("ENV")?.lowercase() == "development" ||
            System.getProperty("ENV")?.lowercase() == "development" ||
            System.getProperty("ktor.testing") != null

    /** When true, authenticated routes accept unauthenticated requests (dev convenience). */
    val devAuthBypass: Boolean =
        isDevelopment && System.getenv("DEV_AUTH_BYPASS")?.lowercase() != "false"

    const val DEV_USER_ID = "dev-user-001"
}
