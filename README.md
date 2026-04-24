# Vita (COMP2300 Project)

- To examiners, the deliverable for D4 can be found here: [./docs/deliverables/Group 8 - Deliverable Report.pdf](./docs/deliverables/Group%208%20-%20D4%20Deliverable%20Report.pdf)


This is a Kotlin Multiplatform project targeting Android, iOS, and Server.

- [/client](./client/src) is for code that will be shared across your Compose Multiplatform client applications.
  It contains several subfolders:
  - [commonMain](./client/src/commonMain/kotlin) is for code that's common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple's CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./client/src/iosMain/kotlin) folder would be the right place for such calls.
    The shared client structure also includes feature code under [client/src/commonMain/kotlin/com/group8/comp2300/feature](./client/src/commonMain/kotlin/com/group8/comp2300/feature)
    and platform glue under [client/src/commonMain/kotlin/com/group8/comp2300/platform](./client/src/commonMain/kotlin/com/group8/comp2300/platform).

- [/iosApp](./iosApp/iosApp) contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

- [/server](./server/src/main/kotlin) is for the Ktor server application.

- [/shared](./shared/src) is for domain/core code shared between client and server targets.
  The most important subfolder is [commonMain](./shared/src/commonMain/kotlin). If preferred, you
  can add code to the platform-specific folders here too.

- [/client-data](./client-data/src) contains client-only data and infrastructure code (repositories, network, SQLDelight, DI for data wiring).

### Build and Run Android Application

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:

- on macOS/Linux

  ```shell
  ./gradlew :androidApp:assembleDebug
  ```

- on Windows

  ```shell
  .\gradlew.bat :androidApp:assembleDebug
  ```

### Build and Run Server

To build and run the development version of the server, use the run configuration from the run widget
in your IDE’s toolbar or run it directly from the terminal:

- on macOS/Linux

  ```shell
  ./gradlew :server:run
  ```

- on Windows

  ```shell
  .\gradlew.bat :server:run
  ```

The client API base URL is compiled from the Gradle property `vita.api.base.url`.
If you need the app to talk to a server running on a different host, rebuild with that property set:

```shell
./gradlew -Pvita.api.base.url=http://YOUR_HOST:8080 :androidApp:assembleDebug
```

### Build and Run iOS Application

To build and run the development version of the iOS app, use the run configuration from the run widget
in your IDE’s toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.

For the iOS Simulator on the same Mac as the server, `http://localhost:8080` is the default and should work.
For a real iPhone, the build must use `vita.api.base.url=http://YOUR_MAC_IP:8080` instead of `localhost`.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
