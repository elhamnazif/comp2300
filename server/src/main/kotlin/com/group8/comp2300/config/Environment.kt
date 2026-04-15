package com.group8.comp2300.config

object Environment {
    val isDevelopment: Boolean
        get() = System.getenv("ENV")?.lowercase() == "development" ||
            System.getProperty("ENV")?.lowercase() == "development" ||
            System.getProperty("ktor.testing") != null

    /** When true, authenticated routes accept unauthenticated requests (dev convenience). */
    val devAuthBypass: Boolean
        get() = isDevelopment &&
            (System.getenv("DEV_AUTH_BYPASS") ?: System.getProperty("DEV_AUTH_BYPASS"))?.lowercase() != "false"

    val port: Int
        get() = (System.getenv("PORT") ?: System.getProperty("PORT"))?.toIntOrNull() ?: 8080

    const val DEV_USER_ID = "dev-user-001"
}
