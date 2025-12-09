# Outbox 패턴 및 DLQ 구현 보고서

> **목표**: 이벤트 처리 안정성 강화 - Dead Letter Queue(DLQ) 도입

---

## 목차

1. [배경 및 문제 정의](#1-배경-및-문제-정의)
2. [DLQ 아키텍처](#2-dlq-아키텍처)
3. [핵심 구현](#3-핵심-구현)
4. [이벤트 처리 흐름](#4-이벤트-처리-흐름)
5. [모니터링 및 알림](#5-모니터링-및-알림)
6. [운영 가이드](#6-운영-가이드)
7. [결론 및 향후 개선](#7-결론-및-향후-개선)

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

## 7. 결론 및 향후 개선

### 7.1 구현 완료 항목

| 기능 | 상태 | 설명 |
|------|------|------|
| DLQ 엔티티 | ✅ | OutboxEventDlq + Repository |
| DLQ 서비스 | ✅ | moveToDlq, retryFromDlq, resolveManually |
| Processor 연동 | ✅ | handleFailure, handleMissingHandler |
| 알림 서비스 | ✅ | LoggingAlertService (운영 환경에서 교체) |
| 모니터링 스케줄러 | ✅ | 1분 주기 모니터링, 10분 주기 리포트 |

### 7.2 이점

| 항목 | 기존 | 개선 후 |
|------|------|---------|
| 실패 이벤트 처리 | 무한 재시도 | 5회 후 DLQ 격리 |
| 정상 이벤트 지연 | 실패 이벤트와 혼재 | 실패 이벤트 격리 |
| 운영자 인지 | 로그 확인 필요 | 즉시 알림 |
| 수동 개입 | 불가 | retryFromDlq, resolveManually |

### 7.3 향후 개선 방향

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
src/main/kotlin/io/hhplus/ecommerce/common/outbox/
├── OutboxEvent.kt                    # Outbox 이벤트 엔티티
├── OutboxEventRepository.kt          # 리포지토리 인터페이스
├── OutboxEventService.kt             # Outbox 서비스 (incrementRetryAndMarkFailed 추가)
├── OutboxEventProcessor.kt           # 이벤트 프로세서 (DLQ 연동)
├── EventHandler.kt                   # 핸들러 인터페이스
├── EventHandlerRegistry.kt           # 핸들러 레지스트리
└── dlq/
    ├── OutboxEventDlq.kt             # DLQ 엔티티
    ├── OutboxEventDlqRepository.kt   # DLQ 리포지토리 인터페이스
    ├── OutboxEventDlqJpaRepository.kt
    ├── OutboxEventDlqRepositoryImpl.kt
    ├── DlqService.kt                 # DLQ 핵심 서비스
    ├── AlertService.kt               # 알림 인터페이스 + LoggingAlertService
    └── DlqMonitoringScheduler.kt     # 모니터링 스케줄러
```
