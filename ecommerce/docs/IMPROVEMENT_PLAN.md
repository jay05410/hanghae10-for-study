# 랭킹 & 비동기 시스템 개선 계획서

> **작성일**: 2025-12-09
> **목표**: 피드백 기반 성능 개선 + Kafka 전환 대비 아키텍처 정비
> **원칙**: 현재는 Kafka 미도입, 단 전환을 고려한 구조로 개선

---

## 목차

1. [피드백 요약 및 분석](#1-피드백-요약-및-분석)
2. [Redis Streams vs 현재 ZSET 구조 비교](#2-redis-streams-vs-현재-zset-구조-비교)
3. [Phase 1: 즉시 개선 (성능 튜닝)](#3-phase-1-즉시-개선-성능-튜닝)
4. [Phase 2: DLQ 및 실패 처리](#4-phase-2-dlq-및-실패-처리)
5. [Phase 3: Kafka 전환 대비 구조 개선](#5-phase-3-kafka-전환-대비-구조-개선)
6. [Phase 4: 이벤트 소싱 적용 검토](#6-phase-4-이벤트-소싱-적용-검토)
7. [구현 우선순위 및 로드맵](#7-구현-우선순위-및-로드맵)
8. [참고 자료](#8-참고-자료)

---

## 1. 피드백 요약 및 분석

### 1.1 랭킹 시스템

| 항목 | 현재 상태 | 피드백 | 개선 방향 |
|------|----------|--------|----------|
| 아웃박스 패턴 | Outbox 테이블 → 직접 로직 처리 | 진정한 아웃박스 패턴과 거리 있음 | Kafka 발행 구조로 전환 준비 |
| ZINCRBY 처리 | 이벤트 단건별 ZINCRBY 호출 | 50개 배치 시 150 RTT 발생 가능 | 파이프라이닝으로 1 RTT |
| 키 전략 | 일별/주별/누적 분리 | 메모리 사용량 모니터링 필요 | TTL 관리 + 모니터링 추가 |

### 1.2 쿠폰 발급 시스템

| 항목 | 현재 상태 | 피드백 | 개선 방향 |
|------|----------|--------|----------|
| 중복 체크 | `opsForSet().add()` | `addIfAbsent` 권장 | 기능적으로 동일, 명시성 개선 |
| INCR 로직 | 순번 > maxQuantity 비교 | 중복 거부된 경우 실제 발급 수량 부족 가능 | SADD 성공 후에만 INCR, 실패 시 decrement |
| Worker 처리 | 100ms 주기, 단건 DB insert | DB 부하, 100ms 내 처리 불가 | Bulk insert + 주기 조정 (500ms) |
| 매진 플래그 | Soldout 플래그 사용 | 좋은 전략, 관리자 마감 기능 확장 가능 | 유지 + 기능 확장 |

### 1.3 아키텍처

| 항목 | 현재 상태 | 피드백 | 개선 방향 |
|------|----------|--------|----------|
| 도메인 이벤트 | Spring ApplicationEvent 사용 | MSA에서는 Kafka 이벤트가 대세 | Kafka 전환 대비 추상화 |
| 이벤트 소싱 | 미적용 | 포인트/결제는 위험, 주문에는 적합 | 주문 도메인 우선 검토 |

---

## 2. Redis Streams vs 현재 ZSET 구조 비교

### 2.1 현재 구조: SET + INCR + ZSET

```
┌─────────────────────────────────────────────────────────────────┐
│ 현재 쿠폰 발급 흐름 (SET + INCR + ZSET)                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  [요청] → Soldout 체크 → SADD(중복) → INCR(순번) → ZADD(대기열)   │
│                                                                 │
│  Redis Keys:                                                    │
│  ├─ SET   : ecom:cpn:iss:issued:{couponId}  → 발급된 유저       │
│  ├─ STRING: ecom:cpn:iss:cnt:{couponId}     → 순번 카운터       │
│  ├─ STRING: ecom:cpn:iss:soldout:{couponId} → 매진 플래그       │
│  └─ ZSET  : ecom:cpn:iss:queue:{couponId}   → 발급 대기열       │
│                                                                 │
│  Worker:                                                        │
│  └─ ZPOPMIN → 단건 DB insert → 100ms 주기                       │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**장점:**
- 구조가 단순하고 이해하기 쉬움
- SADD 원자성으로 중복 방지
- Soldout 플래그로 빠른 실패

**단점:**
- Consumer Group 미지원 (수평 확장 어려움)
- ACK 메커니즘 없음 (Worker 중단 시 데이터 유실 가능)
- ZPOPMIN 후 처리 실패 시 복구 어려움

### 2.2 Redis Streams 구조

```
┌─────────────────────────────────────────────────────────────────┐
│ Redis Streams 기반 쿠폰 발급 흐름                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  [요청] → Soldout 체크 → SADD(중복) → INCR(순번) → XADD(스트림)   │
│                                                                 │
│  Redis Keys:                                                    │
│  ├─ SET    : ecom:cpn:iss:issued:{couponId}                     │
│  ├─ STRING : ecom:cpn:iss:cnt:{couponId}                        │
│  ├─ STRING : ecom:cpn:iss:soldout:{couponId}                    │
│  └─ STREAM : ecom:cpn:iss:stream:{couponId}   ← 변경점          │
│                                                                 │
│  Consumer Group:                                                │
│  └─ XREADGROUP → XACK → Bulk DB insert → 실패 시 PEL에 남음     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**장점:**
- **Consumer Group**: 여러 Worker 인스턴스가 분산 처리 가능
- **ACK 메커니즘**: 처리 확인 전까지 PEL(Pending Entry List)에 보관
- **자동 재처리**: XPENDING + XCLAIM으로 실패한 메시지 재처리
- **Kafka 유사 구조**: 전환 시 개념적 매핑 용이

**단점:**
- 구조 복잡도 증가
- 메모리 사용량 증가 (MAXLEN으로 제어 필요)
- Consumer Group 관리 오버헤드

### 2.3 비교 분석표

| 기능 | 현재 (ZSET) | Redis Streams | Kafka |
|------|-------------|---------------|-------|
| 수평 확장 | ❌ 단일 Worker | ✅ Consumer Group | ✅ Consumer Group |
| ACK/재처리 | ❌ 없음 | ✅ XACK/XPENDING | ✅ Commit/Offset |
| 순서 보장 | ✅ Score 기반 | ✅ ID 기반 | ✅ Partition 내 |
| DLQ 지원 | ❌ 수동 구현 | ⚠️ 수동 구현 | ✅ 네이티브 지원 |
| 구현 복잡도 | 낮음 | 중간 | 높음 |
| 운영 복잡도 | 낮음 | 중간 | 높음 |

### 2.4 권장 방향

```
┌─────────────────────────────────────────────────────────────────┐
│ 전환 로드맵                                                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  현재 (ZSET)                                                    │
│      ↓                                                          │
│  Phase 1: 성능 개선 (Bulk 처리, 주기 조정)                       │
│      ↓                                                          │
│  Phase 2: Redis Streams 전환 (Consumer Group, ACK, DLQ)         │
│      ↓                                                          │
│  Phase 3: Kafka 전환 (MSA 전환 시)                              │
│                                                                 │
│  ※ Phase 2는 선택적 - Kafka 직접 전환도 가능                    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**결론**:
- 현재 규모에서는 **ZSET + Bulk 처리**로 충분
- Redis Streams는 **수평 확장 필요 시** 또는 **ACK 메커니즘 필요 시** 도입
- Kafka 전환 예정이라면 **Streams 건너뛰고 직접 Kafka 전환**도 고려

---

## 3. Phase 1: 즉시 개선 (성능 튜닝)

### 3.1 랭킹 - ZINCRBY 파이프라이닝

**파일**: `OutboxEventProcessor.kt`, `SalesRankingEventHandler.kt`

**현재 문제:**
```kotlin
// 50개 이벤트 → 50 × 3 = 150 Redis RTT
events.forEach { event ->
    handler.handle(event)  // 내부에서 3번 ZINCRBY
}
```

**개선안:**
```kotlin
// EventHandler 인터페이스 확장
interface EventHandler {
    fun handle(event: OutboxEvent): Boolean
    fun handleBatch(events: List<OutboxEvent>): Boolean = events.all { handle(it) }
    fun supportsBatchProcessing(): Boolean = false
}

// SalesRankingEventHandler - 배치 처리 지원
class SalesRankingEventHandler(
    private val productRankingPort: ProductRankingPort
) : EventHandler {

    override fun supportsBatchProcessing() = true

    override fun handleBatch(events: List<OutboxEvent>): Boolean {
        // 1. 상품별 판매량 집계
        val salesByProduct = mutableMapOf<Long, Int>()
        val today = LocalDate.now()

        events.forEach { event ->
            val payload = parsePayload(event)
            payload.items.forEach { item ->
                salesByProduct.merge(item.productId, item.quantity, Int::plus)
            }
        }

        // 2. Pipeline으로 일괄 처리 (1 RTT)
        productRankingPort.incrementSalesCountBatch(salesByProduct, today)
        return true
    }
}

// RedisProductRankingAdapter - Pipeline 추가
override fun incrementSalesCountBatch(salesByProduct: Map<Long, Int>, date: LocalDate) {
    val dateKey = date.format(DATE_FORMATTER)
    val weekKey = getYearWeek(date)

    val dailyKey = RedisKeyNames.Ranking.dailySalesKey(dateKey)
    val weeklyKey = RedisKeyNames.Ranking.weeklySalesKey(weekKey)
    val totalKey = RedisKeyNames.Ranking.totalSalesKey()

    redisTemplate.executePipelined { connection ->
        salesByProduct.forEach { (productId, quantity) ->
            val productIdBytes = productId.toString().toByteArray()
            val incrementValue = quantity.toDouble()

            connection.zSetCommands().zIncrBy(dailyKey.toByteArray(), incrementValue, productIdBytes)
            connection.zSetCommands().zIncrBy(weeklyKey.toByteArray(), incrementValue, productIdBytes)
            connection.zSetCommands().zIncrBy(totalKey.toByteArray(), incrementValue, productIdBytes)
        }
        null
    }

    // TTL 설정
    setDailyKeyExpire(dateKey, 7)
    setWeeklyKeyExpire(weekKey, 30)
}
```

**예상 효과:**
- 150 RTT → 1 RTT (99% 감소)
- 네트워크 레이턴시 대폭 감소

### 3.2 쿠폰 - INCR 로직 개선

**파일**: `RedisCouponIssueAdapter.kt`

**현재 문제:**
```kotlin
// Race Condition 시나리오
// User A: SADD 성공 → INCR(1) → ZADD
// User B: SADD 실패(중복) → return (INCR 안 함)
// User C: SADD 성공 → INCR(2) → 순번 초과로 롤백
// 결과: Counter=2, 실제 발급=1 (불일치)
```

**개선안:**
```kotlin
override fun tryIssue(couponId: Long, userId: Long, maxQuantity: Int): CouponIssueResult {
    val issuedKey = RedisKeyNames.CouponIssue.issuedKey(couponId)
    val queueKey = RedisKeyNames.CouponIssue.queueKey(couponId)
    val counterKey = RedisKeyNames.CouponIssue.counterKey(couponId)
    val soldoutKey = RedisKeyNames.CouponIssue.soldoutKey(couponId)
    val userIdStr = userId.toString()

    // 1. 매진 플래그 조기 체크 (O(1))
    if (redisTemplate.hasKey(soldoutKey) == true) {
        return CouponIssueResult.SOLD_OUT
    }

    // 2. SADD로 원자적 중복 체크 + 등록
    val added = redisTemplate.opsForSet().add(issuedKey, userIdStr)
    if (added == null || added == 0L) {
        return CouponIssueResult.ALREADY_ISSUED
    }

    // 3. INCR로 순번 획득 (SADD 성공한 경우에만 도달)
    val myOrder = redisTemplate.opsForValue().increment(counterKey) ?: 1L

    // 4. 순번 초과 시 완전 롤백
    if (myOrder > maxQuantity) {
        // SET에서 제거
        redisTemplate.opsForSet().remove(issuedKey, userIdStr)
        // Counter 롤백 (핵심 추가)
        redisTemplate.opsForValue().decrement(counterKey)
        // 매진 플래그 설정
        redisTemplate.opsForValue().set(soldoutKey, "1")
        return CouponIssueResult.SOLD_OUT
    }

    // 5. 대기열 등록
    redisTemplate.opsForZSet().add(queueKey, userIdStr, myOrder.toDouble())
    return CouponIssueResult.QUEUED
}
```

**주의**: `decrement` 추가로 Counter가 정확해지지만, 극단적 동시성에서 Counter가 maxQuantity 미만으로 내려갈 수 있음. 이는 **추가 발급 여지**를 만들어 오히려 유리할 수 있음.

### 3.3 쿠폰 Worker - Bulk 처리

**파일**: `CouponIssueWorker.kt`

**개선안:**
```kotlin
@Component
@ConditionalOnProperty(
    prefix = "coupon.issue.worker",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class CouponIssueWorker(
    private val couponIssueService: CouponIssueService,
    private val couponDomainService: CouponDomainService,
    private val couponIssueHistoryRepository: CouponIssueHistoryRepository,
    private val userCouponRepository: UserCouponRepository
) {
    private val logger = KotlinLogging.logger {}

    companion object {
        private const val PROCESS_INTERVAL_MS = 500L  // 100ms → 500ms
        private const val MAX_BATCH_SIZE = 50
    }

    @Scheduled(fixedDelay = PROCESS_INTERVAL_MS)
    fun processPendingIssues() {
        try {
            val activeCoupons = couponDomainService.getAvailableCoupons()
            activeCoupons.forEach { coupon ->
                processCouponIssuesBatch(coupon)
            }
        } catch (e: Exception) {
            logger.error("발급 대기열 처리 중 예상치 못한 오류", e)
        }
    }

    @Transactional
    private fun processCouponIssuesBatch(coupon: Coupon) {
        val userIds = couponIssueService.popPendingUsers(coupon.id, MAX_BATCH_SIZE)
        if (userIds.isEmpty()) return

        val now = LocalDateTime.now()

        // 1. UserCoupon 배치 생성
        val userCoupons = userIds.map { userId ->
            UserCoupon.create(
                couponId = coupon.id,
                userId = userId,
                expiresAt = coupon.validTo
            )
        }

        // 2. Bulk Insert (saveAll)
        val savedCoupons = userCouponRepository.saveAll(userCoupons)

        // 3. 발급 이력 배치 저장
        val histories = userIds.mapIndexed { index, userId ->
            CouponIssueHistory.create(
                couponId = coupon.id,
                userId = userId,
                couponName = coupon.name,
                issuedAt = now
            )
        }
        couponIssueHistoryRepository.saveAll(histories)

        logger.info("쿠폰 배치 발급 완료 - couponId: {}, count: {}", coupon.id, userIds.size)
    }
}
```

**예상 효과:**
- 50건 × 2 (UserCoupon + History) = 100 단건 insert → 2 bulk insert
- 100ms 주기 → 500ms 주기로 DB 부하 80% 감소

---

## 4. Phase 2: DLQ 및 실패 처리

### 4.1 DLQ 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│ Dead Letter Queue (DLQ) 아키텍처                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  [Outbox Event]                                                 │
│       ↓                                                         │
│  OutboxEventProcessor                                           │
│       ↓                                                         │
│  ┌─────────────┐    성공    ┌─────────────────┐                │
│  │   Handler   │ ────────→ │ markAsProcessed │                │
│  └─────────────┘            └─────────────────┘                │
│       │                                                         │
│       │ 실패 (retryCount < maxRetry)                           │
│       ↓                                                         │
│  ┌─────────────────┐                                           │
│  │ incrementRetry  │ → 다음 주기에 재시도                       │
│  └─────────────────┘                                           │
│       │                                                         │
│       │ 실패 (retryCount >= maxRetry)                          │
│       ↓                                                         │
│  ┌─────────────────────────────────────────┐                   │
│  │ DLQ 테이블 (outbox_event_dlq)           │                   │
│  │ - 원본 이벤트 정보                       │                   │
│  │ - 실패 사유                              │                   │
│  │ - 마지막 시도 시각                       │                   │
│  │ - 수동 재처리 플래그                     │                   │
│  └─────────────────────────────────────────┘                   │
│       ↓                                                         │
│  [알림] Slack/Email → 운영자 확인                               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 4.2 DLQ 엔티티 및 서비스

```kotlin
// DLQ 엔티티
@Entity
@Table(name = "outbox_event_dlq")
class OutboxEventDlq(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val originalEventId: Long,

    @Column(nullable = false, length = 100)
    val eventType: String,

    @Column(nullable = false, length = 100)
    val aggregateType: String,

    @Column(nullable = false)
    val aggregateId: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val payload: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val errorMessage: String,

    @Column(nullable = false)
    val failedAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val retryCount: Int,

    @Column(nullable = false)
    var resolved: Boolean = false,

    @Column(nullable = true)
    var resolvedAt: LocalDateTime? = null,

    @Column(nullable = true)
    var resolvedBy: String? = null
) {
    companion object {
        fun fromOutboxEvent(event: OutboxEvent, errorMessage: String): OutboxEventDlq {
            return OutboxEventDlq(
                originalEventId = event.id,
                eventType = event.eventType,
                aggregateType = event.aggregateType,
                aggregateId = event.aggregateId,
                payload = event.payload,
                errorMessage = errorMessage,
                retryCount = event.retryCount
            )
        }
    }

    fun resolve(resolvedBy: String) {
        this.resolved = true
        this.resolvedAt = LocalDateTime.now()
        this.resolvedBy = resolvedBy
    }
}

// DLQ 서비스
@Service
class DlqService(
    private val dlqRepository: OutboxEventDlqRepository,
    private val outboxEventService: OutboxEventService,
    private val alertService: AlertService  // Slack/Email 알림
) {
    private val logger = KotlinLogging.logger {}

    companion object {
        private const val MAX_RETRY_COUNT = 5
    }

    fun moveToDlq(event: OutboxEvent, errorMessage: String) {
        val dlqEvent = OutboxEventDlq.fromOutboxEvent(event, errorMessage)
        dlqRepository.save(dlqEvent)

        // 원본 이벤트 삭제 또는 마킹
        outboxEventService.markAsMovedToDlq(event.id)

        // 알림 발송
        alertService.sendDlqAlert(dlqEvent)

        logger.warn("[DLQ] 이벤트 이동: eventId=${event.id}, type=${event.eventType}, error=$errorMessage")
    }

    fun shouldMoveToDlq(event: OutboxEvent): Boolean {
        return event.retryCount >= MAX_RETRY_COUNT
    }

    @Transactional
    fun retryFromDlq(dlqEventId: Long, operatorId: String): Boolean {
        val dlqEvent = dlqRepository.findById(dlqEventId).orElseThrow()

        // 원본 이벤트 재생성
        val newEvent = OutboxEvent.create(
            eventType = dlqEvent.eventType,
            aggregateType = dlqEvent.aggregateType,
            aggregateId = dlqEvent.aggregateId,
            payload = dlqEvent.payload
        )
        outboxEventService.save(newEvent)

        // DLQ 이벤트 해결 처리
        dlqEvent.resolve(operatorId)
        dlqRepository.save(dlqEvent)

        return true
    }
}
```

### 4.3 OutboxEventProcessor 개선

```kotlin
@Component
class OutboxEventProcessor(
    private val outboxEventService: OutboxEventService,
    private val eventHandlerRegistry: EventHandlerRegistry,
    private val dlqService: DlqService
) {
    private val logger = KotlinLogging.logger {}

    companion object {
        private const val BATCH_SIZE = 50
    }

    @Scheduled(fixedDelay = 5000)
    fun processEvents() {
        val events = outboxEventService.getUnprocessedEvents(BATCH_SIZE)
        if (events.isEmpty()) return

        // 배치 처리 지원 핸들러별 그룹핑
        val eventsByType = events.groupBy { it.eventType }

        eventsByType.forEach { (eventType, typeEvents) ->
            val handlers = eventHandlerRegistry.getHandlers(eventType)

            if (handlers.isEmpty()) {
                typeEvents.forEach { event ->
                    handleMissingHandler(event)
                }
                return@forEach
            }

            // 배치 처리 가능한 핸들러 확인
            val batchHandler = handlers.find { it.supportsBatchProcessing() }

            if (batchHandler != null) {
                processBatch(typeEvents, batchHandler)
            } else {
                typeEvents.forEach { event -> processEvent(event, handlers) }
            }
        }
    }

    private fun processBatch(events: List<OutboxEvent>, handler: EventHandler) {
        try {
            val success = handler.handleBatch(events)
            if (success) {
                events.forEach { outboxEventService.markAsProcessed(it.id) }
            } else {
                events.forEach { handleFailure(it, "배치 처리 실패") }
            }
        } catch (e: Exception) {
            events.forEach { handleFailure(it, e.message ?: "배치 처리 중 예외") }
        }
    }

    private fun handleFailure(event: OutboxEvent, errorMessage: String) {
        if (dlqService.shouldMoveToDlq(event)) {
            dlqService.moveToDlq(event, errorMessage)
        } else {
            outboxEventService.incrementRetryAndMarkFailed(event.id, errorMessage)
        }
    }

    private fun handleMissingHandler(event: OutboxEvent) {
        val errorMessage = "핸들러를 찾을 수 없습니다: ${event.eventType}"
        dlqService.moveToDlq(event, errorMessage)
    }
}
```

---

## 5. Phase 3: Kafka 전환 대비 구조 개선

### 5.1 현재 vs 목표 아키텍처

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

### 5.2 추상화 계층 설계

```kotlin
// 메시지 발행 추상화
interface MessagePublisher {
    fun publish(topic: String, key: String, payload: Any)
    fun publishBatch(topic: String, messages: List<Message>)
}

data class Message(
    val key: String,
    val payload: Any,
    val headers: Map<String, String> = emptyMap()
)

// 현재 구현: Spring ApplicationEvent 기반
@Component
@Profile("!kafka")
class InMemoryMessagePublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) : MessagePublisher {

    override fun publish(topic: String, key: String, payload: Any) {
        val event = DomainEvent(topic = topic, key = key, payload = payload)
        applicationEventPublisher.publishEvent(event)
    }

    override fun publishBatch(topic: String, messages: List<Message>) {
        messages.forEach { publish(topic, it.key, it.payload) }
    }
}

// Kafka 전환 시 구현
@Component
@Profile("kafka")
class KafkaMessagePublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) : MessagePublisher {

    override fun publish(topic: String, key: String, payload: Any) {
        kafkaTemplate.send(topic, key, payload)
    }

    override fun publishBatch(topic: String, messages: List<Message>) {
        messages.forEach { msg ->
            val record = ProducerRecord(topic, msg.key, msg.payload)
            msg.headers.forEach { (k, v) ->
                record.headers().add(k, v.toByteArray())
            }
            kafkaTemplate.send(record)
        }
    }
}
```

### 5.3 이벤트 스키마 표준화 (CloudEvents)

```kotlin
// CloudEvents 표준 기반 이벤트 래퍼
// traceId: Snowflake 사용 (시간순 정렬 가능, 생성 시점 추출 가능)
data class CloudEvent<T>(
    val specversion: String = "1.0",
    val id: String,               // Snowflake ID (16진수)
    val source: String,           // "/order-service"
    val type: String,             // "io.hhplus.ecommerce.order.completed"
    val subject: String?,         // aggregateId
    val time: Instant = Instant.now(),
    val datacontenttype: String = "application/json",
    val data: T,

    // 확장 속성 - Snowflake 기반 traceId
    val traceid: String,          // Snowflake ID (시간순 정렬, 생성시점 추출 가능)
    val correlationid: String? = null
) {
    companion object {
        fun <T> create(
            snowflakeGenerator: SnowflakeGenerator,
            source: String,
            type: String,
            subject: String?,
            data: T,
            correlationId: String? = null
        ): CloudEvent<T> {
            val eventId = snowflakeGenerator.nextId().toString(16).uppercase()
            val traceId = MDC.get("traceId")
                ?: snowflakeGenerator.nextId().toString(16).uppercase()

            return CloudEvent(
                id = eventId,
                source = source,
                type = type,
                subject = subject,
                data = data,
                traceid = traceId,
                correlationid = correlationId
            )
        }
    }
}

// 토픽/이벤트 타입 상수
object EventTypes {
    const val ORDER_COMPLETED = "io.hhplus.ecommerce.order.completed"
    const val ORDER_CANCELLED = "io.hhplus.ecommerce.order.cancelled"
    const val COUPON_ISSUED = "io.hhplus.ecommerce.coupon.issued"
    const val PAYMENT_COMPLETED = "io.hhplus.ecommerce.payment.completed"
}

object Topics {
    const val ORDER = "order"
    const val COUPON = "coupon"
    const val PAYMENT = "payment"
}
```

### 5.4 TraceId 전파 전략 (Snowflake 기반)

**왜 Snowflake인가?**

| 비교 항목 | UUID | Snowflake |
|----------|------|-----------|
| 크기 | 128bit (36자) | 64bit (16진수 16자) |
| 시간 순 정렬 | ❌ 불가 | ✅ 가능 |
| 생성 시점 추출 | ❌ 불가 | ✅ `extractTimestamp()` |
| 로그 가독성 | 김 (`550e8400-e29b-41d4-a716-446655440000`) | 짧음 (`1A2B3C4D5E6F7890`) |

**TraceId 흐름:**

```
┌─────────────────────────────────────────────────────────────────┐
│ TraceId 전파 흐름                                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. HTTP 요청 수신                                               │
│     └─ Filter/Interceptor에서 traceId 생성 (Snowflake)          │
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
│     └─ 로그에 동일 traceId로 연결                               │
│                                                                 │
│  5. 문제 발생 시                                                 │
│     └─ traceId로 전체 흐름 추적                                 │
│     └─ snowflakeGenerator.extractTimestamp(traceId) → 생성시점  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**구현:**

```kotlin
// TraceId 필터 (HTTP 요청)
@Component
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

// Kafka Consumer에서 traceId 전파
@Component
class TraceIdConsumerInterceptor(
    private val snowflakeGenerator: SnowflakeGenerator
) {
    fun extractAndSetTraceId(headers: Headers) {
        val traceIdHeader = headers.lastHeader("x-trace-id")
        val traceId = traceIdHeader?.value()?.let { String(it) }
            ?: snowflakeGenerator.nextId().toString(16).uppercase()

        MDC.put("traceId", traceId)
    }
}
```

### 5.5 OutboxEventPublisher 개선

```kotlin
@Component
class OutboxEventPublisher(
    private val outboxEventService: OutboxEventService,
    private val messagePublisher: MessagePublisher,
    private val dlqService: DlqService
) {
    private val logger = KotlinLogging.logger {}

    companion object {
        private const val BATCH_SIZE = 50
    }

    @Scheduled(fixedDelay = 5000)
    fun publishEvents() {
        val events = outboxEventService.getUnprocessedEvents(BATCH_SIZE)
        if (events.isEmpty()) return

        // 토픽별 그룹핑
        val eventsByTopic = events.groupBy { mapEventTypeToTopic(it.eventType) }

        eventsByTopic.forEach { (topic, topicEvents) ->
            try {
                val messages = topicEvents.map { event ->
                    Message(
                        key = event.aggregateId,
                        payload = event.payload,
                        headers = mapOf(
                            "eventType" to event.eventType,
                            "eventId" to event.id.toString()
                        )
                    )
                }

                messagePublisher.publishBatch(topic, messages)

                // 발행 성공 시 처리 완료 마킹
                topicEvents.forEach { outboxEventService.markAsProcessed(it.id) }

            } catch (e: Exception) {
                logger.error("[Publisher] 발행 실패: topic=$topic, error=${e.message}", e)
                topicEvents.forEach { event ->
                    if (dlqService.shouldMoveToDlq(event)) {
                        dlqService.moveToDlq(event, e.message ?: "발행 실패")
                    } else {
                        outboxEventService.incrementRetryAndMarkFailed(event.id, e.message)
                    }
                }
            }
        }
    }

    private fun mapEventTypeToTopic(eventType: String): String {
        return when {
            eventType.contains("Order") -> Topics.ORDER
            eventType.contains("Coupon") -> Topics.COUPON
            eventType.contains("Payment") -> Topics.PAYMENT
            else -> "default"
        }
    }
}
```

---

## 6. Phase 4: 이벤트 소싱 적용 검토

### 6.1 적용 가능 영역 분석

| 도메인 | 적합성 | 이유 | 권장 |
|--------|--------|------|------|
| **포인트** | ❌ 부적합 | 금융 데이터, ACID 필수, 잔액 정합성 중요 | 현행 유지 |
| **결제** | ❌ 부적합 | 외부 PG 연동, 롤백 복잡, 정산 요구사항 | 현행 유지 |
| **쿠폰 발급** | ⚠️ 제한적 | 발급 이력은 유용하나 재고 관리와 충돌 | 이력 테이블로 충분 |
| **주문** | ✅ 적합 | 상태 전이 명확, 감사 추적 필요 | Event Sourcing 검토 |
| **재고 예약** | ⚠️ 제한적 | 상태 전이 있으나 동시성 복잡 | 현행 유지 |

### 6.2 주문 도메인 Event Sourcing 설계

```
┌─────────────────────────────────────────────────────────────────┐
│ 주문 이벤트 소싱 아키텍처                                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Command                                                        │
│       ↓                                                         │
│  OrderAggregate.handle(command)                                │
│       ↓                                                         │
│  OrderEvent 생성                                                │
│       ↓                                                         │
│  ┌─────────────────────────────────────┐                       │
│  │ order_events (Event Store)          │                       │
│  │ - id                                │                       │
│  │ - aggregate_id (orderId)            │                       │
│  │ - event_type                        │                       │
│  │ - payload                           │                       │
│  │ - version (Optimistic Lock)         │                       │
│  │ - occurred_at                       │                       │
│  └─────────────────────────────────────┘                       │
│       ↓                                                         │
│  Event Replay → 현재 상태 재구성                                 │
│       ↓                                                         │
│  Projection → Read Model (orders 테이블)                       │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

```kotlin
// 주문 이벤트 정의
sealed class OrderEvent {
    abstract val orderId: Long
    abstract val occurredAt: Instant

    data class OrderCreated(
        override val orderId: Long,
        val userId: Long,
        val items: List<OrderItem>,
        val totalAmount: Long,
        override val occurredAt: Instant = Instant.now()
    ) : OrderEvent()

    data class OrderPaid(
        override val orderId: Long,
        val paymentId: Long,
        val paidAmount: Long,
        override val occurredAt: Instant = Instant.now()
    ) : OrderEvent()

    data class OrderShipped(
        override val orderId: Long,
        val trackingNumber: String,
        val carrier: String,
        override val occurredAt: Instant = Instant.now()
    ) : OrderEvent()

    data class OrderDelivered(
        override val orderId: Long,
        override val occurredAt: Instant = Instant.now()
    ) : OrderEvent()

    data class OrderCancelled(
        override val orderId: Long,
        val reason: String,
        val cancelledBy: String,
        override val occurredAt: Instant = Instant.now()
    ) : OrderEvent()
}

// 주문 Aggregate
class OrderAggregate private constructor() {
    var id: Long = 0L
    var status: OrderStatus = OrderStatus.CREATED
    var items: List<OrderItem> = emptyList()
    var totalAmount: Long = 0L
    var version: Int = 0

    companion object {
        fun create(orderId: Long, userId: Long, items: List<OrderItem>): Pair<OrderAggregate, OrderEvent> {
            val aggregate = OrderAggregate()
            val event = OrderEvent.OrderCreated(
                orderId = orderId,
                userId = userId,
                items = items,
                totalAmount = items.sumOf { it.price * it.quantity }
            )
            aggregate.apply(event)
            return aggregate to event
        }

        fun rebuild(events: List<OrderEvent>): OrderAggregate {
            val aggregate = OrderAggregate()
            events.forEach { aggregate.apply(it) }
            return aggregate
        }
    }

    fun apply(event: OrderEvent) {
        when (event) {
            is OrderEvent.OrderCreated -> {
                this.id = event.orderId
                this.items = event.items
                this.totalAmount = event.totalAmount
                this.status = OrderStatus.CREATED
            }
            is OrderEvent.OrderPaid -> {
                require(status == OrderStatus.CREATED) { "결제 가능한 상태가 아닙니다" }
                this.status = OrderStatus.PAID
            }
            is OrderEvent.OrderShipped -> {
                require(status == OrderStatus.PAID) { "배송 가능한 상태가 아닙니다" }
                this.status = OrderStatus.SHIPPED
            }
            is OrderEvent.OrderDelivered -> {
                require(status == OrderStatus.SHIPPED) { "배송 완료 가능한 상태가 아닙니다" }
                this.status = OrderStatus.DELIVERED
            }
            is OrderEvent.OrderCancelled -> {
                require(status in listOf(OrderStatus.CREATED, OrderStatus.PAID)) { "취소 가능한 상태가 아닙니다" }
                this.status = OrderStatus.CANCELLED
            }
        }
        this.version++
    }

    fun pay(paymentId: Long, paidAmount: Long): OrderEvent {
        require(status == OrderStatus.CREATED) { "결제 가능한 상태가 아닙니다" }
        val event = OrderEvent.OrderPaid(id, paymentId, paidAmount)
        apply(event)
        return event
    }

    fun ship(trackingNumber: String, carrier: String): OrderEvent {
        require(status == OrderStatus.PAID) { "배송 가능한 상태가 아닙니다" }
        val event = OrderEvent.OrderShipped(id, trackingNumber, carrier)
        apply(event)
        return event
    }

    fun cancel(reason: String, cancelledBy: String): OrderEvent {
        require(status in listOf(OrderStatus.CREATED, OrderStatus.PAID)) { "취소 가능한 상태가 아닙니다" }
        val event = OrderEvent.OrderCancelled(id, reason, cancelledBy)
        apply(event)
        return event
    }
}
```

### 6.3 Event Store 및 Repository

```kotlin
// Event Store 엔티티
@Entity
@Table(name = "order_events")
class OrderEventEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val aggregateId: Long,

    @Column(nullable = false, length = 100)
    val eventType: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val payload: String,

    @Column(nullable = false)
    val version: Int,

    @Column(nullable = false)
    val occurredAt: Instant
)

// Event Store Repository
interface OrderEventRepository : JpaRepository<OrderEventEntity, Long> {
    fun findByAggregateIdOrderByVersionAsc(aggregateId: Long): List<OrderEventEntity>

    @Lock(LockModeType.OPTIMISTIC)
    fun findTopByAggregateIdOrderByVersionDesc(aggregateId: Long): OrderEventEntity?
}

// Event Store Service
@Service
class OrderEventStore(
    private val orderEventRepository: OrderEventRepository,
    private val objectMapper: ObjectMapper,
    private val messagePublisher: MessagePublisher  // Outbox 연동
) {
    fun save(event: OrderEvent, expectedVersion: Int): OrderEventEntity {
        val currentVersion = orderEventRepository
            .findTopByAggregateIdOrderByVersionDesc(event.orderId)
            ?.version ?: 0

        if (currentVersion != expectedVersion) {
            throw OptimisticLockException("버전 충돌: expected=$expectedVersion, current=$currentVersion")
        }

        val entity = OrderEventEntity(
            aggregateId = event.orderId,
            eventType = event::class.simpleName!!,
            payload = objectMapper.writeValueAsString(event),
            version = expectedVersion + 1,
            occurredAt = event.occurredAt
        )

        val saved = orderEventRepository.save(entity)

        // 외부 발행 (Outbox 또는 Kafka)
        messagePublisher.publish(
            topic = Topics.ORDER,
            key = event.orderId.toString(),
            payload = event
        )

        return saved
    }

    fun load(orderId: Long): OrderAggregate {
        val events = orderEventRepository.findByAggregateIdOrderByVersionAsc(orderId)
        if (events.isEmpty()) {
            throw EntityNotFoundException("주문을 찾을 수 없습니다: $orderId")
        }

        val domainEvents = events.map { deserialize(it) }
        return OrderAggregate.rebuild(domainEvents)
    }

    private fun deserialize(entity: OrderEventEntity): OrderEvent {
        val clazz = when (entity.eventType) {
            "OrderCreated" -> OrderEvent.OrderCreated::class.java
            "OrderPaid" -> OrderEvent.OrderPaid::class.java
            "OrderShipped" -> OrderEvent.OrderShipped::class.java
            "OrderDelivered" -> OrderEvent.OrderDelivered::class.java
            "OrderCancelled" -> OrderEvent.OrderCancelled::class.java
            else -> throw IllegalArgumentException("Unknown event type: ${entity.eventType}")
        }
        return objectMapper.readValue(entity.payload, clazz)
    }
}
```

### 6.4 이벤트 소싱 적용 시 이점

| 이점 | 설명 |
|------|------|
| **완벽한 감사 추적** | 모든 상태 변경 히스토리 보존 |
| **Temporal Query** | 특정 시점의 주문 상태 조회 가능 |
| **디버깅 용이** | 문제 발생 시 이벤트 재생으로 원인 분석 |
| **Kafka 연동 자연스러움** | 이벤트 기반으로 외부 시스템과 통합 용이 |
| **CQRS 적용 기반** | Read/Write 모델 분리 가능 |

### 6.5 이벤트 소싱 적용 시 주의사항

| 주의사항 | 대응 방안 |
|----------|----------|
| 복잡도 증가 | 핵심 도메인(주문)에만 우선 적용 |
| 조회 성능 | Read Model (Projection) 별도 유지 |
| 스키마 진화 | 이벤트 버저닝 및 Upcaster 구현 |
| 스냅샷 필요 | 이벤트 수 임계치 초과 시 스냅샷 저장 |

---

## 7. 구현 우선순위 및 로드맵

### 7.1 우선순위 매트릭스

```
                    영향도
            낮음          높음
         ┌───────────┬───────────┐
    낮음 │           │   3.1     │
         │           │ Pipeline  │
 복잡도  ├───────────┼───────────┤
         │   3.2     │   4.1     │
    높음 │ INCR 개선  │   DLQ     │
         └───────────┴───────────┘
```

### 7.2 구현 로드맵

| Phase | 작업 | 파일 | 복잡도 | 영향도 | 우선순위 |
|-------|------|------|--------|--------|----------|
| **1** | 쿠폰 Worker Bulk 처리 | `CouponIssueWorker.kt` | 낮음 | 높음 | 🔴 P0 |
| **1** | INCR 로직 개선 | `RedisCouponIssueAdapter.kt` | 낮음 | 중간 | 🟠 P1 |
| **1** | 랭킹 ZINCRBY Pipeline | `RedisProductRankingAdapter.kt`, `SalesRankingEventHandler.kt` | 중간 | 높음 | 🟠 P1 |
| **2** | DLQ 엔티티 및 서비스 | `OutboxEventDlq.kt`, `DlqService.kt` | 중간 | 높음 | 🟠 P1 |
| **2** | OutboxEventProcessor 개선 | `OutboxEventProcessor.kt` | 중간 | 높음 | 🟠 P1 |
| **3** | MessagePublisher 추상화 | `MessagePublisher.kt` | 중간 | 중간 | 🟡 P2 |
| **3** | CloudEvents 스키마 표준화 | `CloudEvent.kt`, `EventTypes.kt` | 중간 | 중간 | 🟡 P2 |
| **3** | OutboxEventPublisher 리팩터링 | `OutboxEventPublisher.kt` | 중간 | 중간 | 🟡 P2 |
| **4** | 주문 Event Sourcing (선택) | `OrderAggregate.kt`, `OrderEventStore.kt` | 높음 | 중간 | 🟢 P3 |
| **-** | Redis Streams 전환 (선택) | `RedisCouponIssueAdapter.kt` | 높음 | 중간 | 🔵 Optional |

### 7.3 체크리스트

#### Phase 1: 즉시 개선 ✅ 완료 (2025-12-09)
- [x] `CouponIssueWorker` - `processCouponIssuesBatch()` 구현
- [x] `UserCouponRepository` - `saveAll()` 배치 지원
- [x] `CouponIssueHistoryRepository` - `saveAll()` 배치 지원
- [x] `CouponDomainService` - `issueCouponsBatch()` 메서드 추가
- [x] `CouponIssueHistoryService` - `recordIssuesBatch()` 메서드 추가
- [x] `RedisCouponIssueAdapter.tryIssue()` - decrement 롤백 추가
- [x] `ProductRankingPort` - `incrementSalesCountBatch()` 인터페이스 추가
- [x] `RedisProductRankingAdapter` - Pipeline 구현
- [x] `EventHandler` - `handleBatch()`, `supportsBatchProcessing()` 인터페이스 추가
- [x] `ProductRankingEventHandler` - 배치 처리 구현
- [x] `OutboxEventProcessor` - 배치 핸들러 지원

#### Phase 2: DLQ 및 실패 처리 ✅ 완료 (2025-12-09)
> 상세 내용: [OUTBOX_DLQ_REPORT.md](./OUTBOX_DLQ_REPORT.md) 참조

- [x] `OutboxEventDlq` 엔티티 생성
- [x] `OutboxEventDlqRepository` 생성 (interface + JPA + impl)
- [x] `DlqService` 구현 (moveToDlq, retryFromDlq, resolveManually)
- [x] `AlertService` 구현 (LoggingAlertService - 운영 환경에서 Slack/Email로 교체)
- [x] `OutboxEventProcessor` - DLQ 연동 (handleFailure, handleMissingHandler)
- [x] `OutboxEventService` - `incrementRetryAndMarkFailed` 메서드 추가
- [x] `DlqMonitoringScheduler` - 1분 주기 모니터링, 10분 주기 리포트

#### Phase 3: Kafka 전환 대비 ✅ 완료 (2025-12-09)
- [x] `MessagePublisher` 인터페이스 정의 (`common/messaging/`)
- [x] `Message` 데이터 클래스 정의
- [x] `InMemoryMessagePublisher` 구현 (@Profile("!kafka"))
- [x] `DomainEvent` 래퍼 클래스 구현
- [x] `CloudEvent` 데이터 클래스 정의 (CloudEvents 1.0 표준)
- [x] `CloudEventTypes` 상수 정의 (네임스페이스 포함)
- [x] `Topics` 상수 및 매핑 함수 정의
- [x] `TraceIdFilter` 구현 (Snowflake 기반 traceId, MDC 연동)
- [ ] `OutboxEventPublisher` 리팩터링 (선택 - 현재 Processor 구조 유지)
- [ ] Spring Profile 기반 Kafka 전환 (Kafka 도입 시 구현)

#### Phase 4: 이벤트 소싱 ✅ 완료 (2025-12-09)
> 상세 내용: [OUTBOX_DLQ_REPORT.md](./OUTBOX_DLQ_REPORT.md) 섹션 9 참조

- [x] `OrderEvent` sealed class 정의 (OrderCreated, OrderConfirmed, OrderCompleted, OrderCancelled, OrderFailed)
- [x] `OrderAggregate` 구현 (이벤트 적용, 상태 재구성, 스냅샷 지원)
- [x] `order_events` 테이블 생성 (OrderEventJpaEntity + Repository)
- [x] `OrderEventStore` 서비스 구현 (저장, 로드, Temporal Query)
- [x] Read Model Projection 구현 (OrderProjection, OrderProjectionRebuilder)

---

## 8. 참고 자료

### 2025 트렌드 및 베스트 프랙티스

- [Event Driven Architecture Done Right: Scale Systems in 2025](https://www.growin.com/blog/event-driven-architecture-scale-systems-2025/)
- [Kafka + Redis Streams + Spring Boot (20,000 RPS Blueprint)](https://blog.stackademic.com/async-messaging-at-scale-kafka-redis-streams-spring-boot-20-000-rps-blueprint-2025-7d19a3f1f746)
- [Building Event-Driven Microservices with Redis Streams & Kafka](https://medium.com/@tuteja_lovish/building-event-driven-microservices-in-spring-boot-with-redis-streams-kafka-8e3255074f68)

### Outbox 패턴 및 이벤트 소싱

- [Microservices Pattern: Transactional Outbox](https://microservices.io/patterns/data/transactional-outbox.html)
- [Event Sourcing vs CDC (Debezium)](https://debezium.io/blog/2020/02/10/event-sourcing-vs-cdc/)
- [Stop Overusing the Outbox Pattern](https://www.squer.io/blog/stop-overusing-the-outbox-pattern)

### Redis Streams

- [Redis Streams Documentation](https://redis.io/docs/latest/develop/data-types/streams/)
- [Replacing Kafka with Redis Streams](https://blog.arcjet.com/replacing-kafka-with-redis-streams/)
- [Redis Streams: A Comprehensive Guide](https://dev.to/mehmetakar/redis-streams-a-comprehensive-guide-cal)

### Kafka Best Practices

- [Kafka Event-Driven Architecture Done Right](https://estuary.dev/blog/kafka-event-driven-architecture/)
- [Real-Time Payment Microservices with Kafka, Spring Boot, Redis Streams](https://medium.com/@tharusha.wijayabahu/real-time-payment-microservices-with-apache-kafka-spring-boot-and-redis-streams-7d47665daf1e)

---

## 변경 이력

| 날짜 | 버전 | 내용 |
|------|------|------|
| 2025-12-09 | 1.0 | 초안 작성 |
