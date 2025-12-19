package org.comp2300.buildlogic

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project

internal fun Project.configureSpotless(extension: SpotlessExtension) = extension.apply {
    val ktlintVersion = libs.versions.ktlint.get().toString()

    extension.apply {
        ratchetFrom("origin/main")
        kotlin {
            target("src/*/kotlin/**/*.kt", "src/*/java/**/*.kt")
            targetExclude("**/build/**/*.kt")
            ktlint(ktlintVersion).setEditorConfigPath(rootProject.file("config/spotless/.editorconfig").path)
            //licenseHeaderFile(rootProject.file("config/spotless/copyright.kt"))
        }
        kotlinGradle {
            target("**/*.gradle.kts")
            ktlint(ktlintVersion).setEditorConfigPath(rootProject.file("config/spotless/.editorconfig").path)
            //licenseHeaderFile(
            //    rootProject.file("config/spotless/copyright.kts"),
            //    "(^(?![\\/ ]\\*).*$)"
            //)
        }
        format("xml") {
            target("**/*.xml")
            targetExclude("**/build/**/*.xml")
            //licenseHeader(
            //    rootProject.file("config/spotless/copyright.xml"),
            //    "(^(?![\\/ ]\\*).*$)"
            //)
        }
    }
}
