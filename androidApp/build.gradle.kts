import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

fun Project.gradleStringProperty(name: String): String? =
    providers.gradleProperty(name).orNull?.takeIf { it.isNotBlank() }
        ?: System.getenv(name)?.takeIf { it.isNotBlank() }

fun Project.gradleIntProperty(name: String, defaultValue: Int): Int =
    gradleStringProperty(name)?.toIntOrNull() ?: defaultValue

plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.application)
    alias(libs.plugins.comp2300.spotless)
    alias(libs.plugins.comp2300.detekt)
}

android {
    namespace = "com.group8.comp2300.androidApp"
    compileSdk = 36

    // Automatically set up adb port reverse for local development server
    // This allows the emulator to reach localhost:8080 on the host machine
    afterEvaluate {
        abstract class AdbReverseTask : DefaultTask() {
            @get:Inject
            abstract val execOperations: ExecOperations

            @TaskAction
            fun run() {
                try {
                    val result = execOperations.exec {
                        commandLine = listOf("adb", "reverse", "tcp:8080", "tcp:8080")
                        isIgnoreExitValue = true
                    }
                    if (result.exitValue == 0) {
                        logger.lifecycle("✓ ADB port reverse configured: tcp:8080 → tcp:8080")
                    } else {
                        logger.lifecycle(
                            "⚠ ADB port reverse failed (exit code ${result.exitValue}). Ensure an emulator or device is connected.",
                        )
                    }
                } catch (_: Exception) {
                    logger.lifecycle(
                        $$"""
                        |⚠ ADB not found. Port reverse skipped.
                        |
                        |To set up ADB:
                        |  1. Install Android Studio: https://developer.android.com/studio
                        |  2. ADB is included in the Android SDK platform-tools
                        |  3. Add to your PATH:
                        |       Linux/macOS: export PATH="$PATH:$HOME/Android/Sdk/platform-tools"
                        |       Windows: Add %LOCALAPPDATA%\Android\Sdk\platform-tools to PATH
                        |  4. Or install standalone platform-tools: https://developer.android.com/tools/releases/platform-tools
                        |
                        |Docs: https://developer.android.com/tools/adb
                        """.trimMargin(),
                    )
                }
            }
        }

        tasks.register("adbReverse", AdbReverseTask::class.java) {
            group = "build"
            description = "Set up adb port reverse for local development server"
        }

        tasks.named("preBuild").configure {
            dependsOn(tasks.named("adbReverse"))
        }
    }

    defaultConfig {
        minSdk = 24
        targetSdk = 36

        applicationId = "com.group8.comp2300.androidApp"
        versionCode = gradleIntProperty("VITA_RELEASE_VERSION_CODE", 1)
        versionName = gradleStringProperty("VITA_RELEASE_VERSION_NAME") ?: "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)

        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
            "-Xexplicit-backing-fields",
        )
    }
}

dependencies {
    implementation(project(":client"))
    implementation(libs.androidx.activityCompose)
    implementation(libs.compose.nav3)
    implementation(libs.androidx.appcompat)
}
