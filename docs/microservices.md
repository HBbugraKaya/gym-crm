# Gym CRM microservices

The final microservices task adds a separate trainer workload service and a
separate Eureka discovery service.

## Services

| Service | Port | Responsibility |
|---|---:|---|
| `spring-boot-gym-crm` | 8080 | Main REST API and source of training changes |
| `gym-crm-discovery-service` | 8761 | Eureka service registry |
| `trainer-workload-service` | 8091 | In-memory monthly trainer workload summaries |
| `trainee-report-service` | 8092 | In-memory trainee deletion report receiver |

The main application sends an `ADD` or `DELETE` workload event after a
training is created or cancelled. The call uses:

- Eureka service discovery (`trainer-workload-service`)
- the caller's JWT as a Bearer token
- the `X-Transaction-Id` header
- a Resilience4j circuit breaker
- a short HTTP connect/read timeout

If the workload service is unavailable or times out, the main API returns a
structured `503 Service Unavailable` response instead of reporting a false
success.

Training cancellation is exposed by the main service as
`DELETE /api/v1/trainings/{trainingId}`. The training ID is included in the
trainee and trainer training-list responses.

Trainee profile deletion invokes the report service through
`POST /api/v1/trainee-deletion-reports`. The report payload contains only the
trainee identity and active status; passwords are never sent downstream.
The report receiver accepts deletion events only from a JWT with the
`ROLE_TRAINEE` authority, and its PII-containing GET endpoint is not exposed
to ordinary application roles.

## Workload service endpoints

With a valid JWT:

```text
POST /api/v1/trainer-workloads
GET  /api/v1/trainer-workloads/{trainerUsername}?year=2026&month=8
GET  /api/v1/trainer-workloads/{trainerUsername}/summary
```

The POST body contains:

```json
{
  "trainerUsername": "coach.one",
  "trainerFirstName": "Coach",
  "trainerLastName": "One",
  "isActive": true,
  "trainingDate": "2026-08-09",
  "trainingDurationMinutes": 60,
  "actionType": "ADD"
}
```

`action` is `ADD` or `DELETE`. The service keeps a concurrent in-memory
structure grouped by trainer, year, and month.

All services use the same `GYMCRM_JWT_SECRET` and `GYMCRM_JWT_ISSUER`
configuration contract. The default `local` profile provides only a
development secret; set both variables for dev, staging, and production.

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
http://localhost:8091/swagger-ui/index.html
http://localhost:8092/swagger-ui/index.html
```

## Run locally

Use JDK 21 or newer.

```powershell
# terminal 1
cd discovery-service
..\mvnw.cmd spring-boot:run

# terminal 2
cd workload-service
$env:GYMCRM_JWT_ISSUER = "gym-crm"
$env:GYMCRM_JWT_SECRET = "local-development-secret-must-be-at-least-32-characters"
..\mvnw.cmd spring-boot:run

# terminal 3
$env:GYMCRM_JWT_ISSUER = "gym-crm"
$env:GYMCRM_JWT_SECRET = "local-development-secret-must-be-at-least-32-characters"
.\mvnw.cmd spring-boot:run

# terminal 4
cd report-service
$env:GYMCRM_JWT_ISSUER = "gym-crm"
$env:GYMCRM_JWT_SECRET = "local-development-secret-must-be-at-least-32-characters"
..\mvnw.cmd spring-boot:run
```

The main application's workload service URL defaults to the Eureka service
name `http://trainer-workload-service`. For a direct local endpoint without
discovery, set `GYMCRM_WORKLOAD_SERVICE_URL` to a reachable service URL and
adjust the client configuration accordingly.
