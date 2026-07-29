# Spring Boot Gym CRM

Java 21 and Spring Boot REST API for managing trainees, trainers, assignments and training sessions.

## Architecture

The application uses a small layered design and lets Spring Boot own framework concerns:

- `web`: REST controllers, DTO mapping, OpenAPI metadata and transaction-ID filtering
- `bootstrap`: startup data initialization
- `service`: use cases, transaction boundaries and method-level authorization
- `repository`: Spring Data JPA derived queries and focused `@EntityGraph` declarations
- `security`: Spring Security JWT authentication, BCrypt password hashing and login protection
- `domain`: trainees, trainers, users, training types and trainings
- `observability`: Actuator health indicators and low-cardinality Micrometer metrics

There are no custom authentication principals, repository implementations or framework-level error wrappers.
Flyway and Docker are intentionally out of scope.

## Build and run

Requirements:

- JDK 21 or newer

Run all tests and the 80% line-coverage gate:

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

Trainee and trainer registration (`POST /api/v1/trainees`, `POST /api/v1/trainers`) and login
(`POST /api/v1/auth/login`) are public. Login accepts a username/password pair and returns a one-hour
Bearer JWT. Every other `/api/**` endpoint requires that JWT. Passwords are stored with BCrypt;
registration still returns the one-time plaintext password in the response body. Profile-specific
operations verify that the authenticated username matches the requested profile. Spring Method Security
owns the role and self-access checks.

Three failed login attempts lock an account for five minutes. `POST /api/v1/auth/logout` revokes the
current token, so it cannot be used again before it expires. Set `GYMCRM_JWT_SECRET` to a secret of at
least 32 characters outside the `local` profile. Browser clients are allowed only from the configured
`gymcrm.security.cors.allowed-origins` origins.

Actuator endpoints (`/actuator/**`) are public operational endpoints and do not require gym credentials.
HTTP and validation errors use Spring Boot's standard responses. Unauthorized requests include
`WWW-Authenticate: Basic realm="gym-crm"`.

## OpenAPI and Swagger UI

Springdoc generates an OpenAPI 3 description and an interactive Swagger UI from the Spring MVC endpoints and
`io.swagger.v3.oas` annotations:

- OpenAPI JSON: `/v3/api-docs`
- Swagger UI: `/swagger-ui/index.html`

The documentation declares the API title/version, endpoint tags and the Bearer JWT security scheme.
Profile registration and login operations are documented as public; protected operations reference `bearerAuth`. Documentation
endpoints are public so the API can be explored before credentials are created.

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
when the related service operation succeeds.

## Logging

Every request receives or reuses a canonical `X-Transaction-Id`, which is propagated through MDC and returned in
the response. Logs include method, path, status and duration. Authorization values, passwords, bodies, addresses
and dates of birth are never logged. Application logging is `DEBUG` in local/dev and `INFO` in stg/prod.

## Tests

The suite prefers plain JUnit/Mockito unit tests for business rules, controllers, validation, mapping and filters.
Spring Boot/H2/MockMvc integration tests cover framework wiring, cross-profile authorization and the complete
assignment/training/cascade lifecycle. JaCoCo fails the build below 80% line coverage.
