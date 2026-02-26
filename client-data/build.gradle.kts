import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.comp2300.spotless)
    alias(libs.plugins.comp2300.detekt)
    alias(libs.plugins.sqlDelight)
}

sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("com.group8.comp2300.data.database")
        }
    }
}

kotlin {
    android {
        namespace = "com.group8.comp2300.sharedclientdata"
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
            "-Xannotation-default-target=param-property"
        )
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.serialization.json)
            implementation(libs.sqlDelight.coroutines)
            implementation(libs.koin.core)
            implementation(libs.kermit)
            implementation(libs.kermit.koin)
        }

        commonTest.dependencies { implementation(libs.kotlin.test) }

        androidMain.dependencies {
            implementation(libs.sqlDelight.driver.android)
            implementation(libs.ktor.client.okhttp)
        }

        jvmMain.dependencies {
            implementation(libs.sqlDelight.driver.sqlite)
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.sqlDelight.driver.native)
            implementation(libs.ktor.client.darwin)
        }
    }

    targets
        .withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>()
        .matching { it.konanTarget.family.isAppleFamily }
        .configureEach {
            binaries {
                framework {
                    baseName = "ClientDataKit"
                    isStatic = true
                }
            }
        }
}
