plugins {
    alias(libs.plugins.kotlin.jvm)
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

application {
    mainClass.set("com.group8.comp2300.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.logback)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.sqlDelight.driver.sqlite)

    // Koin DI
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}
