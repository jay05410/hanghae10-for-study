/**
 * 체크아웃 비동기 큐 테스트
 *
 * POST /api/v1/checkout 엔드포인트
 * - Kafka 큐로 요청 → 순차 처리 → SSE 결과 푸시
 * - 동기 방식 대비 락 경합 감소
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { randomIntBetween, randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// 메트릭
const checkoutSuccess = new Counter('checkout_success');
const checkoutFail = new Counter('checkout_fail');
const checkoutDuration = new Trend('checkout_duration');
const errorRate = new Rate('errors');

export const options = {
    scenarios: {
        checkout_test: {
            executor: 'ramping-vus',
            startVUs: 10,
            stages: [
                { duration: '10s', target: 100 },
                { duration: '30s', target: 300 },
                { duration: '30s', target: 300 },
                { duration: '10s', target: 0 },
            ],
        },
    },
    thresholds: {
        'checkout_duration': ['p(95)<1000'],
        'errors': ['rate<0.1'],
    },
};

const HOT_PRODUCTS = [1, 2, 3, 4, 5];

export default function () {
    const userId = randomIntBetween(1, 1000);
    const productId = randomItem(HOT_PRODUCTS);

    // 새 API 형식: items 배열
    const payload = JSON.stringify({
        userId: userId,
        items: [
            {
                productId: productId,
                quantity: 1,
                giftWrap: false
            }
        ]
    });

    const startTime = Date.now();
    const response = http.post(
        `${BASE_URL}/api/v1/checkout`,
        payload,
        {
            headers: { 'Content-Type': 'application/json' },
            timeout: '10s',
        }
    );
    checkoutDuration.add(Date.now() - startTime);

    if (response.status === 200) {
        checkoutSuccess.add(1);
        errorRate.add(0);
    } else {
        checkoutFail.add(1);
        errorRate.add(1);
    }

    sleep(randomIntBetween(1, 3) * 0.1);
}

export function handleSummary(data) {
    const success = data.metrics.checkout_success?.values?.count || 0;
    const fail = data.metrics.checkout_fail?.values?.count || 0;
    const p95 = data.metrics.checkout_duration?.values?.['p(95)']?.toFixed(2) || 0;
    const total = data.metrics.http_reqs?.values?.count || 0;
    const tps = data.metrics.http_reqs?.values?.rate?.toFixed(2) || 0;

    console.log('\n');
    console.log('╔══════════════════════════════════════════════════════════════╗');
    console.log('║              체크아웃 비동기 테스트 결과                      ║');
    console.log('╠══════════════════════════════════════════════════════════════╣');
    console.log(`║  총 요청:       ${total.toString().padStart(10)}                             ║`);
    console.log(`║  TPS:           ${tps.toString().padStart(10)} req/s                       ║`);
    console.log(`║  성공:          ${success.toString().padStart(10)}                             ║`);
    console.log(`║  실패:          ${fail.toString().padStart(10)}                             ║`);
    console.log(`║  응답시간 p95:  ${p95.toString().padStart(10)} ms                          ║`);
    console.log('╚══════════════════════════════════════════════════════════════╝');
    console.log('\n');

    return {};
}
