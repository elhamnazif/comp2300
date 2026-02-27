# Vita Server

Ktor-based backend API for the Vita health application.

## Quick Start

```bash
# Run the server (default port 8080)
./gradlew :server:run

# Run with custom port
SERVER_PORT=3000 ./gradlew :server:run

# Run tests
./gradlew :server:test
```

The server will be available at `http://localhost:8080`

## Connecting from Mobile Devices

### Android Emulator
Works automatically - the emulator can access `localhost:8080` on your machine.

### Real Android Device (via USB)
Run this command once after connecting your device:
```bash
adb reverse tcp:8080 tcp:8080
```
This forwards port 8080 from the device to your development machine.

### iOS Simulator
Works automatically - the simulator can access `localhost:8080`.

### Real iOS Device
Connect your device and Mac to the same Wi-Fi network, then use your Mac's local IP address:
```bash
# Find your Mac's IP
ipconfig getifaddr en0
# Then use http://THAT_IP:8080 in the app
```

## Architecture

The server follows **Clean Architecture** with layered separation:

```
server/
├── src/main/kotlin/com/group8/comp2300/
│   ├── Application.kt              # Main entry point, Ktor configuration
│   ├── di/
│   │   └── AppModule.kt            # Koin DI module
│   ├── database/
│   │   └── DatabaseFactory.kt      # SQLDelight setup, seeding
│   ├── data/
│   │   └── repository/
│   │       └── ProductRepository.kt # Data access layer
│   └── routes/
│       └── Products.kt             # API endpoint handlers
└── src/main/sqldelight/
    └── com/group8/comp2300/database/
        └── ServerDatabase.sq       # Database schema
```

## Technology Stack

| Technology | Purpose | Version |
|------------|---------|---------|
| **Ktor** | Web framework | 3.4.0 |
| **Netty** | Server engine | (via Ktor) |
| **SQLDelight** | Database ORM | 2.2.1 |
| **SQLite** | Embedded database | JDBC driver |
| **Koin** | Dependency injection | 4.x |
| **Kotlinx Serialization** | JSON handling | (via Ktor) |

## Development Mode

The server supports a development mode with authentication bypass for easier testing:

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `ENV` | (unset) | Set to `development` to enable dev mode |
| `DEV_AUTH_BYPASS` | `true` (in dev) | Set to `false` to disable auth bypass |
| `JWT_SECRET` | `dev-secret-key-change-in-production` | JWT signing secret |
| `JWT_REALM` | `comp2300` | JWT realm |
| `JWT_ISSUER` | `http://0.0.0.0:8080` | JWT issuer |
| `JWT_AUDIENCE` | `http://0.0.0.0:8080` | JWT audience |
| `RESEND_API_KEY` | (empty) | Resend API key for sending emails |
| `RESEND_FROM_EMAIL` | `Vita <noreply@vita.local>` | Sender email address |
| `APP_BASE_URL` | `http://localhost:8080` | Base URL used in email links |

### Development User

When running in development mode with auth bypass enabled (`ENV=development`), a test user is automatically seeded:

| Field | Value |
|-------|-------|
| **Email** | `dev@vita.local` |
| **Password** | `devpassword1` |
| **User ID** | `dev-user-001` |

This allows you to test authenticated endpoints without creating an account.

### Auth Bypass Behavior

When `DEV_AUTH_BYPASS` is enabled (default in development):
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
- Use the development user (`dev@vita.local` / `devpassword1`)
- Set up a local mail catcher like [Mailhog](https://github.com/mailhog/MailHog)

## API Endpoints

### Health
- `GET /` - Server greeting
- `GET /api/health` - Health check

### Authentication
- `POST /api/auth/register` - Create new account (legacy, use preregister instead)
- `POST /api/auth/preregister` - Start registration with email/password (sends verification email)
- `GET /api/auth/activate?token=...` - Activate account via email link
- `POST /api/auth/activate` - Activate account with token in body
- `POST /api/auth/login` - Authenticate and receive tokens
- `POST /api/auth/refresh` - Refresh access token
- `POST /api/auth/forgot-password` - Request password reset email
- `POST /api/auth/reset-password` - Reset password with token
- `GET /api/auth/profile` - Get current user (authenticated)
- `POST /api/auth/logout` - Revoke refresh tokens (authenticated)
- `POST /api/auth/complete-profile` - Complete profile after activation (authenticated)

### Products
- `GET /api/products` - Get all products (authenticated)
- `GET /api/products/{id}` - Get product by ID (authenticated)

## Database

The server uses **SQLite** with file storage (`vita.db` in working directory).

### Schema (ServerDatabase.sq)

```sql
CREATE TABLE ProductEntity (
    id TEXT NOT NULL PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    price REAL NOT NULL,
    category TEXT NOT NULL,
    insuranceCovered INTEGER NOT NULL,
    imageUrl TEXT
);
```

### Seeding

On first run, the database auto-seeds with sample products from the shared module:
- HIV Self-Test, PrEP Refill, Full STI Panel
- DoxyPEP, Premium Condoms, Lube

## Adding New Features

### 1. Add a New Route

Create a new file in `routes/`:

```kotlin
// routes/Users.kt
package com.group8.comp2300.routes

import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.koin.ktor.ext.inject

fun Route.userRoutes() {
    get("/api/users") {
        call.respond(mapOf("users" to listOf()))
    }
}
```

Register in `Application.kt`:

```kotlin
routing {
    productRoutes()
    userRoutes()  // Add here
}
```

### 2. Add Database Table

Edit `ServerDatabase.sq`:

```sql
CREATE TABLE UserEntity (
    id TEXT NOT NULL PRIMARY KEY,
    name TEXT NOT NULL
);

selectAllUsers:
SELECT * FROM UserEntity;
```

SQLDelight will generate the query methods automatically.

### 3. Add Repository

Create in `data/repository/`:

```kotlin
class UserRepository(private val database: ServerDatabase) {
    fun getAll(): List<User> =
        database.serverDatabaseQueries.selectAllUsers().executeAsList()
}
```

Register in `AppModule.kt`:

```kotlin
val serverModule = module {
    single<ServerDatabase> { createServerDatabase() }
    single { ProductRepository(get()) }
    single { UserRepository(get()) }  // Add here
}
```

## Learning Resources

### Ktor
- [Official Documentation](https://ktor.io/docs/) - Complete Ktor reference
- [Routing Guide](https://ktor.io/docs/routing.html) - How to define and organize routes
- [Application Structure](https://ktor.io/docs/server-application-structure.html) - Best practices for organizing Ktor apps
- [What's New in Ktor 3.4](https://ktor.io/docs/whats-new-340.html) - Latest features including OpenAPI support

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
3. **Testing**: Use Ktor's test host for endpoint testing (see `ApplicationTest.kt`)
4. **Code Style**: Run `./gradlew spotlessApply` before committing
5. **Static Analysis**: Run `./gradlew detekt` to check code quality

## Next Steps for Your Team

1. **Familiarize** with Ktor routing and Koin DI using the links above
2. **Explore** the existing `Products.kt` route and `ProductRepository.kt`
3. **Add** authentication middleware for protected endpoints
4. **Implement** additional feature routes (bookings, medical records, etc.)
5. **Consider** migrating to PostgreSQL/MySQL for production
