package com.group8.comp2300.config

import java.nio.file.Path
import kotlin.io.path.bufferedReader
import kotlin.io.path.exists

object Environment {
    private val dotEnvValues: Map<String, String> by lazy { loadDotEnv() }

    fun value(name: String): String? = (
        System.getenv(name)
            ?: System.getProperty(name)
            ?: dotEnvValues[name]
        )?.takeIf { it.isNotBlank() }

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

    private fun loadDotEnv(): Map<String, String> {
        val dotEnvPath = dotEnvCandidates.firstOrNull(Path::exists) ?: return emptyMap()

        return dotEnvPath.bufferedReader().useLines { lines ->
            lines.mapNotNull(::parseDotEnvLine).toMap()
        }
    }

    private fun parseDotEnvLine(line: String): Pair<String, String>? {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return null

        val assignment = trimmed.removePrefix("export ").trim()
        val separatorIndex = assignment.indexOf('=')
        if (separatorIndex <= 0) return null

        val key = assignment.substring(0, separatorIndex).trim()
        if (key.isEmpty()) return null

        val rawValue = assignment.substring(separatorIndex + 1).trim()
        val value = rawValue.removeSurrounding("\"").removeSurrounding("'")
        return key to value
    }

    private val dotEnvCandidates: List<Path>
        get() {
            val workingDir = Path.of("").toAbsolutePath().normalize()
            return listOf(
                workingDir.resolve(".env"),
                workingDir.resolve("server").resolve(".env"),
            ).distinct()
        }
}
