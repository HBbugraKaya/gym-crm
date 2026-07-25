# `refactor` branch — yapı referansı

> Cross-chat hızlı bakış dosyası. Kaynak: `refactor` @ `a68e279`  
> (`refactor: simplify Spring Boot architecture and modernize OpenAPI`)  
> Bu dosya **hedef kopya değil**; öğrenme yolunda referans harita. Biz `training` üzerinde modern + KISS/YAGNI ile yeniden kuruyoruz.

---

## Stack

| | |
|---|---|
| Java | **21** (LTS) |
| Spring Boot | **3.5.x** |
| Build | Maven Wrapper + JaCoCo (line coverage gate **%80**) |
| Persistence | Spring Data JPA |
| DB | H2 (`local`/`dev`) · PostgreSQL (`stg`/`prod`) |
| Security | HTTP Basic + BCrypt |
| API docs | springdoc OpenAPI 3 |
| Ops | Actuator (`health`, `info`, `prometheus`) |
| Bilinçli dışarıda | JWT, Flyway, Docker |

**Bağımlılıklar:** `web`, `data-jpa`, `validation`, `actuator`, `security`, `micrometer-registry-prometheus`, `springdoc-openapi-starter-webmvc-ui`, `h2`, `postgresql`, test + `spring-security-test`.

---

## Katmanlı paket haritası

Baz paket: `com.example.gymcrm`

```
GymCrmApplication
├── config/          # Boot config, seed, OpenAPI bean
├── domain/          # JPA entity + enum
├── repository/      # Spring Data (+ custom training filtre fragment)
├── service/         # Use-case, @Transactional
│   ├── command/     # Input record’lar (web DTO’dan ayrı)
│   └── criteria/    # Training liste filtreleri
├── generator/       # Username + password üretimi
├── exception/       # Domain/app exception hiyerarşisi
├── security/        # SecurityFilterChain, UserDetails, CurrentUser
├── web/
│   ├── controller/
│   ├── dto/
│   ├── mapper/      # Entity ↔ DTO
│   ├── error/       # @ControllerAdvice + ApiError
│   └── filter/      # X-Transaction-Id / MDC
└── observability/   # Custom health + Micrometer counters
```

**Akış:** `Controller` → `Mapper`/`Command` → `Service` → `Repository` → `Domain`  
Facade yok; controller doğrudan service çağırır.

---

## Domain (`domain/`)

| Tip | Rol |
|-----|-----|
| `User` | Ortak hesap (username, password hash, active, first/last name) |
| `Trainee` | `User` OneToOne; `Trainer` ManyToMany |
| `Trainer` | `User` OneToOne; `TrainingType` specialization; trainees mappedBy |
| `Training` | Trainee + Trainer + TrainingType + name/date/duration |
| `TrainingType` | Katalog entity |
| `TrainingTypeName` | Enum (katalog değerleri) |

---

## Repository (`repository/`)

Tercih: **derived query** + gerektiğinde `@EntityGraph`. Custom/JPQL sadece dinamik training filtresi ve “unassigned trainers” için.

| Interface | Önemli metotlar |
|-----------|-----------------|
| `UserRepository` | `findByUsernameIgnoreCase`, `existsByUsernameIgnoreCase` |
| `TraineeRepository` | `findByUserUsernameIgnoreCase`; graph’li `findDistinctByUserUsernameIgnoreCase` (trainers…) |
| `TrainerRepository` | username find + graph; `findUnassignedActiveTrainers`; username list |
| `TrainingTypeRepository` | `findByName` / `existsByName` |
| `TrainingRepository` | `JpaRepository` + `TrainingRepositoryCustom` |
| `TrainingRepositoryCustom` (+ `Impl`) | `findByTraineeUsername(criteria)`, `findByTrainerUsername(criteria)` |
| `UsernameNormalizer` | trim/normalize yardımcısı |

Seed: `config/TrainingTypeInitializer` (`ApplicationRunner`) — eksik `TrainingType` kayıtlarını açılışta ekler.

---

## Service (`service/`)

Transaction sınırı burada. Profil işlemleri `CurrentUser` ile auth kimliğine bağlanır.

| Service | Use-case özeti |
|---------|----------------|
| `TraineeService` | create, find, update, delete; trainings; unassigned trainers; replace trainers |
| `TrainerService` | create, find, update; trainings |
| `TrainingService` | add training (+ trainee’ye trainer assign) |
| `TrainingTypeService` | `findAll` |
| `UserAccountService` | change password, change active status |
| `CreatedAccount<T>` | kayıt sonrası entity + raw password |

**Commands:** `CreateTrainee/Trainer`, `UpdateTrainee/Trainer`, `AddTraining`  
**Criteria:** `TraineeTrainingCriteria`, `TrainerTrainingCriteria`

**Generators:** `UniqueUsernameGenerator`, `SecurePasswordGenerator`

**Exceptions:** `GymCrmException` ← `EntityNotFoundException`, `ValidationException`, `AuthenticationException`, `ProfileStateException`

---

## Web (`web/`)

API prefix: `/api/v1`

| Controller | Mapping | Not |
|------------|---------|-----|
| `TraineeController` | `/trainees` | POST public; GET/PUT/DELETE profile; available-trainers; trainers; trainings |
| `TrainerController` | `/trainers` | POST public; profile; trainings |
| `TrainingController` | `/trainings` | POST add |
| `TrainingTypeController` | `/training-types` | GET list |
| `UserAccountController` | `/users` | password PUT; status PATCH |
| `AuthenticationController` | `/auth/login` | Basic challenge / login ping |

- **DTO:** request/response record’lar (`*Request`, `*Response`)
- **Mapper:** `GymWebMapper` (tek mapper sınıfı)
- **Error:** `RestExceptionHandler` → `ApiError` + `FieldViolation`
- **Filter:** `TransactionIdFilter` (`X-Transaction-Id` + MDC)

---

## Security (`security/`)

| Sınıf | Görev |
|-------|--------|
| `SecurityConfig` | Filter chain; register public; `/api/**` authenticated; actuator public |
| `GymUserDetailsService` | username → UserDetails |
| `GymUserPrincipal` | Principal sarmalayıcı |
| `RestAuthenticationEntryPoint` | 401 JSON `ApiError` |
| `CurrentUser` | Auth’dan trainee/trainer/username zorunlu alma |

Şifre: BCrypt. Kayıt yanıtında **tek seferlik** plaintext password döner. Inactive user yine authenticate olabilir (`isEnabled` her zaman true — refactor kararı).

---

## Observability (`observability/`)

| Sınıf | Görev |
|-------|--------|
| `ApplicationProfileHealthIndicator` | Tam olarak bir env profili aktif mi |
| `TrainingTypeCatalogHealthIndicator` | Katalog hazır mı |
| `GymCrmMetrics` | `gymcrm.profiles.created` (tag `type`), `gymcrm.trainings.created` — commit sonrası increment |

---

## Config / profiles (`src/main/resources/`)

| Dosya | Profil | DB | `ddl-auto` |
|-------|--------|-----|------------|
| `application.yml` | ortak | — | `open-in-view: false` |
| `application-local.yml` | `local` (default) | H2 mem | `create-drop` |
| `application-dev.yml` | `dev` | H2 file | `update` |
| `application-stg.yml` | `stg` | PostgreSQL (env) | `validate` |
| `application-prod.yml` | `prod` | PostgreSQL (env) | `validate` |

OpenAPI: `config/OpenApiConfig` · UI `/swagger-ui/index.html` · docs `/v3/api-docs`

---

## Test stratejisi (`src/test/java/...`)

- **Ağırlık:** düz JUnit + Mockito (service, controller, mapper, filter, error, generator, security helpers)
- **Az entegrasyon:** `integration/GymCrmApiIntegrationTest` — Actuator/OpenAPI/Security wiring + JPA lifecycle
- Diğer: `DomainModelTest`, profile config, Swagger doc, validation testleri

---

## Bizim `training` yolunda kullanım

1. Yapıyı **aşama aşama** kur (domain → repository → service → web → security → docs/ops).
2. Her aşamada YAGNI: refactor’daki her sınıfı aynı anda taşıma.
3. İyileştirme fırsatları (bilinçli sapma OK):
   - Java 21 LTS tut
   - Facade ekleme
   - Custom repo’yu sadece gerçek dinamik sorgu için kullan
   - Entity Lombok/`equals` tuzaklarına dikkat
4. Commit/PR öncesi bu dosyayla “eksik katman var mı?” diye tara.

---

## Hızlı kontrol komutları

```powershell
# refactor’taki main Java ağacı
git ls-tree -r --full-tree --name-only refactor -- src/main/java

# tek dosya oku
git show refactor:src/main/java/com/example/gymcrm/repository/UserRepository.java

# README mimari özeti
git show refactor:README.md
```
