# SOAP_BANK — Kubernetes + Monitoring Kurulum Rehberi

## 0. Ön Koşullar

- Docker
- Minikube (`minikube version` ile kontrol et)
- kubectl
- Helm (Prometheus/Grafana kurulumu için)

```bash
minikube start --cpus=4 --memory=6g
minikube addons enable metrics-server   # HPA için ZORUNLU
```

---

## 1. Kod Tarafında Yapman Gerekenler (deploy'dan ÖNCE)

### 1.1 Actuator/Micrometer ekle
`docs/pom-additions.md` ve `docs/application-properties-additions.md` dosyalarındaki
snippet'leri `account-service` ve `transaction-service`'in `pom.xml` / `application.properties`
dosyalarına ekle.

### 1.2 ⚠️ ÖNEMLİ: Hardcoded `localhost` adreslerini değiştir
Şu an muhtemelen iki yerde `localhost` hardcoded:

- `transaction-service/pom.xml` içindeki `jaxws-maven-plugin` config'inde `wsdlUrl` olarak
  `http://localhost:8081/ws/accounts.wsdl` — bu **sadece build-time kod üretimi** için, cluster'da
  sorun değil çünkü sadece `mvn package` sırasında kullanılıyor.
- **Asıl önemli olan**: `transaction-service` içinde SOAP client'ın runtime'da çağırdığı endpoint
  adresi (muhtemelen bir `@Configuration` sınıfında `BindingProvider.ENDPOINT_ADDRESS_PROPERTY`
  ile set ediliyor, ya da client'ın kendi WSDL'inden geliyor). Bunu ortam değişkeninden
  okuyacak şekille değiştir, örnek:

```java
@Value("${account.service.url:http://localhost:8081/ws/accounts}")
private String accountServiceUrl;
```

Bu sayede k8s manifest'indeki `ACCOUNT_SERVICE_URL` env var'ı devreye girer, cluster içinde
`localhost` yerine `account-service` servis adını kullanır.

### 1.3 Redis bağlantı ayarını kontrol et
`application.properties`'te muhtemelen `spring.data.redis.host=localhost` var. Bunu
`${SPRING_DATA_REDIS_HOST:localhost}` şeklinde env var'a bağımlı yap (k8s manifest'inde bu
zaten `redis` olarak set ediliyor).

---

## 2. Docker İmajlarını Build Et

Minikube'ün kendi Docker daemon'ını kullan (yerel imajları doğrudan cluster'a taşımak için):

```bash
eval $(minikube docker-env)

docker build -f account-service.Dockerfile -t soap-bank/account-service:latest ./account-service
docker build -f transaction-service.Dockerfile -t soap-bank/transaction-service:latest ./transaction-service
docker build -f frontend.Dockerfile -t soap-bank/frontend:latest ./frontend
```

> `transaction-service` build'i sırasında `wsimport` çalışır ve `account-service`'in WSDL'ine
> erişmeye çalışır. Build makinesi (Docker build context) bu adrese erişemeyeceği için, ya
> `src/generated/java` klasörünü önceden lokal makinende üretip build context'e dahil et
> (gitignore'dan geçici çıkar) ya da build'den önce `account-service`'i lokalde `docker run`
> ile ayrı ayağa kaldırıp build makinesinin erişebileceği bir adres ver. En pratik yol: bir kez
> lokalde `mvn clean compile` çalıştırıp üretilen `src/generated` klasörünü Dockerfile'ın
> `COPY src ./src` satırına dahil etmek (bu durumda wsimport adımını Docker build'den tamamen
> çıkarabilirsin).

---

## 3. Kubernetes'e Deploy Et

```bash
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/01-redis.yaml
kubectl apply -f k8s/02-account-service.yaml
kubectl apply -f k8s/03-transaction-service.yaml
kubectl apply -f k8s/04-frontend.yaml
kubectl apply -f k8s/05-hpa.yaml

# Durumu kontrol et
kubectl get pods -n soap-bank
kubectl get hpa -n soap-bank
```

Frontend'e erişim:
```bash
minikube service frontend -n soap-bank
```

---

## 4. Monitoring: Prometheus + Grafana (kube-prometheus-stack)

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

helm install monitoring prometheus-community/kube-prometheus-stack \
  --namespace monitoring --create-namespace
```

`monitoring` release adı verdiğimiz için `06-servicemonitor.yaml`'daki `release: monitoring`
etiketi zaten uyumlu — direkt uygula:

```bash
kubectl apply -f k8s/06-servicemonitor.yaml
```

Grafana'ya eriş:
```bash
kubectl port-forward -n monitoring svc/monitoring-grafana 3000:80
```
Tarayıcıda `http://localhost:3000` — kullanıcı adı `admin`, şifre:
```bash
kubectl get secret -n monitoring monitoring-grafana -o jsonpath="{.data.admin-password}" | base64 --decode
```

Prometheus'a eriş (opsiyonel, sorgu debug için):
```bash
kubectl port-forward -n monitoring svc/monitoring-kube-prometheus-prometheus 9090:9090
```

Grafana'da import edebileceğin hazır dashboard: **JVM (Micrometer) - Dashboard ID: 4701**
(Grafana → Dashboards → New → Import → ID gir).

---

## 5. HPA'yı Test Etmek (Yük Testi)

Otomatik ölçeklendirmeyi canlı görmek için basit bir yük testi:

```bash
kubectl run load-generator -n soap-bank --image=busybox --restart=Never -- \
  /bin/sh -c "while true; do wget -q -O- http://account-service:8081/actuator/health; done"

# Ayrı bir terminalde izle
kubectl get hpa -n soap-bank -w
```

CPU kullanımı %60'ı geçince `account-service` pod sayısının 1'den 2'ye, 3'e çıktığını
canlı göreceksin. Test bitince:
```bash
kubectl delete pod load-generator -n soap-bank
```

---

## Özet: Neyi Nereden Görüyorsun

| Ne | Nerede |
|---|---|
| Pod sayısı otomatik artıyor mu | `kubectl get hpa -n soap-bank -w` |
| Servis logları | `kubectl logs -f deployment/account-service -n soap-bank` |
| JVM/HTTP metrikleri (grafik) | Grafana `localhost:3000` |
| Raw Prometheus metrikleri | `http://localhost:9090` veya pod içinde `/actuator/prometheus` |
| Frontend | `minikube service frontend -n soap-bank` |
