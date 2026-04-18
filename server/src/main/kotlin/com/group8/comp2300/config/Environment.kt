package com.group8.comp2300.config

object Environment {
    fun value(name: String): String? = (System.getenv(name) ?: System.getProperty(name))
        ?.takeIf { it.isNotBlank() }

    val isTesting: Boolean
        get() = System.getProperty("ktor.testing") != null

    val environmentName: String
        get() = value("ENV")?.lowercase() ?: if (isTesting) "test" else "production"

    val isDevelopment: Boolean
        get() = environmentName == "development" || isTesting

    /** When true, authenticated routes accept unauthenticated requests (dev convenience). */
    val devAuthBypass: Boolean
        get() = isDevelopment &&
            value("DEV_AUTH_BYPASS")?.lowercase() != "false"

    val port: Int
        get() = value("PORT")?.toIntOrNull() ?: 8080

    val dbPath: String
        get() = value("DB_PATH") ?: "jdbc:sqlite:vita.db"

    const val DEV_USER_ID = "dev-user-001"
}
