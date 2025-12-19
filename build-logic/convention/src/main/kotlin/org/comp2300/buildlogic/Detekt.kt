package org.comp2300.buildlogic

import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.named
import java.io.File

internal fun Project.configureDetekt(extension: DetektExtension) = extension.apply {
    extension.apply {
        toolVersion.set(libs.versions.detekt.get().toString())
        config.setFrom("$rootDir/config/detekt/detekt.yml")
        buildUponDefaultConfig.set(true)
        allRules.set(false)
        source.setFrom(
            files(
                "src/main/java",
                "src/main/kotlin",
            ),
        )
    }
    tasks.named<Detekt>("detekt") {
        reports {
            checkstyle.required.set(true)
            html.required.set(true)
            sarif.required.set(true)
            markdown.required.set(true)
        }
        reports.checkstyle.outputLocation.set(File("$rootDir/build/reports/detekt/detekt.xml"))
        reports.html.outputLocation.set(File("$rootDir/build/reports/detekt/detekt.html"))
        reports.sarif.outputLocation.set(File("$rootDir/build/reports/detekt/detekt.sarif"))
        reports.markdown.outputLocation.set(File("$rootDir/build/reports/detekt/detekt.md"))
    }
    dependencies {
        "detektPlugins"(libs.detekt.formatting.get())
        "detektPlugins"(libs.detekt.compose.get())
    }
}