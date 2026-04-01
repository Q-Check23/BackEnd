# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew clean build

# Run (requires env vars below)
./gradlew bootRun

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "team23.q_check.club.service.ClubServiceTest"
```

**Required environment variables for local run:**
```bash
LOCAL_MYSQL_URL=jdbc:mysql://localhost:3306/qcheck
LOCAL_MYSQL_USER=<user>
LOCAL_MYSQL_PASSWORD=<password>
```

Swagger UI is served at `/swagger-ui.html`.

## Architecture

**Stack:** Spring Boot 3.4.2 · Spring Data JPA · MySQL · Flyway · Lombok · SpringDoc OpenAPI 2.8.5 · JUnit 5 + Mockito

**Domain modules** under `src/main/java/team23/q_check/`:
- `identity` — user management (`/api/users`, `/api/identities`)
- `club` — clubs and membership roles (`/api/clubs`)
- `event` — events, form fields, registrations, QR check-in, attendance (`/api/events`)
- `settlement` — expense tracking (domain models only, no controller yet)
- `common` — shared auth resolution, global error handling, `ApiResponse<T>` wrapper, OpenAPI config

Each module follows the pattern: `domain/` → `controller/`, `service/`, `repository/`, `dto/` (request & response), `entity/` or root-level entity classes.

**Database:** Flyway SQL migrations in `src/main/resources/db/migration/`. `ddl-auto: validate` — schema changes require a new migration file. Key enums in DB: `ClubRole` (OWNER/ADMIN/MEMBER), `RegistrationStatus` (REGISTERED/CANCELED/CHECKED_IN), `FieldType` (TEXT/SELECT/BOOLEAN/NUMBER), `AttendanceMethod`.

## Auth

Header-based dev auth only — no JWT/OAuth2. All secured endpoints read `X-USER-ID` (Long) from the request header. The `@CurrentUserId` annotation on controller parameters is resolved by `CurrentUserIdArgumentResolver`. Missing header → 401, non-parseable value → 400. Swagger UI includes an `X-USER-ID` API key security scheme for testing.

## API Conventions

All responses are wrapped in `ApiResponse<T>` (a Java record with `success`, `code`, `message`, `data`, `timestamp`).

Errors are thrown as `AppException(ErrorCode, message)` and handled globally by `GlobalExceptionHandler`. `ErrorCode` drives the HTTP status (400/401/403/404/409/500).

Permission checks (e.g. ADMIN+ required to create events) are performed in service classes via `ClubAuthorizationService`.

## Testing

Tests use `MockMvcBuilders.standaloneSetup(controller)` — no Spring context loaded. Services and repositories are mocked with Mockito. Test files mirror the main package structure.
