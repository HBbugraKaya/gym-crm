# Post-service refactor backlog

> **Status:** Gate geçildi — service + web + security + error handling tamam.  
> **Son güncelleme:** 2026-08-06

---

## Gate ✅

- [x] `TraineeService` — complete (incl. trainings list, trainer list)
- [x] `TrainerService` — complete (incl. trainings list)
- [x] `UserAccountService` — complete
- [x] `TrainingService` — complete (add training)
- [x] TrainingType DB seed exists

---

## Refactor backlog

### 1. Lombok — `@RequiredArgsConstructor` on services ✅

Done on all services including `GymUserDetailsService`, `TrainingTypeService`.

---

### 2. Lombok — `@Builder` on entity creation (optional)

**Status:** skipped — setters still readable. Revisit only if create flows get noisy.

---

### 3. Password encoding (BCrypt) ✅

- `SecurityConfig` → `BCryptPasswordEncoder` bean
- Encode on create: `TraineeService`, `TrainerService`
- Match/encode on: `UserAccountService`

---

### 4. Exceptions ✅

- `EntityNotFoundException`, `ValidationException`
- `web/error/ApiError` + `RestExceptionHandler` (`@RestControllerAdvice`)
- Services and `UserAccountController` use custom exceptions (404 / 400 JSON in Postman)

---

### 5. `@Transactional` audit

**Rule:** only on writes or read+write in same method.

**Done:** removed from `selectByUsername` on Trainee/Trainer.

**Open:** `TraineeService.getUnassignedTrainers` — remove after `@EntityGraph` on `TraineeRepository`.

---

## Still open (YAGNI order)

| Item | Notes |
|------|--------|
| `@Valid` on DTOs | REST task — not started |
| `@EntityGraph` on `TraineeRepository` | N+1 / lazy for `trainers` |
| Delete cascade audit | Trainee delete + trainings + User orphan |
| Tests | only `GymCrmApplicationTests` today |
| Actuator / profiles | dependency in pom; no yml config |
| OpenAPI | not started |

---

## Sanity check

```powershell
.\mvnw.cmd compile
.\mvnw.cmd test
```

Manual smoke (verified 2026-08-06):

- create trainee (No Auth) → profile with Basic Auth → 404 for missing user → wrong login → 400 JSON

---

*Next learning focus: microservices (separate repo or second app). Return here for validation/tests/ops when ready.*
