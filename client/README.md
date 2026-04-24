# Client Module

This is the shared Compose Multiplatform UI module for the Vita client applications.

## Purpose

The `client` module implements the shared app shell, navigation, screens, and platform integration points. It depends on `shared`, `client-data`, and `i18n`, while the platform hosts live in `androidApp` and `iosApp`.

## Architecture

The module is organised around the current feature-first layout:
- **`app/`**: App shell and top-level navigation wiring.
- **`feature/`**: User-facing feature screens and their ViewModels.
- **`core/`**: Shared UI, formatting, error, and security helpers used across features.
- **`platform/`**: Platform seams such as biometrics, files, notifications, system UI, and theming.

## Key Areas

Feature code lives under `client/src/commonMain/kotlin/com/group8/comp2300/feature/` and includes areas such as:
- `auth`: Login and authentication flows.
- `booking`, `calendar`, `records`, `routine`: Care and medical workflows.
- `home`, `education`, `shop`, `profile`, `settings`: Main product surfaces.
- `chatbot`, `selfdiagnosis`: Specialized flows built on the shared app shell.

## Getting Started

The `client` module is a shared library, not the platform app entrypoint.

- Android: build and run `androidApp`.
- iOS: open `iosApp/iosApp.xcodeproj` in Xcode.
- Shared UI changes usually need a platform build plus any relevant module tests.
