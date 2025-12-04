# 캐시 구현 및 성능 최적화 보고서

## 📋 개요

이커머스 시스템의 상품 조회 성능 최적화를 위한 다층 캐시 아키텍처를 구현하고, 성능 개선 효과를 측정한 보고서입니다.

## 🎯 캐시 적용 목적

### 해결하고자 한 문제
1. **상품 상세 조회 빈번한 DB 접근**
2. **상품 목록 조회 시 일관성 문제** (서버별 상이한 결과)
3. **인기상품 조회 시 실시간 계산 부하** (1000명 동시 조회 시 Redis 과부하)
4. **대용량 트래픽 대응 필요**

### 기대 효과
- 응답시간 90% 이상 단축
- DB 부하 80% 이상 감소
- 동시 사용자 처리량 10배 증가
- 캐시 히트율 85% 이상 달성

## 🏗️ 캐시 아키텍처 설계

### 하이브리드 캐시 전략
```
┌─────────────────┬─────────────────┬─────────────────┐
│   로컬 캐시      │   Redis 캐시     │   2단계 캐싱     │
│   (Caffeine)    │   (분산)        │   (복합)        │
├─────────────────┼─────────────────┼─────────────────┤
│ • 상품 상세      │ • 상품 목록      │ • 인기상품 조회   │
│ • 쿠폰 정보      │ • 카테고리별     │                │
│                │ • 활성 쿠폰      │                │
└─────────────────┴─────────────────┴─────────────────┘
```

## 📊 캐시 적용 상세

### 1. 로컬 캐시 (Caffeine)

#### 적용 대상
| 기능 | 캐시명 | TTL | 최대 크기 | 적용 이유 |
|-----|--------|-----|----------|----------|
| **상품 상세 조회** | `PRODUCT_DETAIL` | 10분 | 500개 | 가장 빈번한 조회, 빠른 응답 필요 |
| **쿠폰 정보** | `COUPON_INFO` | 5분 | 200개 | 변경 빈도 고려한 TTL |
| **활성 쿠폰 목록** | `COUPON_ACTIVE_LIST` | 5분 | 100개 | 쿠폰 상태 변경 반영 |

#### 적용 위치
```kotlin
// ProductService.kt:47
@Cacheable(value = [CacheNames.PRODUCT_DETAIL], key = "#productId")
fun getProduct(productId: Long): Product

// CacheConfig.kt:56-60
cacheManager.setCacheSpecification(mapOf(
    CacheNames.PRODUCT_DETAIL to "maximumSize=500,expireAfterWrite=10m",
    CacheNames.COUPON_INFO to "maximumSize=200,expireAfterWrite=5m",
    CacheNames.COUPON_ACTIVE_LIST to "maximumSize=100,expireAfterWrite=5m"
))
```

### 2. Redis 분산 캐시

#### 적용 대상
| 기능 | 캐시명 | TTL | 적용 이유 |
|-----|--------|-----|----------|
| **상품 목록 조회** | `PRODUCT_LIST` | 5분 | 서버간 일관성, 커서 페이징 결과 |
| **카테고리별 목록** | `PRODUCT_CATEGORY_LIST` | 5분 | 분산 환경 동기화 |
| **인기상품 목록** | `PRODUCT_POPULAR` | 30초 | 실시간성 중요, 대량 트래픽 처리 |

#### 적용 위치
```kotlin
// ProductService.kt:38
@Cacheable(value = [CacheNames.PRODUCT_LIST],
    key = "'cursor:' + (#lastId ?: 'first') + ':' + #size",
    cacheManager = "redisCacheManager")
fun getProductsWithCursor(lastId: Long?, size: Int): Cursor<Product>

// GetProductQueryUseCase.kt:105
@Cacheable(value = [CacheNames.PRODUCT_POPULAR], key = "#limit", cacheManager = "redisCacheManager")
fun getPopularProducts(limit: Int = 10): List<Product>
```

### 3. 2단계 캐싱 (인기상품 최적화)

#### 문제 상황
- 1000명이 동시에 인기상품 조회 시 Redis 스캔 + 정렬 연산 반복
- EventBasedStatisticsService의 실시간 계산 부하

#### 해결책
```
1단계: Redis 통계 로그 → 실시간 인기 순위 계산 (복잡한 로직)
       ↓
2단계: @Cacheable → 최종 결과를 Redis 캐시 (30초 TTL)
       ↓
사용자: 캐시된 결과 즉시 반환 (대량 트래픽 처리)
```

## 🔄 캐시 무효화 전략

### 자동 무효화 (TTL 기반)
- **상품 상세**: 10분 (변경 빈도 낮음)
- **상품 목록**: 5분 (정기적 갱신)
- **인기상품**: 30초 (실시간성 보장)

### 수동 무효화 (이벤트 기반)
```kotlin
// ProductCommandUseCase.kt:59-64
@Caching(evict = [
    CacheEvict(value = [CacheNames.PRODUCT_DETAIL], key = "#productId"),
    CacheEvict(value = [CacheNames.PRODUCT_LIST], allEntries = true, cacheManager = "redisCacheManager"),
    CacheEvict(value = [CacheNames.PRODUCT_CATEGORY_LIST], allEntries = true, cacheManager = "redisCacheManager"),
    CacheEvict(value = [CacheNames.PRODUCT_POPULAR], allEntries = true, cacheManager = "redisCacheManager")
])
fun updateProduct(productId: Long, request: UpdateProductRequest): Product
```

## 🚀 성능 최적화 구현

### 1. 커서 기반 페이징
- **기존**: `OFFSET + LIMIT` → 대용량 데이터에서 성능 저하
- **개선**: `WHERE id > lastId` → 일정한 성능, 일관성 보장

```sql
-- 기존 (OFFSET 방식)
SELECT * FROM products WHERE deleted_at IS NULL ORDER BY id LIMIT 20 OFFSET 1000;

-- 개선 (커서 방식)
SELECT * FROM products WHERE deleted_at IS NULL AND id > 1020 ORDER BY id LIMIT 20;
```

### 2. 캐시 키 전략
```kotlin
// 상품 목록: 커서 + 크기별 캐시
key = "'cursor:' + (#lastId ?: 'first') + ':' + #size"

// 카테고리별: 카테고리 + 커서 조합
key = "#categoryId + ':cursor:' + (#lastId ?: 'first') + ':' + #size"
```

## 📈 예상 성능 개선 효과

### 응답시간 개선 (예상)
| 기능 | 캐시 적용 전 | 캐시 적용 후 | 개선율 |
|-----|-------------|-------------|--------|
| 상품 상세 조회 | 50ms | 5ms | **90% ↓** |
| 상품 목록 조회 | 200ms | 30ms | **85% ↓** |
| 인기상품 조회 | 500ms | 15ms | **97% ↓** |

### 처리량 개선 (예상)
| 시나리오 | 캐시 적용 전 | 캐시 적용 후 | 개선율 |
|---------|-------------|-------------|--------|
| 동시 사용자 | 100명 | 1000명+ | **10배 ↑** |
| 초당 요청 수 | 500 RPS | 5000+ RPS | **10배 ↑** |

## 🔧 기술 구현 세부사항

### 의존성 추가
```toml
# gradle/libs.versions.toml
caffeine = "3.1.8"

# build.gradle.kts
implementation(libs.caffeine)
```

### 캐시 설정
```kotlin
@Configuration
@EnableCaching
class CacheConfig {

    @Bean
    @Primary
    fun localCacheManager(): CacheManager // Caffeine

    @Bean("redisCacheManager")
    fun redisCacheManager(): RedisCacheManager // Redis
}
```

## 📊 성능 측정 계획

### 측정 메트릭
1. **응답시간** - 평균/P95/P99
2. **처리량** - RPS (Requests Per Second)
3. **캐시 히트율** - 각 캐시별 히트율
4. **메모리 사용량** - JVM 힙/Redis 메모리
5. **DB 쿼리 수** - 캐시로 인한 감소량
6. **에러율** - 안정성 검증

### 부하테스트 시나리오
- **K6 스크립트** 를 통한 자동화된 성능 측정
- **그라파나 대시보드** 를 통한 실시간 모니터링
- **캐시 적용 전후 비교** 를 통한 정량적 효과 측정

## 📋 모니터링 및 운영

### 메트릭 수집
- **Prometheus + Micrometer**: Spring Boot Actuator 메트릭
- **Grafana Dashboard**: 캐시 성능 시각화
- **K6**: 부하테스트 자동화

### 알림 및 임계치
- 캐시 히트율 < 80% 시 알림
- 응답시간 > 100ms 시 알림
- 메모리 사용량 > 80% 시 알림

## 🎯 결론

다층 캐시 아키텍처 구현을 통해:
1. **성능**: 응답시간 90% 이상 단축 예상
2. **확장성**: 동시 사용자 10배 증가 처리 가능
3. **안정성**: 캐시 무효화 전략으로 데이터 일관성 보장
4. **운영성**: 모니터링 체계로 안정적 운영 기반 구축

실제 성능 개선 효과는 K6 부하테스트를 통해 검증하고, 그라파나 대시보드를 통해 지속적으로 모니터링할 예정입니다.