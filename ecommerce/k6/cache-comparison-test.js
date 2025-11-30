import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// 비교 테스트용 메트릭
export let beforeCacheMetrics = {
  responseTime: new Trend('before_cache_response_time'),
  throughput: new Counter('before_cache_requests'),
  errors: new Counter('before_cache_errors')
};

export let afterCacheMetrics = {
  responseTime: new Trend('after_cache_response_time'),
  throughput: new Counter('after_cache_requests'),
  errors: new Counter('after_cache_errors'),
  cacheHits: new Counter('cache_hits'),
  cacheMisses: new Counter('cache_misses')
};

// 테스트 설정
export let options = {
  scenarios: {
    // 캐시 비활성화 시나리오 (비교 기준)
    without_cache: {
      executor: 'ramping-vus',
      exec: 'testWithoutCache',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 10 },
        { duration: '1m', target: 50 },
        { duration: '2m', target: 100 },
        { duration: '30s', target: 0 },
      ],
      tags: { scenario: 'without_cache' },
    },
    // 캐시 활성화 시나리오
    with_cache: {
      executor: 'ramping-vus',
      exec: 'testWithCache',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 10 },
        { duration: '1m', target: 50 },
        { duration: '2m', target: 100 },
        { duration: '1m', target: 200 },  // 캐시 있을 때만 고부하 테스트
        { duration: '30s', target: 0 },
      ],
      tags: { scenario: 'with_cache' },
      startTime: '4m', // 첫 번째 시나리오 완료 후 시작
    }
  },
};

const BASE_URL = 'http://localhost:8080/api/v1';
const headers = { 'Content-Type': 'application/json', 'User-Id': '1' };

/**
 * 캐시 비활성화 상태 테스트
 * 실제로는 캐시를 끌 수 없으므로, 다양한 파라미터로 캐시 미스 유도
 */
export function testWithoutCache() {
  // 매번 다른 상품 ID로 캐시 미스 유도
  const productId = Math.floor(Math.random() * 10000) + 1000;

  const startTime = Date.now();
  const response = http.get(`${BASE_URL}/products/${productId}`, {
    headers: headers,
    tags: { cache_status: 'miss_simulation' }
  });
  const duration = Date.now() - startTime;

  const success = check(response, {
    'status is 200 or 404': (r) => r.status === 200 || r.status === 404,
  });

  beforeCacheMetrics.responseTime.add(duration);
  beforeCacheMetrics.throughput.add(1);

  if (!success) {
    beforeCacheMetrics.errors.add(1);
  }

  sleep(0.1);
}

/**
 * 캐시 활성화 상태 테스트
 * 동일한 데이터 세트를 반복 조회하여 캐시 히트 유도
 */
export function testWithCache() {
  // 제한된 상품 ID 풀에서 선택하여 캐시 히트 유도
  const productIds = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
  const productId = productIds[Math.floor(Math.random() * productIds.length)];

  const startTime = Date.now();
  const response = http.get(`${BASE_URL}/products/${productId}`, {
    headers: headers,
    tags: { cache_status: 'hit_expected' }
  });
  const duration = Date.now() - startTime;

  const success = check(response, {
    'status is 200': (r) => r.status === 200,
    'response time < 50ms (cache hit)': (r) => r.timings.duration < 50,
  });

  afterCacheMetrics.responseTime.add(duration);
  afterCacheMetrics.throughput.add(1);

  // 캐시 히트/미스 판정 (응답시간 기준)
  if (duration < 50) {
    afterCacheMetrics.cacheHits.add(1);
  } else {
    afterCacheMetrics.cacheMisses.add(1);
  }

  if (!success) {
    afterCacheMetrics.errors.add(1);
  }

  // 추가 테스트: 상품 목록과 인기상품도 함께 테스트
  if (Math.random() > 0.7) {
    testProductListWithCache();
  }

  if (Math.random() > 0.8) {
    testPopularProductsWithCache();
  }

  sleep(0.1);
}

/**
 * 캐시 적용된 상품 목록 테스트
 */
function testProductListWithCache() {
  const sizes = [10, 20];
  const size = sizes[Math.floor(Math.random() * sizes.length)];

  const response = http.get(`${BASE_URL}/products?size=${size}`, {
    headers: headers,
    tags: { endpoint: 'product_list', cache_type: 'redis' }
  });

  check(response, {
    'product list cached response < 100ms': (r) => r.timings.duration < 100,
  });

  afterCacheMetrics.responseTime.add(response.timings.duration);
}

/**
 * 캐시 적용된 인기상품 테스트
 */
function testPopularProductsWithCache() {
  const response = http.get(`${BASE_URL}/products/popular?limit=10`, {
    headers: headers,
    tags: { endpoint: 'popular_products', cache_type: 'redis_2stage' }
  });

  check(response, {
    'popular products cached response < 30ms': (r) => r.timings.duration < 30,
  });

  afterCacheMetrics.responseTime.add(response.timings.duration);
}

/**
 * 캐시 워밍업
 */
export function setup() {
  console.log('🔥 Starting cache comparison test setup...');

  // 캐시 활성화 시나리오를 위한 데이터 워밍업
  const warmupIds = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];

  console.log('Warming up product detail cache...');
  for (const id of warmupIds) {
    http.get(`${BASE_URL}/products/${id}`, { headers });
  }

  console.log('Warming up product list cache...');
  http.get(`${BASE_URL}/products?size=20`, { headers });

  console.log('Warming up popular products cache...');
  http.get(`${BASE_URL}/products/popular?limit=10`, { headers });

  console.log('✅ Cache warm-up completed!');

  return {
    startTime: new Date().toISOString(),
    warmupCompleted: true
  };
}

/**
 * 테스트 결과 정리 및 요약
 */
export function teardown(data) {
  console.log('\n📊 === Cache Performance Comparison Test Results ===');
  console.log(`🕐 Test Duration: ${data.startTime} → ${new Date().toISOString()}`);
  console.log('\n📈 Expected Improvements with Cache:');
  console.log('• Response Time: 85-95% reduction');
  console.log('• Throughput: 5-10x increase');
  console.log('• Cache Hit Rate: >80%');
  console.log('• Error Rate: <1%');
  console.log('\n💡 Check Grafana dashboard for detailed metrics!');
  console.log('🎯 Key metrics to monitor:');
  console.log('  - before_cache_response_time vs after_cache_response_time');
  console.log('  - cache_hits / (cache_hits + cache_misses)');
  console.log('  - Total throughput improvement');
}