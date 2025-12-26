/**
 * 장애 시나리오 5: 복합 이벤트 시뮬레이션 (블랙프라이데이/타임세일)
 *
 * 시나리오:
 *   - 실제 대규모 이벤트 상황 시뮬레이션
 *   - 500명이 동시에 다양한 API 요청
 *   - 쿠폰 발급 + 상품 조회 + 주문 + 결제가 동시에 발생
 *
 * 예상 장애 지점:
 *   1. 전체적인 서버 과부하
 *   2. 다중 분산락 경합 (쿠폰 + 재고 + 포인트)
 *   3. DB 커넥션 풀 완전 고갈
 *   4. Redis 연결 풀 고갈
 *   5. Kafka 메시지 처리 지연
 *   6. JVM 메모리 부족 (GC 오버헤드)
 *
 * 사용법:
 *   k6 run stress-event-simulation.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { randomIntBetween, randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// ===== 커스텀 메트릭 =====
// API별 카운터
const productViewCounter = new Counter('product_view');
const couponIssueCounter = new Counter('coupon_issue');
const cartAddCounter = new Counter('cart_add');
const checkoutCounter = new Counter('checkout');
const paymentCounter = new Counter('payment');

// 성공/실패
const successCounter = new Counter('total_success');
const failCounter = new Counter('total_fail');

// 장애 지표
const lockTimeoutCounter = new Counter('lock_timeout');
const connectionTimeoutCounter = new Counter('connection_timeout');
const serverErrorCounter = new Counter('server_error');

// 응답시간
const productDuration = new Trend('product_duration');
const couponDuration = new Trend('coupon_duration');
const checkoutDuration = new Trend('checkout_duration');
const paymentDuration = new Trend('payment_duration');

const errorRate = new Rate('errors');

// ===== 테스트 옵션 =====
export const options = {
    scenarios: {
        // 이벤트 시작 전 트래픽
        pre_event: {
            executor: 'constant-vus',
            vus: 50,
            duration: '30s',
            startTime: '0s',
            exec: 'browsingScenario',
        },
        // 이벤트 오픈! (폭발적 증가)
        event_open: {
            executor: 'ramping-vus',
            startVUs: 50,
            stages: [
                { duration: '10s', target: 300 },  // 급격한 증가
                { duration: '30s', target: 500 },  // 최대 부하
                { duration: '1m', target: 500 },   // 유지
                { duration: '30s', target: 300 },  // 감소
            ],
            startTime: '30s',
            exec: 'eventScenario',
        },
        // 이벤트 후 잔여 트래픽
        post_event: {
            executor: 'ramping-vus',
            startVUs: 300,
            stages: [
                { duration: '30s', target: 100 },
                { duration: '20s', target: 0 },
            ],
            startTime: '3m',
            exec: 'browsingScenario',
        },
    },

    thresholds: {
        'http_req_duration': ['p(95)<5000'],
        'product_duration': ['p(95)<2000'],
        'coupon_duration': ['p(95)<5000'],
        'checkout_duration': ['p(95)<10000'],
        'payment_duration': ['p(95)<10000'],
        'errors': ['rate<0.3'],
    },
};

// ===== 테스트 데이터 =====
const LIMITED_COUPON_IDS = [1, 2, 3, 9, 10];  // 한정 쿠폰
const LIMITED_STOCK_PRODUCTS = [1, 2, 3, 4, 5];  // 재고 제한 상품
const NORMAL_PRODUCTS = Array.from({ length: 95 }, (_, i) => i + 6);

// ===== 이벤트 전 브라우징 시나리오 =====
export function browsingScenario() {
    const userId = randomIntBetween(1, 1000);

    const rand = randomIntBetween(1, 100);

    if (rand <= 60) {
        // 60%: 상품 조회
        browseProducts(userId);
    } else if (rand <= 80) {
        // 20%: 인기상품/랭킹
        viewPopularProducts();
    } else {
        // 20%: 장바구니 조회
        viewCart(userId);
    }

    sleep(randomIntBetween(1, 3));
}

// ===== 이벤트 시나리오 (메인 부하) =====
export function eventScenario() {
    const userId = randomIntBetween(1, 1000);

    // 이벤트 시 트래픽 분배
    const rand = randomIntBetween(1, 100);

    if (rand <= 20) {
        // 20%: 쿠폰 발급 러시
        group('쿠폰 발급', () => {
            issueCoupon(userId);
        });
    } else if (rand <= 40) {
        // 20%: 상품 상세 조회 (구매 의도)
        group('상품 조회', () => {
            viewProductDetail(userId);
        });
    } else if (rand <= 55) {
        // 15%: 장바구니 추가
        group('장바구니 추가', () => {
            addToCart(userId);
        });
    } else if (rand <= 75) {
        // 20%: 체크아웃 → 결제
        group('주문/결제', () => {
            const checkoutResult = initiateCheckout(userId);
            if (checkoutResult && checkoutResult.orderId) {
                sleep(0.1);
                processPayment(checkoutResult.orderId, userId, checkoutResult.amount);
            }
        });
    } else if (rand <= 90) {
        // 15%: 인기상품/랭킹 조회
        group('인기상품 조회', () => {
            viewPopularProducts();
        });
    } else {
        // 10%: 포인트 조회
        group('포인트 조회', () => {
            viewPoints(userId);
        });
    }

    // 이벤트 시에는 매우 짧은 대기
    sleep(randomIntBetween(0, 2) * 0.1);
}

// ===== API 함수들 =====

function browseProducts(userId) {
    const productId = randomItem([...LIMITED_STOCK_PRODUCTS, ...NORMAL_PRODUCTS.slice(0, 20)]);

    const startTime = Date.now();
    const response = http.get(
        `${BASE_URL}/api/v1/products/${productId}`,
        {
            headers: { 'Content-Type': 'application/json', 'User-Id': String(userId) },
            timeout: '15s',
        }
    );
    productDuration.add(Date.now() - startTime);
    productViewCounter.add(1);

    handleResponse(response, 'product');
}

function viewProductDetail(userId) {
    // 이벤트 시에는 인기 상품에 집중
    const productId = randomIntBetween(1, 100) <= 70
        ? randomItem(LIMITED_STOCK_PRODUCTS)
        : randomItem(NORMAL_PRODUCTS.slice(0, 20));

    const startTime = Date.now();
    const response = http.get(
        `${BASE_URL}/api/v1/products/${productId}`,
        {
            headers: { 'Content-Type': 'application/json', 'User-Id': String(userId) },
            timeout: '15s',
        }
    );
    productDuration.add(Date.now() - startTime);
    productViewCounter.add(1);

    handleResponse(response, 'product');
}

function viewPopularProducts() {
    const startTime = Date.now();
    const response = http.get(
        `${BASE_URL}/api/v1/products/popular?limit=10`,
        { headers: { 'Content-Type': 'application/json' }, timeout: '15s' }
    );
    productDuration.add(Date.now() - startTime);

    handleResponse(response, 'popular');
}

function viewCart(userId) {
    const response = http.get(
        `${BASE_URL}/api/v1/cart?userId=${userId}`,
        { headers: { 'Content-Type': 'application/json' }, timeout: '15s' }
    );

    handleResponse(response, 'cart');
}

function viewPoints(userId) {
    const response = http.get(
        `${BASE_URL}/api/v1/points/${userId}`,
        { headers: { 'Content-Type': 'application/json' }, timeout: '15s' }
    );

    handleResponse(response, 'points');
}

function issueCoupon(userId) {
    const couponId = randomItem(LIMITED_COUPON_IDS);

    const payload = JSON.stringify({ couponId: couponId });

    const startTime = Date.now();
    const response = http.post(
        `${BASE_URL}/api/v1/coupons/issue?userId=${userId}`,
        payload,
        { headers: { 'Content-Type': 'application/json' }, timeout: '30s' }
    );
    couponDuration.add(Date.now() - startTime);
    couponIssueCounter.add(1);

    handleResponse(response, 'coupon', true);  // 409도 정상으로 처리
}

function addToCart(userId) {
    const productId = randomItem([...LIMITED_STOCK_PRODUCTS, ...NORMAL_PRODUCTS.slice(0, 30)]);

    const payload = JSON.stringify({
        productId: productId,
        quantity: 1,
    });

    const response = http.post(
        `${BASE_URL}/api/v1/cart/items?userId=${userId}`,
        payload,
        { headers: { 'Content-Type': 'application/json' }, timeout: '15s' }
    );
    cartAddCounter.add(1);

    handleResponse(response, 'cart_add');
}

function initiateCheckout(userId) {
    const productId = randomIntBetween(1, 100) <= 50
        ? randomItem(LIMITED_STOCK_PRODUCTS)
        : randomItem(NORMAL_PRODUCTS.slice(0, 30));

    const payload = JSON.stringify({
        userId: userId,
        directOrderItems: [{ productId: productId, quantity: 1 }],
    });

    const startTime = Date.now();
    const response = http.post(
        `${BASE_URL}/api/v1/checkout/initiate`,
        payload,
        { headers: { 'Content-Type': 'application/json' }, timeout: '30s' }
    );
    checkoutDuration.add(Date.now() - startTime);
    checkoutCounter.add(1);

    if (response.status === 200) {
        successCounter.add(1);
        try {
            const body = JSON.parse(response.body);
            return { orderId: body.data?.orderId, amount: body.data?.finalAmount || 0 };
        } catch {
            return null;
        }
    } else {
        handleResponse(response, 'checkout', true);  // 재고 부족도 정상
        return null;
    }
}

function processPayment(orderId, userId, amount) {
    const payload = JSON.stringify({
        orderId: orderId,
        userId: userId,
        amount: amount,
        paymentMethod: 'BALANCE',
    });

    const startTime = Date.now();
    const response = http.post(
        `${BASE_URL}/api/v1/payments/process`,
        payload,
        { headers: { 'Content-Type': 'application/json' }, timeout: '60s' }
    );
    paymentDuration.add(Date.now() - startTime);
    paymentCounter.add(1);

    handleResponse(response, 'payment', true);
}

function handleResponse(response, apiName, allowClientError = false) {
    if (response.status === 200) {
        successCounter.add(1);
    } else if ((response.status === 400 || response.status === 409) && allowClientError) {
        // 비즈니스 로직 에러 (정상)
        successCounter.add(1);
    } else if (response.status === 500 || response.status === 503) {
        failCounter.add(1);
        serverErrorCounter.add(1);
        errorRate.add(1);

        const body = response.body || '';
        if (body.includes('분산락') || body.includes('lock')) {
            lockTimeoutCounter.add(1);
        }
    } else if (response.status === 0) {
        failCounter.add(1);
        connectionTimeoutCounter.add(1);
        errorRate.add(1);
    } else {
        failCounter.add(1);
        errorRate.add(1);
    }
}

// ===== 요약 리포트 =====
export function handleSummary(data) {
    const productView = data.metrics.product_view?.values?.count || 0;
    const couponIssue = data.metrics.coupon_issue?.values?.count || 0;
    const cartAdd = data.metrics.cart_add?.values?.count || 0;
    const checkout = data.metrics.checkout?.values?.count || 0;
    const payment = data.metrics.payment?.values?.count || 0;

    const totalSuccess = data.metrics.total_success?.values?.count || 0;
    const totalFail = data.metrics.total_fail?.values?.count || 0;
    const lockTimeout = data.metrics.lock_timeout?.values?.count || 0;
    const connTimeout = data.metrics.connection_timeout?.values?.count || 0;
    const serverError = data.metrics.server_error?.values?.count || 0;

    const total = data.metrics.http_reqs?.values?.count || 0;
    const tps = data.metrics.http_reqs?.values?.rate?.toFixed(2) || 0;

    const productP95 = data.metrics.product_duration?.values?.['p(95)']?.toFixed(2) || 0;
    const couponP95 = data.metrics.coupon_duration?.values?.['p(95)']?.toFixed(2) || 0;
    const checkoutP95 = data.metrics.checkout_duration?.values?.['p(95)']?.toFixed(2) || 0;
    const paymentP95 = data.metrics.payment_duration?.values?.['p(95)']?.toFixed(2) || 0;

    console.log('\n');
    console.log('╔══════════════════════════════════════════════════════════════════════╗');
    console.log('║         장애 시나리오 5: 복합 이벤트 시뮬레이션 (블랙프라이데이)     ║');
    console.log('╠══════════════════════════════════════════════════════════════════════╣');
    console.log(`║  총 요청 수:          ${total.toString().padStart(10)}                              ║`);
    console.log(`║  TPS:                 ${tps.toString().padStart(10)} req/s                        ║`);
    console.log('╠══════════════════════════════════════════════════════════════════════╣');
    console.log('║  [API별 요청 수]                                                     ║');
    console.log(`║    상품 조회:         ${productView.toString().padStart(10)}                              ║`);
    console.log(`║    쿠폰 발급:         ${couponIssue.toString().padStart(10)}                              ║`);
    console.log(`║    장바구니 추가:     ${cartAdd.toString().padStart(10)}                              ║`);
    console.log(`║    체크아웃:          ${checkout.toString().padStart(10)}                              ║`);
    console.log(`║    결제:              ${payment.toString().padStart(10)}                              ║`);
    console.log('╠══════════════════════════════════════════════════════════════════════╣');
    console.log('║  [처리 결과]                                                         ║');
    console.log(`║    성공:              ${totalSuccess.toString().padStart(10)}                              ║`);
    console.log(`║    실패:              ${totalFail.toString().padStart(10)}                              ║`);
    console.log('╠══════════════════════════════════════════════════════════════════════╣');
    console.log('║  [장애 지표]                                                         ║');
    console.log(`║    분산락 타임아웃:   ${lockTimeout.toString().padStart(10)}  ⚠️  장애                 ║`);
    console.log(`║    연결 타임아웃:     ${connTimeout.toString().padStart(10)}  ⚠️  장애                 ║`);
    console.log(`║    서버 에러 (5xx):   ${serverError.toString().padStart(10)}  ⚠️  장애                 ║`);
    console.log('╠══════════════════════════════════════════════════════════════════════╣');
    console.log('║  [응답시간 p95]                                                      ║');
    console.log(`║    상품 조회:         ${productP95.toString().padStart(10)} ms                           ║`);
    console.log(`║    쿠폰 발급:         ${couponP95.toString().padStart(10)} ms                           ║`);
    console.log(`║    체크아웃:          ${checkoutP95.toString().padStart(10)} ms                           ║`);
    console.log(`║    결제:              ${paymentP95.toString().padStart(10)} ms                           ║`);
    console.log('╠══════════════════════════════════════════════════════════════════════╣');

    // 장애 판정
    const hasFailure = lockTimeout > 0 || connTimeout > 0 || serverError > 10 ||
                       parseFloat(checkoutP95) > 10000 || parseFloat(paymentP95) > 10000;

    if (hasFailure) {
        console.log('║  [결과] ❌ 장애 발생 - 이벤트 트래픽 처리 실패                       ║');
        if (lockTimeout > 0) console.log('║    - 다중 분산락 경합으로 인한 타임아웃                           ║');
        if (connTimeout > 0) console.log('║    - 서버 과부하로 인한 연결 실패                                 ║');
        if (serverError > 10) console.log('║    - 다수의 서버 에러 발생                                        ║');
        if (parseFloat(checkoutP95) > 10000) console.log('║    - 체크아웃 지연으로 인한 사용자 이탈 예상                     ║');
        if (parseFloat(paymentP95) > 10000) console.log('║    - 결제 지연으로 인한 매출 손실 예상                           ║');
    } else {
        console.log('║  [결과] ✅ 이벤트 트래픽 정상 처리                                   ║');
    }
    console.log('╚══════════════════════════════════════════════════════════════════════╝');
    console.log('\n');

    return {
        'stdout': '',
        './k6/results/event-simulation-result.json': JSON.stringify(data, null, 2),
    };
}
