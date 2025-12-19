package org.comp2300.buildlogic

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.the
import java.io.FileInputStream
import java.util.Properties

//internal val Project.libs
//    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal val Project.libs
    get(): LibrariesForLibs = the()

internal val Project.configProperties: Properties
    get() {
        val properties = Properties()
        val propertiesFile = rootProject.file("config.properties")
        if (propertiesFile.exists()) {
            FileInputStream(propertiesFile).use { properties.load(it) }
        }
        return properties
    }