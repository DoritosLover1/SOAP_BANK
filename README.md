# SOAP_BANK

Çok modüllü, SOAP tabanlı (contract-first) bir banka simülasyon projesi. `account-service` ve `transaction-service` olmak üzere iki bağımsız Spring Boot servisi, aralarında SOAP/WSDL üzerinden haberleşir; performans için Redis cache katmanı, arayüz için de bir React/Vite frontend içerir.

## Mimari

```
frontend (React/Vite)
        |
        v
transaction-service (8082)  --SOAP client-->  account-service (8081)
        |                                            |
        v                                            v
   (transfer/işlem mantığı)                   Redis cache (accounts, TTL: 20s)
```

- **account-service**: Contract-first SOAP web servisi. Hesap bilgilerini yönetir, WSDL üzerinden dışarıya servis sunar. `AccountCacheService` ile Redis üzerinden hesap sorguları cache'lenir (`@Cacheable(value = "accounts")`, TTL 20 saniye).
- **transaction-service**: `account-service`'in WSDL'inden (`wsimport` ile) otomatik üretilen SOAP client kodunu kullanarak hesaplar arası transfer işlemlerini yürütür.
- **frontend**: React + Vite tabanlı arayüz (transfer işlemlerini tetiklemek için).

## Teknoloji Yığını

- Java 21, Spring Boot 4.1.0
- SOAP / JAX-WS (contract-first, WSDL tabanlı), Jakarta namespace (javax değil)
- Maven, `jaxws-maven-plugin` (wsimport ile client kod üretimi)
- Redis (cache, TTL bazlı invalidation)
- React + Vite (frontend)

## Servisler ve Portlar

| Servis | Port | WSDL |
|---|---|---|
| account-service | 8081 | `http://localhost:8081/ws/accounts.wsdl` |
| transaction-service | 8082 | `http://localhost:8082/ws/transactions.wsdl` |

## Kurulum ve Çalıştırma

Redis'in lokalde çalışır durumda olması gerekir (varsayılan port 6379).

```bash
# Terminal 1 - account-service
cd account-service
mvn clean compile
mvn spring-boot:run

# Terminal 2 - transaction-service
cd transaction-service
mvn clean compile
mvn spring-boot:run

# Terminal 3 - frontend
cd frontend
npm install
npm run dev
```

> Not: `transaction-service` ilk derlemede `account-service`'in WSDL'ini okuyarak `src/generated/java` altına SOAP client kodu üretir (`wsimport`). Bu yüzden `account-service`'in derlemeden önce ayakta olması gerekmez ama WSDL'e erişilebilir olması (yani en azından bir kez ayağa kalkmış / erişilebilir olması) gerekir.

## Bilinen Sorunlar ve Çözümleri

### Jakarta / javax namespace uyuşmazlığı
`jaxws-maven-plugin` (v2.6) varsayılan olarak eski `javax.*` paketlerine referans veren client kodu üretiyordu; proje ise Spring Boot 4 / Jakarta EE 9+ ile `jakarta.*` kullanıyor. Çözüm:
- `jaxws-maven-plugin` bloğuna `com.sun.xml.ws:jaxws-tools:4.0.3` bağımlılığı eklenerek plugin'in kendi iç aracı jakarta uyumlu sürüme zorlandı.
- Projeye `jakarta.xml.bind-api` ve `jaxb-runtime` bağımlılıkları eklendi.
- `src/generated/java` altındaki eski (javax tabanlı) üretilmiş dosyalar temizlenip yeniden üretildi.

## Test Senaryoları (SoapUI)

`transaction-service` WSDL'i SoapUI'a import edilerek şu senaryolar test edildi:

| Senaryo | Girdi | Beklenen Sonuç |
|---|---|---|
| Başarılı transfer | Var olan `fromAccount`/`toAccount`, bakiyeden az `amount` | `status: SUCCESS`, `transactionId` döner |
| Yetersiz bakiye | Var olan hesap, bakiyeden fazla `amount` | `status: FAILED`, "Yetersiz bakiye" mesajı |
| Olmayan hesap | Var olmayan `fromAccount` | `status: FAILED`, "Gönderen hesap bulunamadı" mesajı |

## Cache Stratejisi

`account-service` içinde hesap sorguları Redis'te `accounts` isimli cache bölgesinde tutulur, 20 saniyelik TTL ile. İsimlendirme, `@Cacheable(value = "accounts", ...)` ile `RedisCacheConfig`'teki `withCacheConfiguration("accounts", ...)` arasındaki eşleşme üzerinden otomatik çalışır.

## .gitignore Notu

`src/generated/`, `target/`, `.env`, `node_modules/` gibi otomatik üretilen/hassas dosyalar repoya dahil edilmez. Yeni bir ortamda projeyi çalıştırırken `src/generated/java` otomatik olarak `wsimport` tarafından yeniden oluşturulur.
