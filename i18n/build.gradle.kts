import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    android {
        namespace = "com.group8.comp2300.i18n"
        compileSdk = 36
        minSdk = 24
        androidResources.enable = true
        compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }
    }

    iosArm64()
    iosSimulatorArm64()

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.compose.resources)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        androidMain.dependencies {}

        iosMain.dependencies {}

        jvmMain.dependencies {}
    }

    targets
        .withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>()
        .matching { it.konanTarget.family.isAppleFamily }
        .configureEach {
            binaries {
                framework {
                    baseName = "i18nKit"
                    isStatic = true
                }
            }
        }
}

compose.resources {
    publicResClass = true
}