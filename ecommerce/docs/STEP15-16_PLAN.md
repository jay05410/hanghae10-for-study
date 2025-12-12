# STEP 15-16 과제 계획서

> **작성일**: 2025-12-11
> **목표**: Application Event 기반 관심사 분리 + 분산 트랜잭션 설계

---

## 빠른 실행 가이드

```
Phase 1 실행: "Phase 1 실행해줘" (DataPlatformClient + DTO)
Phase 2 실행: "Phase 2 실행해줘" (EventHandler 구현)
Phase 3 실행: "Phase 3 실행해줘" (테스트 작성)
Phase 4 실행: "Phase 4 실행해줘" (TRANSACTION_DIAGNOSIS.md)
Phase 5 실행: "Phase 5 실행해줘" (선택: 성능 최적화)
최종 검토:   "최종 검토해줘" (체크리스트 점검 + 커밋)
```

---

## 목차

1. [현재 상태 요약](#1-현재-상태-요약)
2. [Phase 1: DataPlatformClient 구현](#2-phase-1-dataplatformclient-구현)
3. [Phase 2: EventHandler 구현](#3-phase-2-eventhandler-구현)
4. [Phase 3: 테스트 작성](#4-phase-3-테스트-작성)
5. [Phase 4: Transaction Diagnosis 문서](#5-phase-4-transaction-diagnosis-문서)
6. [Phase 5: 성능 최적화 (선택)](#6-phase-5-성능-최적화-선택)
7. [최종 체크리스트](#7-최종-체크리스트)

---

## 1. 현재 상태 요약

### 1.1 이미 구현된 것 (강점)

| 항목 | 상태 | 비고 |
|------|------|------|
| Outbox 패턴 | ✅ | DB 트랜잭션과 이벤트 발행 원자성 보장 |
| DLQ 구조 | ✅ | 5회 재시도 후 DLQ 이동 |
| ZINCRBY 파이프라이닝 | ✅ | 150 RTT → 1 RTT |
| 쿠폰 Worker Bulk 처리 | ✅ | 500ms 주기, saveAll() |
| MessagePublisher 추상화 | ✅ | Kafka 전환 대비 |
| Event Sourcing (주문) | ✅ | 감사 추적 지원 |
| Saga 패턴 | ✅ | 보상 트랜잭션 구현됨 |

### 1.2 과제 요구사항 Gap

| 요구사항 | 현재 상태 | 필요 작업 |
|----------|----------|----------|
| **STEP 15**: 데이터 플랫폼 전송 | ❌ 미구현 | Phase 1-3 |
| **STEP 16**: 분산 트랜잭션 문서 | ❌ 미작성 | Phase 4 |

### 1.3 크리티컬 이슈

| 이슈 | 심각도 | Phase |
|------|--------|-------|
| 데이터 플랫폼 전송 기능 없음 | **높음** | 1-2 |
| popFromQueue 단건 반복 (N RTT) | 중간 | 5 |
| TRANSACTION_DIAGNOSIS.md 미작성 | 중간 | 4 |

---

## 2. Phase 1: DataPlatformClient 구현

> **실행 명령**: `"Phase 1 실행해줘"`

### 2.1 목표
- 데이터 플랫폼 클라이언트 인터페이스 정의
- Mock 구현체 작성
- 주문 정보 전송용 DTO 정의

### 2.2 생성할 파일

| 파일 | 위치 | 설명 |
|------|------|------|
| `DataPlatformClient.kt` | `common/external/` | 인터페이스 |
| `MockDataPlatformClient.kt` | `common/external/` | Mock 구현 |
| `DataPlatformResponse.kt` | `common/external/` | 응답 DTO |
| `OrderInfoDto.kt` | `order/application/dto/` | 주문 정보 DTO |

### 2.3 상세 스펙

```kotlin
// DataPlatformClient.kt
interface DataPlatformClient {
    fun sendOrderInfo(orderInfo: OrderInfoDto): DataPlatformResponse
}

// DataPlatformResponse.kt
data class DataPlatformResponse(
    val success: Boolean,
    val message: String?,
    val timestamp: Instant = Instant.now()
)

// OrderInfoDto.kt
data class OrderInfoDto(
    val orderId: Long,
    val orderNumber: String,
    val userId: Long,
    val items: List<OrderItemInfoDto>,
    val totalAmount: Long,
    val discountAmount: Long,
    val finalAmount: Long,
    val status: String,
    val createdAt: LocalDateTime
)

// MockDataPlatformClient.kt
@Component
@Profile("!production")
class MockDataPlatformClient : DataPlatformClient {
    // 90% 성공률 시뮬레이션
    // 로깅으로 전송 내용 확인
}
```

### 2.4 완료 조건
- [ ] DataPlatformClient 인터페이스 생성
- [ ] DataPlatformResponse DTO 생성
- [ ] OrderInfoDto 생성
- [ ] MockDataPlatformClient 구현 (@Profile 적용)
- [ ] 컴파일 성공

---

## 3. Phase 2: EventHandler 구현

> **실행 명령**: `"Phase 2 실행해줘"`

### 3.1 목표
- PAYMENT_COMPLETED 이벤트 수신 시 데이터 플랫폼에 주문 정보 전송
- 실패 시 DLQ 이동 (기존 구조 활용)

### 3.2 생성할 파일

| 파일 | 위치 | 설명 |
|------|------|------|
| `DataPlatformEventHandler.kt` | `order/application/handler/` | 이벤트 핸들러 |

### 3.3 상세 스펙

```kotlin
@Component
class DataPlatformEventHandler(
    private val dataPlatformClient: DataPlatformClient,
    private val orderQueryUseCase: OrderQueryUseCase,
    private val objectMapper: ObjectMapper
) : EventHandler {

    override fun supportedEventTypes() = listOf(EventRegistry.Payment.PAYMENT_COMPLETED)

    override fun handle(event: OutboxEvent): Boolean {
        // 1. 페이로드에서 orderId 추출
        // 2. 주문 정보 조회
        // 3. DTO 변환
        // 4. 데이터 플랫폼 전송
        // 5. 실패 시 false 반환 → DLQ 이동
    }
}
```

### 3.4 트랜잭션 흐름

```
[TX 1: 주문 생성]
OrderCommandUseCase.createOrder()
    ↓
Order + OutboxEvent 저장 (같은 TX)
    ↓
COMMIT

─────────── 트랜잭션 경계 ───────────

[TX 2: 이벤트 처리] (5초 후)
OutboxEventProcessor.processEvents()
    ↓
PaymentEventHandler → PAYMENT_COMPLETED 발행
    ↓
DataPlatformEventHandler → 데이터 플랫폼 전송
    ↓
전송 실패해도 주문에 영향 없음 ✅
```

### 3.5 완료 조건
- [ ] DataPlatformEventHandler 구현
- [ ] EventRegistry에 등록 확인
- [ ] 컴파일 성공
- [ ] 애플리케이션 정상 기동

---

## 4. Phase 3: 테스트 작성

> **실행 명령**: `"Phase 3 실행해줘"`

### 4.1 목표
- DataPlatformEventHandler 단위 테스트
- MockDataPlatformClient 동작 검증

### 4.2 생성할 파일

| 파일 | 위치 | 설명 |
|------|------|------|
| `DataPlatformEventHandlerTest.kt` | `order/application/handler/` | 핸들러 테스트 |
| `MockDataPlatformClientTest.kt` | `common/external/` | Mock 테스트 |

### 4.3 테스트 케이스

```kotlin
class DataPlatformEventHandlerTest {
    // 성공 케이스
    @Test fun `should return true when data platform responds success`()

    // 실패 케이스
    @Test fun `should return false when data platform responds failure`()

    // 예외 케이스
    @Test fun `should return false when exception occurs`()

    // 주문 조회 실패
    @Test fun `should return false when order not found`()
}
```

### 4.4 완료 조건
- [ ] DataPlatformEventHandlerTest 작성
- [ ] 모든 테스트 통과
- [ ] `./gradlew test` 성공

---

## 5. Phase 4: Transaction Diagnosis 문서

> **실행 명령**: `"Phase 4 실행해줘"`

### 5.1 목표
- 분산 트랜잭션 진단 및 설계 문서 작성
- 아키텍처 다이어그램 포함

### 5.2 생성할 파일

| 파일 | 위치 | 설명 |
|------|------|------|
| `TRANSACTION_DIAGNOSIS.md` | `docs/` | 분산 트랜잭션 문서 |

### 5.3 문서 구조

```markdown
# 분산 트랜잭션 진단 및 설계

## 1. 현재 아키텍처 분석
   - 모놀리식 구조
   - 트랜잭션 범위
   - 도메인 간 결합 지점

## 2. MSA 전환 시 문제점
   - 2PC 불가
   - 부분 실패 시나리오
   - 데이터 불일치 위험

## 3. 해결 방안: Saga 패턴
   - Orchestration vs Choreography
   - 현재 구현 방식
   - 보상 트랜잭션

## 4. 구현된 분산 트랜잭션 처리
   - Outbox 패턴
   - DLQ 및 재시도
   - 이벤트 흐름

## 5. 아키텍처 다이어그램
   - 현재 구조
   - MSA 전환 후 구조
   - Saga 흐름도
```

### 5.4 포함할 다이어그램

1. **현재 아키텍처** (모놀리식 + 이벤트 기반)
2. **MSA 전환 후 아키텍처** (도메인별 분리)
3. **Saga 패턴 흐름도** (정상/실패)
4. **Outbox 패턴 흐름도**

### 5.5 완료 조건
- [ ] TRANSACTION_DIAGNOSIS.md 작성
- [ ] 다이어그램 4개 포함
- [ ] 코드 예시 포함

---

## 6. Phase 5: 성능 최적화 (선택)

> **실행 명령**: `"Phase 5 실행해줘"`

### 6.1 목표
- popFromQueue 배치 최적화
- 코치님 피드백 추가 반영

### 6.2 수정할 파일

| 파일 | 수정 내용 |
|------|----------|
| `RedisCouponIssueAdapter.kt` | ZPOPMIN 배치 처리 |

### 6.3 현재 문제

```kotlin
// 현재: N번 RTT
override fun popFromQueue(couponId: Long, count: Int): List<Long> {
    repeat(count) {
        redisTemplate.opsForZSet().popMin(queueKey)  // 매번 RTT 발생
    }
}

// 개선: 1번 RTT (Lua 스크립트 또는 popMin with count)
override fun popFromQueue(couponId: Long, count: Int): List<Long> {
    return redisTemplate.opsForZSet().popMin(queueKey, count.toLong())
        ?.mapNotNull { it.value?.toString()?.toLongOrNull() }
        ?: emptyList()
}
```

### 6.4 완료 조건
- [ ] popFromQueue 배치 처리 구현
- [ ] 기존 테스트 통과
- [ ] 성능 개선 확인

---

## 7. 최종 체크리스트

> **실행 명령**: `"최종 검토해줘"`

### 7.1 STEP 15 체크리스트

| 항목 | 상태 | Phase |
|------|------|-------|
| DataPlatformClient 인터페이스 | ⬜ | 1 |
| MockDataPlatformClient 구현 | ⬜ | 1 |
| OrderInfoDto 정의 | ⬜ | 1 |
| DataPlatformEventHandler 구현 | ⬜ | 2 |
| 단위 테스트 작성 | ⬜ | 3 |
| 트랜잭션 분리 확인 | ⬜ | 2 |

### 7.2 STEP 16 체크리스트

| 항목 | 상태 | Phase |
|------|------|-------|
| 현재 아키텍처 분석 | ⬜ | 4 |
| MSA 전환 시 문제점 기술 | ⬜ | 4 |
| Saga 패턴 설명 | ⬜ | 4 |
| 보상 트랜잭션 예시 | ⬜ | 4 |
| 아키텍처 다이어그램 | ⬜ | 4 |

### 7.3 커밋 전략

```bash
# Phase 1 완료 후
git add . && git commit -m "feat(external): DataPlatformClient 인터페이스 및 Mock 구현

- DataPlatformClient 인터페이스 정의
- MockDataPlatformClient 구현 (@Profile 분기)
- OrderInfoDto, DataPlatformResponse 정의"

# Phase 2 완료 후
git add . && git commit -m "feat(order): 주문 정보 데이터 플랫폼 전송 EventHandler 구현

- DataPlatformEventHandler 구현
- PAYMENT_COMPLETED 이벤트 수신 시 전송
- 실패 시 DLQ 이동 처리"

# Phase 3 완료 후
git add . && git commit -m "test(order): DataPlatformEventHandler 단위 테스트"

# Phase 4 완료 후
git add . && git commit -m "docs(transaction): 분산 트랜잭션 진단 및 설계 문서

- TRANSACTION_DIAGNOSIS.md 작성
- 아키텍처 다이어그램 추가
- Saga 패턴 흐름도 추가"
```

### 7.4 PR 템플릿

```markdown
## :pushpin: [STEP 15-16] {이름} - e-commerce

---
### STEP 15 Application Event
- [x] 주문/예약 정보를 원 트랜잭션이 종료된 이후에 전송
- [x] 주문/예약 정보를 전달하는 부가 로직에 대한 관심사를 메인 서비스에서 분리

### STEP 16 Transaction Diagnosis
- [x] 도메인별로 트랜잭션이 분리되었을 때 발생 가능한 문제 파악
- [x] 트랜잭션이 분리되더라도 데이터 일관성을 보장할 수 있는 분산 트랜잭션 설계

### 간단 회고 (3줄 이내)
- **잘한 점**:
- **어려운 점**:
- **다음 시도**:
```

---

## 변경 이력

| 날짜 | 버전 | 내용 |
|------|------|------|
| 2025-12-11 | 1.0 | 초안 작성 |
| 2025-12-11 | 2.0 | Phase 기반 실행 구조로 개선 |
