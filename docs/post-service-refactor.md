# ⚠️ POST-SERVICE REFACTOR — DO NOT START UNTIL SERVICE LAYER IS DONE

> **Status:** LOCKED — finish all service methods first, then refactor in the order below.  
> **Why this file exists:** capture improvements we agreed on so we don't forget or mix them into learning work.

---

## Gate (must be true before touching anything here)

- [ ] `TraineeService` — complete (incl. trainings list, trainer list)
- [ ] `TrainerService` — complete (incl. trainings list)
- [ ] `UserAccountService` — complete
- [ ] `TrainingService` — complete (add + list criteria)
- [ ] TrainingType DB seed exists

**Until the gate passes: YAGNI. No refactors. Ship features.**

---

## Refactor backlog (priority order)

### 1. Lombok — `@RequiredArgsConstructor` on services

**Problem:** Every service repeats the same constructor boilerplate for `final` fields.

**Target:** `TraineeService`, `TrainerService`, `UserAccountService`, `TrainingService`

```java
@Service
@RequiredArgsConstructor
public class TrainingService {
    private final TrainingRepository trainingRepository;
    private final TraineeRepository traineeRepository;
    // ...
}
```

**Remove:** hand-written constructors. Spring injects via generated constructor.

---

### 2. Lombok — `@Builder` on entity creation (optional)

**Problem:** 6+ setter lines when building `Training` (and similar in create flows).

**Target:** `Training` entity first; evaluate `Trainee`/`Trainer` only if still noisy.

```java
// on Training:
@Builder

Training training = Training.builder()
        .trainee(trainee)
        .trainer(trainer)
        .trainingName(trainingName)
        .trainingType(type)
        .trainingDate(trainingDate)
        .trainingDuration(duration)
        .build();
```

**Skip if:** setters are still readable after service layer is done.

---

### 3. Password encoding (BCrypt)

**Problem:** Passwords stored and compared as plain text.

**Target:**
- `pom.xml` → `spring-security-crypto` only (not full Security yet)
- `PasswordConfig` → `BCryptPasswordEncoder` bean
- Encode on create: `TraineeService`, `TrainerService`
- Match/encode on: `UserAccountService`

**Do not mix with:** HTTP Basic / `SecurityConfig` — that comes with REST layer.

---

### 4. Exceptions

**Problem:** `RuntimeException("... not found")` everywhere.

**Target:** `EntityNotFoundException`, `ValidationException` + `@ControllerAdvice` when web layer starts.

---

### 5. `@Transactional` audit

**Rule we settled on:** only on writes or read+write in same method.

**Already done:** removed from `selectByUsername` on Trainee/Trainer.

**Re-check:** any new select-only methods don't get `@Transactional`.

---

## Explicitly NOT doing (YAGNI)

| Idea | Verdict |
|------|---------|
| Builder on every entity | No — only if create code stays noisy |
| Full `spring-boot-starter-security` before REST | No |
| Custom exception hierarchy before web layer | No |
| DTOs / mappers before controllers | No |

---

## After refactor: sanity check

```powershell
.\mvnw.cmd compile
.\mvnw.cmd test
```

Manual smoke: create trainee → create trainer → add training → login → change password.

---

*Last noted: service layer in progress — `TrainingService.create` done.*
