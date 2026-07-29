# Gym CRM — Over-Engineering İnceleme Bulguları

Branch: `refactor` (en güncel). İnceleme kod okunarak yapıldı; ardından `docs/` altındaki task
PDF'leri (Spring Core, Hibernate, REST, Spring Boot) ile karşılaştırıldı.

## Genel değerlendirme

Proje büyük ölçüde dengeli tasarlanmış: katmanlar temiz, isimlendirme tutarlı, testler ve %80
coverage gate var. Ağır/gereksiz bir soyutlama yığını **yok**. İlk incelemede "fazla" görünen
şeylerin çoğu, task'ların açıkça istediği gereksinimler çıktı. Aşağıda önce bunları (ellenmeyecekler)
ayırdım, sonra gerçekten sadeleştirilebilecek kısımları listeledim.

---

## Task gereği yapılmış — SAÇMA GÖRÜNSE DE ELLENMEYECEK

Bu maddeler doğrudan bir task gereksinimine dayanıyor; ilk bakışta "ağır" görünse de kaldırmak
task uyumunu bozar.

| Öğe | İlgili task | Karar |
|---|---|---|
| `TransactionIdFilter` (transactionId üretimi/propagasyonu, REST çağrı logu) | REST #17.1 + #17.2 (iki seviye logging: transaction-level + rest-call detayları) | **Kalsın** |
| `RestExceptionHandler` geniş exception kapsamı | REST #15 & #18 ("error handling for all endpoints") | **Kalsın** |
| `GymCrmMetrics` + Prometheus sayaçları (commit sonrası artırma dahil) | Spring Boot ("custom metrics using Prometheus") | **Kalsın** |
| `ApplicationProfileHealthIndicator` + `TrainingTypeCatalogHealthIndicator` | Spring Boot ("a few custom health indicators") | **Kalsın** |
| `local/dev/stg/prod` profilleri + ayrı DB property'leri | Spring Boot ("each environment - different db properties") | **Kalsın** |
| Swagger/OpenAPI anotasyonları | REST #19 | **Kalsın** |
| `*TrainingCriteria` record'ları (from/to/isim/tip + validasyon) | Hibernate #14 & #15 (criteria ile filtreleme) | **Kalsın** |
| `TrainingRepositoryCustom` + `Impl` (dinamik JPQL filtre) | Yukarıdaki criteria filtresini karşılıyor | **Kalsın** |
| Authentication + parola eşleşmesi + "kendi profili" kontrolü | Hibernate #2 / REST #3 / Boot notu (auth zorunlu) | **Kalsın** |
| Username/parola üreticileri | Spring Core #7 | **Kalsın** |

### Önceki incelemede "over-engineering" dediğim ama artık geri çektiklerim

- **Test-seam constructor'lar** (`RestExceptionHandler(Clock)`, `TransactionIdFilter(LongSupplier, Supplier)`):
  Bunlar deterministik unit testleri mümkün kılıyor. Task'lar "cover code with unit tests" istiyor;
  kaldırmak mevcut testleri bozar ve test kalitesini düşürür. → **Kalsın.**
- **`GymCrmMetrics` commit-sonrası artırma**: Task-zorunlu metriklere bağlı, doğruluk açısından makul.
  → **Kalsın.**
- **`TransactionIdFilter` kanonik-UUID doğrulaması**: "aynı transactionId downstream servislere
  iletilebilir" gereksinimini (REST #17.1) güvenli karşılıyor. → **Kalsın.**

---

## Gerçekten sadeleştirilebilir (task-dışı)

### 1. `service/command/*` katmanı — web DTO'larını neredeyse 1:1 tekrarlıyor (ANA BULGU)

Hiçbir task bir command/CQRS katmanı istemiyor. 5 command record'u (`CreateTraineeCommand`,
`UpdateTraineeCommand`, `CreateTrainerCommand`, `UpdateTrainerCommand`, `AddTrainingCommand`)
karşılık gelen web request DTO'larıyla neredeyse birebir aynı ve hiçbirinde validasyon/mantık yok —
saf taşıyıcılar. Controller'larda alan alan elle kopyalanıyorlar.

Örn. `CreateTraineeCommand` ≈ `TraineeRegistrationRequest` (+ `active`).

Etki alanı: 5 command dosyası + 3 servis + 3 controller + 6 test dosyası.

> Not: `*Criteria` record'ları buradan ayrı tutuluyor — onlarda gerçek validasyon var, task da
> criteria istiyor. Sadece `command` paketi hedefte.

### 2. (Küçük/opsiyonel) Tekrarlanan "kendi profili mi" kontrolü

`requireSameUser` (TraineeService, TrainerService), `requireOwnAccount` (UserAccountService),
`requireSameTrainer` (TrainingService) — 4 serviste neredeyse aynı `equalsIgnoreCase` bloğu.
Küçük, zararsız bir DRY iyileştirmesi olarak ortak bir yardımcıya taşınabilir. Zorunlu değil.

---

## Yapılan sadeleştirmeler

Seçilen yön: **B** (servisler düz parametre alır; web DTO bağımlılığı yok) + ownership kontrolü
merkezileştirildi. `./mvnw clean verify` (JDK 21) yeşil — tüm testler ve %80 JaCoCo gate geçti.

1. **`service/command` paketi silindi** (5 record). Servisler artık domain / Java tipleriyle çalışıyor;
   controller DTO'dan parametre ayırıp servise iletiyor.
   - `TraineeService.create(firstName, lastName, dateOfBirth, address)`
   - `TraineeService.update(username, firstName, lastName, dateOfBirth, address, active)`
   - `TrainerService.create(firstName, lastName, specialization)`
   - `TrainerService.update(username, firstName, lastName, active)`
   - `TrainingService.addTraining(traineeUsername, trainerUsername, trainingName, trainingDate, durationMinutes)`
   - Kayıt (registration) her zaman `active=true` olduğu için bu default servise taşındı.
   - Katman disiplini: `service` paketi artık `web.dto` import etmiyor.
2. **Ownership kontrolü tek yerde toplandı**: `SelfAccess.require(...)` (mock'lanmayan static
   yardımcı; servis unit testlerinde çalışmaya devam eder). `requireSameUser` / `requireOwnAccount`
   / `requireSameTrainer` içlerinden buna delege ediyor; mesajlar korundu.
3. **Bonus (command kaldırmanın doğal sonucu)**: `AddTrainingCommand`'daki nullable `trainingType` +
   fazladan constructor + `TrainingService.resolveTrainingType` dalı REST'ten hiç erişilmeyen ölü
   koddu (tip her zaman trainer'ın specialization'ından türer — REST #14). Bu dal ve
   `TrainingService`'in `TrainingTypeRepository` bağımlılığı kaldırıldı. Davranış aynı.

Güncellenen testler: `Trainee/Trainer/TrainingServiceTest`, `Trainee/Trainer/TrainingControllerTest`.
