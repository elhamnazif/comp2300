import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.comp2300.spotless)
    alias(libs.plugins.comp2300.detekt)
}

kotlin {
    android {
        namespace = "com.group8.comp2300.shared"
        compileSdk = 36
        minSdk = 24
        compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }
    }

    iosArm64()
    iosSimulatorArm64()

    jvm()

    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.uuid.ExperimentalUuidApi",
            "-opt-in=kotlin.time.ExperimentalTime",
            "-Xexpect-actual-classes",
            "-Xannotation-default-target=param-property",
        )
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.mp.stools)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
        }
        commonTest.dependencies { implementation(libs.kotlin.test) }
    }

    targets
        .withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>()
        .matching { it.konanTarget.family.isAppleFamily }
        .configureEach {
            binaries {
                framework {
                    baseName = "sharedKit"
                    isStatic = true
                }
            }
        }
}
