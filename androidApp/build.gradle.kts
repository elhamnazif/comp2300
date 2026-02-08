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
            "-Xexplicit-backing-fields",
        )
    }
}

dependencies {
    implementation(project(":sharedUI"))
    implementation(libs.androidx.activityCompose)
    implementation(libs.compose.nav3)
}
