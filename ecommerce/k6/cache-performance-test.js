import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// 커스텀 메트릭 정의
export let cacheHitRate = new Rate('cache_hit_rate');
export let dbQueryCount = new Counter('db_query_count');
export let responseTime = new Trend('response_time_detail');

// 테스트 옵션 설정
export let options = {
  stages: [
    { duration: '30s', target: 10 },   // 워밍업
    { duration: '1m', target: 50 },   // 점진적 증가
    { duration: '2m', target: 100 },  // 안정적 부하
    { duration: '2m', target: 200 },  // 높은 부하
    { duration: '1m', target: 500 },  // 스파이크 테스트
    { duration: '30s', target: 0 },   // 정리
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95%의 요청이 500ms 이하
    http_req_failed: ['rate<0.01'],   // 에러율 1% 이하
    cache_hit_rate: ['rate>0.8'],     // 캐시 히트율 80% 이상
  },
};

// 기본 설정
const BASE_URL = 'http://localhost:8080/api/v1';
const headers = {
  'Content-Type': 'application/json',
  'User-Id': '1'
};

// 테스트 데이터 풀
const PRODUCT_IDS = Array.from({length: 100}, (_, i) => i + 1);
const CATEGORY_IDS = [1, 2, 3, 4, 5];

export default function () {
  // 1. 상품 상세 조회 테스트 (로컬 캐시)
  testProductDetail();

  sleep(0.1);

  // 2. 상품 목록 조회 테스트 (Redis 캐시)
  testProductList();

  sleep(0.1);

  // 3. 인기상품 조회 테스트 (2단계 캐시)
  testPopularProducts();

  sleep(0.1);

  // 4. 카테고리별 상품 조회 테스트 (Redis 캐시)
  testCategoryProducts();

  sleep(0.1);
}

/**
 * 상품 상세 조회 테스트
 * - 로컬 캐시(Caffeine) 성능 측정
 * - 동일 상품 반복 조회로 캐시 히트율 확인
 */
function testProductDetail() {
  const productId = PRODUCT_IDS[Math.floor(Math.random() * PRODUCT_IDS.length)];

  const response = http.get(`${BASE_URL}/products/${productId}`, {
    headers: headers,
    tags: { name: 'product_detail', cache_type: 'local' }
  });

  check(response, {
    'product detail status is 200': (r) => r.status === 200,
    'product detail response time < 100ms': (r) => r.timings.duration < 100,
    'product detail has data': (r) => JSON.parse(r.body).data !== null,
  });

  // 캐시 히트 여부 추정 (응답시간 기준)
  cacheHitRate.add(response.timings.duration < 50);
  responseTime.add(response.timings.duration);
}

/**
 * 상품 목록 조회 테스트
 * - Redis 캐시 + 커서 페이징 성능 측정
 * - 다양한 페이지 크기로 테스트
 */
function testProductList() {
  const size = [10, 20, 50][Math.floor(Math.random() * 3)];
  const lastId = Math.random() > 0.7 ? Math.floor(Math.random() * 100) : null;

  let url = `${BASE_URL}/products?size=${size}`;
  if (lastId) {
    url += `&lastId=${lastId}`;
  }

  const response = http.get(url, {
    headers: headers,
    tags: { name: 'product_list', cache_type: 'redis' }
  });

  check(response, {
    'product list status is 200': (r) => r.status === 200,
    'product list response time < 200ms': (r) => r.timings.duration < 200,
    'product list has cursor': (r) => {
      const data = JSON.parse(r.body).data;
      return data.contents && Array.isArray(data.contents);
    },
  });

  // 캐시 히트 여부 추정
  cacheHitRate.add(response.timings.duration < 100);
}

/**
 * 인기상품 조회 테스트
 * - 2단계 캐시 성능 측정
 * - 대량 동시 요청 처리 능력 확인
 */
function testPopularProducts() {
  const limit = [5, 10, 20][Math.floor(Math.random() * 3)];

  const response = http.get(`${BASE_URL}/products/popular?limit=${limit}`, {
    headers: headers,
    tags: { name: 'popular_products', cache_type: 'redis_2stage' }
  });

  check(response, {
    'popular products status is 200': (r) => r.status === 200,
    'popular products response time < 50ms': (r) => r.timings.duration < 50,
    'popular products count matches limit': (r) => {
      const products = JSON.parse(r.body).data;
      return products.length <= limit;
    },
  });

  // 2단계 캐시는 매우 빠른 응답이 기대됨
  cacheHitRate.add(response.timings.duration < 30);
}

/**
 * 카테고리별 상품 조회 테스트
 * - Redis 캐시 성능 측정
 * - 카테고리별 캐시 키 분리 확인
 */
function testCategoryProducts() {
  const categoryId = CATEGORY_IDS[Math.floor(Math.random() * CATEGORY_IDS.length)];
  const size = [10, 20][Math.floor(Math.random() * 2)];

  const response = http.get(`${BASE_URL}/products?categoryId=${categoryId}&size=${size}`, {
    headers: headers,
    tags: { name: 'category_products', cache_type: 'redis' }
  });

  check(response, {
    'category products status is 200': (r) => r.status === 200,
    'category products response time < 150ms': (r) => r.timings.duration < 150,
    'category products has data': (r) => {
      const data = JSON.parse(r.body).data;
      return data.contents !== null;
    },
  });

  cacheHitRate.add(response.timings.duration < 80);
}

/**
 * 설정 시나리오: 캐시 워밍업
 * - 테스트 시작 전 캐시를 미리 워밍업
 */
export function setup() {
  console.log('Starting cache warm-up...');

  // 상품 상세 캐시 워밍업
  for (let i = 1; i <= 20; i++) {
    http.get(`${BASE_URL}/products/${i}`, { headers });
  }

  // 상품 목록 캐시 워밍업
  http.get(`${BASE_URL}/products?size=20`, { headers });

  // 인기상품 캐시 워밍업
  http.get(`${BASE_URL}/products/popular?limit=10`, { headers });

  console.log('Cache warm-up completed!');
  return { timestamp: new Date().toISOString() };
}

/**
 * 정리 시나리오: 테스트 결과 요약
 */
export function teardown(data) {
  console.log(`Test completed at: ${new Date().toISOString()}`);
  console.log(`Test started at: ${data.timestamp}`);
}