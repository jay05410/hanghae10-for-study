/**
 * 기준선(Baseline) 개별 API 테스트
 *
 * 목적: 각 API의 단독 성능 측정 (병목 없는 상태의 기준값)
 * 사용법:
 *   k6 run baseline-api-test.js                                    # 전체 API
 *   k6 run --env API=products baseline-api-test.js                 # 특정 API만
 *   k6 run --env API=products --vus 20 --duration 1m baseline-api-test.js
 *
 * 지원 API:
 *   products, product-detail, popular, ranking,
 *   cart, cart-add, checkout, payment, coupon-issue, points
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend, Counter, Rate } from 'k6/metrics';
import { randomIntBetween, randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TARGET_API = __ENV.API || 'all';

// API별 응답시간 트렌드
const apiDuration = {
    products: new Trend('api_products_duration'),
    productDetail: new Trend('api_product_detail_duration'),
    popular: new Trend('api_popular_duration'),
    ranking: new Trend('api_ranking_duration'),
    cart: new Trend('api_cart_duration'),
    cartAdd: new Trend('api_cart_add_duration'),
    checkout: new Trend('api_checkout_duration'),
    payment: new Trend('api_payment_duration'),
    couponIssue: new Trend('api_coupon_issue_duration'),
    points: new Trend('api_points_duration'),
};

const successCounter = new Counter('api_success');
const failCounter = new Counter('api_fail');
const errorRate = new Rate('errors');

export const options = {
    stages: [
        { duration: '10s', target: 10 },  // Warm-up
        { duration: '1m', target: 10 },   // 안정 상태 측정
        { duration: '5s', target: 0 },    // Ramp-down
    ],

    thresholds: {
        'http_req_duration': ['p(95)<1500'],
        'http_req_failed': ['rate<0.05'],
    },
};

// ===== API 테스트 함수들 =====

function testProductsList() {
    const response = http.get(
        `${BASE_URL}/api/v1/products?size=20`,
        { headers: { 'Content-Type': 'application/json' } }
    );
    apiDuration.products.add(response.timings.duration);

    if (check(response, { 'products_list_ok': (r) => r.status === 200 })) {
        successCounter.add(1);
    } else {
        failCounter.add(1);
        errorRate.add(1);
    }
}

function testProductDetail() {
    const productId = randomIntBetween(1, 100);
    const userId = randomIntBetween(1, 1000);

    const response = http.get(
        `${BASE_URL}/api/v1/products/${productId}`,
        {
            headers: {
                'Content-Type': 'application/json',
                'User-Id': String(userId),
            }
        }
    );
    apiDuration.productDetail.add(response.timings.duration);

    if (check(response, { 'product_detail_ok': (r) => r.status === 200 })) {
        successCounter.add(1);
    } else {
        failCounter.add(1);
        errorRate.add(1);
    }
}

function testPopularProducts() {
    const response = http.get(
        `${BASE_URL}/api/v1/products/popular?limit=10`,
        { headers: { 'Content-Type': 'application/json' } }
    );
    apiDuration.popular.add(response.timings.duration);

    if (check(response, { 'popular_ok': (r) => r.status === 200 })) {
        successCounter.add(1);
    } else {
        failCounter.add(1);
        errorRate.add(1);
    }
}

function testDailyRanking() {
    const response = http.get(
        `${BASE_URL}/api/v1/products/ranking/daily?limit=10`,
        { headers: { 'Content-Type': 'application/json' } }
    );
    apiDuration.ranking.add(response.timings.duration);

    if (check(response, { 'ranking_ok': (r) => r.status === 200 })) {
        successCounter.add(1);
    } else {
        failCounter.add(1);
        errorRate.add(1);
    }
}

function testCart() {
    const userId = randomIntBetween(1, 1000);

    const response = http.get(
        `${BASE_URL}/api/v1/cart?userId=${userId}`,
        { headers: { 'Content-Type': 'application/json' } }
    );
    apiDuration.cart.add(response.timings.duration);

    if (check(response, { 'cart_ok': (r) => r.status === 200 })) {
        successCounter.add(1);
    } else {
        failCounter.add(1);
        errorRate.add(1);
    }
}

function testCartAdd() {
    const userId = randomIntBetween(1, 1000);
    const productId = randomIntBetween(1, 100);

    const payload = JSON.stringify({
        productId: productId,
        quantity: 1,
    });

    const response = http.post(
        `${BASE_URL}/api/v1/cart/items?userId=${userId}`,
        payload,
        { headers: { 'Content-Type': 'application/json' } }
    );
    apiDuration.cartAdd.add(response.timings.duration);

    if (check(response, { 'cart_add_ok': (r) => r.status === 200 })) {
        successCounter.add(1);
    } else {
        failCounter.add(1);
        errorRate.add(1);
    }
}

function testCheckout() {
    const userId = randomIntBetween(1, 1000);
    const productId = randomIntBetween(6, 100); // 재고 충분한 상품

    const payload = JSON.stringify({
        userId: userId,
        directOrderItems: [
            { productId: productId, quantity: 1 }
        ],
    });

    const response = http.post(
        `${BASE_URL}/api/v1/checkout/initiate`,
        payload,
        { headers: { 'Content-Type': 'application/json' } }
    );
    apiDuration.checkout.add(response.timings.duration);

    if (check(response, { 'checkout_ok': (r) => r.status === 200 })) {
        successCounter.add(1);
        // orderId 반환
        try {
            const body = JSON.parse(response.body);
            return body.data?.orderId;
        } catch {
            return null;
        }
    } else {
        failCounter.add(1);
        errorRate.add(1);
        return null;
    }
}

function testPayment() {
    const orderId = testCheckout();
    if (!orderId) return;

    sleep(0.1);

    const userId = randomIntBetween(1, 1000);
    const payload = JSON.stringify({
        orderId: orderId,
        userId: userId,
        paymentMethod: 'BALANCE',
        usePoints: 0,
    });

    const response = http.post(
        `${BASE_URL}/api/v1/payments/process`,
        payload,
        { headers: { 'Content-Type': 'application/json' } }
    );
    apiDuration.payment.add(response.timings.duration);

    if (check(response, { 'payment_ok': (r) => r.status === 200 })) {
        successCounter.add(1);
    } else {
        failCounter.add(1);
        errorRate.add(1);
    }
}

function testCouponIssue() {
    const userId = randomIntBetween(1, 1000);
    const couponId = randomIntBetween(4, 10); // 수량 넉넉한 쿠폰

    const payload = JSON.stringify({
        couponId: couponId,
    });

    const response = http.post(
        `${BASE_URL}/api/v1/coupons/issue?userId=${userId}`,
        payload,
        { headers: { 'Content-Type': 'application/json' } }
    );
    apiDuration.couponIssue.add(response.timings.duration);

    // 이미 발급받은 경우 409도 정상
    if (check(response, { 'coupon_ok': (r) => r.status === 200 || r.status === 409 })) {
        successCounter.add(1);
    } else {
        failCounter.add(1);
        errorRate.add(1);
    }
}

function testPoints() {
    const userId = randomIntBetween(1, 1000);

    const response = http.get(
        `${BASE_URL}/api/v1/points/${userId}`,
        { headers: { 'Content-Type': 'application/json' } }
    );
    apiDuration.points.add(response.timings.duration);

    if (check(response, { 'points_ok': (r) => r.status === 200 })) {
        successCounter.add(1);
    } else {
        failCounter.add(1);
        errorRate.add(1);
    }
}

// ===== 메인 함수 =====
export default function () {
    const apiTests = {
        'products': testProductsList,
        'product-detail': testProductDetail,
        'popular': testPopularProducts,
        'ranking': testDailyRanking,
        'cart': testCart,
        'cart-add': testCartAdd,
        'checkout': testCheckout,
        'payment': testPayment,
        'coupon-issue': testCouponIssue,
        'points': testPoints,
    };

    if (TARGET_API === 'all') {
        // 모든 API 순차 테스트
        for (const [name, testFn] of Object.entries(apiTests)) {
            group(name, () => {
                testFn();
            });
            sleep(0.5);
        }
    } else if (apiTests[TARGET_API]) {
        // 특정 API만 테스트
        apiTests[TARGET_API]();
    } else {
        console.log(`Unknown API: ${TARGET_API}`);
        console.log(`Available APIs: ${Object.keys(apiTests).join(', ')}`);
    }

    sleep(1);
}

export function handleSummary(data) {
    console.log('\n========== Baseline API 테스트 결과 ==========');

    const apiNames = [
        'products', 'product_detail', 'popular', 'ranking',
        'cart', 'cart_add', 'checkout', 'payment', 'coupon_issue', 'points'
    ];

    for (const name of apiNames) {
        const metric = data.metrics[`api_${name}_duration`];
        if (metric && metric.values) {
            console.log(`\n[${name}]`);
            console.log(`  Count: ${metric.values.count || 0}`);
            console.log(`  Avg: ${metric.values.avg?.toFixed(2) || 0}ms`);
            console.log(`  p50: ${metric.values.med?.toFixed(2) || 0}ms`);
            console.log(`  p95: ${metric.values['p(95)']?.toFixed(2) || 0}ms`);
            console.log(`  p99: ${metric.values['p(99)']?.toFixed(2) || 0}ms`);
        }
    }

    console.log('\n==============================================\n');

    return {
        'stdout': '',
        'baseline-result.json': JSON.stringify(data, null, 2),
    };
}
