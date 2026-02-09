# iosApp Module

This is the native iOS application wrapper for the Kotlin Multiplatform project.

## Purpose

The `iosApp` module provides the entry point for running the application on iOS devices. It uses SwiftUI to host the shared Compose Multiplatform UI.

## Technology

- **SwiftUI**: Used for the native wrapper and `ContentView`.
- **Kotlin Multiplatform**: Integrates with the `shared` and `sharedUI` KMP modules.

## Getting Started

1. Ensure you have Xcode installed.
2. Open `iosApp/iosApp.xcodeproj` in Xcode.
3. Select a simulator or physical device and run the project.

Alternatively, you can run it from the command line using Gradle tasks if configured, or via Android Studio's runner.
