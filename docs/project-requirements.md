# Gym CRM — Proje İsterleri (tek bakış)

> Kaynaklar: `docs/Task_Spring_Core.pdf`, `Task_Hibernate.pdf`, `Task_Rest.pdf`, `Task_Spring_Boot.pdf`  
> Amaç: Koda bakarken “bu sınıf hangi isteri karşılıyor?” diye hızlı eşlemek.  
> PDF’ler resmi görev metni; bu dosya onların **birleşik özeti** (uygulama notlarıyla).

Modüller birikip üst üste biner: Core → Hibernate → REST → Spring Boot.

---

## 1. Büyük resim

Spor salonu CRM’i:

| Varlık | Rol |
|--------|-----|
| **User** | Ortak hesap (ad, soyad, username, password, active) |
| **Trainee** | Üye profili (doğum tarihi, adres) + User (1-1) |
| **Trainer** | Eğitmen profili (specialization) + User (1-1) |
| **Training** | Bir trainee–trainer seansı |
| **TrainingType** | Sabit katalog (uygulamadan güncellenmez) |

**Kimlik:** İşlemler çoğunlukla **username** ile yapılır (id ile değil).

**Auth (Hibernate/REST notu):** Create Trainee/Trainer hariç tüm işlevler önce username+password doğrulaması ister. (Spring Boot/Security aşamasında HTTP Basic vb. ile karşılanır.)

---

## 2. Spring Core isterleri (`Task_Spring_Core.pdf`)

Odak: Spring container, DAO, in-memory Map (tarihsel ilk modül).

### Servisler

| Servis | İşlevler |
|--------|----------|
| TraineeService | create / update / delete / **select** profile |
| TrainerService | create / update / **select** profile |
| TrainingService | create / **select** training |

### Altyapı notları (Core dönemi)

- Annotation veya Java-based Spring context
- Her entity için DAO; veri `Map` bean’lerinde (ayrı namespace)
- Startup’ta dosyadan initial data (`BeanPostProcessor`)
- DAO → setter injection; Facade → constructor injection
- Unit test + logging
- Hassas veri loglanmaz (password, address, DoB)

### Username / password kuralları (tüm modüllerde geçerli)

Kayıt (create Trainee/Trainer) sırasında:

1. **Username** = `FirstName.LastName` (nokta ile birleştir)
2. Aynı first+last zaten varsa username’e **sıralı numara** ekle (`John.Smith`, `John.Smith1`, …)
3. **Password** = rastgele **10 karakter** string

> Kod karşılığı (bizim projede): `UniqueUsernameGenerator`, `SecurePasswordGenerator` + `UserRepository.existsByUsername…`

---

## 3. Hibernate / persistence isterleri (`Task_Hibernate.pdf`)

Odak: DB şeması + önceki codebase üzerine kalıcı işlevler.

### İşlev listesi

| # | İster | Not |
|---|--------|-----|
| 1 | Create Trainer profile | Username/password Core kuralları |
| 2 | Create Trainee profile | Username/password Core kuralları |
| 3 | Trainee username+password matching | Auth |
| 4 | Trainer username+password matching | Auth |
| 5 | Select Trainer profile **by username** | |
| 6 | Select Trainee profile **by username** | |
| 7 | Trainee password change | |
| 8 | Trainer password change | |
| 9 | Update Trainer profile | |
| 10 | Update Trainee profile | |
| 11 | Activate / De-activate Trainee | Idempotent **değil** |
| 12 | Activate / De-activate Trainer | Idempotent **değil** |
| 13 | Delete Trainee profile **by username** | Hard delete + ilgili training’ler cascade |
| 14 | Get Trainee trainings list | Filtre: from/to date, trainer name, training type |
| 15 | Get Trainer trainings list | Filtre: from/to date, trainee name |
| 16 | Add training | |
| 17 | Get trainers **not assigned** to trainee (by trainee username) | |
| 18 | Update Trainee’s trainers list | |

### Domain / DB kuralları

| Kural | Anlam |
|-------|--------|
| User ↔ Trainee / Trainer | **One-to-one** (parent-child) |
| Trainee ↔ Trainer | **Many-to-many** |
| Training → Trainee, Trainer | FK |
| Training ↔ TrainingType | Ayrı tablolar, ilişki (one-to-many düşün) |
| Training Types | **Sabit liste**; uygulamadan update **yok** |
| Training duration | number |
| Training date, Trainee DoB | Date |
| Is Active | Boolean (User/profile) |
| Her tabloda PK | |
| Transaction | Gereken yerde transaction management |
| Create/Update öncesi | Required field validation |
| Auth | Create hariç tüm fonksiyonlar authentication sonrası |
| Test + logging | Zorunlu |

> Kod karşılığı: `domain/*`, `repository/*`, sonra `service/*`.  
> Training Types: enum + DB seed; REST’te sadece GET list.

---

## 4. REST isterleri (`Task_Rest.pdf`)

Odak: Aynı işlevlerin HTTP API’si (`@RestController`).

| # | Endpoint özeti | Method | Auth |
|---|----------------|--------|------|
| 1 | Trainee registration | POST | Public |
| 2 | Trainer registration | POST | Public |
| 3 | Login | GET | Username+password |
| 4 | Change login (password) | PUT | Auth |
| 5 | Get Trainee profile | GET | Auth |
| 6 | Update Trainee profile | PUT | Auth |
| 7 | Delete Trainee profile | DELETE | Auth |
| 8 | Get Trainer profile | GET | Auth |
| 9 | Update Trainer profile | PUT | Auth |
| 10 | Get not-assigned active trainers | GET | Auth |
| 11 | Update Trainee’s trainer list | PUT | Auth |
| 12 | Get Trainee trainings (criteria) | GET | Auth |
| 13 | Get Trainer trainings (criteria) | GET | Auth |
| 14 | Add training | POST | Auth |
| 15 | Activate/De-activate Trainee | PATCH | Auth |
| 16 | Activate/De-activate Trainer | PATCH | Auth |
| 17 | Get training types | GET | (genelde auth; refactor’da API kurallarına bak) |

### Registration request/response (özet)

**Trainee POST request:** firstName*, lastName*, dateOfBirth?, address?  
**Trainer POST request:** firstName*, lastName*, specialization* (training type)  
**Registration response:** username, password (tek seferlik plaintext)

### Önemli REST notları

1. Username/password üretimi önceki modül kuralları  
2. Aynı kişi hem trainer hem trainee **olamaz**  
3. Create hariç her şey authentication sonrası  
4. Endpoint validation  
5. User 1-1 Trainee/Trainer  
6. Training için REST üzerinden **delete/update yok**  
7. **Username değiştirilemez**  
8. Trainee↔Trainer M:N  
9. Activate/de-activate idempotent değil  
10. Trainee delete = hard delete + training cascade  
11–13. Tip kuralları (duration number, dates, active boolean)  
14. Training types sabit  
15–18. Error handling, unit tests, transactionId logging + REST call logging, Swagger annotations  

> Kod karşılığı: `web/controller`, `web/dto`, `web/error`, `web/filter`, mapper.  
> Bizde sıra: önce service, sonra web.

---

## 5. Spring Boot isterleri (`Task_Spring_Boot.pdf`)

Önceki modül → Spring Boot uygulaması:

1. Spring Boot’a dönüştür  
2. Actuator aç  
3. Birkaç **custom health indicator**  
4. Birkaç **custom metric** (Prometheus)  
5. Ortamlar: **local, dev, stg, prod** (Spring profiles)  
6. Her ortamda **farklı DB özellikleri**  
7. Unit test + logging  
8. Auth kuralı aynı (create hariç)

> Kod karşılığı: `application-*.yml`, `observability/*`, actuator bağımlılıkları. En sonda.

---

## 6. İster → katman eşlemesi (analiz için)

| İster grubu | Domain | Repository | Service | Web | Security | Ops |
|-------------|:------:|:----------:|:-------:|:---:|:--------:|:---:|
| Entity / ilişkiler | ✅ | | | | | |
| Username unique / password gen | | exists | generators + create | | | |
| Select/update/delete by username | | findBy…Username | ✅ | DTO/controller | | |
| Password change / active | | User find | ✅ | ✅ | auth | |
| Training add / list+criteria | | save + custom later | ✅ | ✅ | | |
| Training types catalog | enum + entity | find/exists/findAll | seed + list | GET | | |
| Login / auth gate | | | | | ✅ | |
| Actuator / metrics / profiles | | | | | | ✅ |

---

## 7. Bilinçli sırayla inşa (bu repo — `training`)

| Aşama | Durum (hedef) | Ana ister kaynağı |
|-------|---------------|-------------------|
| 1 Domain | ✅ | Hibernate ilişkiler + Core model |
| 2 Persistence (yml + repos) | ✅ (minimal) | Hibernate select-by-username, types |
| 3 Generators | ✅ | Core username/password |
| 4 Service | ✅ | Hibernate işlev 1–18 |
| 5 Web + errors | ✅ (validation `@Valid` sonra) | REST 1–17 |
| 6 Security | ✅ | Auth notları — HTTP Basic + BCrypt |
| 7 OpenAPI / logging filter | sonra | REST notes |
| 8 Actuator / metrics / profiles | sonra (dependency var) | Spring Boot task |

---

## 8. Hızlı “kodda ara” ipuçları

- “By username” görürsen → `*Repository.findByUserUsername…` veya `UserRepository.findByUsername…`  
- “Already exists … suffix” → `existsByUsername` + generator  
- “Constant training types” → enum + seed; **update API yok**  
- “Criteria from/to …” → service criteria + (ileride) custom training query  
- “Hard delete + cascade trainings” → JPA cascade / orphan kuralları + delete use-case  
- “Not assigned trainers” → özel trainer sorgusu (service aşamasında)

Resmi metin için her zaman `docs/Task_*.pdf` dosyalarına dön.
