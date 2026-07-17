# Spring Boot Gym CRM

Java 21 and Spring Boot REST API for managing trainees, trainers, assignments and training sessions.

## Architecture

The application keeps the existing layered design and lets Spring Boot own infrastructure bootstrap:

- `web`: REST controllers, DTO mapping, error handling and transaction-ID filtering
- `facade`: coordination boundary retained from the previous module
- `service`: use cases, authentication and transaction boundaries
- `repository`: JPA persistence through `EntityManager`
- `domain`: trainees, trainers, users, training types and trainings
- `observability`: Actuator health indicators and low-cardinality Micrometer metrics

The migration deliberately does not rewrite repositories as Spring Data interfaces, introduce JWT,
Flyway, Docker or a metrics server. Those changes are not required by this module and would expand the
solution without improving the requested behavior.

Security is provided by Spring Security with stateless HTTP Basic authentication and BCrypt password
hashing.

## Build and run

Requirements:

- JDK 21 or newer

Run all tests and the coverage gate:

```powershell
./mvnw.cmd clean verify
```

Run with the default `local` profile:

```powershell
./mvnw.cmd spring-boot:run
```

Or build and run the executable jar:

```powershell
./mvnw.cmd clean package
java -jar target/spring-boot-gym-crm-1.0.0-SNAPSHOT.jar
```

Select another environment with `--spring.profiles.active=dev`, `stg` or `prod`.

## Environment profiles

Each profile has independent database properties:

| Profile | Database | Schema strategy | Intended use |
|---|---|---|---|
| `local` | in-memory H2 `gymcrm-local` | `create-drop` | local development and tests |
| `dev` | file H2 `gymcrm-dev` | `update` | persistent developer data |
| `stg` | PostgreSQL from `GYMCRM_STG_DB_*` | `validate` | staging |
| `prod` | PostgreSQL from `GYMCRM_PROD_DB_*` | `validate` | production |

Staging variables:

- `GYMCRM_STG_DB_URL`
- `GYMCRM_STG_DB_USERNAME`
- `GYMCRM_STG_DB_PASSWORD`

Production uses the equivalent `GYMCRM_PROD_DB_URL`, `GYMCRM_PROD_DB_USERNAME` and
`GYMCRM_PROD_DB_PASSWORD` variables. No staging or production credentials are stored in the repository.

## Authentication

Trainee and trainer registration (`POST /api/v1/trainees`, `POST /api/v1/trainers`) are public. Every
other `/api/**` endpoint requires HTTP Basic credentials validated by Spring Security. Passwords are
stored with BCrypt; registration still returns the one-time plaintext password in the response body.
Profile-specific operations verify that the authenticated username matches the requested profile.
Inactive users can still authenticate because `UserDetails.isEnabled()` is always true.

Actuator endpoints (`/actuator/**`) are public operational endpoints and do not require gym credentials.
Unauthorized API requests return JSON `ApiError` responses with `WWW-Authenticate: Basic realm="gym-crm"`.

## Actuator and Prometheus

The application exposes only the required operational endpoints:

- `/actuator/health`
- `/actuator/info`
- `/actuator/prometheus`

Custom health components verify that exactly one environment profile is active and that the required training-type
catalog is ready. Extra non-environment profiles are allowed. Local and dev show health details; stg and prod return
only aggregate status.

Custom business metrics:

- `gymcrm.profiles.created`, tagged only with `type=trainee|trainer`
- `gymcrm.trainings.created`

Prometheus exports these counters as `gymcrm_profiles_total` and `gymcrm_trainings_total`. Counters are incremented
only after the related database transaction commits.

## Logging

Every request receives or reuses a canonical `X-Transaction-Id`, which is propagated through MDC and returned in
the response. Logs include method, path, status and duration. Authorization values, passwords, bodies, addresses
and dates of birth are never logged. Application logging is `DEBUG` in local/dev and `INFO` in stg/prod.

## Tests

The suite combines unit tests, controller tests and a Spring Boot/H2/MockMvc integration flow. It verifies the
application bootstrap, automatic filter registration, authentication behavior, custom health indicators,
Prometheus metrics and profile-specific database properties. JaCoCo fails the build below 80% line coverage.
