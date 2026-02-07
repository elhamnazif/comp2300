package org.comp2300.buildlogic

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project

internal fun Project.configureSpotless(extension: SpotlessExtension) =
    extension.apply {
        val ktlintVersion = libs.versions.ktlint.get()

        // ratchetFrom("origin/main")  // Uncomment when git history is established
        kotlin {
            target("src/*/kotlin/**/*.kt", "src/*/java/**/*.kt")
            targetExclude("**/build/**/*.kt", "**/generated/**/*.kt", "**/symbols/**/*.kt")
            ktlint(ktlintVersion)
                .setEditorConfigPath(rootProject.file("config/spotless/.editorconfig").path)
        }
        kotlinGradle {
            target("**/*.gradle.kts")
            targetExclude("**/generated/**/*.gradle.kts")
            ktlint(ktlintVersion)
                .setEditorConfigPath(rootProject.file("config/spotless/.editorconfig").path)
        }
        format("xml") {
            target("**/*.xml")
            targetExclude("**/build/**/*.xml")
            trimTrailingWhitespace()
            endWithNewline()
            leadingTabsToSpaces(4)
        }
    }
