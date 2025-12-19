plugins {
    `kotlin-dsl`
    alias(libs.plugins.spotless)
    alias(libs.plugins.detekt)
}

// Configure the build-logic plugins to target JDK 21
// This matches the JDK used to build the project, and is not related to what is running on device.
kotlin {
    jvmToolchain(21)
}

dependencies {
    compileOnly(libs.android.gradleApiPlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.spotless.gradlePlugin)
    detektPlugins(libs.detekt.formatting)

    // https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
    compileOnly(files(libs::class.java.superclass.protectionDomain.codeSource.location))
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

spotless {
    val ktlintVersion = libs.versions.ktlint.get()
    ratchetFrom("origin/main")
    kotlin {
        target("src/*/kotlin/**/*.kt", "src/*/java/**/*.kt")
        targetExclude("**/build/**/*.kt")
        ktlint(ktlintVersion).setEditorConfigPath(rootProject.file("../config/spotless/.editorconfig").path)
        //licenseHeaderFile(rootProject.file("../config/spotless/copyright.kt"))
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        ktlint(ktlintVersion).setEditorConfigPath(rootProject.file("../config/spotless/.editorconfig").path)
        //licenseHeaderFile(
        //    rootProject.file("../config/spotless/copyright.kts"),
        //    "(^(?![\\/ ]\\*).*$)"
        //)
    }
}

detekt {
    toolVersion = libs.versions.detekt.get()
    config.setFrom(rootProject.file("../config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = false
    baseline = file("detekt-baseline.xml")
    source.setFrom(
        files(
            "src/main/java",
            "src/main/kotlin",
        )
    )
}

gradlePlugin {
    plugins {
        register("comp2300Detekt") {
            id = libs.plugins.comp2300.detekt.get().pluginId
            implementationClass = "DetektConventionPlugin"
        }
        register("comp2300Spotless") {
            id = libs.plugins.comp2300.spotless.get().pluginId
            implementationClass = "SpotlessConventionPlugin"
        }
    }
}