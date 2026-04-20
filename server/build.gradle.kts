plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.ktor)
    alias(libs.plugins.comp2300.spotless)
    alias(libs.plugins.comp2300.detekt)
    alias(libs.plugins.sqlDelight)
    application
}

sqldelight {
    databases {
        create("ServerDatabase") {
            packageName.set("com.group8.comp2300.database")
        }
    }
}

group = "com.group8.comp2300"

version = "1.0.0"

fun loadDotEnv(file: File): Map<String, String> = if (!file.exists()) {
    emptyMap()
} else {
    file.readLines()
        .mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                return@mapNotNull null
            }

            val assignment = trimmed.removePrefix("export ").trim()
            val separatorIndex = assignment.indexOf('=')
            if (separatorIndex <= 0) {
                return@mapNotNull null
            }

            val key = assignment.substring(0, separatorIndex).trim()
            if (key.isEmpty()) {
                return@mapNotNull null
            }

            val rawValue = assignment.substring(separatorIndex + 1).trim()
            val value = rawValue.removeSurrounding("\"").removeSurrounding("'")
            key to value
        }
        .toMap()
}

application {
    mainClass.set("com.group8.comp2300.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment", "-DENV=development")
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.logback)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.sqlDelight.driver.sqlite)
    implementation(libs.kotlinx.datetime)

    // Auth
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.bcrypt)
    implementation(libs.java.jwt)

    // Email
    implementation(libs.resend)

    // Koin DI
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.kotlin.test.junit)

    // Test mockk
    testImplementation(libs.mockk)
}

tasks.withType<Test>().configureEach {
    // Set ENV=development for tests to allow default JWT secret
    systemProperty("ENV", "development")
    // Also set ktor.testing property as backup
    systemProperty("ktor.testing", "true")
}

tasks.named<JavaExec>("run") {
    val dotEnvFile = project.file(".env")
    val dotEnvValues = loadDotEnv(dotEnvFile)
    val effectiveDotEnvValues = dotEnvValues.filterKeys { key -> System.getenv(key) == null }

    if (effectiveDotEnvValues.isNotEmpty()) {
        environment(effectiveDotEnvValues)
    }

    doFirst {
        if (dotEnvValues.isNotEmpty()) {
            logger.lifecycle(
                "Loaded ${dotEnvValues.size} values from ${project.relativePath(dotEnvFile)}. " +
                    "Existing shell environment variables take precedence.",
            )
        }
    }
}
