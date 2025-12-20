# Server Module

The `server` module provides the backend implementation for the project.

## Purpose

To host the API and server-side logic required by the application. Currently, it serves as a lightweight Ktor backend.

## Technology

- **Ktor**: A framework for building asynchronous servers in Kotlin.
- **Netty**: The engine used to run the server.

## Running the Server

To start the server locally, run:
```bash
./gradlew :server:run
```
The server will be available at `http://0.0.0.0:8080` (or the configured `SERVER_PORT`).

## Structure

- `src/main/kotlin/com/group8/comp2300/Application.kt`: The main entry point and routing configuration.
