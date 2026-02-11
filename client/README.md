# Client Module

This is the main application module, containing the UI and presentation logic for the cross-platform application.

## Purpose

The `client` module implements the user interface using Jetpack Compose Multiplatform. It handles navigation, state management (ViewModels), and interacts with the `shared`, `client-data`, and `i18n` modules.

## Architecture

The module follows a clean architecture approach:
- **Presentation**: UI components and screens (`presentation/ui`) and ViewModels (`presentation/viewmodel`).
- **Navigation**: Typed navigation using the Navigation for Compose library (`navigation/`).
- **DI**: Dependency injection setup using Koin (`di/`).
- **Data access**: Provided via dependencies from `client-data` and domain contracts from `shared`.

## Key Screens

Screens are organized by feature area in `presentation/ui/screens/`:
- `auth`: Login and authentication flows.
- `home`: The main dashboard.
- `medical`: Medical features like bookings and clinic search.
- `shop`: E-commerce features.
- `education`: Quizzes and educational videos.
- `profile`: User settings and profile management.

## Getting Started

To run the application, use the Gradle tasks for the specific platform:
- Android: `./gradlew :client:assembleDebug`
- Desktop: `./gradlew :client:run`
- iOS: Opening the `iosApp` Xcode project.
