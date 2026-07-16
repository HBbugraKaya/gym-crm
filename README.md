# Spring REST Gym CRM

Java 21 and Spring MVC REST API for managing trainees, trainers, assignments, training sessions, and the constant training-type catalog. The application uses Spring Core, Spring MVC, and Hibernate without Spring Boot.

## Requirements

- JDK 21 or newer
- A Servlet 6 compatible container such as Tomcat 10.1
- No external database is required for local use; the default profile uses in-memory H2.

## Build and run

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
./mvnw.cmd clean verify
```

Deploy `target/spring-rest-gym-crm-1.0.0-SNAPSHOT.war` to the servlet container. The context path is normally the WAR file name unless the container is configured otherwise. `WebAppInitializer` registers the `DispatcherServlet` and transaction-ID filter programmatically.

## Project structure

- `config`: root Spring context, persistence, transactions, and training-type initialization
- `domain`, `repository`, `service`: the application core inherited from the Hibernate module
- `facade`: the existing coordination boundary kept for compatibility with the previous module
- `web/config`: servlet-container entry point and Spring MVC configuration
- `web/controller`, `web/dto`, `web/mapper`: REST transport layer
- `web/error`, `web/filter`, `web/security`: cross-cutting HTTP concerns

There is intentionally no `application` package or `main` method. This is a classic Spring MVC WAR, so the servlet container starts the application through `WebAppInitializer`.

Controller methods are documented with Swagger 2 `io.swagger.annotations` annotations as required by the task. The project intentionally does not add a Swagger UI or runtime document generator because the task only requires method annotations.

## Authentication

Trainee and trainer registration are public. Every other business endpoint uses HTTP Basic authentication. Registration returns the generated username and password exactly once. Authorization headers and password-bearing payloads are never logged.

Each request accepts an optional canonical UUID in `X-Transaction-Id`. Missing or invalid IDs are replaced, propagated through MDC for service logs, and returned in the response header and error body.

## REST API

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/v1/trainees` | Register a trainee |
| `POST` | `/api/v1/trainers` | Register a trainer |
| `GET` | `/api/v1/auth/login` | Validate Basic credentials |
| `PUT` | `/api/v1/users/{username}/password` | Change a user's password |
| `PATCH` | `/api/v1/users/{username}/status` | Activate or deactivate the underlying user |
| `GET` | `/api/v1/trainees/{username}` | Get a trainee profile and trainers |
| `PUT` | `/api/v1/trainees/{username}` | Replace editable trainee profile fields |
| `DELETE` | `/api/v1/trainees/{username}` | Hard-delete a trainee and cascade trainings |
| `GET` | `/api/v1/trainees/{username}/available-trainers` | List active, unassigned trainers |
| `PUT` | `/api/v1/trainees/{username}/trainers` | Replace trainer assignments |
| `GET` | `/api/v1/trainees/{username}/trainings` | Filter trainee trainings |
| `GET` | `/api/v1/trainers/{username}` | Get a trainer profile and trainees |
| `PUT` | `/api/v1/trainers/{username}` | Replace editable trainer fields; specialization remains read-only |
| `GET` | `/api/v1/trainers/{username}/trainings` | Filter trainer trainings |
| `POST` | `/api/v1/trainings` | Add a training using the trainer's specialization |
| `GET` | `/api/v1/training-types` | List the immutable training-type catalog |

Profile and assignment `PUT` operations use replacement semantics. Registration and training creation are non-idempotent. Repeating a status `PATCH` with the current state returns `409 Conflict`, as required by the task.

## Error handling and logging

Errors use a consistent JSON record containing timestamp, HTTP status, message, request path, transaction ID, and field violations. The global handler maps validation, authentication, missing resources, state conflicts, unsupported methods/media types, and unexpected failures to precise HTTP statuses.

Logging uses `DEBUG`, `INFO`, `WARN`, and `ERROR` levels. Request logs contain only the method, path without query parameters, status, duration, and transaction ID. Passwords, authorization values, request bodies, addresses, and dates of birth are excluded.

## Tests and coverage

```powershell
./mvnw.cmd clean verify
```

The suite combines fast Mockito unit tests, Bean Validation contract tests, filter/error tests, controller and mapper tests, plus Spring MVC/H2/MockMvc integration flows. JaCoCo checks the complete production codebase with no package exclusions and fails the build below 80% line coverage.

## Database configuration

Defaults can be overridden with JVM system properties supplied to the servlet container:

```powershell
$env:CATALINA_OPTS = "-Dgym.db.url=jdbc:postgresql://localhost:5432/gymcrm -Dgym.db.username=gym -Dgym.db.password=secret -Dgym.db.driver=org.postgresql.Driver -Dgym.hibernate.ddl-auto=validate"
```

Add the matching JDBC driver dependency when switching from H2 to another database.
