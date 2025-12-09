# Outbox 패턴, DLQ 및 Kafka 전환 준비 보고서

> **목표**: 이벤트 처리 안정성 강화 + Kafka 전환 대비 아키텍처 구축

---

## 목차

1. [배경 및 문제 정의](#1-배경-및-문제-정의)
2. [DLQ 아키텍처](#2-dlq-아키텍처)
3. [핵심 구현](#3-핵심-구현)
4. [이벤트 처리 흐름](#4-이벤트-처리-흐름)
5. [모니터링 및 알림](#5-모니터링-및-알림)
6. [운영 가이드](#6-운영-가이드)
7. [Kafka 전환 대비 구조](#7-kafka-전환-대비-구조)
8. [Event Sourcing (주문 도메인)](#8-event-sourcing-주문-도메인)
9. [결론 및 향후 개선](#9-결론-및-향후-개선)

---

## 1. 배경 및 문제 정의

### 1.1 기존 Outbox 패턴의 한계

```
┌─────────────────────────────────────────────────────────────────┐
│ 기존 문제점                                                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  [이벤트 실패]                                                   │
│       ↓                                                         │
│  markAsFailed() → errorMessage 저장                             │
│       ↓                                                         │
│  다음 폴링에서 재시도 (무한 반복 가능)                            │
│       ↓                                                         │
│  문제:                                                          │
│  - 영구적 실패 이벤트가 계속 재시도됨                             │
│  - 실패 이벤트가 정상 이벤트 처리를 지연시킴                       │
│  - 운영자가 실패 상황을 인지하기 어려움                           │
│  - 수동 개입 방법 없음                                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 해결 목표

| 문제 | 해결 방안 |
|------|----------|
| 무한 재시도 | 최대 재시도 횟수(5회) 제한 후 DLQ 이동 |
| 정상 이벤트 지연 | 실패 이벤트를 DLQ로 격리 |
| 운영자 인지 불가 | AlertService로 알림 발송 |
| 수동 개입 불가 | retryFromDlq(), resolveManually() 제공 |

---

## 2. DLQ 아키텍처

### 2.1 전체 흐름

```
┌─────────────────────────────────────────────────────────────────┐
│ Dead Letter Queue (DLQ) 아키텍처                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  [Outbox Event]                                                 │
│       ↓                                                         │
│  OutboxEventProcessor (5초 주기, 50개 배치)                      │
│       ↓                                                         │
│  ┌─────────────┐    성공    ┌─────────────────┐                │
│  │   Handler   │ ────────→ │ markAsProcessed │                │
│  └─────────────┘            └─────────────────┘                │
│       │                                                         │
│       │ 실패                                                    │
│       ↓                                                         │
│  ┌──────────────────────────────────┐                          │
│  │ retryCount < 5?                  │                          │
│  │   YES → incrementRetryAndMarkFailed                         │
│  │         (다음 폴링에서 재시도)                                │
│  │   NO  → moveToDlq()                                         │
│  └──────────────────────────────────┘                          │
│       │                                                         │
│       │ DLQ 이동                                                │
│       ↓                                                         │
│  ┌─────────────────────────────────────────┐                   │
│  │ outbox_event_dlq 테이블                  │                   │
│  │ - 원본 이벤트 정보                       │                   │
│  │ - 실패 사유 (errorMessage)               │                   │
│  │ - 재시도 횟수 (retryCount)               │                   │
│  │ - 해결 상태 (resolved)                   │                   │
│  └─────────────────────────────────────────┘                   │
│       ↓                                                         │
│  AlertService.sendDlqAlert()                                   │
│       ↓                                                         │
│  운영자 확인 → retryFromDlq() 또는 resolveManually()            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 테이블 설계

#### outbox_event (기존)

```sql
CREATE TABLE outbox_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processed_at DATETIME,
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);
```

#### outbox_event_dlq (신규)

```sql
CREATE TABLE outbox_event_dlq (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_event_id BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    error_message TEXT NOT NULL,
    failed_at DATETIME NOT NULL,
    retry_count INT NOT NULL,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at DATETIME,
    resolved_by VARCHAR(100),
    resolution_note TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    INDEX idx_dlq_resolved (resolved),
    INDEX idx_dlq_event_type (event_type),
    INDEX idx_dlq_failed_at (failed_at)
);
```

---

## 3. 핵심 구현

### 3.1 OutboxEventDlq 엔티티

```kotlin
@Entity
@Table(name = "outbox_event_dlq")
class OutboxEventDlq(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val originalEventId: Long,
    val eventType: String,
    val aggregateType: String,
    val aggregateId: String,
    val payload: String,
    val errorMessage: String,
    val failedAt: LocalDateTime = LocalDateTime.now(),
    val retryCount: Int,

    var resolved: Boolean = false,
    var resolvedAt: LocalDateTime? = null,
    var resolvedBy: String? = null,
    var resolutionNote: String? = null
) {
    companion object {
        fun fromOutboxEvent(event: OutboxEvent, errorMessage: String): OutboxEventDlq
    }

    fun resolve(resolvedBy: String, note: String? = null)
    fun toOutboxEvent(): OutboxEvent
}
```

### 3.2 DlqService

```kotlin
@Service
class DlqService(
    private val dlqRepository: OutboxEventDlqRepository,
    private val outboxEventRepository: OutboxEventRepository,
    private val alertService: AlertService,
    @Value("\${outbox.max-retry-count:5}")
    private val maxRetryCount: Int = 5,
    @Value("\${outbox.dlq.alert-threshold:10}")
    private val alertThreshold: Long = 10
) {
    /**
     * 이벤트를 DLQ로 이동
     */
    fun moveToDlq(event: OutboxEvent, errorMessage: String): OutboxEventDlq {
        // 1. DLQ 이벤트 생성 및 저장
        val dlqEvent = OutboxEventDlq.fromOutboxEvent(event, errorMessage)
        val savedDlqEvent = dlqRepository.save(dlqEvent)

        // 2. 원본 이벤트 삭제
        outboxEventRepository.deleteById(event.id)

        // 3. 알림 발송
        alertService.sendDlqAlert(savedDlqEvent)

        // 4. 임계치 체크 및 추가 알림
        checkThresholdAndAlert()

        return savedDlqEvent
    }

    /**
     * DLQ로 이동 필요 여부 판단
     */
    fun shouldMoveToDlq(event: OutboxEvent): Boolean {
        return event.retryCount >= maxRetryCount
    }

    /**
     * DLQ 이벤트 재처리
     */
    fun retryFromDlq(dlqEventId: Long, operatorId: String): Boolean {
        val dlqEvent = dlqRepository.findById(dlqEventId)
            ?: throw IllegalArgumentException("DLQ 이벤트를 찾을 수 없습니다")

        // 새 Outbox 이벤트 생성 (retryCount=0으로 초기화)
        val newEvent = dlqEvent.toOutboxEvent()
        outboxEventRepository.save(newEvent)

        // DLQ 이벤트 해결 처리
        dlqEvent.resolve(operatorId, "재처리를 위해 Outbox로 복원됨")
        dlqRepository.save(dlqEvent)

        return true
    }

    /**
     * DLQ 이벤트 수동 해결
     */
    fun resolveManually(dlqEventId: Long, operatorId: String, note: String)
}
```

### 3.3 OutboxEventProcessor 개선

```kotlin
@Component
class OutboxEventProcessor(
    private val outboxEventService: OutboxEventService,
    private val eventHandlerRegistry: EventHandlerRegistry,
    private val dlqService: DlqService  // 신규 의존성
) {
    /**
     * 실패 처리 - DLQ 이동 또는 재시도
     */
    private fun handleFailure(event: OutboxEvent, errorMessage: String) {
        if (dlqService.shouldMoveToDlq(event)) {
            // 최대 재시도 초과 → DLQ로 이동
            dlqService.moveToDlq(event, errorMessage)
        } else {
            // 재시도 횟수 증가 후 다음 폴링에서 재시도
            outboxEventService.incrementRetryAndMarkFailed(event.id, errorMessage)
        }
    }

    /**
     * 핸들러 없는 이벤트 처리
     */
    private fun handleMissingHandler(event: OutboxEvent, eventType: String) {
        // 핸들러가 없는 이벤트는 즉시 DLQ로 이동
        dlqService.moveToDlq(event, "핸들러를 찾을 수 없습니다: $eventType")
    }
}
```

---

## 4. 이벤트 처리 흐름

### 4.1 정상 처리

```
이벤트 생성 → Outbox 저장 → Processor 폴링 → Handler 처리 → markAsProcessed
```

### 4.2 일시적 실패 (재시도 성공)

```
이벤트 생성 → Outbox 저장 → Processor 폴링 → Handler 실패
                                              ↓
                              incrementRetryAndMarkFailed (retryCount=1)
                                              ↓
                              5초 후 재폴링 → Handler 성공 → markAsProcessed
```

### 4.3 영구적 실패 (DLQ 이동)

```
이벤트 생성 → Outbox 저장 → Processor 폴링 → Handler 실패 (5회 반복)
                                              ↓
                              retryCount >= 5 → moveToDlq()
                                              ↓
                              AlertService.sendDlqAlert()
                                              ↓
                              운영자 확인 → retryFromDlq() 또는 resolveManually()
```

---

## 5. 모니터링 및 알림

### 5.1 AlertService

```kotlin
interface AlertService {
    fun sendDlqAlert(dlqEvent: OutboxEventDlq)
    fun sendDlqThresholdAlert(unresolvedCount: Long, threshold: Long)
}

/**
 * 로깅 기반 알림 서비스 (기본 구현)
 * 운영 환경에서는 SlackAlertService, EmailAlertService 등으로 교체
 */
@Service
class LoggingAlertService : AlertService {
    override fun sendDlqAlert(dlqEvent: OutboxEventDlq) {
        logger.error("""
            |===================== DLQ ALERT =====================
            | Event ID     : ${dlqEvent.originalEventId}
            | Event Type   : ${dlqEvent.eventType}
            | Aggregate    : ${dlqEvent.aggregateType}#${dlqEvent.aggregateId}
            | Retry Count  : ${dlqEvent.retryCount}
            | Error        : ${dlqEvent.errorMessage}
            | Failed At    : ${dlqEvent.failedAt}
            |=====================================================
        """.trimMargin())
    }
}
```

### 5.2 DlqMonitoringScheduler

```kotlin
@Component
class DlqMonitoringScheduler(
    private val dlqService: DlqService,
    private val alertService: AlertService,
    @Value("\${outbox.dlq.alert-threshold:10}")
    private val alertThreshold: Long = 10
) {
    /**
     * 1분마다 DLQ 상태 모니터링
     */
    @Scheduled(fixedDelay = 60000)
    fun monitorDlqStatus() {
        val unresolvedCount = dlqService.countUnresolved()
        if (unresolvedCount >= alertThreshold) {
            alertService.sendDlqThresholdAlert(unresolvedCount, alertThreshold)
        }
    }

    /**
     * 10분마다 DLQ 상세 리포트 로깅
     */
    @Scheduled(fixedDelay = 600000)
    fun logDlqReport() {
        // 이벤트 타입별 통계, 가장 오래된 이벤트 정보 등
    }
}
```

---

## 6. 운영 가이드

### 6.1 DLQ 이벤트 확인

```kotlin
// 미해결 DLQ 이벤트 목록 조회
val unresolvedEvents = dlqService.getUnresolvedEvents()

// 미해결 DLQ 이벤트 수 조회
val count = dlqService.countUnresolved()
```

### 6.2 DLQ 이벤트 재처리

```kotlin
// 문제 해결 후 재처리 (새 Outbox 이벤트로 복원)
dlqService.retryFromDlq(
    dlqEventId = 123,
    operatorId = "admin@example.com"
)
```

### 6.3 DLQ 이벤트 수동 해결

```kotlin
// 재처리 없이 해결 처리 (수동 처리 완료, 더 이상 필요 없음 등)
dlqService.resolveManually(
    dlqEventId = 123,
    operatorId = "admin@example.com",
    note = "수동으로 데이터 보정 완료"
)
```

### 6.4 설정값

| 설정 | 기본값 | 설명 |
|------|--------|------|
| `outbox.max-retry-count` | 5 | 최대 재시도 횟수 |
| `outbox.dlq.alert-threshold` | 10 | 알림 발송 임계치 |

---

## 7. Kafka 전환 대비 구조

### 7.1 현재 vs 목표 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│ 현재: Outbox + 직접 처리                                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  OrderService                                                   │
│       ↓ (same TX)                                               │
│  outbox_event 저장                                              │
│       ↓                                                         │
│  OutboxEventProcessor (5초 주기)                                │
│       ↓                                                         │
│  EventHandler.handle()  ← 직접 로직 처리                        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ 목표: Outbox + Message Publisher (Kafka 전환 준비)               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  OrderService                                                   │
│       ↓ (same TX)                                               │
│  outbox_event 저장                                              │
│       ↓                                                         │
│  OutboxEventPublisher (5초 주기)                                │
│       ↓                                                         │
│  MessagePublisher.publish()  ← 추상화 계층                      │
│       ↓                                                         │
│  ┌─────────────────────────────────────────┐                   │
│  │ 현재: InMemoryMessagePublisher          │                   │
│  │       → ApplicationEventPublisher       │                   │
│  │                                          │                   │
│  │ 전환 시: KafkaMessagePublisher          │                   │
│  │          → KafkaTemplate                │                   │
│  └─────────────────────────────────────────┘                   │
│       ↓                                                         │
│  @EventListener / @KafkaListener                               │
│       ↓                                                         │
│  EventHandler.handle()                                         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 7.2 MessagePublisher 추상화

```kotlin
/**
 * 메시지 발행 추상화 인터페이스
 *
 * Kafka 전환 대비:
 * - 현재: InMemoryMessagePublisher (Spring ApplicationEvent 기반)
 * - 전환 시: KafkaMessagePublisher (KafkaTemplate 기반)
 */
interface MessagePublisher {
    fun publish(topic: String, key: String, payload: Any)
    fun publishBatch(topic: String, messages: List<Message>)
}

data class Message(
    val key: String,
    val payload: Any,
    val headers: Map<String, String> = emptyMap()
)

/**
 * InMemory 구현 (현재)
 */
@Component
@Profile("!kafka")
class InMemoryMessagePublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) : MessagePublisher {

    override fun publish(topic: String, key: String, payload: Any) {
        val event = DomainEvent(topic = topic, key = key, payload = payload)
        applicationEventPublisher.publishEvent(event)
    }
}

/**
 * Kafka 구현 (전환 시)
 */
@Component
@Profile("kafka")
class KafkaMessagePublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) : MessagePublisher {

    override fun publish(topic: String, key: String, payload: Any) {
        kafkaTemplate.send(topic, key, payload)
    }
}
```

### 7.3 CloudEvents 표준 스키마

```kotlin
/**
 * CloudEvents 1.0 표준 기반 이벤트 래퍼
 *
 * Kafka 전환 시 이벤트 스키마 표준으로 사용
 */
data class CloudEvent<T>(
    val specversion: String = "1.0",
    val id: String,               // Snowflake ID (16진수)
    val source: String,           // "/order-service"
    val type: String,             // "io.hhplus.ecommerce.order.completed"
    val subject: String?,         // aggregateId
    val time: Instant = Instant.now(),
    val datacontenttype: String = "application/json",
    val data: T,
    val traceid: String,          // Snowflake 기반 분산 추적 ID
    val correlationid: String? = null
)

/**
 * CloudEvents 이벤트 타입 (네임스페이스 포함)
 */
object CloudEventTypes {
    const val ORDER_CREATED = "io.hhplus.ecommerce.order.created"
    const val PAYMENT_COMPLETED = "io.hhplus.ecommerce.payment.completed"
    // ...
}

/**
 * 토픽 정의
 */
object Topics {
    const val ORDER = "ecommerce.order"
    const val PAYMENT = "ecommerce.payment"
    // ...
}
```

### 7.4 TraceId 전파 (Snowflake 기반)

```
┌─────────────────────────────────────────────────────────────────┐
│ TraceId 전파 흐름                                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. HTTP 요청 수신                                               │
│     └─ TraceIdFilter에서 traceId 생성 (Snowflake)               │
│     └─ MDC.put("traceId", snowflakeId)                         │
│                                                                 │
│  2. 비즈니스 로직 처리                                           │
│     └─ 로그에 자동 포함: [traceId=1A2B3C4D5E6F7890]             │
│                                                                 │
│  3. 이벤트 발행                                                  │
│     └─ CloudEvent.traceid = MDC.get("traceId")                 │
│     └─ Kafka Header: x-trace-id = traceId                      │
│                                                                 │
│  4. Consumer 수신                                                │
│     └─ Header에서 traceId 추출                                  │
│     └─ MDC.put("traceId", headerTraceId)                       │
│                                                                 │
│  5. 문제 발생 시                                                 │
│     └─ traceId로 전체 흐름 추적                                 │
│     └─ snowflakeGenerator.extractTimestamp(traceId) → 생성시점  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**TraceIdFilter 구현:**

```kotlin
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class TraceIdFilter(
    private val snowflakeGenerator: SnowflakeGenerator
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val traceId = request.getHeader("X-Trace-Id")
            ?: snowflakeGenerator.nextId().toString(16).uppercase()

        MDC.put("traceId", traceId)
        response.setHeader("X-Trace-Id", traceId)

        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove("traceId")
        }
    }
}
```

**Snowflake vs UUID 비교:**

| 비교 항목 | UUID | Snowflake |
|----------|------|-----------|
| 크기 | 128bit (36자) | 64bit (16자) |
| 시간 순 정렬 | ❌ 불가 | ✅ 가능 |
| 생성 시점 추출 | ❌ 불가 | ✅ `extractTimestamp()` |
| 로그 가독성 | 낮음 | 높음 |

### 7.5 Kafka 전환 시 필요 작업

| 작업 | 현재 | 전환 시 |
|------|------|---------|
| Profile 변경 | `!kafka` | `kafka` |
| 의존성 추가 | - | `spring-kafka` |
| KafkaMessagePublisher | 미구현 | 구현 필요 |
| KafkaListener 추가 | @EventListener | @KafkaListener |
| 토픽 생성 | 불필요 | Kafka 토픽 생성 필요 |

---

## 8. Event Sourcing (주문 도메인)

### 8.1 Event Sourcing 개요

```
┌─────────────────────────────────────────────────────────────────┐
│ Event Sourcing 아키텍처                                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  [Command]                                                      │
│       ↓                                                         │
│  OrderAggregate.handle(command)                                │
│       ↓                                                         │
│  OrderEvent 생성 (불변)                                          │
│       ↓                                                         │
│  ┌─────────────────────────────────────┐                       │
│  │ order_events (Event Store)          │                       │
│  │ - aggregateId (orderId)             │                       │
│  │ - eventType                         │                       │
│  │ - payload (JSON)                    │                       │
│  │ - version (Optimistic Lock)         │                       │
│  │ - occurredAt                        │                       │
│  └─────────────────────────────────────┘                       │
│       ↓                                                         │
│  MessagePublisher.publish()                                    │
│       ↓                                                         │
│  OrderProjection (Read Model 업데이트)                          │
│       ↓                                                         │
│  ┌─────────────────────────────────────┐                       │
│  │ orders (Read Model)                 │                       │
│  │ - 조회 최적화된 테이블               │                       │
│  └─────────────────────────────────────┘                       │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 8.2 적용 범위

| 도메인 | 적합성 | 이유 |
|--------|--------|------|
| **포인트** | ❌ 부적합 | 금융 데이터, ACID 필수 |
| **결제** | ❌ 부적합 | 외부 PG 연동, 롤백 복잡 |
| **쿠폰** | ⚠️ 제한적 | 발급 이력 테이블로 충분 |
| **주문** | ✅ **적합** | 상태 전이 명확, 감사 추적 필요 |
| **재고** | ⚠️ 제한적 | 동시성 복잡 |

### 8.3 OrderEvent 정의

```kotlin
sealed class OrderEvent {
    abstract val orderId: Long
    abstract val occurredAt: Instant
    abstract val eventType: String

    // 주문 생성
    data class OrderCreated(
        override val orderId: Long,
        val orderNumber: String,
        val userId: Long,
        val totalAmount: Long,
        val discountAmount: Long,
        val finalAmount: Long,
        val usedCouponId: Long?,
        val items: List<OrderItemSnapshot>,
        override val occurredAt: Instant = Instant.now()
    ) : OrderEvent()

    // 주문 확정 (결제 완료)
    data class OrderConfirmed(...)

    // 주문 완료 (배송 완료)
    data class OrderCompleted(...)

    // 주문 취소
    data class OrderCancelled(
        override val orderId: Long,
        val reason: String,
        val cancelledBy: String,
        val refundAmount: Long?,
        override val occurredAt: Instant = Instant.now()
    ) : OrderEvent()

    // 주문 실패
    data class OrderFailed(...)
}
```

### 8.4 OrderAggregate

```kotlin
class OrderAggregate private constructor() {
    var id: Long = 0L
    var orderNumber: String = ""
    var status: OrderStatus = OrderStatus.PENDING
    var version: Int = 0

    // 미발행 이벤트 (저장 대기 중)
    private val _uncommittedEvents = mutableListOf<OrderEvent>()

    companion object {
        // 새 주문 생성
        fun create(orderId: Long, ...): OrderAggregate

        // 이벤트 목록으로부터 상태 재구성
        fun rebuild(events: List<OrderEvent>): OrderAggregate

        // 스냅샷 + 이후 이벤트로 재구성
        fun fromSnapshot(snapshot: OrderSnapshot, events: List<OrderEvent>): OrderAggregate
    }

    // Command 처리 → Event 생성
    fun confirm(paymentId: Long?): OrderEvent
    fun complete(): OrderEvent
    fun cancel(reason: String, cancelledBy: String): OrderEvent
    fun fail(reason: String): OrderEvent

    // Event 적용 (상태 변경)
    private fun apply(event: OrderEvent)
}
```

### 8.5 OrderEventStore

```kotlin
@Service
class OrderEventStore(
    private val orderEventRepository: OrderEventRepository,
    private val messagePublisher: MessagePublisher,
    private val snapshotRepository: OrderSnapshotRepository? = null
) {
    // Aggregate 로드 (이벤트 재생)
    fun load(orderId: Long): OrderAggregate

    // Aggregate 저장 (이벤트 저장 + 발행)
    fun save(aggregate: OrderAggregate): OrderAggregate

    // 편의 메서드
    fun createOrder(...): OrderAggregate
    fun confirmOrder(orderId: Long, paymentId: Long?): OrderAggregate
    fun cancelOrder(orderId: Long, reason: String, cancelledBy: String): OrderAggregate

    // Temporal Query (특정 시점 상태 조회)
    fun loadAtVersion(orderId: Long, targetVersion: Int): OrderAggregate
}
```

### 8.6 CQRS: Read Model Projection

```kotlin
@Component
class OrderProjection(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository
) {
    @EventListener
    @Transactional
    fun handle(domainEvent: DomainEvent) {
        // Event Store의 이벤트를 구독하여
        // Read Model (orders 테이블) 업데이트
        when (payload) {
            is OrderEvent.OrderCreated -> handleOrderCreated(payload)
            is OrderEvent.OrderConfirmed -> handleOrderConfirmed(payload)
            is OrderEvent.OrderCancelled -> handleOrderCancelled(payload)
            // ...
        }
    }
}

// Read Model 재구축 (복구용)
@Component
class OrderProjectionRebuilder(...) {
    fun rebuildOrder(orderId: Long)  // 특정 주문 재구축
}
```

### 8.7 Event Store 테이블

```sql
CREATE TABLE order_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    aggregate_id BIGINT NOT NULL,        -- Order ID
    event_type VARCHAR(100) NOT NULL,    -- OrderCreated, OrderConfirmed, etc.
    payload TEXT NOT NULL,               -- JSON
    version INT NOT NULL,                -- Optimistic Lock
    occurred_at TIMESTAMP NOT NULL,
    metadata TEXT,                       -- traceId, correlationId 등
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    UNIQUE KEY uk_aggregate_version (aggregate_id, version),
    INDEX idx_occurred_at (occurred_at)
);
```

### 8.8 Event Sourcing 이점

| 이점 | 설명 |
|------|------|
| **완벽한 감사 추적** | 모든 상태 변경 히스토리 영구 보존 |
| **Temporal Query** | 특정 시점의 주문 상태 조회 가능 |
| **디버깅 용이** | 이벤트 재생으로 원인 분석 |
| **Kafka 연동 자연스러움** | 이벤트 기반 외부 시스템 통합 |
| **CQRS 기반** | Read/Write 모델 분리 가능 |
| **복구 가능** | Event Store로부터 Read Model 재구축 |

---

## 9. 결론 및 향후 개선

### 9.1 구현 완료 항목

| 기능 | 상태 | 설명 |
|------|------|------|
| DLQ 엔티티 | ✅ | OutboxEventDlq + Repository |
| DLQ 서비스 | ✅ | moveToDlq, retryFromDlq, resolveManually |
| Processor 연동 | ✅ | handleFailure, handleMissingHandler |
| 알림 서비스 | ✅ | LoggingAlertService (운영 환경에서 교체) |
| 모니터링 스케줄러 | ✅ | 1분 주기 모니터링, 10분 주기 리포트 |
| MessagePublisher | ✅ | 인터페이스 + InMemoryMessagePublisher |
| CloudEvent | ✅ | CloudEvents 1.0 표준 스키마 |
| TraceIdFilter | ✅ | Snowflake 기반 분산 추적 |
| Event Sourcing | ✅ | 주문 도메인 (OrderEvent, OrderAggregate, OrderEventStore) |
| CQRS Projection | ✅ | OrderProjection, OrderProjectionRebuilder |

### 9.2 이점

| 항목 | 기존 | 개선 후 |
|------|------|---------|
| 실패 이벤트 처리 | 무한 재시도 | 5회 후 DLQ 격리 |
| 정상 이벤트 지연 | 실패 이벤트와 혼재 | 실패 이벤트 격리 |
| 운영자 인지 | 로그 확인 필요 | 즉시 알림 |
| 수동 개입 | 불가 | retryFromDlq, resolveManually |
| Kafka 전환 | 전체 재작성 필요 | Profile 변경만으로 전환 |
| 분산 추적 | 없음 | TraceId 자동 전파 |
| 주문 이력 관리 | DB 상태만 보관 | Event Store로 전체 히스토리 보존 |

### 9.3 향후 개선 방향

1. **Slack/Email 알림 구현**
   - 현재: LoggingAlertService
   - 개선: SlackAlertService, EmailAlertService

2. **DLQ 관리 API**
   - REST API로 DLQ 조회/재처리/해결 기능 제공
   - 운영 대시보드 연동

3. **자동 재처리 정책**
   - 특정 에러 유형은 일정 시간 후 자동 재처리
   - 백오프 전략 적용

4. **메트릭 수집**
   - DLQ 이벤트 수, 이벤트 타입별 통계
   - Prometheus/Grafana 연동

---

## 부록: 관련 파일 구조

```
src/main/kotlin/io/hhplus/ecommerce/
├── common/
│   ├── outbox/
│   │   ├── OutboxEvent.kt                    # Outbox 이벤트 엔티티
│   │   ├── OutboxEventRepository.kt          # 리포지토리 인터페이스
│   │   ├── OutboxEventService.kt             # Outbox 서비스 (incrementRetryAndMarkFailed 추가)
│   │   ├── OutboxEventProcessor.kt           # 이벤트 프로세서 (DLQ 연동)
│   │   ├── EventHandler.kt                   # 핸들러 인터페이스
│   │   ├── EventHandlerRegistry.kt           # 핸들러 레지스트리
│   │   └── dlq/
│   │       ├── OutboxEventDlq.kt             # DLQ 엔티티
│   │       ├── OutboxEventDlqRepository.kt   # DLQ 리포지토리 인터페이스
│   │       ├── OutboxEventDlqJpaRepository.kt
│   │       ├── OutboxEventDlqRepositoryImpl.kt
│   │       ├── DlqService.kt                 # DLQ 핵심 서비스
│   │       ├── AlertService.kt               # 알림 인터페이스 + LoggingAlertService
│   │       └── DlqMonitoringScheduler.kt     # 모니터링 스케줄러
│   ├── messaging/
│   │   ├── MessagePublisher.kt               # 메시지 발행 인터페이스 + Message
│   │   ├── InMemoryMessagePublisher.kt       # Spring Event 기반 구현 (@Profile("!kafka"))
│   │   ├── DomainEvent.kt                    # ApplicationEvent 래퍼
│   │   └── CloudEvent.kt                     # CloudEvents 1.0 스키마 + Types + Topics
│   ├── filter/
│   │   └── TraceIdFilter.kt                  # Snowflake 기반 TraceId 필터
│   └── util/
│       └── SnowflakeGenerator.kt             # Snowflake ID 생성기
│
└── order/
    ├── domain/
    │   └── eventsourcing/                    # Event Sourcing 패키지
    │       ├── OrderEvent.kt                 # 주문 이벤트 sealed class
    │       ├── OrderAggregate.kt             # 주문 Aggregate + Snapshot
    │       ├── OrderEventRepository.kt       # 이벤트 저장소 인터페이스
    │       ├── OrderEventStore.kt            # Event Store 서비스
    │       └── OrderProjection.kt            # CQRS Read Model Projection
    └── infra/
        └── persistence/
            ├── entity/
            │   └── OrderEventJpaEntity.kt    # Event Store JPA 엔티티
            ├── repository/
            │   └── OrderEventJpaRepository.kt # Event Store JPA Repository
            └── adapter/
                └── OrderEventRepositoryImpl.kt # 이벤트 저장소 구현체
```
