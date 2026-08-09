# Gym CRM — AI-native vizyon (proje sonu hedefi)

> **Durum:** fikir / backlog — **EPAM isterleri tamamen bittikten sonra**, öğrenme amaçlı kişisel capstone.  
> **Öncelik:** Resmi görevler (REST, validation, test, ops vb.) → microservices (plan) → bu vizyon (vakit bulunursa).

---

## Ne istiyoruz?

Klasik CRM: trainee ekle, training ekle, listele → Postman veya UI formları.

**AI-native CRM:** Kullanıcı (salon sahibi, admin, ileride belki trainee) doğal dilde yazar; sistem anlar, mevcut REST API’yi çağırır, gerekirse proaktif mesaj atar.

Bu proje EPAM isterlerini öğrenerek kuruluyor; AI katmanı **kendi eklediğimiz capstone** — resmi PDF isterlerinin yerine geçmez, üstüne biner.

---

## Örnek senaryolar

### 1. Basit komut — trainee ekle

**Kullanıcı:**  
> Ahmet Muhsin Kaya 04.03.2014 yeni trainee ekle

**Beklenen:**  
- İsim, soyisim, doğum tarihi parse edilir  
- `POST /api/trainees` (veya ilgili service) çağrılır  
- Chat’te özet: username, tek seferlik password (veya “kayıt tamam”)

### 2. Sorgu — listele

**Kullanıcı:**  
> demo.trainee’nin traininglerini listele

**Beklenen:**  
- Kimlik / username çözülür  
- `GET /api/trainees/{username}/trainings`  
- Sonuç sohbet dilinde özetlenir

### 3. Proaktif / satış destekli (ileri seviye)

**Sistem (veya zamanlanmış job + chat):**  
> Şu trainingleri alan üyelere mesaj at: “Yardımcı training almak ister misin? %50 indirimli.”

**Kullanıcı cevabı olumluysa:**  
- Training tipi / trainer / tarih netleştirilir (eksikse AI sorar)  
- `POST /api/trainings` veya uygun endpoint  
- Onay mesajı chat’te

---

## Mimari fikir (erken taslak)

```
[ Chat UI ]  ←→  [ AI orchestrator ]  ←→  [ Mevcut gym-crm REST API ]
                      │
                      ├─ intent: add_trainee | list_trainings | add_training | ...
                      ├─ slot filling: firstName, lastName, dateOfBirth, username, ...
                      └─ tool / function calls → TraineeController, TrainingController, ...
```

- **Monolith API kalır** — AI yeni domain yazmaz; var olan endpoint’leri “tool” gibi kullanır.
- **Auth:** Chat oturumu da HTTP Basic veya ileride token; AI kullanıcı adına API çağırır.
- **Microservices öğrenimi** bittikten sonra AI servisi ayrı process olarak da düşünülebilir (`ai-gateway` → `gym-crm`).

---

## Aşamalı yol (öneri)

| Aşama | Ne | Bağımlılık |
|-------|-----|------------|
| **0** | Monolith REST + auth + hatalar | ✅ (şu an) |
| **1** | Tek intent: “trainee ekle” → API çağrısı | REST stabil |
| **2** | Birkaç intent: listele, training ekle, profil getir | OpenAPI veya net endpoint listesi |
| **3** | Basit chat UI (web veya CLI) | 1–2 |
| **4** | Proaktif mesaj / kampanya akışı | messaging + iş kuralları |
| **5** | İndirim, onay, çok adımlı diyalog | 4 + domain genişlemesi gerekirse |

YAGNI: önce **Aşama 1** — tek cümle → tek API çağrısı → chat’te sonuç.

---

## Açık sorular (ileride cevaplanacak)

- Chat kimler için? (sadece admin / trainer / herkes?)
- Proaktif mesaj kanalı: in-app chat mi, SMS/email mi?
- “%50 indirim” iş kuralı API’de yok — pricing/promo domain’i gerekir mi?
- Hangi LLM / API (OpenAI, local, Cursor SDK, vb.) — maliyet ve gizlilik

---

## İlgili dosyalar

- `docs/current-state.md` — güncel monolith durumu  
- `docs/project-requirements.md` — resmi EPAM isterleri (AI bunların dışında)

---

*Not: Bu dosya ürün vizyonudur; her sprint’te implement edilmez. Microservices ve ops konularından sonra capstone olarak ele alınır.*
