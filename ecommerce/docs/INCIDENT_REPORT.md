# 장애 대응 보고서

> 테스트: 2025-12-26 | k6 (100 VUs, 30초) | Docker (AWS t4g.micro 시뮬레이션)

## 테스트 환경

| 컴포넌트 | 스펙 | AWS 대응 |
|---------|------|----------|
| MySQL | 1GB RAM | db.t4g.micro |
| Redis | 1GB RAM | t4g.micro |
| Kafka | 2GB RAM | t4g.small |

---

## 장애 1: Redis 캐시 역직렬화 오류

### 증상
```
HTTP 500: class java.util.LinkedHashMap cannot be cast to class
io.hhplus.ecommerce.common.response.Cursor
```
- 캐시 히트 시 100% 에러 → API 전면 장애

### 원인
Jackson ObjectMapper가 Kotlin `data class`(기본 final)의 제네릭 타입 정보 미보존
- `Cursor<T>` → Redis 저장 → `LinkedHashMap`으로 역직렬화 → `ClassCastException`

### 조치
**RedisConfig.kt**: `ObjectMapper.DefaultTyping.NON_FINAL` → `EVERYTHING`
- 모든 타입에 `@class` 정보 포함하여 타입 보존

### 결과
| 지표 | 전 | 후 |
|------|---|---|
| 에러율 | 100% | **0%** |
| 캐시 히트율 | 0% | **100%** |
| 인기상품 p95 | - | **7ms** |

---

## 장애 2: 재고 경합 시 체크아웃 응답시간 폭증

### 증상
| 지표 | 전 | 후 | 개선율 |
|------|---|---|--------|
| 체크아웃 p95 | 5,423ms | **221ms** | 24배 ↓ |
| 전체 플로우 p95 | 7,746ms | **476ms** | 16배 ↓ |
| 처리량 | 2,405 req | 11,394 req | 4.7배 ↑ |

### 원인
동일 상품에 `SELECT FOR UPDATE` 집중 → DB 락 대기 수 초 발생

### 조치
**Kafka 2단계 체크아웃** (선착순 상품용):
1. 요청 즉시 Kafka 큐 등록 → 대기열 순번 반환
2. Consumer가 순차 처리 (상품 ID = 파티션 키)
3. SSE로 결과 실시간 푸시

---

## 장애 3: 결제 성공률 저하

### 증상
결제 성공률 76.5% → 동일 사용자 동시 체크아웃 시 포인트 소진

### 조치
**CheckoutUseCase.kt**: 체크아웃 시 포인트 잔액 사전 검증
- 잔액 부족 시 체크아웃 거부 (`InsufficientBalance`)

### 결과
결제 성공률 **100%**, 실패 **0건**

---

## 장애 4: 재고 예약 시 낙관적 락 충돌

### 증상
```
Row was updated or deleted by another transaction
[InventoryJpaEntity#4]
```

### 원인
`checkStockAvailability()` → JPA 캐시 저장 → `reserveStock()`의 `FOR UPDATE`가 stale 버전 반환 → 버전 불일치

### 조치
1. **별도 가용성 체크 제거**: `reserveStock()` 하나로 원자적 수행
2. **락 모드 강화**: `PESSIMISTIC_WRITE` → `PESSIMISTIC_FORCE_INCREMENT`
3. **재시도 로직**: 최대 3회, 지수 백오프 (50/100/150ms)

### 결과
500 에러 **0건**, 체크아웃 p95 **304ms**

---

## 장애 5: 블랙프라이데이 시뮬레이션 (500 VUs)

> 테스트: stress-event-simulation.js (500 VUs, 3분 50초)

### 증상
| 지표 | 결과 |
|------|------|
| 총 요청 | 56,148 |
| 성공률 | 99.4% |
| 5xx 에러 | 224건 |
| 연결 타임아웃 | 101건 |

| API | p95 |
|-----|----:|
| 상품 조회 | 1,156ms |
| 쿠폰 발급 | 1,190ms |
| 체크아웃 | 2,933ms |
| 결제 | 2,770ms |

### 리소스 모니터링 결과
| 컴포넌트 | 사용량 | 상태 |
|---------|--------|:----:|
| MySQL | 617MB / 1GB (60%) | ✅ 여유 |
| Redis | 40MB / 1GB (4%), 연결 12개 | ✅ 여유 |
| Kafka | 598MB / 2GB (29%) | ✅ 여유 |

### 원인 분석

**5xx 에러 (224건): Outbox 이벤트 처리 지연**
```
OrderEventHandler - 주문 확정 실패: 현재: EXPIRED, 시도: CONFIRMED
```
- 결제 완료 → Outbox 이벤트 발행 → 스케줄러 처리 대기
- 대기 중 주문 만료 스케줄러 먼저 실행 → 주문 EXPIRED 처리
- 이후 Outbox 이벤트 처리 시 상태 불일치 발생

**연결 타임아웃 (101건): 응답 지연**
- 서버 로그에 타임아웃 에러 없음
- 트래픽 증가로 응답 지연 → k6 클라이언트 타임아웃 (15초)

### 조치: Debezium CDC 기반 실시간 처리

**문제**: Outbox 폴링 방식 (5초 주기)이 주문 만료보다 느림

**해결**: CDC (Change Data Capture)로 즉시 처리
```
기존: Outbox INSERT → 5초 대기 → 폴링 → 처리
개선: Outbox INSERT → Debezium 감지 → Kafka 발행 → 즉시 처리
```

**변경 사항**:
1. `OutboxCdcConsumer` 생성: Debezium 메시지 실시간 소비
2. `OutboxEventProcessor` 주기 변경: 5초 → 60초 (fallback)
3. `outbox-connector.json`: eventId 헤더 추가

**기대 효과**: 주문 만료 전 결제 완료 이벤트 처리 보장

---

## 최종 결과

### SLA 달성 현황 (100 VUs 기준)

| 항목 | 목표 | 전 | 후 | 판정 |
|------|------|---|---|:----:|
| 체크아웃 p95 | < 2,000ms | 5,423ms | **304ms** | ✅ |
| 전체 플로우 p95 | < 2,000ms | 7,746ms | **408ms** | ✅ |
| 캐시 에러율 | < 10% | 100% | **0%** | ✅ |
| 결제 성공률 | > 95% | 76.5% | **100%** | ✅ |
| 낙관적 락 에러 | 0 | 다수 | **0건** | ✅ |
| 쿠폰 발급 p95 | < 2,000ms | - | **35ms** | ✅ |

### 스펙별 처리 한계

| VUs | 성공률 | 체크아웃 p95 | 판정 |
|:---:|:------:|:-----------:|:----:|
| 100 | 100% | 304ms | ✅ 정상 |
| 500 | 99.4% | 2,933ms | ⚠️ 스펙 한계 |
