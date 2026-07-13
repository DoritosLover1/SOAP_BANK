# pom.xml'e Eklenecekler

Her iki serviste (`account-service` ve `transaction-service`) `<dependencies>` bloğuna ekle:

```xml
<!-- Actuator: health check, metrics endpoint'leri sağlar -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Micrometer: metrikleri Prometheus'un anlayacağı formata çevirir -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
    <scope>runtime</scope>
</dependency>
```

Bu iki bağımlılık eklenince `/actuator/prometheus` endpoint'i otomatik olarak (config ile birlikte) açılır ve
Prometheus buradan JVM metrikleri (heap, GC, thread), HTTP istek sayıları/süreleri, ve custom metrikleri toplayabilir.
