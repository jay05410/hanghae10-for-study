/**
 * E-commerce 부하 테스트 스크립트
 *
 * 트래픽 분배 비율 (실제 이커머스 패턴 기반):
 * - 상품 조회: 45%
 * - 인기상품/랭킹: 25%
 * - 장바구니: 12%
 * - 체크아웃/주문: 10%
 * - 결제: 5%
 * - 쿠폰 발급: 3%
 *
 * 사용법:
 *   k6 run load-test.js                              # 기본 테스트
 *   k6 run --vus 50 --duration 2m load-test.js       # 50 VUs, 2분
 *   k6 run --env BASE_URL=http://host:port load-test.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { randomIntBetween, randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// ===== 환경 설정 =====
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// ===== 커스텀 메트릭 =====
const productViewCounter = new Counter('product_views');
const orderCounter = new Counter('orders_created');
const paymentCounter = new Counter('payments_processed');
const couponIssueCounter = new Counter('coupons_issued');
const errorRate = new Rate('errors');
const productDetailDuration = new Trend('product_detail_duration');
const checkoutDuration = new Trend('checkout_duration');
const paymentDuration = new Trend('payment_duration');
const couponIssueDuration = new Trend('coupon_issue_duration');

// ===== 테스트 옵션 =====
export const options = {
    // 단계별 부하 증가
    stages: [
        { duration: '30s', target: 10 },   // Warm-up: 10 VUs
        { duration: '1m', target: 30 },    // Ramp-up: 30 VUs
        { duration: '2m', target: 50 },    // 안정 상태: 50 VUs
        { duration: '1m', target: 100 },   // 피크: 100 VUs
        { duration: '30s', target: 0 },    // Ramp-down
    ],

    // SLA 기준 (t4g.micro 목표)
    thresholds: {
        'http_req_duration': ['p(95)<2000', 'p(99)<5000'],  // p95 < 2s, p99 < 5s
        'http_req_failed': ['rate<0.1'],                    // 에러율 10% 미만
        'http_reqs': ['rate>20'],                           // TPS 20 이상
        'product_detail_duration': ['p(95)<1000'],          // 상품 상세 p95 < 1s
        'checkout_duration': ['p(95)<3000'],                // 체크아웃 p95 < 3s
        'payment_duration': ['p(95)<3000'],                 // 결제 p95 < 3s
        'coupon_issue_duration': ['p(95)<2000'],            // 쿠폰 발급 p95 < 2s
        'errors': ['rate<0.1'],                             // 전체 에러율 10% 미만
    },
};

// ===== 테스트 데이터 =====
const TEST_DATA = {
    // 사용자 ID: 1~1000 (02-test-data.sql 기준)
    userIds: Array.from({ length: 1000 }, (_, i) => i + 1),

    // 상품 ID: 1~100 (02-test-data.sql 기준)
    productIds: Array.from({ length: 100 }, (_, i) => i + 1),

    // 재고 제한 상품 (동시성 테스트용): 1~5
    limitedStockProducts: [1, 2, 3, 4, 5],

    // 쿠폰 ID: 1~10 (02-test-data.sql 기준)
    couponIds: Array.from({ length: 10 }, (_, i) => i + 1),

    // 선착순 쿠폰 (동시성 테스트용): 1~3
    limitedCoupons: [1, 2, 3],
};

// ===== 헬퍼 함수 =====
function getRandomUserId() {
    return randomItem(TEST_DATA.userIds);
}

function getRandomProductId() {
    return randomItem(TEST_DATA.productIds);
}

function getRandomLimitedProductId() {
    return randomItem(TEST_DATA.limitedStockProducts);
}

function getRandomCouponId() {
    return randomItem(TEST_DATA.couponIds);
}

function getRandomLimitedCouponId() {
    return randomItem(TEST_DATA.limitedCoupons);
}

function getHeaders(userId = null) {
    const headers = {
        'Content-Type': 'application/json',
    };
    if (userId) {
        headers['User-Id'] = String(userId);
    }
    return headers;
}

// ===== 시나리오 함수 =====

/**
 * 상품 목록 조회 (트래픽 45% 중 일부)
 */
function browseProducts() {
    const lastId = randomIntBetween(0, 80);
    const size = randomItem([10, 20, 30]);

    const response = http.get(
        `${BASE_URL}/api/v1/products?lastId=${lastId}&size=${size}`,
        { headers: getHeaders() }
    );

    check(response, {
        'products_list_status_200': (r) => r.status === 200,
        'products_list_has_data': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.data && body.data.contents;
            } catch {
                return false;
            }
        },
    }) || errorRate.add(1);

    productViewCounter.add(1);
}

/**
 * 상품 상세 조회 (조회수 증가 포함)
 */
function viewProductDetail() {
    const userId = getRandomUserId();
    const productId = getRandomProductId();

    const response = http.get(
        `${BASE_URL}/api/v1/products/${productId}`,
        { headers: getHeaders(userId) }
    );

    productDetailDuration.add(response.timings.duration);

    check(response, {
        'product_detail_status_200': (r) => r.status === 200,
        'product_detail_has_id': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.data && body.data.id === productId;
            } catch {
                return false;
            }
        },
    }) || errorRate.add(1);

    productViewCounter.add(1);
}

/**
 * 인기 상품 조회 (캐시 히트율 테스트)
 */
function viewPopularProducts() {
    const limit = randomItem([5, 10, 20]);

    const response = http.get(
        `${BASE_URL}/api/v1/products/popular?limit=${limit}`,
        { headers: getHeaders() }
    );

    check(response, {
        'popular_status_200': (r) => r.status === 200,
    }) || errorRate.add(1);
}

/**
 * 판매 랭킹 조회
 */
function viewDailyRanking() {
    const response = http.get(
        `${BASE_URL}/api/v1/products/ranking/daily?limit=10`,
        { headers: getHeaders() }
    );

    check(response, {
        'ranking_status_200': (r) => r.status === 200,
    }) || errorRate.add(1);
}

/**
 * 장바구니 조회
 */
function viewCart() {
    const userId = getRandomUserId();

    const response = http.get(
        `${BASE_URL}/api/v1/cart?userId=${userId}`,
        { headers: getHeaders() }
    );

    check(response, {
        'cart_view_status_200': (r) => r.status === 200,
    }) || errorRate.add(1);
}

/**
 * 장바구니에 상품 추가
 */
function addToCart() {
    const userId = getRandomUserId();
    const productId = getRandomProductId();
    const quantity = randomIntBetween(1, 3);

    const payload = JSON.stringify({
        productId: productId,
        quantity: quantity,
    });

    const response = http.post(
        `${BASE_URL}/api/v1/cart/items?userId=${userId}`,
        payload,
        { headers: getHeaders() }
    );

    check(response, {
        'cart_add_status_200': (r) => r.status === 200,
    }) || errorRate.add(1);
}

/**
 * 체크아웃 (주문하기)
 * 재고 예약 + PENDING_PAYMENT 주문 생성
 */
function initiateCheckout() {
    const userId = getRandomUserId();
    const productId = randomIntBetween(1, 100) <= 10
        ? getRandomLimitedProductId()  // 10% 확률로 재고 제한 상품
        : getRandomProductId();
    const quantity = randomIntBetween(1, 2);

    const payload = JSON.stringify({
        userId: userId,
        directOrderItems: [
            {
                productId: productId,
                quantity: quantity,
            }
        ],
    });

    const response = http.post(
        `${BASE_URL}/api/v1/checkout/initiate`,
        payload,
        { headers: getHeaders() }
    );

    checkoutDuration.add(response.timings.duration);

    const success = check(response, {
        'checkout_status_200': (r) => r.status === 200,
        'checkout_has_order_id': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.data && body.data.orderId;
            } catch {
                return false;
            }
        },
    });

    if (!success) {
        errorRate.add(1);
    } else {
        orderCounter.add(1);
    }

    // 생성된 orderId와 결제 정보 반환 (결제 시나리오용)
    try {
        const body = JSON.parse(response.body);
        return body.data ? {
            orderId: body.data.orderId,
            userId: userId,
            finalAmount: body.data.finalAmount
        } : null;
    } catch {
        return null;
    }
}

/**
 * 결제 처리
 */
function processPayment(checkoutResult = null) {
    // checkoutResult가 없으면 먼저 체크아웃
    if (!checkoutResult) {
        checkoutResult = initiateCheckout();
        if (!checkoutResult) {
            return; // 체크아웃 실패 시 결제 진행 불가
        }
        sleep(0.1); // 체크아웃 후 짧은 대기
    }

    const payload = JSON.stringify({
        orderId: checkoutResult.orderId,
        userId: checkoutResult.userId,
        amount: checkoutResult.finalAmount,
        paymentMethod: 'BALANCE',
    });

    const response = http.post(
        `${BASE_URL}/api/v1/payments/process`,
        payload,
        { headers: getHeaders() }
    );

    paymentDuration.add(response.timings.duration);

    const success = check(response, {
        'payment_status_200': (r) => r.status === 200,
    });

    if (!success) {
        errorRate.add(1);
    } else {
        paymentCounter.add(1);
    }
}

/**
 * 쿠폰 발급 (동시성 테스트)
 */
function issueCoupon() {
    const userId = getRandomUserId();
    // 50% 확률로 선착순 쿠폰 선택 (동시성 경합 테스트)
    const couponId = randomIntBetween(1, 100) <= 50
        ? getRandomLimitedCouponId()
        : getRandomCouponId();

    const payload = JSON.stringify({
        couponId: couponId,
    });

    const response = http.post(
        `${BASE_URL}/api/v1/coupons/issue?userId=${userId}`,
        payload,
        { headers: getHeaders() }
    );

    couponIssueDuration.add(response.timings.duration);

    // 쿠폰 발급은 수량 소진 시 실패 가능하므로 200 또는 적절한 에러 허용
    const success = check(response, {
        'coupon_issue_status_ok': (r) => r.status === 200 || r.status === 409 || r.status === 400,
    });

    if (!success) {
        errorRate.add(1);
    }

    if (response.status === 200) {
        couponIssueCounter.add(1);
    }
}

/**
 * 포인트 조회
 */
function viewPoints() {
    const userId = getRandomUserId();

    const response = http.get(
        `${BASE_URL}/api/v1/points/${userId}`,
        { headers: getHeaders() }
    );

    check(response, {
        'points_status_200': (r) => r.status === 200,
    }) || errorRate.add(1);
}

// ===== 메인 테스트 함수 =====
export default function () {
    // 트래픽 비율에 따른 시나리오 선택
    const rand = randomIntBetween(1, 100);

    if (rand <= 25) {
        // 25%: 상품 목록 조회
        group('상품 목록 조회', () => {
            browseProducts();
        });
    } else if (rand <= 45) {
        // 20%: 상품 상세 조회
        group('상품 상세 조회', () => {
            viewProductDetail();
        });
    } else if (rand <= 60) {
        // 15%: 인기 상품 조회
        group('인기 상품 조회', () => {
            viewPopularProducts();
        });
    } else if (rand <= 70) {
        // 10%: 판매 랭킹 조회
        group('판매 랭킹 조회', () => {
            viewDailyRanking();
        });
    } else if (rand <= 77) {
        // 7%: 장바구니 조회
        group('장바구니 조회', () => {
            viewCart();
        });
    } else if (rand <= 82) {
        // 5%: 장바구니 추가
        group('장바구니 추가', () => {
            addToCart();
        });
    } else if (rand <= 92) {
        // 10%: 체크아웃/주문
        group('체크아웃', () => {
            initiateCheckout();
        });
    } else if (rand <= 97) {
        // 5%: 결제
        group('결제', () => {
            processPayment();
        });
    } else {
        // 3%: 쿠폰 발급
        group('쿠폰 발급', () => {
            issueCoupon();
        });
    }

    // Think time (사용자 행동 시뮬레이션)
    sleep(randomIntBetween(1, 3));
}

// ===== 요약 리포트 =====
export function handleSummary(data) {
    const summary = {
        'metrics': {
            'total_requests': data.metrics.http_reqs?.values?.count || 0,
            'avg_duration_ms': data.metrics.http_req_duration?.values?.avg?.toFixed(2) || 0,
            'p95_duration_ms': data.metrics.http_req_duration?.values?.['p(95)']?.toFixed(2) || 0,
            'p99_duration_ms': data.metrics.http_req_duration?.values?.['p(99)']?.toFixed(2) || 0,
            'error_rate': ((data.metrics.http_req_failed?.values?.rate || 0) * 100).toFixed(2) + '%',
            'tps': data.metrics.http_reqs?.values?.rate?.toFixed(2) || 0,
        },
        'custom_metrics': {
            'product_views': data.metrics.product_views?.values?.count || 0,
            'orders_created': data.metrics.orders_created?.values?.count || 0,
            'payments_processed': data.metrics.payments_processed?.values?.count || 0,
            'coupons_issued': data.metrics.coupons_issued?.values?.count || 0,
        },
        'thresholds': data.thresholds || {},
    };

    console.log('\n========== 테스트 결과 요약 ==========');
    console.log(`총 요청 수: ${summary.metrics.total_requests}`);
    console.log(`TPS: ${summary.metrics.tps}`);
    console.log(`평균 응답시간: ${summary.metrics.avg_duration_ms}ms`);
    console.log(`p95 응답시간: ${summary.metrics.p95_duration_ms}ms`);
    console.log(`p99 응답시간: ${summary.metrics.p99_duration_ms}ms`);
    console.log(`에러율: ${summary.metrics.error_rate}`);
    console.log('---------- 커스텀 메트릭 ----------');
    console.log(`상품 조회 수: ${summary.custom_metrics.product_views}`);
    console.log(`주문 생성 수: ${summary.custom_metrics.orders_created}`);
    console.log(`결제 처리 수: ${summary.custom_metrics.payments_processed}`);
    console.log(`쿠폰 발급 수: ${summary.custom_metrics.coupons_issued}`);
    console.log('=====================================\n');

    return {
        'stdout': JSON.stringify(summary, null, 2),
        'summary.json': JSON.stringify(data, null, 2),
    };
}
