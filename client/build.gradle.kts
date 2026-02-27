import io.github.kingsword09.symbolcraft.model.SymbolVariant
import io.github.kingsword09.symbolcraft.model.SymbolWeight
import org.gradle.kotlin.dsl.withType
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.net.URI

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.symbolCraft)
    alias(libs.plugins.comp2300.spotless)
    alias(libs.plugins.comp2300.detekt)
    alias(libs.plugins.spmForKmp)
}

kotlin {
    android {
        namespace = "com.group8.comp2300"
        compileSdk = 36
        minSdk = 24
        androidResources.enable = true
        compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }
    }

    jvm()

    iosArm64()
    iosSimulatorArm64()

    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.uuid.ExperimentalUuidApi",
            "-opt-in=kotlin.time.ExperimentalTime",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=org.koin.core.annotation.KoinExperimentalAPI",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-Xexpect-actual-classes",
            "-Xannotation-default-target=param-property",
            "-Xexplicit-backing-fields",
        )
    }

    // https://maplibre.org/maplibre-compose/getting-started/#set-up-desktop-jvm
    fun detectTarget(): String {
        val hostOs =
            when (val os = System.getProperty("os.name").lowercase()) {
                "mac os x" -> "macos"
                else -> os.split(" ").first()
            }
        val hostArch =
            when (val arch = System.getProperty("os.arch").lowercase()) {
                "x86_64" -> "amd64"
                "arm64" -> "aarch64"
                else -> arch
            }
        val renderer =
            when (hostOs) {
                "macos" -> "metal"
                else -> "opengl"
            }
        return "$hostOs-$hostArch-$renderer"
    }

    targets
        .withType<KotlinNativeTarget>()
        .matching { it.konanTarget.family.isAppleFamily }
        .configureEach {
            compilations {
                getByName("main") {
                    cinterops.create("spmMaplibre")
                }
            }
            binaries {
                framework {
                    baseName = "clientKit"
                    isStatic = true
                }
            }
        }

    sourceSets {
        commonMain {
            kotlin.srcDir(tasks.named("generateSymbolCraftIcons"))
            dependencies {
                api(project(":shared"))
                implementation(project(":client-data"))
                api(project(":i18n"))

                // Compose
                api(libs.compose.runtime)
                api(libs.compose.ui)
                api(libs.compose.foundation)
                api(libs.compose.resources)
                api(libs.compose.ui.tooling.preview)
                implementation(libs.compose.material3)

                // Compose Adaptive
                implementation(libs.material3.adaptive)
                implementation(libs.material3.adaptive.navigation3)
                implementation(libs.material3.adaptive.layout)
                implementation(libs.material3.adaptive.navigation.suite)

                // Lifecycles
                implementation(libs.androidx.lifecycle.viewmodel)
                implementation(libs.androidx.lifecycle.runtime)
                implementation(libs.androidx.lifecycle.viewmodel.navigation3)
                implementation(libs.androidx.lifecycle.viewmodel.savedstate)

                // Navigation3
                implementation(libs.compose.nav3)

                // MapLibre
                implementation(libs.maplibre.compose)
                implementation(libs.maplibre.composeMaterial3)

                // Koin
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)
                implementation(libs.koin.compose.navigation3)

                // Theme
                implementation(libs.materialKolor)

                // Kotlinx
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            val target = detectTarget()
            runtimeOnly(
                libs.maplibre.nativeBindingsJni
                    .get()
                    .toString(),
            ) {
                capabilities {
                    requireCapability("org.maplibre.compose:maplibre-native-bindings-jni-$target")
                }
            }
        }

        iosMain.dependencies {
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.ui.tooling)
}

symbolCraft {
    // Basic configuration
    packageName.set("com.group8.comp2300.symbols")
    outputDirectory.set(layout.buildDirectory.dir("generated/symbolcraft").map { it.asFile.absolutePath })
    cacheEnabled.set(true)

    // Preview generation configuration (optional)
    generatePreview.set(false)

    // Convenient batch configuration methods
    materialSymbols("chevron_right", "chevron_left") {
        standardWeights()
    }

    // expand_more and expand_less (replacing keyboard_arrow_down/up)
    materialSymbols("expand_more", "expand_less") {
        standardWeights()
    }

    // phone icon
    materialSymbols("call") {
        weights(SymbolWeight.W400, SymbolWeight.W500, variant = SymbolVariant.OUTLINED)
        bothFills(weight = SymbolWeight.W400, variant = SymbolVariant.OUTLINED)
        bothFills(weight = SymbolWeight.W500, variant = SymbolVariant.OUTLINED)
    }

    // Batch configure multiple icons
    materialSymbols(
        "star",
        "bookmark",
        "fingerprint",
        "health_and_safety",
        "stethoscope",
        "play_circle",
        "article",
        "quiz",
        "lightbulb",
        "shield",
        "visibility",
        "visibility_off",
        "calendar_month",
        "local_pharmacy",
    ) {
        weights(SymbolWeight.W500, variant = SymbolVariant.OUTLINED)
    }

    // Icons replacing material-icons-core
    materialSymbols(
        // Navigation
        "arrow_back",
        "arrow_forward",
        "arrow_drop_down",
        "home",
        "person",
        // Actions
        "add",
        "check",
        "check_circle",
        "close",
        "delete",
        "done",
        "edit",
        "search",
        "send",
        "share",
        // Content & Media
        "play_arrow",
        "thumb_up",
        // Social & Communication
        "mail_outline",
        "notifications",
        // Shopping & Commerce
        "shopping_cart",
        // Information & UI
        "info",
        // Places & Location
        "location_on",
        // Dates & Time
        "date_range",
        // Security & Account
        "lock",
        "account_box",
        "face",
        // Misc
        "favorite",
    ) {
        weights(SymbolWeight.W400, SymbolWeight.W500, variant = SymbolVariant.OUTLINED)
        bothFills(weight = SymbolWeight.W400, variant = SymbolVariant.OUTLINED)
        bothFills(weight = SymbolWeight.W500, variant = SymbolVariant.OUTLINED)
    }
}

// Make sure Spotless runs after SymbolCraft icon generation
tasks.named("spotlessKotlin") {
    dependsOn("generateSymbolCraftIcons")
}

// Ensure compilation tasks depend on code generation tasks
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn("generateSymbolCraftIcons")
}

// Ensure native compile tasks depend on code generation tasks
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile>().configureEach {
    dependsOn("generateSymbolCraftIcons")
}

// Ensure metadata compile task depends on code generation tasks
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    if (name.contains("compile", ignoreCase = true)) {
        dependsOn("generateSymbolCraftIcons")
    }
}

// Workaround for prepareAndroidMainArtProfile which doesn't seem to respect srcDir dependencies
tasks.matching { it.name.contains("prepareAndroidMainArtProfile", ignoreCase = true) }.configureEach {
    dependsOn("generateSymbolCraftIcons")
}

swiftPackageConfig {
    create("spmMaplibre") {
        dependency {
            remotePackageVersion(
                url = URI("https://github.com/maplibre/maplibre-gl-native-distribution.git"),
                products = { add("MapLibre", exportToKotlin = true) },
                packageName = "maplibre-gl-native-distribution",
                version = "6.17.1",
            )
        }
    }
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
