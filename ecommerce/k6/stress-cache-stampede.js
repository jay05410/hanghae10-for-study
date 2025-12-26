/**
 * 장애 시나리오 3: 인기상품 조회 캐시 스탬피드 (Cache Stampede)
 *
 * 시나리오:
 *   - 캐시가 만료되거나 없는 상태에서 400명이 동시에 인기상품/랭킹 조회
 *   - Thundering Herd 문제 시뮬레이션
 *   - 카테고리별 상품 조회 + 인기상품 + 일일 랭킹 동시 요청
 *
 * 예상 장애 지점:
 *   1. DB 부하 폭증 (캐시 미스 시 모든 요청이 DB로)
 *   2. DB 커넥션 풀 고갈 (HikariCP 10개)
 *   3. Redis 연결 풀 고갈 (max-active 20개)
 *   4. 응답시간 급증 및 타임아웃
 *   5. 서버 OOM (메모리 부족)
 *
 * 사용법:
 *   k6 run stress-cache-stampede.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { randomIntBetween, randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// ===== 커스텀 메트릭 =====
const productListCounter = new Counter('product_list_requests');
const productDetailCounter = new Counter('product_detail_requests');
const popularCounter = new Counter('popular_requests');
const rankingCounter = new Counter('ranking_requests');

const cacheHitCounter = new Counter('cache_hit');
const cacheMissCounter = new Counter('cache_miss');
const dbTimeoutCounter = new Counter('db_timeout');
const connectionTimeoutCounter = new Counter('connection_timeout');

const productListDuration = new Trend('product_list_duration');
const popularDuration = new Trend('popular_duration');
const rankingDuration = new Trend('ranking_duration');

const errorRate = new Rate('errors');
const successRate = new Rate('success');

// ===== 테스트 옵션 =====
export const options = {
    scenarios: {
        // 시나리오: 캐시 만료 후 동시 요청 폭주
        cache_stampede: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '5s', target: 200 },   // 급격한 증가 (캐시 미스 유발)
                { duration: '10s', target: 400 },  // 400명 동시 요청
                { duration: '1m', target: 400 },   // 유지 (캐시 워밍 후에도 부하 지속)
                { duration: '30s', target: 200 },  // 감소
                { duration: '10s', target: 0 },    // 종료
            ],
            gracefulRampDown: '10s',
        },
    },

    thresholds: {
        'http_req_duration': ['p(95)<3000'],       // p95 < 3초
        'popular_duration': ['p(95)<2000'],        // 인기상품 p95 < 2초
        'ranking_duration': ['p(95)<2000'],        // 랭킹 p95 < 2초
        'product_list_duration': ['p(95)<2000'],   // 상품목록 p95 < 2초
        'errors': ['rate<0.2'],                    // 에러율 20% 이내
    },
};

// 카테고리 ID (1~10)
const CATEGORY_IDS = Array.from({ length: 10 }, (_, i) => i + 1);

// 상품 ID (캐시 테스트용 - 인기 상품 집중)
const POPULAR_PRODUCT_IDS = Array.from({ length: 20 }, (_, i) => i + 1);

export default function () {
    const userId = randomIntBetween(1, 1000);

    // 요청 분배: 조회 API들에 집중
    const rand = randomIntBetween(1, 100);

    if (rand <= 30) {
        // 30%: 인기상품 조회
        group('인기상품 조회', () => {
            getPopularProducts();
        });
    } else if (rand <= 55) {
        // 25%: 일일 랭킹 조회
        group('일일 랭킹 조회', () => {
            getDailyRanking();
        });
    } else if (rand <= 75) {
        // 20%: 카테고리별 상품 조회
        group('카테고리 상품 조회', () => {
            getProductsByCategory();
        });
    } else if (rand <= 90) {
        // 15%: 상품 상세 조회 (조회수 증가 포함)
        group('상품 상세 조회', () => {
            getProductDetail(userId);
        });
    } else {
        // 10%: 상품 목록 조회
        group('상품 목록 조회', () => {
            getProductList();
        });
    }

    // 매우 짧은 대기 (캐시 스탬피드 시뮬레이션)
    sleep(randomIntBetween(0, 1) * 0.05);  // 0~50ms
}

/**
 * 인기상품 조회
 */
function getPopularProducts() {
    const limit = randomItem([5, 10, 20]);

    const startTime = Date.now();
    const response = http.get(
        `${BASE_URL}/api/v1/products/popular?limit=${limit}`,
        {
            headers: { 'Content-Type': 'application/json' },
            timeout: '15s',
        }
    );
    popularDuration.add(Date.now() - startTime);
    popularCounter.add(1);

    analyzeResponse(response, 'popular');
}

/**
 * 일일 랭킹 조회
 */
function getDailyRanking() {
    const startTime = Date.now();
    const response = http.get(
        `${BASE_URL}/api/v1/products/ranking/daily?limit=10`,
        {
            headers: { 'Content-Type': 'application/json' },
            timeout: '15s',
        }
    );
    rankingDuration.add(Date.now() - startTime);
    rankingCounter.add(1);

    analyzeResponse(response, 'ranking');
}

/**
 * 카테고리별 상품 조회
 */
function getProductsByCategory() {
    const categoryId = randomItem(CATEGORY_IDS);

    const startTime = Date.now();
    const response = http.get(
        `${BASE_URL}/api/v1/products?categoryId=${categoryId}&size=20`,
        {
            headers: { 'Content-Type': 'application/json' },
            timeout: '15s',
        }
    );
    productListDuration.add(Date.now() - startTime);
    productListCounter.add(1);

    analyzeResponse(response, 'category');
}

/**
 * 상품 상세 조회
 */
function getProductDetail(userId) {
    const productId = randomItem(POPULAR_PRODUCT_IDS);

    const startTime = Date.now();
    const response = http.get(
        `${BASE_URL}/api/v1/products/${productId}`,
        {
            headers: {
                'Content-Type': 'application/json',
                'User-Id': String(userId),
            },
            timeout: '15s',
        }
    );
    productDetailCounter.add(1);

    analyzeResponse(response, 'detail');
}

/**
 * 상품 목록 조회
 */
function getProductList() {
    const lastId = randomIntBetween(0, 80);

    const startTime = Date.now();
    const response = http.get(
        `${BASE_URL}/api/v1/products?lastId=${lastId}&size=20`,
        {
            headers: { 'Content-Type': 'application/json' },
            timeout: '15s',
        }
    );
    productListDuration.add(Date.now() - startTime);
    productListCounter.add(1);

    analyzeResponse(response, 'list');
}

/**
 * 응답 분석
 */
function analyzeResponse(response, apiName) {
    if (response.status === 200) {
        successRate.add(1);
        check(response, { [`${apiName}_success`]: () => true });

        // 캐시 히트 추정 (응답시간 기반)
        if (response.timings.duration < 50) {
            cacheHitCounter.add(1);
        } else if (response.timings.duration > 500) {
            cacheMissCounter.add(1);
        }
    } else if (response.status === 500 || response.status === 503) {
        errorRate.add(1);
        const body = response.body || '';
        if (body.includes('timeout') || body.includes('connection')) {
            dbTimeoutCounter.add(1);
        }
        console.log(`[${apiName} ERROR] ${response.status}: ${body.substring(0, 150)}`);
    } else if (response.status === 0) {
        connectionTimeoutCounter.add(1);
        errorRate.add(1);
        console.log(`[${apiName} TIMEOUT] Connection timeout`);
    } else {
        errorRate.add(1);
    }
}

// ===== 요약 리포트 =====
export function handleSummary(data) {
    const productList = data.metrics.product_list_requests?.values?.count || 0;
    const productDetail = data.metrics.product_detail_requests?.values?.count || 0;
    const popular = data.metrics.popular_requests?.values?.count || 0;
    const ranking = data.metrics.ranking_requests?.values?.count || 0;
    const total = data.metrics.http_reqs?.values?.count || 0;

    const cacheHit = data.metrics.cache_hit?.values?.count || 0;
    const cacheMiss = data.metrics.cache_miss?.values?.count || 0;
    const dbTimeout = data.metrics.db_timeout?.values?.count || 0;
    const connTimeout = data.metrics.connection_timeout?.values?.count || 0;

    const popularP95 = data.metrics.popular_duration?.values?.['p(95)']?.toFixed(2) || 0;
    const rankingP95 = data.metrics.ranking_duration?.values?.['p(95)']?.toFixed(2) || 0;
    const listP95 = data.metrics.product_list_duration?.values?.['p(95)']?.toFixed(2) || 0;
    const avgDuration = data.metrics.http_req_duration?.values?.avg?.toFixed(2) || 0;

    const cacheHitRate = (cacheHit + cacheMiss) > 0
        ? ((cacheHit / (cacheHit + cacheMiss)) * 100).toFixed(1)
        : 'N/A';

    console.log('\n');
    console.log('╔══════════════════════════════════════════════════════════════╗');
    console.log('║        장애 시나리오 3: 캐시 스탬피드 (Cache Stampede)       ║');
    console.log('╠══════════════════════════════════════════════════════════════╣');
    console.log(`║  총 요청 수:          ${total.toString().padStart(10)}                        ║`);
    console.log('╠══════════════════════════════════════════════════════════════╣');
    console.log('║  [API별 요청 수]                                             ║');
    console.log(`║    인기상품 조회:     ${popular.toString().padStart(10)}                        ║`);
    console.log(`║    일일 랭킹:         ${ranking.toString().padStart(10)}                        ║`);
    console.log(`║    상품 목록:         ${productList.toString().padStart(10)}                        ║`);
    console.log(`║    상품 상세:         ${productDetail.toString().padStart(10)}                        ║`);
    console.log('╠══════════════════════════════════════════════════════════════╣');
    console.log('║  [캐시 효율성]                                               ║');
    console.log(`║    캐시 히트 (추정):  ${cacheHit.toString().padStart(10)}                        ║`);
    console.log(`║    캐시 미스 (추정):  ${cacheMiss.toString().padStart(10)}                        ║`);
    console.log(`║    캐시 히트율:       ${cacheHitRate.toString().padStart(10)} %                    ║`);
    console.log('╠══════════════════════════════════════════════════════════════╣');
    console.log('║  [장애 지표]                                                 ║');
    console.log(`║    DB 타임아웃:       ${dbTimeout.toString().padStart(10)}  ⚠️  장애           ║`);
    console.log(`║    연결 타임아웃:     ${connTimeout.toString().padStart(10)}  ⚠️  장애           ║`);
    console.log('╠══════════════════════════════════════════════════════════════╣');
    console.log('║  [응답시간]                                                  ║');
    console.log(`║    평균:              ${avgDuration.toString().padStart(10)} ms                     ║`);
    console.log(`║    인기상품 p95:      ${popularP95.toString().padStart(10)} ms                     ║`);
    console.log(`║    랭킹 p95:          ${rankingP95.toString().padStart(10)} ms                     ║`);
    console.log(`║    상품목록 p95:      ${listP95.toString().padStart(10)} ms                     ║`);
    console.log('╠══════════════════════════════════════════════════════════════╣');

    // 장애 판정
    const hasFailure = dbTimeout > 0 || connTimeout > 0 ||
                       parseFloat(popularP95) > 2000 || parseFloat(rankingP95) > 2000;

    if (hasFailure) {
        console.log('║  [결과] ❌ 장애 발생                                         ║');
        if (dbTimeout > 0) console.log('║    - DB 타임아웃: 캐시 미스로 인한 DB 과부하                 ║');
        if (connTimeout > 0) console.log('║    - 연결 타임아웃: 커넥션 풀 고갈                           ║');
        if (parseFloat(popularP95) > 2000) console.log('║    - 인기상품 조회 지연: 캐시 효율 저하                      ║');
        if (parseFloat(cacheMiss) > parseFloat(cacheHit)) console.log('║    - 캐시 히트율 저조: Thundering Herd 발생                  ║');
    } else {
        console.log('║  [결과] ✅ 정상 처리                                         ║');
    }
    console.log('╚══════════════════════════════════════════════════════════════╝');
    console.log('\n');

    return {
        'stdout': '',
        './k6/results/cache-stampede-result.json': JSON.stringify(data, null, 2),
    };
}
