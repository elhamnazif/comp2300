# Shared Module

The `shared` module contains common business logic, domain models, and platform-specific abstractions.

## Purpose

To provide a single source of truth for the domain layer and any reusable logic that should be shared between the client (`sharedUI`) and potentially the `server`.

## Contents

- **Domain Models**: Located in `src/commonMain/kotlin/.../domain/model/`. Organised by feature area (education, medical, reminder, shop, user).
- **Mocks**: Located in `src/commonMain/kotlin/.../mock/`, providing sample data for previews and development.
- **Platform Abstractions**: The `Platform` interface and platform-specific implementations.

## Usage

This module is a dependency for `sharedUI` and `server`. It ensures that both the frontend and backend share the same data structures.
