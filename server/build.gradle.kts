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

    // Koin DI
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.kotlin.test.junit)
}

tasks.withType<Test>().configureEach {
    // Set ENV=development for tests to allow default JWT secret
    systemProperty("ENV", "development")
    // Also set ktor.testing property as backup
    systemProperty("ktor.testing", "true")
}
