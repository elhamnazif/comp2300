package org.comp2300.buildlogic

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project

internal fun Project.configureSpotless(extension: SpotlessExtension) =
    extension.apply {
        val ktlintVersion = libs.versions.ktlint.get()

        // Check for local .editorconfig in module directory first, then fall back to root config
        val defaultEditorConfig = rootProject.file("config/spotless/.editorconfig").path
        val localEditorConfig = file(".editorconfig").takeIf { it.exists() }?.path ?: defaultEditorConfig
        val editorConfigPath = localEditorConfig ?: defaultEditorConfig

        // ratchetFrom("origin/main")  // Uncomment when git history is established
        kotlin {
            target("src/*/kotlin/**/*.kt", "src/*/java/**/*.kt")
            targetExclude("**/build/**/*.kt", "**/generated/**/*.kt", "**/symbols/**/*.kt")
            ktlint(ktlintVersion)
                .setEditorConfigPath(editorConfigPath)
        }
        kotlinGradle {
            target("**/*.gradle.kts")
            targetExclude("**/generated/**/*.gradle.kts")
            ktlint(ktlintVersion)
                .setEditorConfigPath(editorConfigPath)
        }
        format("xml") {
            target("**/*.xml")
            targetExclude("**/build/**/*.xml")
            trimTrailingWhitespace()
            endWithNewline()
            leadingTabsToSpaces(4)
        }
    }
