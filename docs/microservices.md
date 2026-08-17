# Gym CRM microservices

The trainer workload and trainee report services receive write events over
ActiveMQ. Query endpoints stay on REST.

## Services

| Service | Port | Responsibility |
|---|---:|---|
| `spring-boot-gym-crm` | 8080 | Main REST API and source of training changes |
| `gym-crm-discovery-service` | 8761 | Eureka service registry |
| `trainer-workload-service` | 8091 | In-memory monthly trainer workload summaries |
| `trainee-report-service` | 8092 | In-memory trainee deletion report receiver |

The main application publishes an `ADD` or `DELETE` workload event after a
training is created or cancelled, and a trainee deletion report after a
trainee profile is removed. Both writes use Spring JMS (`JmsTemplate` /
`@JmsListener`) with JSON text messages:

- queue `gym.trainer.workload`
- queue `gym.trainee.deletion-report`
- JMS property `transactionId` (same value as `X-Transaction-Id`)
- logical Jackson type ids `TrainerWorkloadRequest` and
  `TraineeDeletionReportRequest` (not fully qualified class names)

If the broker is unavailable, the main API returns a structured
`503 Service Unavailable` response. If a consumer is down, the message stays
on the queue until a consumer starts.

Invalid messages (missing required fields, or a business rule such as deleting
more workload than exists) are moved to a dead-letter queue:

- `gym.trainer.workload.dlq`
- `gym.trainee.deletion-report.dlq`

Unexpected listener failures are redelivered up to three times, then ActiveMQ
sends them to its default DLQ. Consumers use a queue (not a topic) with
listener concurrency `1` to `3` and queue prefetch `1`, so extra instances compete
for messages. The in-memory stores are still per process: GET results reflect
the instance that processed those messages.

Training cancellation is exposed by the main service as
`DELETE /api/v1/trainings/{trainingId}`. The training ID is included in the
trainee and trainer training-list responses.

The report payload contains only the trainee identity and active status;
passwords are never sent downstream. The report receiver still accepts REST
deletion events only from a JWT with the `ROLE_TRAINEE` authority, and its
PII-containing GET endpoint is not exposed to ordinary application roles.

## Workload service endpoints

With a valid JWT:

```text
POST /api/v1/trainer-workloads
GET  /api/v1/trainer-workloads/{trainerUsername}?year=2026&month=8
GET  /api/v1/trainer-workloads/{trainerUsername}/summary
```

POST remains for Swagger or manual testing. The main CRM application does not
call it; gym-crm publishes to `gym.trainer.workload` instead.

The message / POST body contains:

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

`actionType` is `ADD` or `DELETE`. The service keeps a concurrent in-memory
structure grouped by trainer, year, and month.

## ActiveMQ profiles

Queue names are shared. Broker connection is per profile:

- `local`: embedded in-memory broker (`vm://localhost`), so a single service
  starts without Docker. Inter-service messaging needs a shared broker; use
  the `dev` profile, or override `spring.activemq.in-memory=false` and
  `spring.activemq.broker-url`.
- `dev`: `ACTIVEMQ_BROKER_URL` (default `tcp://localhost:61616`)
- `stg` / `prod`: required `ACTIVEMQ_BROKER_URL`, optional
  `ACTIVEMQ_USER` / `ACTIVEMQ_PASSWORD`

Do not log broker passwords, JWTs, or message payloads that include names
beyond the username identifier already used in application logs.

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

The `local` profile starts an embedded broker inside each process. To let
gym-crm, workload, and report share one broker, run ActiveMQ on `61616` and
start the services with the `dev` profile (or set `ACTIVEMQ_BROKER_URL` and
`spring.activemq.in-memory=false`).
