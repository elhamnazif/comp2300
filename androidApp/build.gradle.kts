import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
                    execOperations.exec {
                        commandLine = listOf("adb", "reverse", "tcp:8080", "tcp:8080")
                        isIgnoreExitValue = true
                    }
                } catch (_: Exception) {
                    // Ignore if adb not available or no device connected
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
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
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
            "-Xexplicit-backing-fields"
        )
    }
}

dependencies {
    implementation(project(":client"))
    implementation(libs.androidx.activityCompose)
    implementation(libs.compose.nav3)
}
