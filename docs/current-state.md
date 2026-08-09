# Gym CRM — Current State (`training` branch)

> Yeni chat / agent için tek kaynak. Son güncelleme: **2026-08-06** (security + error handling bitti).

---

## Amaç

EPAM Gym CRM isterlerine uygun **Spring Boot REST API** sıfırdan öğrenerek kuruluyor.

- **Kullanıcı kodu yazar** — agent önce öğretir, sonra yönlendirir (bkz. `.cursor/rules/mentoring-juniors.mdc`).
- Sıra: domain → persistence → service → web → security → ops (PDF sırası değil).
- İster: `docs/project-requirements.md` + `docs/Task_*.pdf`.
- `refactor` branch referans; kör kopya yok.
- İlkeler: **YAGNI, KISS**, az kod, önce sil sonra ekle.
- **Fetch:** N+1 için `@EntityGraph` (lazy’yi service’te `@Transactional` ile “açık tutma” anti-pattern’inden kaçın).
- **`@Transactional`:** sadece yazma (veya gerçekten tek transaction gereken çok adımlı yazma). Salt okuma + EntityGraph → service’te tx yok.

---

## Stack

| | |
|--|--|
| Branch | `training` |
| Java | **21** |
| Spring Boot | **4.1.0** |
| DB | H2 in-memory — `application.yml`: port **8081**, `jdbc:h2:mem:gymcrm` |
| JPA | Spring Data JPA |
| Security | `spring-boot-starter-security` — HTTP Basic + BCrypt |
| Actuator | dependency var; yapılandırma yok |
| Build | `.\mvnw.cmd compile` |
| Lombok | Entity’ler + service `@RequiredArgsConstructor` |

---

## Paket yapısı

```
com.example.gymcrm
├── GymCrmApplication
├── config/
│   ├── SecurityConfig           BCrypt + SecurityFilterChain (HTTP Basic)
│   ├── TrainingTypeSeed         @Order(1) — enum → DB seed
│   └── DemoUserSeed             @Order(2) — demo.trainee / demo.trainer
├── entity/                      User, Trainee, Trainer, Training, TrainingType, TrainingTypeName
├── exception/
│   ├── EntityNotFoundException  → 404
│   └── ValidationException      → 400
├── repository/                  5 JPA repo
├── service/
│   ├── GymUserDetailsService    UserDetailsService — ROLE_TRAINEE / ROLE_TRAINER
│   ├── CreatedAccount<T>
│   ├── TraineeService
│   ├── TrainerService
│   ├── TrainingService
│   ├── TrainingTypeService
│   └── UserAccountService
├── utility/                     PasswordGenerator, UsernameGenerator
└── web/
    ├── controller/              Trainee, Trainer, Training, TrainingType, UserAccount
    ├── dto/                     request/response record’lar
    └── error/
        ├── ApiError             JSON error body (status, message)
        └── RestExceptionHandler @RestControllerAdvice
```

**Yok / sonra:** `@Valid` validation, OpenAPI, TransactionIdFilter, anlamlı testler, Actuator/profiles yapılandırması.

---

## Domain

| Entity | Not |
|--------|-----|
| User | firstName, lastName, username, password (**BCrypt hash**), isActive |
| Trainee | 1-1 User; M-N Trainer; dateOfBirth, address |
| Trainer | 1-1 User; M-1 TrainingType (specialization); M-N Trainee |
| Training | trainee, trainer, trainingType, name, date, duration |
| TrainingType | DB satırı; `TrainingTypeName` enum |

Kimlik: **username** (id değil). Username: `First.Last` + suffix; lookup **IgnoreCase**.

Kayıt kuralı: aynı firstName+lastName ile hem trainee hem trainer olamaz.

---

## Repository

| Repo | Methodlar |
|------|-----------|
| `UserRepository` | `findByUsernameIgnoreCase`, `existsByUsernameIgnoreCase` |
| `TraineeRepository` | `findByUserUsernameIgnoreCase`, `existsByUser_FirstNameIgnoreCaseAndUser_LastNameIgnoreCase` |
| `TrainerRepository` | `findByUserUsernameIgnoreCase`, `existsByUser_FirstNameIgnoreCaseAndUser_LastNameIgnoreCase`, `findByUser_IsActiveTrue`, `findByUser_IsActiveTrueAndIdNotIn` |
| `TrainingTypeRepository` | `findByName`, `existsByName` |
| `TrainingRepository` | `findByTrainee_User_UsernameIgnoreCase` (**@EntityGraph** trainer.user, trainingType), `findByTrainer_User_UsernameIgnoreCase` (**@EntityGraph** trainee.user) |

**EntityGraph eksik:** `TraineeRepository.findByUserUsernameIgnoreCase` → `trainers` (getUnassignedTrainers için).

---

## Config

| Sınıf | Rol |
|-------|-----|
| `TrainingTypeSeed` | Startup’ta her `TrainingTypeName` yoksa insert |
| `DemoUserSeed` | `demo.trainee` / `demo.trainer`, password: `password` |
| `SecurityConfig` | POST register public; GET login public; diğer `/api/**` authenticated; HTTP Basic |

---

## Security (Postman)

| Senaryo | Auth | Beklenen |
|---------|------|----------|
| `GET /api/trainees/{username}` | Basic Auth | 200 |
| Aynı endpoint | Auth yok | 401 |
| Yanlış password | Basic Auth | 401 |
| `POST /api/trainees` | **No Auth** | 200 + username/password |
| `GET /api/trainees/nobody` | Basic Auth | 404 + `ApiError` JSON |
| `GET /api/users/login?password=wrong` | No Auth | 400 + `ApiError` JSON |

Demo kullanıcılar: `demo.trainee` / `demo.trainer` — password: `password`.

---

## Web REST

| Controller | Base path | Not |
|------------|-----------|-----|
| `TraineeController` | `/api/trainees` | POST public; profile, trainers, trainings |
| `TrainerController` | `/api/trainers` | POST public; profile, trainings |
| `TrainingController` | `/api/trainings` | POST add training |
| `TrainingTypeController` | `/api/training-types` | GET list |
| `UserAccountController` | `/api/users` | login GET, password PUT |

---

## Service durumu

Tüm Hibernate işlevleri (1–18) service katmanında ✅.

| Service | Not |
|---------|-----|
| `TraineeService` | `@RequiredArgsConstructor`; duplicate trainer name check; custom exceptions |
| `TrainerService` | `@RequiredArgsConstructor`; duplicate trainee name check |
| `TrainingService` | `@RequiredArgsConstructor`; create returns `Training` (controller void — IDE hint OK) |
| `UserAccountService` | BCrypt match/encode; custom exceptions |
| `GymUserDetailsService` | username → UserDetails + role |

**Parking lot:** `getUnassignedTrainers` hâlâ `@Transactional` (EntityGraph sonrası kalkacak). `TrainerService.getTrainings` / `TraineeService.getTrainings` stream filtreleri — sonra gözden geçir.

---

## Tamamlanan refactor maddeleri

| Madde | Durum |
|-------|--------|
| `@RequiredArgsConstructor` services | ✅ |
| BCrypt passwords | ✅ |
| HTTP Basic security | ✅ |
| `EntityNotFoundException` + `ValidationException` + `@RestControllerAdvice` | ✅ |
| Web REST layer | ✅ |
| Duplicate trainee/trainer by name | ✅ |

---

## Teknik borç / sonra

1. `@Valid` + `spring-boot-starter-validation` on DTOs
2. `@EntityGraph` on `TraineeRepository` + `@Transactional` audit
3. Delete Trainee → trainings cascade + User orphan netleştir
4. Unit / integration tests
5. Actuator / profiles / metrics (Spring Boot task)
6. OpenAPI / Swagger

**Kullanıcı planı:** Önce EPAM isterleri komple bitecek; microservices (~5 gün). **AI-native chat** yalnızca vakit bulunursa, öğrenme capstone — bkz. [`future-ai-native.md`](future-ai-native.md).

---

## Sıradaki adımlar

1. **Microservices:** iki küçük Spring Boot app, HTTP ile konuşma (RestClient/WebClient).
2. **Gym-crm’e dönüş (opsiyonel):** `@Valid`, EntityGraph, testler, Actuator.

### Yeni chat açılış

```
gym-crm — docs/current-state.md oku.
Monolith REST + security + error handling bitti. Microservices'e geçiyorum.
Ben yazarım, sen önce öğret sonra yönlendir. YAGNI/KISS.
```

---

## Komutlar

```powershell
cd C:\Users\husey\Documents\GitHub\gym-crm
.\mvnw.cmd compile
.\mvnw.cmd spring-boot:run
```

---

## Docs

| Dosya | Ne |
|-------|-----|
| `current-state.md` | Bu dosya |
| `project-requirements.md` | İster özeti |
| `post-service-refactor.md` | Refactor backlog (çoğu madde tamam) |
| `refactor-branch-structure.md` | Eski referans |
| `future-ai-native.md` | Proje sonu AI chat capstone vizyonu |
| `Task_*.pdf` | Resmi görev |
