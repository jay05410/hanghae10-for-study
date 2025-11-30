# K6 부하테스트 가이드

## 📋 테스트 개요

캐시 구현 전후의 성능을 비교 측정하기 위한 K6 부하테스트 스크립트입니다.

## 🎯 테스트 시나리오

### 1. cache-performance-test.js
**캐시 적용 후 통합 성능 테스트**

#### 테스트 대상
- ✅ 상품 상세 조회 (로컬 캐시)
- ✅ 상품 목록 조회 (Redis 캐시 + 커서 페이징)
- ✅ 인기상품 조회 (2단계 캐시)
- ✅ 카테고리별 상품 조회 (Redis 캐시)

#### 부하 패턴
```
10명 → 50명 → 100명 → 200명 → 500명 (스파이크)
30초   1분    2분    2분    1분
```

### 2. cache-comparison-test.js
**캐시 적용 전후 비교 테스트**

#### 시나리오 구성
- **Without Cache**: 캐시 미스 시뮬레이션 (랜덤 상품 ID)
- **With Cache**: 캐시 히트 유도 (제한된 상품 ID 풀)

## 🚀 실행 방법

### 사전 준비
```bash
# K6 설치
brew install k6

# 애플리케이션 실행
./gradlew bootRun

# Redis 실행 확인
redis-cli ping
```

### 1. 통합 성능 테스트
```bash
cd k6
k6 run --out prometheus cache-performance-test.js
```

### 2. 캐시 비교 테스트
```bash
k6 run --out prometheus cache-comparison-test.js
```

### 3. 커스텀 옵션으로 실행
```bash
# VU 수와 지속시간 조정
k6 run --vus 100 --duration 5m cache-performance-test.js

# 결과를 JSON으로 출력
k6 run --out json=results.json cache-performance-test.js
```

## 📊 측정 메트릭

### 기본 K6 메트릭
- `http_req_duration`: 응답시간 (avg, p95, p99)
- `http_req_rate`: 초당 요청 수 (RPS)
- `http_req_failed`: 실패율
- `vus`: 가상 사용자 수

### 커스텀 메트릭
- `cache_hit_rate`: 캐시 히트율 (Rate)
- `db_query_count`: DB 쿼리 수 (Counter)
- `response_time_detail`: 상세 응답시간 (Trend)

### 비교 메트릭
- `before_cache_response_time`: 캐시 적용 전 응답시간
- `after_cache_response_time`: 캐시 적용 후 응답시간
- `cache_hits` / `cache_misses`: 히트/미스 횟수

## 🎯 성능 목표

### 응답시간 목표
| 기능 | 캐시 적용 전 | 캐시 적용 후 | 목표 개선율 |
|-----|-------------|-------------|------------|
| 상품 상세 | ~200ms | <50ms | **75% ↓** |
| 상품 목록 | ~500ms | <100ms | **80% ↓** |
| 인기상품 | ~1000ms | <30ms | **97% ↓** |

### 처리량 목표
- **동시 사용자**: 100명 → 500명+
- **RPS**: 500 → 2000+
- **캐시 히트율**: 80% 이상

### 임계치 설정
```javascript
thresholds: {
  http_req_duration: ['p(95)<500'],    // 95%가 500ms 이하
  http_req_failed: ['rate<0.01'],      // 에러율 1% 이하
  cache_hit_rate: ['rate>0.8'],        // 캐시 히트율 80% 이상
}
```

## 📈 결과 분석

### 1. K6 HTML 리포트
```bash
k6 run --out web-dashboard cache-performance-test.js
```

### 2. Prometheus + Grafana
- K6 메트릭이 Prometheus로 전송됨
- Grafana 대시보드에서 실시간 모니터링 가능

### 3. 주요 확인 포인트

#### 응답시간 개선
```
P95 응답시간 비교:
- 상품 상세: 200ms → 50ms (75% 개선)
- 상품 목록: 500ms → 100ms (80% 개선)
```

#### 캐시 효율성
```
캐시 히트율:
- 로컬 캐시 (상품 상세): >90%
- Redis 캐시 (상품 목록): >85%
- 2단계 캐시 (인기상품): >95%
```

#### 처리량 개선
```
동시 사용자 처리:
- 캐시 적용 전: 100명에서 타임아웃 발생
- 캐시 적용 후: 500명까지 안정적 처리
```

## 🔧 문제 해결

### 타임아웃 발생 시
```bash
# 타임아웃 시간 증가
k6 run --http-debug=full --timeout 30s cache-performance-test.js
```

### 메모리 부족 시
```bash
# VU 수 감소
k6 run --vus 50 --duration 2m cache-performance-test.js
```

### Redis 연결 문제 시
```bash
# Redis 상태 확인
redis-cli info memory
redis-cli info stats
```

## 📝 테스트 리포트 예시

### 성능 개선 결과 요약
```
🎯 캐시 구현 성과:

📊 응답시간 개선:
• 상품 상세: 180ms → 25ms (86% ↓)
• 상품 목록: 450ms → 85ms (81% ↓)
• 인기상품: 800ms → 20ms (97% ↓)

🚀 처리량 개선:
• 동시 사용자: 100명 → 500명 (5배 ↑)
• RPS: 400 → 2100 (5.25배 ↑)

💾 캐시 효율성:
• 전체 히트율: 87%
• 로컬 캐시: 92%
• Redis 캐시: 84%
• 2단계 캐시: 96%
```

이 테스트를 통해 캐시 구현의 실질적 성능 효과를 정량적으로 검증할 수 있습니다.