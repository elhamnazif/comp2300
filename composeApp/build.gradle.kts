import io.github.kingsword09.symbolcraft.model.SymbolVariant
import io.github.kingsword09.symbolcraft.model.SymbolWeight
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.symbolCraft)
}

kotlin {
    androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm()

    // https://maplibre.org/maplibre-compose/getting-started/#set-up-desktop-jvm
    fun detectTarget(): String {
        val hostOs = when (val os = System.getProperty("os.name").lowercase()) {
            "mac os x" -> "macos"
            else -> os.split(" ").first()
        }
        val hostArch = when (val arch = System.getProperty("os.arch").lowercase()) {
            "x86_64" -> "amd64"
            "arm64" -> "aarch64"
            else -> arch
        }
        val renderer = when (hostOs) {
            "macos" -> "metal"
            else -> "opengl"
        }
        return "${hostOs}-${hostArch}-${renderer}"
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlin.time.ExperimentalTime")
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.appcompat)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)

            implementation(libs.lifecycle.viewmodelCompose)
            implementation(libs.lifecycle.runtimeCompose)
            implementation(libs.lifecycle.viewmodelNavigation3)
            implementation(libs.lifecycle.viewmodelSavedstate)

            // implementation(libs.navigation3.runtime)
            implementation(libs.navigation3.ui)

            // Adaptive Navigation
            implementation(libs.material3.adaptive)
            implementation(libs.material3.adaptive.navigation3)
            implementation(libs.material3.adaptive.layout)
            implementation(libs.material3.adaptive.navigation.suite)

            implementation(libs.kotlinx.datetime)
            implementation(libs.material.icons.core)

            // Map Libre
            implementation(libs.maplibre.compose)
            implementation(libs.maplibre.composeMaterial3)

            implementation(libs.kotlinx.serialization.json)

            // Koin for dependency injection (Multiplatform and Compose)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.navigation3)

            implementation(projects.shared)
        }
        commonTest.dependencies { implementation(libs.kotlin.test) }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            runtimeOnly("org.maplibre.compose:maplibre-native-bindings-jni:0.12.1") {
                capabilities {
                    requireCapability("org.maplibre.compose:maplibre-native-bindings-jni-${detectTarget()}")
                }
            }
        }
    }
}

android {
    namespace = "com.group8.comp2300"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.group8.comp2300"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
    buildTypes { getByName("release") { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies { debugImplementation(libs.compose.ui.tooling) }

symbolCraft {
    // Basic configuration
    packageName.set("com.app.symbols")
    outputDirectory.set("src/commonMain/kotlin") // Support multiplatform projects
    cacheEnabled.set(true)

    // Preview generation configuration (optional)
    // TODO: Fix once they've moved over to using Androidx's Preview Generators
    generatePreview.set(false) // Enable preview generation

    // Convenient batch configuration methods
    materialSymbols("chevron_right", "chevron_left") {
        standardWeights() // Auto-add 400, 500, 700 weights
    }

    // Batch configure multiple icons
    materialSymbols(
        "star",
        "bookmark",
        "fingerprint",
        "health_and_safety",
        "play_circle",
        "article",
        "quiz",
        "lightbulb",
        "shield",
        "visibility",
        "visibility_off",
        "calendar_month",
        "local_pharmacy"
    ) { weights(SymbolWeight.W500, variant = SymbolVariant.OUTLINED) }

    // Local SVG files stored in the repository
    //    localIcons {
    //        directory = "src/commonMain/resources/icons"
    //        // include("**/*.svg") // optional, defaults to **/*.svg
    //    }

    //    localIcons(libraryName = "brand") {
    //        directory = "design/exported"
    //        include("brand/**/*.svg")
    //        exclude("legacy/**")
    //    }
}

compose.desktop {
    application {
        mainClass = "com.group8.comp2300.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.group8.comp2300"
            packageVersion = "1.0.0"
        }
    }
}
