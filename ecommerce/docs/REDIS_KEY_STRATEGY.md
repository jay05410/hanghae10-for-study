# Redis 키 네이밍 및 관리 전략

## 개요

이 문서는 Redis 키 네이밍 규칙, TTL 전략, 메모리 최적화 방안을 정의합니다.

---

## 1. 키 네이밍 규칙

### 1.1 기본 형식

```
{service}:{domain}:{purpose}:{identifier}
```

| 세그먼트 | 설명 | 예시 |
|----------|------|------|
| service | 서비스 약어 (2-4자) | `ecom` |
| domain | 도메인 약어 (2-4자) | `stat`, `cpn`, `ord` |
| purpose | 목적 약어 (2-4자) | `rt` (realtime), `cache`, `lock` |
| identifier | 식별자 | `productId`, `userId:couponId` |

### 1.2 축약어 매핑 테이블

#### 서비스
| 원본 | 축약 |
|------|------|
| ecommerce | `ecom` |

#### 도메인
| 원본 | 축약 | 설명 |
|------|------|------|
| statistics | `stat` | 통계 |
| coupon | `cpn` | 쿠폰 |
| order | `ord` | 주문 |
| product | `prod` | 상품 |
| point | `pt` | 포인트 |
| inventory | `inv` | 재고 |

#### 목적 (Purpose)
| 원본 | 축약 | 설명 |
|------|------|------|
| realtime | `rt` | 실시간 데이터 |
| cache | `cache` | 캐시 데이터 |
| lock | `lock` | 분산락 |
| queue | `queue` | 대기열 |
| window | `win` | 시간 윈도우 |

#### 리소스
| 원본 | 축약 | 설명 |
|------|------|------|
| view | `view` | 조회수 |
| sales | `sales` | 판매량 |
| wish | `wish` | 찜 |
| popular | `pop` | 인기 |
| detail | `dtl` | 상세 |
| list | `list` | 목록 |
| waiting | `wait` | 대기 |
| user | `usr` | 사용자 |

### 1.3 키 예시

```
# 실시간 통계
ecom:stat:rt:view:{productId}:{minute}     # 상품별 분 단위 조회수
ecom:stat:rt:sales:{productId}             # 상품별 누적 판매량
ecom:stat:rt:wish:{productId}              # 상품별 찜 수
ecom:stat:pop:win:{windowId}               # 10분 윈도우별 인기 상품

# 캐시
ecom:cache:prod:dtl:{productId}            # 상품 상세 캐시
ecom:cache:prod:list:{cursor}              # 상품 목록 캐시
ecom:cache:prod:pop:{limit}                # 인기 상품 캐시

# 쿠폰 큐
ecom:cpn:queue:wait:{couponId}             # 쿠폰 대기열
ecom:cpn:queue:req:{queueId}               # 큐 요청 데이터
ecom:cpn:queue:usr:{userId}:{couponId}     # 사용자-쿠폰 매핑

# 분산락
ecom:lock:ord:process:{userId}             # 주문 처리 락
ecom:lock:pt:charge:{userId}               # 포인트 충전 락
ecom:lock:cpn:issue:{couponId}             # 쿠폰 발급 락
```

---

## 2. TTL 전략

### 2.1 목적별 TTL 분류

| 분류 | TTL | 용도 | 예시 |
|------|-----|------|------|
| **Realtime** | 10-30분 | 실시간 집계, 윈도우 데이터 | 조회수 윈도우, 인기 상품 윈도우 |
| **Short Cache** | 5분 | 자주 변경되는 목록 | 상품 목록, 카테고리별 상품 |
| **Long Cache** | 1시간 | 상세 정보, 스케줄러 갱신 대상 | 상품 상세, 인기 상품 (+ 워밍) |
| **Session** | 24시간 | 사용자 세션, 임시 데이터 | 쿠폰 큐 요청 |
| **Permanent** | 없음 | 누적 데이터 | 총 판매량, 찜 수 |

### 2.2 TTL 설정 코드

```kotlin
object RedisTTL {
    object Realtime {
        val VIEW_WINDOW = Duration.ofMinutes(15)
        val POPULAR_WINDOW = Duration.ofMinutes(15)
    }

    object Cache {
        val PRODUCT_LIST = Duration.ofMinutes(5)
        val PRODUCT_DETAIL = Duration.ofHours(1)
        val POPULAR_PRODUCTS = Duration.ofHours(1)  // + 30분 스케줄러 갱신
    }

    object Business {
        val COUPON_QUEUE = Duration.ofHours(1)
    }
}
```

---

## 3. 메모리 최적화

### 3.1 키 길이 비교

| 원본 | 최적화 | 절감 |
|------|--------|------|
| `stats:realtime:view:130:29876543` (34자) | `ecom:stat:rt:view:130:29876543` (31자) | 9% |
| `coupon:queue:user:12345:67890` (30자) | `ecom:cpn:queue:usr:12345:67890` (31자) | -3% |
| `ecommerce:product:popular` (25자) | `ecom:cache:prod:pop` (20자) | 20% |

> **참고**: 가독성을 위해 극단적 축약(2-3자)은 피하고, 중간 수준(3-5자)을 사용

### 3.2 대규모 데이터 처리

#### Sorted Set 샤딩 (1000만+ 상품)

```
# 문제: 단일 Sorted Set에 1000만 항목 → 메모리/성능 문제

# 해결책 1: 시간 윈도우 파티셔닝 (현재 적용)
ecom:stat:pop:win:12345    # 10분 윈도우 1
ecom:stat:pop:win:12346    # 10분 윈도우 2
→ 최근 2개 윈도우만 조회하여 병합

# 해결책 2: Top N 유지
ecom:stat:pop:top100       # 상위 100개만 유지
→ 스케줄러가 주기적으로 전체 집계 후 상위 N개만 저장

# 해결책 3: 해시 샤딩 (대규모)
ecom:stat:pop:shard:0      # productId % 10 == 0
ecom:stat:pop:shard:1      # productId % 10 == 1
...
→ 조회 시 10개 샤드 병합
```

---

## 4. 인스턴스 분리 전략

### 4.1 대규모 서비스 Redis 클러스터 구성

```yaml
redis:
  instances:
    # 캐시용 (휘발 OK, LRU 정책)
    cache:
      host: redis-cache.internal
      port: 6379
      maxmemory-policy: allkeys-lru
      maxmemory: 4gb

    # 세션/큐용 (데이터 보존 필요)
    session:
      host: redis-session.internal
      port: 6379
      maxmemory-policy: noeviction
      persistence:
        rdb: enabled
        aof: enabled

    # 분산락용 (고가용성)
    lock:
      host: redis-lock.internal
      port: 6379
      cluster: true
      replicas: 2
```

### 4.2 현재 프로젝트 (단일 인스턴스)

현재는 단일 Redis 인스턴스를 사용하되, **키 프리픽스로 논리적 분리**:

```
ecom:cache:*   → 캐시 데이터
ecom:stat:*    → 통계 데이터
ecom:lock:*    → 분산락
ecom:cpn:*     → 쿠폰 큐
```

---

## 5. 모니터링 및 관리

### 5.1 키 패턴별 메모리 사용량 확인

```bash
# 패턴별 키 개수
redis-cli --scan --pattern "ecom:stat:*" | wc -l
redis-cli --scan --pattern "ecom:cache:*" | wc -l

# 메모리 사용량 (샘플링)
redis-cli DEBUG OBJECT "ecom:stat:rt:view:1:29876543"
```

### 5.2 TTL 만료 키 모니터링

```bash
# 만료된 키 이벤트 구독
redis-cli psubscribe '__keyevent@0__:expired'
```

---

## 6. 코드 구조

### 6.1 키 관리 클래스

```kotlin
// RedisKeyNames.kt - 모든 Redis 키 중앙 관리
object RedisKeyNames {
    object Stats { ... }
    object Cache { ... }
    object Lock { ... }
    object Queue { ... }
}
```

### 6.2 사용 규칙

```kotlin
// ✅ 올바른 사용 - 상수 사용
val key = RedisKeyNames.Stats.viewKey(productId, minute)

// ❌ 잘못된 사용 - 직접 문자열 작성
val key = "ecom:stat:rt:view:$productId:$minute"
```

> **중요**: 개발자는 항상 `RedisKeyNames`를 통해 키를 생성해야 합니다.
> 직접 문자열을 작성하면 오타 및 불일치 위험이 있습니다.

---

## 변경 이력

| 날짜 | 변경 내용 |
|------|----------|
| 2024-XX-XX | 초기 작성 |
