# COMP2300 - Agent Guide

This file serves as a comprehensive guide for AI agents and developers working on the `comp2300` codebase. Use this as your primary reference for understanding the architecture, conventions, and workflows.

## 1. Project Overview

-   **Type:** Compose Multiplatform Application targeting Android, iOS, Desktop (JVM). Also includes a Server.
-   **Purpose:** Digital Sexual Healthcare management application with booking, education, and shopping features.
-   **Architecture:** MVVM with Modern Multiplatform Development principles.
    -   **UI:** Compose Multiplatform (Material 3).
    -   **State Management:** ViewModels with StateFlow and explicit backing fields (Kotlin 2.3.0).
    -   **Dependency Injection:** Koin.
    -   **Navigation:** Navigation3.
    -   **Data Layer:** Repository pattern with Ktor client/server and shared business logic.

## 2. Codebase Map

| Directory                   | Description                                                                                                                      |
|:----------------------------|:---------------------------------------------------------------------------------------------------------------------------------|
| `composeApp/`               | Main application module. Contains shared UI, ViewModels, and platform-specific entry points. Uses package `com.group8.comp2300`. |
| `server/`                   | Ktor backend server. Uses package `com.group8.comp2300`.                                                                         |
| `shared/`                   | Shared business logic, models, and repository interfaces. Uses package `com.group8.comp2300.shared`.                             |
| `i18n/`                     | **Crucial:** Centralized string resources using Compose Multiplatform Resources.                                                 |
| `build-logic/`              | Custom Gradle convention plugins for Spotless, Detekt, and shared build logic.                                                   |
| `gradle/libs.versions.toml` | **Version Catalog.** All dependencies and versions are defined here.                                                             |

## 3. Development Guidelines

### A. UI Development (Jetpack Compose Multiplatform)
-   **Material 3:** The app uses Material 3 Expressive
-   **Strings:**
    -   Use the **Compose Multiplatform Resource** library in `i18n/`.
    -   **Definition:** Add strings to `i18n/src/commonMain/composeResources/values/strings.xml`.
    -   **Usage:**
        ```kotlin
        import org.jetbrains.compose.resources.stringResource
        import com.group8.comp2300.i18n.Res
        import com.group8.comp2300.i18n.your_string_key

        Text(text = stringResource(Res.string.your_string_key))
        ```
-   **Previews:** Create `@Preview` functions for your Composables to ensure they render correctly.

### B. Architecture & State
-   **ViewModels:** Extend `androidx.lifecycle.ViewModel` and are managed via Koin.
-   **Injection:** Use Koin modules in `di/` packages.
-   **Scopes:** Use `viewModelScope` for coroutines. Avoid `GlobalScope`.
-   **State Management:** Use explicit backing fields (Kotlin 2.3.0 feature):
    ```kotlin
    val state: StateFlow<State>
        field = MutableStateFlow(State(clinics = repository.getAllClinics()))
    ```
-   **Data Flow:** Expose UI state as `StateFlow<UiState>` or `Flow<UiState>`.

### C. Navigation
-   The project uses **Type-Safe Navigation** (Kotlin Serialization & Navigation3).
-   Screens are defined in `domain/model/Screen.kt`.
-   Navigation logic is in `presentation/navigation/Navigator.kt`.

### D. Dependency Management
-   **Never** hardcode versions in `build.gradle.kts` files.
-   **Action:** Add the library and version to `gradle/libs.versions.toml`.
-   **Action:** Apply plugins using the alias from the catalog (e.g., `alias(libs.plugins.kotlinMultiplatform)`).
-   **Alpha Libraries:** Do not be shy about using alpha libraries if they provide the necessary features.

## 4. Quality Assurance

### A. Code Style (Spotless)
-   The project uses **Spotless** to enforce formatting.
-   **Command:** `./gradlew spotlessApply`
-   **Rule:** You **must** run this before submitting any code.

### B. Linting (Detekt)
-   The project uses **Detekt** for static analysis with Compose rules.
-   **Command:** `./gradlew detekt`
-   **Rule:** Ensure zero regressions.

### C. Testing
-   **Unit Tests:** JUnit in `commonTest/` directories. Run with `./gradlew test`.
-   **Platform Tests:** Android-specific tests in `androidTest/`.
-   **Integration Tests:** Use `FakeNavigator` for testing navigation-dependent code.
-   **Module Tests:** `./gradlew :composeApp:test` or `./gradlew :server:test`

## 5. Agent Workflow

1.  **Explore First:** Before making changes, read `gradle/libs.versions.toml` and the relevant `build.gradle.kts` to understand the environment.
2.  **Plan:** Identify which modules (`composeApp`, `shared`, `server`, or `i18n`) need modification.
3.  **Implement:**
    -   If adding a string, modify `i18n/`.
    -   If adding a dependency, modify `libs.versions.toml` first.
4.  **Verify:**
    -   Run `./gradlew spotlessApply` (Essential!).
    -   Run `./gradlew detekt`.
    -   Run relevant tests (e.g., `./gradlew :composeApp:test`).

## 6. Important Context

-   **SymbolCraft:** Material Design symbols are auto-generated and available in `com.app.symbols.materialsymbols.icons.*`.
-   **Package Structure:** All platform-specific code uses `com.group8.comp2300` package namespace.

## 7. Commands Reference

```bash
# Build all modules
./gradlew build

# Build and run specific platforms
./gradlew :composeApp:assembleDebug  # Android
./gradlew :composeApp:run             # Desktop  
./gradlew :server:run                 # Server

# Code quality (essential)
./gradlew spotlessApply              # Auto-fix formatting
./gradlew detekt                     # Static analysis

# Testing
./gradlew test                       # All tests
./gradlew :composeApp:test           # App module only
```

## 8. Troubleshooting

-   **Missing Strings:** If `Res.string.xyz` is unresolved, ensure you have imported the correct package and run a build to generate resources.
-   **Build Errors:** Check `gradle/libs.versions.toml` for version conflicts. Ensure convention plugins are applied correctly.
-   **Platform Issues:** For iOS/Android/Desktop platform-specific code, check the appropriate `*Main/` source set.

---
*Refer to `TO_BE_CREATED.md` for detailed architectural insights and the codebase's MVVM implementation patterns.*