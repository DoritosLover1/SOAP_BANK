# application.properties'e Eklenecekler

Her iki serviste de (`account-service` ve `transaction-service`) ekle:

```properties
# Actuator endpoint'lerini aç (health, info, prometheus)
management.endpoints.web.exposure.include=health,info,prometheus
management.endpoint.health.show-details=always
management.endpoint.prometheus.enabled=true
management.metrics.export.prometheus.enabled=true

# HPA'nın CPU'ya göre karar verebilmesi için pod'lara doğru kaynak etiketi verilmesini
# kolaylaştırır; ayrıca Grafana dashboard'larında servis adı ile ayrım yapılmasını sağlar
management.metrics.tags.application=${spring.application.name}
```

`spring.application.name` her iki serviste de zaten set edilmiş olmalı (değilse ekle):

```properties
# account-service için
spring.application.name=account-service

# transaction-service için
spring.application.name=transaction-service
```

Test etmek için servisi ayağa kaldırıp şu URL'i tarayıcıda aç:
```
http://localhost:8081/actuator/prometheus
```
Metrik satırları (örn. `jvm_memory_used_bytes`, `http_server_requests_seconds_count`) görüyorsan doğru çalışıyor demektir.
