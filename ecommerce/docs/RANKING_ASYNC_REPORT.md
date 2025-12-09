# 랭킹 & 비동기 시스템 구현 보고서

> **목표**: Redis 기반 랭킹 시스템 및 선착순 쿠폰 발급 시스템 구현

---

## 목차

1. [배경 및 문제 정의](#1-배경-및-문제-정의)
2. [STEP 13: 상품 판매 랭킹 시스템](#2-step-13-상품-판매-랭킹-시스템)
3. [STEP 14: 선착순 쿠폰 발급 시스템](#3-step-14-선착순-쿠폰-발급-시스템)
4. [설계 결정 및 근거](#4-설계-결정-및-근거)
5. [테스트 결과](#5-테스트-결과)
6. [결론 및 향후 개선](#6-결론-및-향후-개선)

---

## 1. 배경 및 문제 정의

### 1.1 STEP 13 - 랭킹 시스템

**기존 문제점:**
- 판매량 기반 "랭킹" 기능 부재 (단순 카운터만 존재)
- 인기 상품은 조회수 기반 윈도우만 존재
- 상품별 개별 키로 랭킹 조회 불가

**목표:**
- Redis Sorted Set 기반 실시간 판매 랭킹
- 일별/주별/누적 랭킹 분리 관리
- O(log N + M) 복잡도의 Top N 조회

### 1.2 STEP 14 - 선착순 쿠폰 발급

**기존 문제점:**
```
Race Condition 발생 시나리오:

User A (Thread 1)          User A (Thread 2)
     │                          │
Step 1: getValue() → null       │
     │                     Step 1: getValue() → null
     │                          │
Step 2: enqueue (성공)          │
     │                     Step 2: enqueue (성공) ← 중복!

결과: 동일 유저가 2번 큐에 등록됨
```

- 중복 체크와 등록이 비원자적 (Race Condition)
- 발급 요청당 5회 Redis RTT
- 유저×쿠폰 수만큼 키 폭발적 증가

**목표:**
- SADD 원자성을 활용한 중복 방지
- 정확히 N명만 발급되는 수량 제어
- Redis RTT 최소화

---

## 2. STEP 13: 상품 판매 랭킹 시스템

### 2.1 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│ 구현된 데이터 흐름 (배치 + Pipeline 최적화)                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  주문 완료 (PaymentCompletedEvent)                           │
│       ↓                                                     │
│  OutboxEventProcessor (5초 주기, 50개 배치)                  │
│       ↓                                                     │
│  ProductRankingEventHandler.handleBatch()                   │
│       ↓                                                     │
│  ┌─────────────────────────────────────────┐                │
│  │ 1. 이벤트별 상품 판매량 집계             │                │
│  │    (50개 이벤트 → Map<productId, qty>)  │                │
│  │                                         │                │
│  │ 2. Pipeline으로 일괄 ZINCRBY (1 RTT)    │                │
│  │    ※ 기존: 50 × 3 = 150 RTT            │                │
│  │                                         │                │
│  │ ├─ ecom:rank:sales:d:{yyyyMMdd}  일별   │                │
│  │ ├─ ecom:rank:sales:w:{yyyyWW}    주별   │                │
│  │ └─ ecom:rank:sales:total         누적   │                │
│  └─────────────────────────────────────────┘                │
│       ↓                                                     │
│  ZREVRANGE로 Top N 조회 (O(log N + M))                      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 핵심 구현

#### ProductRankingPort (Hexagonal Architecture)

```kotlin
interface ProductRankingPort {
    // 단건 판매량 증가
    fun incrementSalesCount(productId: Long, quantity: Int): Long

    // 배치 판매량 증가 (Pipeline 최적화)
    fun incrementSalesCountBatch(salesByProduct: Map<Long, Int>)

    // Top N 상품 조회
    fun getDailyTopProducts(date: String, limit: Int): List<Pair<Long, Long>>
    fun getWeeklyTopProducts(yearWeek: String, limit: Int): List<Pair<Long, Long>>
    fun getTotalTopProducts(limit: Int): List<Pair<Long, Long>>

    // 특정 상품 조회
    fun getDailySalesCount(productId: Long, date: String): Long
    fun getDailyRank(productId: Long, date: String): Long?
    fun getTotalSalesCount(productId: Long): Long
}
```

#### RedisProductRankingAdapter

```kotlin
override fun incrementSalesCount(productId: Long, quantity: Int, date: LocalDate) {
    val dailyKey = RedisKeyNames.Ranking.dailySalesKey(date.format(DATE_FORMATTER))
    val weeklyKey = RedisKeyNames.Ranking.weeklySalesKey(getYearWeek(date))
    val totalKey = RedisKeyNames.Ranking.totalSalesKey()

    // Pipeline으로 3개 키 동시 업데이트 (1 RTT)
    redisTemplate.executePipelined { connection ->
        connection.zSetCommands().zIncrBy(dailyKey.toByteArray(), quantity.toDouble(), productId.toString().toByteArray())
        connection.zSetCommands().zIncrBy(weeklyKey.toByteArray(), quantity.toDouble(), productId.toString().toByteArray())
        connection.zSetCommands().zIncrBy(totalKey.toByteArray(), quantity.toDouble(), productId.toString().toByteArray())
        null
    }
}
```

### 2.3 Redis 키 설계

| 키 패턴 | TTL | 용도 |
|---------|-----|------|
| `ecom:rank:sales:d:{yyyyMMdd}` | 7일 | 일별 판매 랭킹 |
| `ecom:rank:sales:w:{yyyyWW}` | 30일 | 주간 판매 랭킹 |
| `ecom:rank:sales:total` | 없음 | 누적 판매 랭킹 |

### 2.4 시간복잡도

| 작업 | 시간복잡도 | 비고 |
|------|-----------|------|
| 판매량 증가 | O(log N) | ZINCRBY |
| Top 10 조회 | O(log N + 10) | ZREVRANGE |
| 특정 상품 순위 | O(log N) | ZREVRANK |

---

## 3. STEP 14: 선착순 쿠폰 발급 시스템

### 3.1 최종 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│ SADD + INCR + Soldout 플래그 패턴                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. Soldout 플래그 체크 (O(1)) ─→ 매진 시 즉시 실패          │
│       ↓                                                     │
│  2. SADD 원자적 중복 체크 + 등록 (O(1))                      │
│       ↓                                                     │
│  3. INCR 원자적 순번 획득 (O(1))                             │
│       ↓                                                     │
│  4. 순번 > maxQuantity? ─→ 롤백 + Soldout 플래그 설정        │
│       ↓                                                     │
│  5. ZADD 대기열 등록 (O(log N))                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 핵심 구현

#### RedisCouponIssueAdapter.tryIssue()

```kotlin
override fun tryIssue(couponId: Long, userId: Long, maxQuantity: Int): CouponIssueResult {
    val issuedKey = RedisKeyNames.CouponIssue.issuedKey(couponId)
    val queueKey = RedisKeyNames.CouponIssue.queueKey(couponId)
    val counterKey = RedisKeyNames.CouponIssue.counterKey(couponId)
    val soldoutKey = RedisKeyNames.CouponIssue.soldoutKey(couponId)
    val userIdStr = userId.toString()

    // 1. 매진 플래그 조기 체크 (O(1), 빠른 실패)
    if (redisTemplate.hasKey(soldoutKey) == true) {
        return CouponIssueResult.SOLD_OUT
    }

    // 2. SADD로 원자적 중복 체크 + 등록
    val added = redisTemplate.opsForSet().add(issuedKey, userIdStr)
    if (added == null || added == 0L) {
        return CouponIssueResult.ALREADY_ISSUED
    }

    // 3. INCR로 원자적 순번 획득
    val myOrder = redisTemplate.opsForValue().increment(counterKey) ?: 1L

    // 4. 순번이 maxQuantity보다 크면 롤백 + 매진 플래그 설정
    if (myOrder > maxQuantity) {
        redisTemplate.opsForSet().remove(issuedKey, userIdStr)
        redisTemplate.opsForValue().decrement(counterKey)  // 카운터 롤백 (정확한 발급 수량 유지)
        redisTemplate.opsForValue().set(soldoutKey, "1")
        return CouponIssueResult.SOLD_OUT
    }

    // 5. 성공 시 ZADD로 대기열 등록
    redisTemplate.opsForZSet().add(queueKey, userIdStr, myOrder.toDouble())

    return CouponIssueResult.QUEUED
}
```

### 3.3 캐시 전략 개선

#### 문제: 로컬 캐시와 동적 데이터 불일치

테스트 중 발견된 문제:
- `@Cacheable`로 `Coupon` 엔티티 전체가 Caffeine 로컬 캐시에 저장
- 첫 번째 테스트(totalQuantity=10)의 캐시가 두 번째 테스트(totalQuantity=100)에 영향
- **결과**: 1000명 동시 요청에서 100명이 아닌 10명만 발급

#### 해결: CouponCacheInfo DTO 분리

```kotlin
/**
 * 쿠폰 캐시용 VO (Value Object)
 *
 * 정적 정보만 캐싱 (totalQuantity 제외)
 * 동적 정보는 Redis maxQuantity로 관리
 */
data class CouponCacheInfo(
    val id: Long,
    val name: String,
    val code: String,
    val discountType: DiscountType,
    val discountValue: Long,
    val minimumOrderAmount: Long,
    val validFrom: LocalDateTime,
    val validTo: LocalDateTime
    // totalQuantity 제외!
)
```

#### 캐시 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│ 정적 정보 (로컬 캐시)          동적 정보 (Redis)              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  CouponCacheInfo                 Redis Keys                 │
│  ├─ id                           ├─ maxQuantity (STRING)    │
│  ├─ name                         ├─ issued SET              │
│  ├─ code                         ├─ counter (STRING)        │
│  ├─ discountType                 ├─ soldout (STRING)        │
│  ├─ discountValue                └─ queue (ZSET)            │
│  ├─ minimumOrderAmount                                      │
│  ├─ validFrom                                               │
│  └─ validTo                                                 │
│                                                             │
│  캐시 무효화: 거의 불필요       실시간 동기화: 항상 최신      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 3.4 Redis 키 설계

| 키 패턴 | 자료구조 | 용도 |
|---------|----------|------|
| `ecom:cpn:iss:issued:{couponId}` | SET | 발급된 유저 목록 |
| `ecom:cpn:iss:queue:{couponId}` | ZSET | 발급 대기열 |
| `ecom:cpn:iss:cnt:{couponId}` | STRING | 순번 카운터 |
| `ecom:cpn:iss:soldout:{couponId}` | STRING | 매진 플래그 |
| `ecom:cpn:iss:max:{couponId}` | STRING | 최대 발급 수량 |

### 3.5 통신 횟수 분석

| 시나리오 | RTT | 연산 상세 |
|----------|-----|----------|
| 성공 | **4 RTT** | hasKey + SADD + INCR + ZADD |
| 중복 거부 | **2 RTT** | hasKey + SADD(0 반환) |
| 수량 초과 (매진 전) | **5 RTT** | hasKey + SADD + INCR + SREM + DECR + SET |
| 수량 초과 (매진 후) | **1 RTT** | hasKey(true) → 즉시 반환 |

**Note**: 수량 초과 시 DECR로 카운터를 롤백하여 정확한 발급 수량 유지
**매진 후 요청은 Soldout 플래그로 즉시 실패 (O(1))**

---

## 4. 설계 결정 및 근거

### 4.1 Sorted Set 선택 이유

| 자료구조 | 장점 | 단점 | 적합성 |
|----------|------|------|--------|
| **Sorted Set** | 정확한 랭킹, O(log N) | 메모리 사용량 | ✅ 최적 |
| HyperLogLog | 메모리 효율적 | 카디널리티만 추정 | ❌ 수량 집계 불가 |
| Count-Min Sketch | 대규모 빈도 추정 | 오차 존재 | ❌ 정확한 랭킹 필요 |

**결론**: 현재 규모(상품 수 만 단위)에서는 Sorted Set이 최적

### 4.2 SADD + INCR 패턴 선택 이유

| 방식 | 장점 | 단점 |
|------|------|------|
| **SADD + INCR** | 단순, 빠름 | 미세 Race Condition 가능 |
| WATCH/MULTI/EXEC | 완벽한 원자성 | 충돌 시 재시도 필요 |
| Lua Script | 서버 측 원자 실행 | 복잡성 증가, 디버깅 어려움 |

**선택**: SADD + INCR + Soldout 플래그
- Soldout 플래그로 매진 후 요청 최적화
- INCR 순번으로 정확한 발급 수량 제어
- 단순하고 이해하기 쉬운 구조

---

## 5. 테스트 결과

### 5.1 랭킹 테스트

```kotlin
describe("상품 판매 랭킹 시스템") {
    it("주문 완료 시 판매 랭킹이 업데이트된다") {
        // 3개 상품 판매 시뮬레이션
        rankingAdapter.incrementSalesCount(productId = 1, quantity = 10, date = today)
        rankingAdapter.incrementSalesCount(productId = 2, quantity = 5, date = today)
        rankingAdapter.incrementSalesCount(productId = 3, quantity = 15, date = today)

        val topProducts = rankingAdapter.getTopProducts(RankingPeriod.DAILY, limit = 3)

        topProducts[0].productId shouldBe 3  // 15개
        topProducts[1].productId shouldBe 1  // 10개
        topProducts[2].productId shouldBe 2  // 5개
    }
}
```

### 5.2 쿠폰 동시성 테스트

```kotlin
describe("선착순 쿠폰 동시성 제어") {
    context("1000명이 100개 한정 쿠폰 동시 발급 시") {
        it("정확히 100명만 발급 성공한다") {
            val coupon = Coupon.create(totalQuantity = 100, ...)
            val savedCoupon = couponRepository.save(coupon)

            val executor = Executors.newFixedThreadPool(100)
            val latch = CountDownLatch(1000)
            val successCount = AtomicInteger(0)

            repeat(1000) { index ->
                executor.submit {
                    try {
                        couponCommandUseCase.issueCoupon(
                            userId = 10000L + index,
                            request = IssueCouponRequest(savedCoupon.id)
                        )
                        successCount.incrementAndGet()
                    } catch (e: Exception) { }
                    finally { latch.countDown() }
                }
            }

            latch.await()

            successCount.get() shouldBe 100  // ✅ 정확히 100명
            couponIssueService.getIssuedCount(savedCoupon.id) shouldBe 100
        }
    }

    context("동일 사용자가 동시에 여러 번 발급 시도 시") {
        it("단 한 번만 발급 성공한다") {
            // 100번 동시 요청 → 1번만 성공
            successCount.get() shouldBe 1  // ✅ SADD 원자성
        }
    }
}
```

**모든 테스트 통과 (4/4)**

---

## 6. 결론 및 향후 개선

### 6.1 구현 완료 항목

| 기능 | 상태 | 핵심 기술 |
|------|------|----------|
| 상품 판매 랭킹 | ✅ | Redis Sorted Set, ZINCRBY |
| 랭킹 배치 처리 | ✅ | Pipeline + handleBatch() (150 RTT → 1 RTT) |
| 선착순 쿠폰 발급 | ✅ | SADD + INCR + DECR 롤백 + Soldout 플래그 |
| 쿠폰 배치 Worker | ✅ | saveAll() 벌크 인서트 (500ms/50건) |
| 캐시 분리 | ✅ | CouponCacheInfo DTO |
| 동시성 테스트 | ✅ | 1000명 동시 요청 검증 |

### 6.2 성능 개선 효과

| 항목 | 기존 | 개선 후 |
|------|------|---------|
| 랭킹 조회 | ❌ 불가능 | O(log N + M) |
| 랭킹 배치 업데이트 | 150 RTT (50이벤트×3키) | **1 RTT** (Pipeline) |
| 쿠폰 Worker 처리 | 100ms 단건 | **500ms 50건 배치** |
| 매진 후 쿠폰 요청 | 3 RTT | **1 RTT** |
| 중복 발급 방지 | Race Condition 존재 | 원자적 처리 |
| 카운터 정확성 | 롤백 없음 | **DECR 롤백** |

### 6.3 구현 현황 및 향후 개선 방향

#### 구현 완료
1. **배치 발급 Worker** ✅
   - 500ms 주기로 최대 50건씩 배치 처리
   - `saveAll()`을 활용한 JPA 벌크 인서트
   - `issueCouponsBatch()`로 도메인 로직 일괄 처리

2. **랭킹 Pipeline 최적화** ✅
   - `incrementSalesCountBatch()`로 배치 ZINCRBY
   - 50개 이벤트 × 3 ZINCRBY = 150 RTT → 1 RTT

3. **카운터 일관성 보장** ✅
   - 수량 초과 시 DECR 롤백으로 정확한 발급 수량 유지

#### 향후 개선 방향
1. **Redis 클러스터 대응**
   - 현재: 단일 Redis 인스턴스 가정
   - 개선: Hash Tag로 같은 슬롯 보장

2. **모니터링 강화**
   - 발급 성공/실패 메트릭
   - 대기열 크기 알림

3. **DLQ(Dead Letter Queue)** ✅
   - 상세 내용: [OUTBOX_DLQ_REPORT.md](./OUTBOX_DLQ_REPORT.md) 참조

### 6.4 배운 점

1. **캐시와 동적 데이터 분리의 중요성**
   - 로컬 캐시(Caffeine)와 분산 캐시(Redis)의 역할 구분
   - 변경 빈도에 따른 캐시 전략 수립

2. **Redis 원자적 연산 활용**
   - SADD의 반환값(0/1)으로 중복 체크
   - INCR로 순번 보장

3. **Soldout 플래그 패턴**
   - 매진 후 불필요한 연산 제거
   - O(1) 빠른 실패 응답

---

## 부록: 관련 파일 구조

```
src/main/kotlin/io/hhplus/ecommerce/
├── common/
│   ├── cache/
│   │   ├── RedisKeyNames.kt              # Redis 키 중앙 관리
│   │   ├── CacheNames.kt                 # Spring Cache 이름
│   │   └── CacheInvalidationPublisher.kt
│   └── outbox/
│       ├── EventHandler.kt               # 이벤트 핸들러 인터페이스 (배치 지원)
│       ├── EventHandlerRegistry.kt       # 핸들러 레지스트리
│       ├── OutboxEventProcessor.kt       # 이벤트 배치 프로세서
│       └── OutboxEventService.kt
├── coupon/
│   ├── application/
│   │   ├── CouponIssueService.kt         # 선착순 발급 서비스
│   │   ├── CouponIssueWorker.kt          # 배치 발급 Worker (500ms/50건)
│   │   ├── CouponIssueHistoryService.kt  # 발급 이력 서비스 (배치 지원)
│   │   ├── port/out/
│   │   │   └── CouponIssuePort.kt        # DIP 포트
│   │   └── usecase/
│   │       └── CouponCommandUseCase.kt
│   ├── domain/
│   │   ├── service/
│   │   │   └── CouponDomainService.kt    # 도메인 서비스 (배치 지원)
│   │   ├── repository/
│   │   │   ├── UserCouponRepository.kt   # saveAll() 추가
│   │   │   └── CouponIssueHistoryRepository.kt  # saveAll() 추가
│   │   └── vo/
│   │       └── CouponCacheInfo.kt        # 캐시용 VO
│   └── infra/
│       └── issue/
│           └── RedisCouponIssueAdapter.kt  # DECR 롤백 포함
└── product/
    ├── application/
    │   ├── handler/
    │   │   └── ProductRankingEventHandler.kt  # 배치 처리 지원
    │   └── port/out/
    │       └── ProductRankingPort.kt     # incrementSalesCountBatch() 추가
    └── infra/
        └── ranking/
            └── RedisProductRankingAdapter.kt  # Pipeline 최적화
```
