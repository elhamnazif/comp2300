import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.android.lint)
}

kotlin {

    jvmToolchain(21)

    androidLibrary {
        namespace = "com.group8.comp2300.i18n"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        // https://youtrack.jetbrains.com/projects/CMP/issues/CMP-8232/org.jetbrains.compose.resources.MissingResourceException-Missing-resource-with-path-composeResources
        experimentalProperties["android.experimental.kmp.enableAndroidResources"] = true

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

     listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
         iosTarget.binaries.framework {
             baseName = "i18nKit"
             isStatic = true
         }
     }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        androidMain.dependencies {}

        iosMain.dependencies {}

        jvmMain.dependencies {}
    }

}

compose.resources {
    publicResClass = true
}