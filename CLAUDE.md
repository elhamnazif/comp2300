# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Vita is a **Kotlin Multiplatform** health application targeting Android, iOS, Desktop (JVM), and Server (Ktor). The codebase follows Clean Architecture with Compose Multiplatform for UI.

## Build Commands

### Android
```bash
./gradlew :sharedUI:assembleDebug  # Build APK (note: module is sharedUI, not composeApp)
```

### Desktop
```bash
./gradlew :sharedUI:run
```

### Server
```bash
./gradlew :server:run
```

### Code Quality
```bash
./gradlew spotlessCheck    # Check code formatting
./gradlew spotlessApply    # Apply formatting fixes
./gradlew detekt           # Run static analysis
```

### Tests
```bash
./gradlew test             # Run all tests
./gradlew :sharedUI:test   # Run tests for sharedUI module only
```

## Module Structure

- **`sharedUI/`** - Main Compose Multiplatform UI module (contains all shared screens, ViewModels, DI)
- **`shared/`** - Common shared code library (utilities)
- **`i18n/`** - Internationalization (string resources)
- **`server/`** - Ktor backend
- **`androidApp/`** - Android entry point
- **`iosApp/`** - iOS entry point
- **`desktopApp/`** - Desktop entry point

## Architecture

### Clean Architecture in sharedUI

```
presentation/
├── navigation/     - Typed navigation with Koin + Navigation3
├── ui/
│   └── screens/    - Feature screens (auth, home, medical, shop, education, profile)
domain/
├── model/          - Domain models (Screen sealed interface)
├── repository/     - Repository interfaces
└── usecase/        - Use cases
data/
├── remote/         - Ktor HTTP client
├── database/       - SQLDelight local DB
├── mapper/         - DTO ↔ domain mappers
└── repository/     - Repository implementations
di/
├── AppModule.kt    - Core DI (network, DB, repos, ViewModels)
├── NavigationModule.kt - Screen routing with Koin navigation3
└── PlatformModule.kt - expect/actual for platform-specific deps
```

### Navigation

- **Navigation3** with typed `Screen` sealed interface (`sharedUI/.../domain/model/Screen.kt`)
- **Koin navigation3 DSL** in `NavigationModule.kt` defines all routes
- **Adaptive List/Detail** for Shop, Booking, Education using `ListDetailSceneStrategy`
- Guest auth flow support via `Navigator.isGuest` and `requireAuth()`

### Dependency Injection

- **Koin 4.x** with three modules:
  - `appModule` - Core dependencies (network, database, repositories, ViewModels)
  - `navigationModule` - Screen routing with `navigation<ScreenType>` DSL
  - `platformModule` - Platform-specific implementations (expect/actual pattern)

### Platform-Specific Code

Use **expect/actual** pattern for platform dependencies:
- `sharedUI/src/commonMain/.../di/PlatformModule.kt` - `expect val platformModule`
- `sharedUI/src/androidMain/.../di/PlatformModule.android.kt` - Android Context, SQLDelight driver
- Similar for iOS (Darwin) and JVM (JDBC)

### Data Layer

- **SQLDelight** for local storage (platform-specific drivers)
- **Ktor** for networking (Okhttp on Android, Darwin on iOS)
- Repository interfaces in domain, implementations in data layer

## Key Dependencies

- Kotlin 2.3.10, Compose Multiplatform 1.10.0
- Koin 4.2.0-RC1 (DI)
- SQLDelight 2.2.1 (local DB)
- Ktor 3.4.0 (networking)
- Material3 Adaptive Navigation Suite (responsive UI)
- MaterialKolor (dynamic theming)

## Feature Areas

- **Auth** - Onboarding, Login with guest mode
- **Home** - Dashboard with quick actions
- **Medical** - Bookings, Clinics, Lab Results, Medication, Self-Diagnosis
- **Shop** - Products with adaptive list/detail layout
- **Education** - Videos, Quizzes
- **Profile** - Settings, Privacy, Notifications, Help
