# Vita Server

Ktor-based backend API for the Vita health application.

## Quick Start

```bash
# Optional first step for local overrides
cp server/.env.example server/.env

# Run the server in local development mode
./gradlew :server:run

# Run with custom port
PORT=3000 ./gradlew :server:run

# Run tests
./gradlew :server:test
```

The server will be available at `http://localhost:8080` by default.

The current developer setup is intentionally light on startup arguments:
- `./gradlew :server:run` already sets `ENV=development`
- `:server:run` automatically loads `server/.env` if that file exists
- auth bypass is enabled by default in development
- a development user is auto-seeded when auth bypass is enabled
- email is optional; if `RESEND_API_KEY` is unset, the server still runs
- startup logs print the resolved environment, port, DB path, auth bypass state, and email configuration state

For local development, copy `server/.env.example` to `server/.env` and adjust what you need. Existing shell environment variables still take precedence over values from `.env`.

## Connecting from Mobile Devices

### Android Emulator
Works automatically - the emulator can access `localhost:8080` on your machine.

### Real Android Device (via USB)
Run this command once after connecting your device:
```bash
adb reverse tcp:8080 tcp:8080
```
This forwards port 8080 from the device to your development machine. If you started the server on a different port, replace both `8080` values with that port.

### iOS Simulator
Works automatically - the simulator can access `localhost:8080`.

### Real iOS Device
Connect your device and Mac to the same Wi-Fi network, then use your Mac's local IP address:
```bash
# Find your Mac's IP
ipconfig getifaddr en0
# Then use http://THAT_IP:8080 in the app
```
If you started the server on a different port, use that port in the device URL instead of `8080`.

## Architecture

The server follows a layered structure:

```
server/
├── src/main/kotlin/com/group8/comp2300/
│   ├── Application.kt                  # Entry point and Ktor wiring
│   ├── config/                         # Runtime config from env vars
│   ├── di/                             # Koin module setup
│   ├── domain/                         # Shared models and repository contracts
│   ├── dto/                            # Request/response payloads
│   ├── routes/                         # HTTP route handlers
│   ├── service/                        # Business logic
│   ├── data/repository/                # SQLDelight-backed repositories
│   ├── infrastructure/database/        # Database creation/bootstrap
│   └── security/                       # JWT, hashing, encryption
└── src/main/sqldelight/
    └── com/group8/comp2300/database/   # SQLDelight schema and queries
```

## Technology Stack

| Technology | Purpose |
|---|---|
| **Ktor** | HTTP server and routing |
| **Netty** | JVM server engine |
| **SQLDelight** | Typed SQLite schema and queries |
| **SQLite** | Local persistent storage |
| **Koin** | Dependency injection |
| **Kotlinx Serialization** | JSON payload handling |

Dependency versions are managed centrally in `gradle/libs.versions.toml` rather than duplicated here.

## Development Mode

The server supports a development mode with authentication bypass for easier testing.

### Runtime Config

These are the current runtime knobs the server reads directly from env vars or system properties:

| Variable | Default | Description |
|---|---|---|
| `ENV` | `development` when run via `:server:run` | Enables development behavior |
| `DEV_AUTH_BYPASS` | `true` in development | Set to `false` to require real auth even in development |
| `PORT` | `8080` | HTTP port |
| `DB_PATH` | `jdbc:sqlite:vita.db` | SQLite JDBC URL |
| `JWT_SECRET` | `dev-secret-key-change-in-production` in development | JWT signing secret |
| `JWT_REALM` | `comp2300` | JWT realm |
| `JWT_ISSUER` | `http://0.0.0.0:8080` | JWT issuer |
| `JWT_AUDIENCE` | `http://0.0.0.0:8080` | JWT audience |
| `MEDICAL_RECORD_ENCRYPTION_KEY` | derived from `JWT_SECRET` in development | Base64-encoded 32-byte AES key for medical-record encryption |
| `RESEND_API_KEY` | empty | Enables email sending when set |
| `RESEND_FROM_EMAIL` | `Vita <noreply@vita.local>` | Sender email address |
| `APP_BASE_URL` | `http://localhost:8080` | Base URL used in email links |
| `APP_NAME` | `Vita` | App name used by email service |

Typical local overrides:

```bash
PORT=3000 DB_PATH=jdbc:sqlite:dev.db ./gradlew :server:run
```

### Development User

When running in development mode with auth bypass enabled, a test user is automatically seeded:

| Field        | Value             |
|--------------|-------------------|
| **Email**    | `dev@example.com` |
| **Password** | `password123`     |
| **User ID**  | `dev-user-001`    |

This allows you to test authenticated endpoints without creating an account.

### Auth Bypass Behavior

When `DEV_AUTH_BYPASS` is enabled:
- Protected endpoints accept requests without authentication
- The server logs a warning: `⚠️ DEV AUTH BYPASS is ENABLED`
- Useful for frontend development and API testing

To disable even in development:
```bash
ENV=development DEV_AUTH_BYPASS=false ./gradlew :server:run
```

## Email Configuration

The server uses [Resend](https://resend.com) for sending transactional emails (account activation, password reset).

### Setup

1. Create a free account at [resend.com](https://resend.com)
2. Generate an API key from [resend.com/api-keys](https://resend.com/api-keys)
3. Configure your environment:

```bash
RESEND_API_KEY=re_your_api_key_here
RESEND_FROM_EMAIL=Vita <noreply@yourdomain.com>
APP_BASE_URL=http://localhost:8080
```

### ⚠️ Important: Graceful Degradation

> **If `RESEND_API_KEY` is not set or is blank, the `EmailService` will be `null` and emails will NOT be sent.**
>
> - Registration and password reset requests will still succeed
> - Users will NOT receive verification or reset emails
> - This is intentional to allow local development without email service
> - Check server logs if emails are not being received

For local development without Resend, you can:
- Check the database directly for activation tokens
- Use the development user (`dev@example.com` / `password123`)
- Set up a local mail catcher like [Mailhog](https://github.com/mailhog/MailHog)

## API Endpoints

### Health
- `GET /` - Server greeting
- `GET /api/health` - Health check

### Authentication
- `POST /api/auth/preregister` - Start registration with email/password (sends verification email)
- `GET /api/auth/activate?token=...` - Activate account via email link
- `POST /api/auth/activate` - Activate account with token in body
- `POST /api/auth/login` - Authenticate and receive tokens
- `POST /api/auth/refresh` - Refresh access token
- `POST /api/auth/forgot-password` - Request password reset email
- `POST /api/auth/reset-password` - Reset password with token
- `POST /api/auth/change-password` - Change password (authenticated)
- `POST /api/auth/change-email/request` - Request email change verification code (authenticated)
- `POST /api/auth/change-email/confirm` - Confirm email change with verification code (authenticated)
- `GET /api/auth/profile` - Get current user (authenticated)
- `POST /api/auth/logout` - Revoke refresh tokens (authenticated)
- `POST /api/auth/deactivate` - Deactivate account (authenticated)

### Products
- `GET /api/products` - Get all products (authenticated)
- `GET /api/products/{id}` - Get product by ID (authenticated)

### Appointments
- `GET /api/appointments` - Get confirmed appointments for the current user
- `POST /api/appointments` - Create an appointment

### Medications And Routines
- `GET /api/medications` - List medications for the current user
- `PUT /api/medications/{id}` - Create or update a medication
- `DELETE /api/medications/{id}` - Delete a medication
- `GET /api/medications/logs` - Get medication history
- `POST /api/medications/logs` - Record a medication event
- `GET /api/routines` - List routines for the current user
- `PUT /api/routines/{id}` - Create or update a routine
- `DELETE /api/routines/{id}` - Delete a routine
- `GET /api/routines/occurrence-overrides` - List routine occurrence overrides
- `PUT /api/routines/occurrence-overrides` - Upsert a routine occurrence override
- `GET /api/routines/agenda?date=YYYY-MM-DD` - Build the medication agenda for a day

### Mood
- `GET /api/moods` - Get mood history
- `POST /api/moods` - Create a mood entry

### Medical Records
- `POST /api/medical-records/upload` - Upload a medical record file
- `GET /api/medical-records/user` - List the current user's records
- `PUT /api/medical-records/reupload/{id}` - Replace an existing file
- `GET /api/medical-records/download/{id}` - Download or preview a file
- `PATCH /api/medical-records/rename/{id}` - Rename a record
- `DELETE /api/medical-records/{id}` - Delete a record

## Database

The server uses **SQLite** with SQLDelight-generated queries. By default it stores data in `vita.db` in the working directory.

The schema is split by feature under `server/src/main/sqldelight/com/group8/comp2300/database/`:
- `data/` contains tables for users, auth tokens, products, appointments, clinics, medications, routines, moods, and medical records
- `view/` contains derived query views such as `MasterCalendar`

If you need to inspect or change persistence behavior, start in the relevant `.sq` file rather than looking for a single monolithic schema file

### Seeding

On first run, the database auto-seeds sample products when the product table is empty:
- HIV Self-Test, PrEP Refill, Full STI Panel
- DoxyPEP, Premium Condoms, Lube

## Extending The Server

When adding a new feature, the current flow is:

1. Add or update SQLDelight queries in the relevant `.sq` file under `server/src/main/sqldelight/com/group8/comp2300/database/`.
2. Add or extend domain contracts in `server/src/main/kotlin/com/group8/comp2300/domain/` when the feature needs a stable repository or model boundary.
3. Implement persistence in `server/src/main/kotlin/com/group8/comp2300/data/repository/`.
4. Add a service in `server/src/main/kotlin/com/group8/comp2300/service/` if the route needs orchestration, validation, or encryption logic.
5. Wire dependencies in `server/src/main/kotlin/com/group8/comp2300/di/AppModule.kt`.
6. Register the route in [Application.kt](/home/user/StudioProjects/comp2300/server/src/main/kotlin/com/group8/comp2300/Application.kt) and add tests under `server/src/test/kotlin/`.

## Learning Resources

### Ktor
- [Official Documentation](https://ktor.io/docs/) - Complete Ktor reference
- [Routing Guide](https://ktor.io/docs/routing.html) - How to define and organize routes
- [Application Structure](https://ktor.io/docs/server-application-structure.html) - Best practices for organizing Ktor apps

### Koin Dependency Injection
- [Kotlin Quick Start](https://insert-koin.io/docs/quickstart/kotlin/) - Get started with Koin in 10 minutes
- [Koin GitHub](https://github.com/InsertKoinIO/koin) - Source code and examples
- [Dependency Injection with Koin](https://auth0.com/blog/dependency-injection-with-kotlin-and-koin/) - In-depth guide

### SQLDelight
- [SQLDelight Documentation](https://cashapp.github.io/sqldelight/) - Official docs
- [GitHub](https://github.com/cashapp/sqldelight) - Source and examples

### Best Practices
- [Building Scalable APIs with Ktor](https://jamshidbekboynazarov.medium.com/best-practices-for-building-scalable-apis-with-ktor-and-kotlin-coroutines-1f773f288664)
- [Domain-Driven Design with Ktor](https://blog.jetbrains.com/kotlin/2025/04/domain-driven-design-guide.html)

## Development Tips

1. **Hot Reload**: Use `./gradlew :server:run --continuous` for faster development
2. **Logging**: Ktor uses Logback - configure in `src/main/resources/logback.xml`
3. **Testing**: Run `./gradlew :server:test`; route coverage lives under `server/src/test/kotlin/`
4. **Code Style**: Run `./gradlew spotlessApply` before committing
5. **Static Analysis**: Run `./gradlew detekt` to check code quality
