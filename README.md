# API Gateway Lab — Edge Service (Spring Cloud Gateway)

## Описание

**API Gateway Lab** — edge-сервис, реализующий паттерн API Gateway
на базе Spring Cloud Gateway (WebFlux).

Gateway выступает единым входным слоем для клиентских приложений и backend-сервисов,
инкапсулируя инфраструктционную логику работы с HTTP-трафиком.

Проект сфокусирован на:

* маршрутизации запросов,
* централизованной безопасности,
* защите от перегрузки (rate limiting),
* отказоустойчивости (circuit breaker),
* стабильном error-контракте,
* трассировке и логировании запросов.

---

## Цель проекта

Проект используется для:

* демонстрации корректной реализации паттерна API Gateway,
* практики edge-паттернов (Security, Rate Limiting, Circuit Breaker),
* изучения Spring Cloud Gateway + WebFlux,
* использования как эталонного Gateway-шаблона,
* подготовки к архитектурным и backend-интервью.

---

## Технологии

* Java 21
* Spring Boot 3.5.x
* Spring Cloud Gateway (WebFlux)
* Reactor (Mono / Flux)
* Resilience4j (Circuit Breaker)
* Spring Boot Actuator
* Lombok
* Gradle

---

## Архитектура проекта

```text
com.project.apigatewaylab
├── filter          // GlobalFilters (traceId, security, rate limiting)
├── error           // error-контракт и обработка ошибок
├── fallback        // fallback endpoints для circuit breaker
├── config          // конфигурация gateway и resilience
└── ApiGatewayApplication
```

Gateway — инфраструктурный слой, а не backend-сервис, в нем отсутствуют:

* controller’ы бизнес-API
* service / repository
* DTO доменной модели

---

## Реализованный функционал

* Декларативная маршрутизация запросов
* Управление URL namespace (StripPrefix)
* Edge-логирование запросов и ответов
* Генерация и прокидывание TraceId
* Централизованная проверка Authorization
* In-memory Rate Limiting
* Circuit Breaker на уровне маршрута
* Fallback при недоступности downstream
* Единый error-контракт
* Корректные HTTP-статусы ошибок
* Health-endpoint для мониторинга

---

### TraceId и логирование

Gateway:

* принимает X-Trace-Id или генерирует новый,
* прокидывает traceId downstream,
* возвращает traceId клиенту,
* логирует входящие запросы и ответы,
* измеряет latency обработки.

TraceId присутствует во всех логах и error-ответах.

---

### Security (edge-уровень)

Gateway реализует минимальную edge-безопасность:

* обязательное наличие заголовка Authorization
* централизованная проверка для всех маршрутов
* отказ при отсутствии заголовка

Gateway не является Auth-сервером и не содержит OAuth/JWT-логики.

---

### Rate Limiting

Для защиты от перегрузки реализован in-memory rate limiting.

#### Характеристики:

* алгоритм Token Bucket
* лимит по IP клиента
* поддержка burst-нагрузки
* отказ с HTTP-статусом 429 Too Many Requests

Rate limiting реализован на уровне Gateway
и предотвращает избыточную нагрузку на downstream-сервисы.

---

### Circuit Breaker и отказоустойчивость

Для защиты от недоступных downstream-сервисов реализован Circuit Breaker.

Поведение:

* отслеживание ошибок вызовов downstream
* автоматическое открытие circuit при превышении порога ошибок
* временный запрет запросов к недоступному сервису
* возврат fallback-ответа без падения Gateway

Используется Resilience4j.

---

### Error-контракт

Все ошибки возвращаются в стабильном JSON-формате, независимом от downstream:

```text
{
  "errorCode": "RATE_LIMIT_EXCEEDED",
  "message": "Превышен лимит запросов",
  "traceId": "fc0989e8acd84125b547ed876400a818"
}
```

#### Особенности:

* предсказуемые **errorCode**
* корректные HTTP-статусы
* отсутствие stacktrace и внутренних деталей
* обязательный **traceId**

---

### Порядок обработки запроса
```text
Client
↓
TraceId + Logging (GlobalFilter)
↓
Security (GlobalFilter)
↓
Rate Limiting (GlobalFilter)
↓
Routing + Circuit Breaker (Route Filter)
↓
Downstream / Fallback
↓
Response with traceId
```
---