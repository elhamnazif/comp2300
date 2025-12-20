# Build-Logic Module

The `build-logic` module contains custom Gradle convention plugins used to share build configuration across the project's modules.

## Purpose

To centralise build logic, reduce duplication in `build.gradle.kts` files, and ensure consistent configuration for Kotlin Multiplatform, Android, Compose, and more.

## Key Components

- **Convention Plugins**: Located in `convention/src/main/kotlin/`. These plugins encapsulate common configurations.
- **Version Catalogue**: Works in conjunction with the project's `gradle/libs.versions.toml`.

## Usage

Modules apply these plugins using their IDs defined in the convention plugins.
