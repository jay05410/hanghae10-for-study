/**
 * 장애 시나리오 2: 재고 동시주문 스트레스 테스트
 *
 * 시나리오:
 *   - 10개 재고 상품 5종에 300명이 동시 주문 시도
 *   - 한정판 상품 출시, 타임세일 등의 트래픽 패턴 시뮬레이션
 *
 * 예상 장애 지점:
 *   1. DB 락 경합 (PESSIMISTIC_WRITE)
 *   2. 분산락 타임아웃 (재고 예약 시 waitTime 10초 초과)
 *   3. 오버셀링 (동시성 제어 실패 시)
 *   4. DB 커넥션 풀 고갈
 *   5. 체크아웃 → 결제 → 주문 완료 과정의 트랜잭션 타임아웃
 *
 * 사용법:
 *   k6 run stress-inventory-contention.js
 *   k6 run --vus 300 --duration 2m stress-inventory-contention.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { randomIntBetween, randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// ===== 커스텀 메트릭 =====
// 체크아웃
const checkoutSuccessCounter = new Counter('checkout_success');
const checkoutFailCounter = new Counter('checkout_fail');
const outOfStockCounter = new Counter('out_of_stock');
const lockTimeoutCounter = new Counter('lock_timeout');
const checkoutDuration = new Trend('checkout_duration');

// 주문 생성 (직접 주문)
const orderSuccessCounter = new Counter('order_success');
const orderFailCounter = new Counter('order_fail');
const orderDuration = new Trend('order_duration');

// 결제
const paymentSuccessCounter = new Counter('payment_success');
const paymentFailCounter = new Counter('payment_fail');
const paymentDuration = new Trend('payment_duration');

// 전체
const connectionTimeoutCounter = new Counter('connection_timeout');
const errorRate = new Rate('errors');
const e2eDuration = new Trend('e2e_order_duration');  // 체크아웃 → 결제 전체

// ===== 테스트 옵션 =====
export const options = {
    scenarios: {
        // 시나리오: 한정판 출시 동시 주문
        flash_sale: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: 100 },  // 빠른 증가
                { duration: '30s', target: 200 },  // 200명까지
                { duration: '1m', target: 300 },   // 300명 유지 (메인 부하)
                { duration: '30s', target: 200 },  // 감소
                { duration: '10s', target: 0 },    // 종료
            ],
            gracefulRampDown: '10s',
        },
    },

    thresholds: {
        'http_req_duration': ['p(95)<5000'],       // p95 < 5초
        'checkout_duration': ['p(95)<10000'],      // 체크아웃 p95 < 10초
        'order_duration': ['p(95)<10000'],         // 주문 p95 < 10초
        'e2e_order_duration': ['p(95)<15000'],     // 전체 플로우 p95 < 15초
        'errors': ['rate<0.6'],                    // 에러율 60% 이내 (재고 부족 포함)
    },
};

// 재고 제한 상품 (10개씩) - 02-test-data.sql 기준
const LIMITED_STOCK_PRODUCTS = [1, 2, 3, 4, 5];

// 일반 재고 상품 (100개씩)
const NORMAL_STOCK_PRODUCTS = Array.from({ length: 95 }, (_, i) => i + 6);

export default function () {
    const userId = randomIntBetween(1, 1000);
    const e2eStartTime = Date.now();

    // 70% 확률로 재고 제한 상품, 30% 확률로 일반 상품
    const isLimitedProduct = randomIntBetween(1, 100) <= 70;
    const productId = isLimitedProduct
        ? randomItem(LIMITED_STOCK_PRODUCTS)
        : randomItem(NORMAL_STOCK_PRODUCTS);

    const quantity = isLimitedProduct ? 1 : randomIntBetween(1, 3);

    // ===== 시나리오 선택 =====
    // 50%: 체크아웃 → 결제 플로우
    // 50%: 직접 주문 생성
    const useCheckoutFlow = randomIntBetween(1, 100) <= 50;

    if (useCheckoutFlow) {
        // ===== 체크아웃 플로우 =====
        group('체크아웃 → 결제 플로우', () => {
            const orderId = initiateCheckout(userId, productId, quantity);

            if (orderId) {
                sleep(randomIntBetween(1, 3) * 0.1);  // 결제 준비 시간
                processPayment(orderId, userId);
            }
        });
    } else {
        // ===== 직접 주문 생성 =====
        group('직접 주문 생성', () => {
            createOrderDirectly(userId, productId, quantity);
        });
    }

    e2eDuration.add(Date.now() - e2eStartTime);

    // 짧은 대기
    sleep(randomIntBetween(1, 3) * 0.1);
}

/**
 * 체크아웃 (재고 예약 + PENDING_PAYMENT 주문 생성)
 */
function initiateCheckout(userId, productId, quantity) {
    const payload = JSON.stringify({
        userId: userId,
        directOrderItems: [
            { productId: productId, quantity: quantity }
        ],
    });

    const startTime = Date.now();
    const response = http.post(
        `${BASE_URL}/api/v1/checkout/initiate`,
        payload,
        {
            headers: { 'Content-Type': 'application/json' },
            timeout: '30s',
        }
    );
    checkoutDuration.add(Date.now() - startTime);

    if (response.status === 200) {
        checkoutSuccessCounter.add(1);
        check(response, { 'checkout_success': () => true });

        try {
            const body = JSON.parse(response.body);
            return body.data?.orderId;
        } catch {
            return null;
        }
    } else if (response.status === 400 || response.status === 409) {
        outOfStockCounter.add(1);
        check(response, { 'out_of_stock': () => true });
    } else if (response.status === 500 || response.status === 503) {
        const body = response.body || '';
        if (body.includes('분산락') || body.includes('lock')) {
            lockTimeoutCounter.add(1);
        }
        checkoutFailCounter.add(1);
        errorRate.add(1);
        console.log(`[CHECKOUT ERROR] ${response.status}: ${body.substring(0, 150)}`);
    } else if (response.status === 0) {
        connectionTimeoutCounter.add(1);
        checkoutFailCounter.add(1);
        errorRate.add(1);
    } else {
        checkoutFailCounter.add(1);
        errorRate.add(1);
    }

    return null;
}

/**
 * 결제 처리
 */
function processPayment(orderId, userId) {
    const payload = JSON.stringify({
        orderId: orderId,
        userId: userId,
        paymentMethod: 'BALANCE',
        usePoints: 50000,
    });

    const startTime = Date.now();
    const response = http.post(
        `${BASE_URL}/api/v1/payments/process`,
        payload,
        {
            headers: { 'Content-Type': 'application/json' },
            timeout: '30s',
        }
    );
    paymentDuration.add(Date.now() - startTime);

    if (response.status === 200) {
        paymentSuccessCounter.add(1);
        check(response, { 'payment_success': () => true });
    } else if (response.status === 0) {
        connectionTimeoutCounter.add(1);
        paymentFailCounter.add(1);
        errorRate.add(1);
    } else {
        paymentFailCounter.add(1);
        errorRate.add(1);
        console.log(`[PAYMENT ERROR] ${response.status}: ${(response.body || '').substring(0, 150)}`);
    }
}

/**
 * 직접 주문 생성 (체크아웃 없이)
 */
function createOrderDirectly(userId, productId, quantity) {
    const payload = JSON.stringify({
        userId: userId,
        items: [
            { productId: productId, quantity: quantity }
        ],
    });

    const startTime = Date.now();
    const response = http.post(
        `${BASE_URL}/api/v1/orders`,
        payload,
        {
            headers: { 'Content-Type': 'application/json' },
            timeout: '30s',
        }
    );
    orderDuration.add(Date.now() - startTime);

    if (response.status === 200 || response.status === 201) {
        orderSuccessCounter.add(1);
        check(response, { 'order_success': () => true });
    } else if (response.status === 400 || response.status === 409) {
        outOfStockCounter.add(1);
    } else if (response.status === 500 || response.status === 503) {
        const body = response.body || '';
        if (body.includes('분산락') || body.includes('lock')) {
            lockTimeoutCounter.add(1);
        }
        orderFailCounter.add(1);
        errorRate.add(1);
        console.log(`[ORDER ERROR] ${response.status}: ${body.substring(0, 150)}`);
    } else if (response.status === 0) {
        connectionTimeoutCounter.add(1);
        orderFailCounter.add(1);
        errorRate.add(1);
    } else {
        orderFailCounter.add(1);
        errorRate.add(1);
    }
}

// ===== 요약 리포트 =====
export function handleSummary(data) {
    const checkoutSuccess = data.metrics.checkout_success?.values?.count || 0;
    const checkoutFail = data.metrics.checkout_fail?.values?.count || 0;
    const orderSuccess = data.metrics.order_success?.values?.count || 0;
    const orderFail = data.metrics.order_fail?.values?.count || 0;
    const paymentSuccess = data.metrics.payment_success?.values?.count || 0;
    const paymentFail = data.metrics.payment_fail?.values?.count || 0;
    const outOfStock = data.metrics.out_of_stock?.values?.count || 0;
    const lockTimeout = data.metrics.lock_timeout?.values?.count || 0;
    const connTimeout = data.metrics.connection_timeout?.values?.count || 0;
    const total = data.metrics.http_reqs?.values?.count || 0;

    const checkoutP95 = data.metrics.checkout_duration?.values?.['p(95)']?.toFixed(2) || 0;
    const orderP95 = data.metrics.order_duration?.values?.['p(95)']?.toFixed(2) || 0;
    const e2eP95 = data.metrics.e2e_order_duration?.values?.['p(95)']?.toFixed(2) || 0;

    const totalSuccess = checkoutSuccess + orderSuccess;
    const totalFail = checkoutFail + orderFail;

    console.log('\n');
    console.log('╔══════════════════════════════════════════════════════════════╗');
    console.log('║        장애 시나리오 2: 재고 동시주문 스트레스 테스트        ║');
    console.log('╠══════════════════════════════════════════════════════════════╣');
    console.log(`║  총 HTTP 요청:        ${total.toString().padStart(10)}                        ║`);
    console.log('╠══════════════════════════════════════════════════════════════╣');
    console.log('║  [체크아웃 → 결제 플로우]                                    ║');
    console.log(`║    체크아웃 성공:     ${checkoutSuccess.toString().padStart(10)}                        ║`);
    console.log(`║    결제 성공:         ${paymentSuccess.toString().padStart(10)}                        ║`);
    console.log(`║    결제 실패:         ${paymentFail.toString().padStart(10)}                        ║`);
    console.log('╠══════════════════════════════════════════════════════════════╣');
    console.log('║  [직접 주문 생성]                                            ║');
    console.log(`║    주문 성공:         ${orderSuccess.toString().padStart(10)}                        ║`);
    console.log(`║    주문 실패:         ${orderFail.toString().padStart(10)}                        ║`);
    console.log('╠══════════════════════════════════════════════════════════════╣');
    console.log('║  [장애 지표]                                                 ║');
    console.log(`║    재고 부족:         ${outOfStock.toString().padStart(10)}  (정상)              ║`);
    console.log(`║    분산락 타임아웃:   ${lockTimeout.toString().padStart(10)}  ⚠️  장애           ║`);
    console.log(`║    연결 타임아웃:     ${connTimeout.toString().padStart(10)}  ⚠️  장애           ║`);
    console.log('╠══════════════════════════════════════════════════════════════╣');
    console.log('║  [응답시간]                                                  ║');
    console.log(`║    체크아웃 p95:      ${checkoutP95.toString().padStart(10)} ms                     ║`);
    console.log(`║    주문생성 p95:      ${orderP95.toString().padStart(10)} ms                     ║`);
    console.log(`║    전체 플로우 p95:   ${e2eP95.toString().padStart(10)} ms                     ║`);
    console.log('╠══════════════════════════════════════════════════════════════╣');

    // 동시성 검증
    const limitedStockTotal = 5 * 10;  // 5개 상품 x 10개 재고
    console.log('║  [오버셀링 검증]                                             ║');
    console.log(`║    재고 제한 상품:    ${limitedStockTotal.toString().padStart(10)} 개                     ║`);
    console.log(`║    성공 주문 수:      ${totalSuccess.toString().padStart(10)} 개                     ║`);

    // 장애 판정
    const hasFailure = lockTimeout > 0 || connTimeout > 0 ||
                       parseFloat(checkoutP95) > 10000 || parseFloat(e2eP95) > 15000;

    if (hasFailure) {
        console.log('╠══════════════════════════════════════════════════════════════╣');
        console.log('║  [결과] ❌ 장애 발생                                         ║');
        if (lockTimeout > 0) console.log('║    - 분산락 타임아웃: 동시 요청 처리 한계 도달              ║');
        if (connTimeout > 0) console.log('║    - 연결 타임아웃: 서버 과부하                             ║');
        if (parseFloat(checkoutP95) > 10000) console.log('║    - 체크아웃 p95 > 10초: 성능 저하                          ║');
        if (parseFloat(e2eP95) > 15000) console.log('║    - 전체 플로우 p95 > 15초: 심각한 지연                     ║');
    } else {
        console.log('╠══════════════════════════════════════════════════════════════╣');
        console.log('║  [결과] ✅ 정상 처리                                         ║');
    }

    // 오버셀링 검증
    if (totalSuccess > limitedStockTotal * 1.5) {  // 70% 비율 고려
        console.log('║  ⚠️  오버셀링 가능성: 성공 주문 > 예상 재고                   ║');
    }

    console.log('╚══════════════════════════════════════════════════════════════╝');
    console.log('\n');

    return {
        'stdout': '',
        './k6/results/inventory-contention-result.json': JSON.stringify(data, null, 2),
    };
}
