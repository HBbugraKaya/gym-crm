# Gym CRM — Current State (`training` branch)

> Yeni chat / agent için tek kaynak. Son güncelleme: **2026-07-23** (service katmanı neredeyse bitti).

---

## Amaç

EPAM Gym CRM isterlerine uygun **Spring Boot REST API** sıfırdan öğrenerek kuruluyor.

- **Kullanıcı kodu yazar** — agent mentorluk eder; istenmeden implement etme.
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
| DB | H2 in-memory — `application.yml` sadece `name` + `jdbc:h2:mem:gymcrm` (minimal) |
| JPA | Spring Data JPA |
| Build | `.\mvnw.cmd compile` |
| Lombok | Entity’lerde |

---

## Paket yapısı

```
com.example.gymcrm
├── GymCrmApplication
├── config/
│   └── TrainingTypeSeed          ApplicationRunner — enum → DB seed
├── domain/                       User, Trainee, Trainer, Training, TrainingType, TrainingTypeName
├── repository/                   5 JPA repo
├── utility/                      PasswordGenerator, UsernameGenerator
└── service/
    ├── CreatedAccount<T>
    ├── TraineeService
    ├── TrainerService
    ├── TrainingService
    └── UserAccountService
```

**Yok:** `web/`, `security/`, custom exception, Actuator, OpenAPI, anlamlı testler.

---

## Domain

| Entity | Not |
|--------|-----|
| User | firstName, lastName, username, password (plaintext), isActive |
| Trainee | 1-1 User; M-N Trainer; dateOfBirth, address |
| Trainer | 1-1 User; M-1 TrainingType (specialization); M-N Trainee |
| Training | trainee, trainer, trainingType, name, date, duration |
| TrainingType | DB satırı; `TrainingTypeName` enum |

Kimlik: **username** (id değil). Username: `First.Last` + suffix; lookup **IgnoreCase**.

---

## Repository

| Repo | Methodlar |
|------|-----------|
| `UserRepository` | `findByUsernameIgnoreCase`, `existsByUsernameIgnoreCase` |
| `TraineeRepository` | `findByUserUsernameIgnoreCase` |
| `TrainerRepository` | `findByUserUsernameIgnoreCase`, `findByUser_IsActiveTrue`, `findByUser_IsActiveTrueAndIdNotIn` |
| `TrainingTypeRepository` | `findByName`, `existsByName` |
| `TrainingRepository` | `findByTrainee_User_UsernameIgnoreCase`, `findByTrainer_User_UsernameIgnoreCase` |

---

## Config

`TrainingTypeSeed` (`ApplicationRunner`): startup’ta her `TrainingTypeName` yoksa insert. Trainer/Training create için şart.

---

## Utility

| Sınıf | Rol |
|-------|-----|
| `PasswordGenerator` | 10 char, `SecureRandom` |
| `UsernameGenerator` | `First.Last` + numeric suffix; `existsByUsernameIgnoreCase` |

**Not:** Lowercase/`normalize` denemesi yapıldı → **geri alındı**. Şu an IgnoreCase + orijinal case.

---

## Service durumu

### `CreatedAccount<T>`
`(T profile, String rawPassword)` — registration cevabı.

### `TraineeService` — tamam (EntityGraph henüz yok)

| Method | Durum |
|--------|--------|
| `create(...)` | ✅ |
| `selectByUsername` | ✅ — `@Transactional` yok |
| `update(...)` | ✅ |
| `deleteByUsername` | ✅ — cascade sonra |
| `updateTrainers` | ✅ |
| `getUnassignedTrainers` | ✅ — şimdilik `@Transactional` (lazy `trainers`); **EntityGraph sonrası tx kalkacak** |
| `getTrainings(from,to,trainerName,type)` | ✅ — stream filtre service’te |

### `TrainerService` — tamam (EntityGraph henüz yok)

| Method | Durum |
|--------|--------|
| `create` / `select` / `update` | ✅ |
| `getTrainings(from,to,traineeName)` | ✅ — stream filtre |

Delete yok. `trainingRepository` → `private final` olsun.

### Kullanıcı notları (parking lot)

- [ ] **`TrainerService.getTrainings`’e sonra tekrar bak** — stream / N+1 / EntityGraph ile gözden geçirilecek (kullanıcı notu).
- [ ] Aynı göz: `TraineeService.getTrainings` stream filtreleri.
### `TrainingService`

| Method | Durum |
|--------|--------|
| `create(trainee, trainer, name, type, date, duration)` | ✅ |

### `UserAccountService`

| Method | Durum |
|--------|--------|
| `matchesCredentials` | ✅ — plain equals; `@Transactional` yok |
| `changePassword` | ✅ — `@Transactional`; managed entity, `save` yok |

---

## Öğrenilen kurallar (agent + kullanıcı)

1. **`@Transactional`:** yazma veya lazy read+sonraki erişim. Tek `find` → gerekmez.
2. **Managed entity update:** session içinde set → commit yeter, `save` opsiyonel.
3. **Specialization:** create’te set; update’te değiştirme.
4. **YAGNI/KISS:** JPQL/`resolveByUsername`/stream-filtre şişirme → reddedildi. En az kod.
5. **Username case:** IgnoreCase bırak; lowercase+normalize deneyi rollback.
6. **Empty list OK:** olmayan trainee training listesi → `[]`, çökmez.
7. **Agent:** kullanıcı yazsın; istenmeden kod yazma / komple method sonra değiştirme.

---

## Hibernate ister map (özet)

| # | İster | Durum |
|---|--------|--------|
| 1–2 | Create Trainer/Trainee | ✅ |
| 3–4 | Credentials match | ✅ `UserAccountService` |
| 5–6 | Select by username | ✅ |
| 7–8 | Password change | ✅ |
| 9–10 | Update profiles | ✅ |
| 11–12 | Activate | ✅ `update(..., active)` |
| 13 | Delete Trainee | ✅ (cascade netleştir) |
| 14 | Trainee trainings + filtre | ✅ service stream |
| 15 | Trainer trainings + filtre | ✅ service stream |
| 16 | Add training | ✅ |
| 17 | Unassigned trainers | ✅ |
| 18 | Update trainers list | ✅ |

---

## Teknik borç / sonra

Tam liste: [`post-service-refactor.md`](post-service-refactor.md) — **service bitince**.

1. BCrypt / `spring-security-crypto` (plaintext şimdi)
2. Domain exceptions + `@ControllerAdvice`
3. Delete Trainee → trainings cascade + User orphan
4. `@RequiredArgsConstructor` services (Lombok)
5. Web REST + validation
6. Security HTTP Basic
7. Tests
8. Actuator / profiles (Spring Boot task)

---

## Sıradaki adımlar

1. **Şimdi:** `@EntityGraph` — `TraineeRepository.findByUserUsernameIgnoreCase` → `attributePaths = {"trainers"}`, sonra `getUnassignedTrainers`’tan `@Transactional` kaldır.
2. **Sonra:** `TrainingRepository` list find’lere graph (`trainer.user`, `trainee.user`, `trainingType`) — getTrainings N+1 / lazy için.
3. Parking lot: `TrainerService.getTrainings` (ve Trainee eşleniği) kullanıcı tekrar bakacak.
4. `private final TrainingRepository` TrainerService’te.
5. Web layer.

### Yeni chat açılış

```
gym-crm — docs/current-state.md oku.
Service işlevleri bitti. Sırada EntityGraph + Transactional audit.
Notum: TrainerService.getTrainings’e sonra tekrar bakacağım.
Ben yazarım, sen mentor ol. YAGNI/KISS.
```---

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
| `post-service-refactor.md` | Service sonrası Lombok/BCrypt/… |
| `refactor-branch-structure.md` | Eski referans |
| `Task_*.pdf` | Resmi görev |
