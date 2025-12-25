/**
 * 장애 시나리오 4: 결제 동시 처리 폭주
 *
 * 시나리오:
 *   - 200명이 동시에 결제 처리 요청
 *   - 포인트 차감 + Kafka 이벤트 발행 부하
 *   - 타임세일 종료 직전 결제 몰림 시뮬레이션
 *
 * 예상 장애 지점:
 *   1. 포인트 분산락 타임아웃 (waitTime 120초지만 동시성 높으면 지연)
 *   2. Kafka 이벤트 발행 지연/실패
 *   3. DB 트랜잭션 타임아웃
 *   4. 포인트 잔액 부족 연쇄 실패
 *   5. 결제-주문 상태 불일치 (트랜잭션 롤백 시)
 *
 * 사용법:
 *   k6 run stress-payment-burst.js
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

// 결제
const paymentSuccessCounter = new Counter('payment_success');
const paymentFailCounter = new Counter('payment_fail');
const insufficientPointsCounter = new Counter('insufficient_points');
const paymentLockTimeoutCounter = new Counter('payment_lock_timeout');
const paymentDuration = new Trend('payment_duration');

// 포인트 충전
const chargeSuccessCounter = new Counter('point_charge_success');
const chargeFailCounter = new Counter('point_charge_fail');

// 전체
const connectionTimeoutCounter = new Counter('connection_timeout');
const kafkaErrorCounter = new Counter('kafka_error');
const errorRate = new Rate('errors');
const e2eDuration = new Trend('e2e_payment_duration');

// ===== 테스트 옵션 =====
export const options = {
    scenarios: {
        // 시나리오: 결제 마감 직전 폭주
        payment_rush: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: 50 },   // 워밍업
                { duration: '20s', target: 150 },  // 급격한 증가
                { duration: '1m', target: 200 },   // 200명 유지 (메인 부하)
                { duration: '30s', target: 100 },  // 감소
                { duration: '10s', target: 0 },    // 종료
            ],
            gracefulRampDown: '10s',
        },
    },

    thresholds: {
        'http_req_duration': ['p(95)<5000'],       // p95 < 5초
        'payment_duration': ['p(95)<10000'],       // 결제 p95 < 10초
        'e2e_payment_duration': ['p(95)<15000'],   // 전체 플로우 p95 < 15초
        'errors': ['rate<0.4'],                    // 에러율 40% 이내
    },
};

export default function () {
    const userId = randomIntBetween(1, 1000);
    const e2eStartTime = Date.now();

    group('결제 플로우', () => {
        // 1. 먼저 체크아웃으로 주문 생성
        const orderId = initiateCheckout(userId);

        if (orderId) {
            sleep(randomIntBetween(1, 3) * 0.1);

            // 2. 결제 처리
            const paymentSuccess = processPayment(orderId, userId);

            // 3. 결제 실패 시 포인트 부족이면 충전 후 재시도
            if (!paymentSuccess) {
                const retryChance = randomIntBetween(1, 100);
                if (retryChance <= 30) {  // 30% 확률로 재시도
                    chargePoints(userId);
                    sleep(0.1);
                    processPayment(orderId, userId);
                }
            }
        }
    });

    e2eDuration.add(Date.now() - e2eStartTime);

    // 짧은 대기
    sleep(randomIntBetween(1, 3) * 0.1);
}

/**
 * 체크아웃 (간소화)
 */
function initiateCheckout(userId) {
    const productId = randomIntBetween(6, 100);  // 재고 충분한 상품
    const quantity = 1;

    const payload = JSON.stringify({
        userId: userId,
        directOrderItems: [
            { productId: productId, quantity: quantity }
        ],
    });

    const response = http.post(
        `${BASE_URL}/api/v1/checkout/initiate`,
        payload,
        {
            headers: { 'Content-Type': 'application/json' },
            timeout: '30s',
        }
    );

    if (response.status === 200) {
        checkoutSuccessCounter.add(1);
        try {
            const body = JSON.parse(response.body);
            return body.data?.orderId;
        } catch {
            return null;
        }
    } else {
        checkoutFailCounter.add(1);
        if (response.status === 0) {
            connectionTimeoutCounter.add(1);
        }
        return null;
    }
}

/**
 * 결제 처리 (핵심 테스트)
 */
function processPayment(orderId, userId) {
    const usePoints = randomIntBetween(5000, 50000);

    const payload = JSON.stringify({
        orderId: orderId,
        userId: userId,
        paymentMethod: 'BALANCE',
        usePoints: usePoints,
    });

    const startTime = Date.now();
    const response = http.post(
        `${BASE_URL}/api/v1/payments/process`,
        payload,
        {
            headers: { 'Content-Type': 'application/json' },
            timeout: '60s',  // 결제는 타임아웃 길게
        }
    );
    paymentDuration.add(Date.now() - startTime);

    if (response.status === 200) {
        paymentSuccessCounter.add(1);
        check(response, { 'payment_success': () => true });
        return true;
    } else {
        const body = response.body || '';

        if (response.status === 400) {
            // 포인트 부족
            if (body.includes('포인트') || body.includes('balance') || body.includes('insufficient')) {
                insufficientPointsCounter.add(1);
            } else {
                paymentFailCounter.add(1);
                errorRate.add(1);
            }
        } else if (response.status === 500 || response.status === 503) {
            paymentFailCounter.add(1);
            errorRate.add(1);

            if (body.includes('분산락') || body.includes('lock')) {
                paymentLockTimeoutCounter.add(1);
            }
            if (body.includes('kafka') || body.includes('Kafka') || body.includes('messaging')) {
                kafkaErrorCounter.add(1);
            }
            console.log(`[PAYMENT ERROR] ${response.status}: ${body.substring(0, 150)}`);
        } else if (response.status === 0) {
            connectionTimeoutCounter.add(1);
            paymentFailCounter.add(1);
            errorRate.add(1);
            console.log(`[PAYMENT TIMEOUT] Connection timeout for order: ${orderId}`);
        } else {
            paymentFailCounter.add(1);
            errorRate.add(1);
        }
        return false;
    }
}

/**
 * 포인트 충전
 */
function chargePoints(userId) {
    const amount = randomIntBetween(50000, 100000);

    const payload = JSON.stringify({
        amount: amount,
        description: 'Load test charge',
    });

    const response = http.post(
        `${BASE_URL}/api/v1/points/${userId}/charge`,
        payload,
        {
            headers: { 'Content-Type': 'application/json' },
            timeout: '30s',
        }
    );

    if (response.status === 200) {
        chargeSuccessCounter.add(1);
    } else {
        chargeFailCounter.add(1);
        if (response.status === 0) {
            connectionTimeoutCounter.add(1);
        }
    }
}

// ===== 요약 리포트 =====
export function handleSummary(data) {
    const checkoutSuccess = data.metrics.checkout_success?.values?.count || 0;
    const checkoutFail = data.metrics.checkout_fail?.values?.count || 0;
    const paymentSuccess = data.metrics.payment_success?.values?.count || 0;
    const paymentFail = data.metrics.payment_fail?.values?.count || 0;
    const insufficientPoints = data.metrics.insufficient_points?.values?.count || 0;
    const paymentLockTimeout = data.metrics.payment_lock_timeout?.values?.count || 0;
    const kafkaError = data.metrics.kafka_error?.values?.count || 0;
    const connTimeout = data.metrics.connection_timeout?.values?.count || 0;
    const chargeSuccess = data.metrics.point_charge_success?.values?.count || 0;
    const total = data.metrics.http_reqs?.values?.count || 0;

    const paymentP95 = data.metrics.payment_duration?.values?.['p(95)']?.toFixed(2) || 0;
    const paymentP99 = data.metrics.payment_duration?.values?.['p(99)']?.toFixed(2) || 0;
    const e2eP95 = data.metrics.e2e_payment_duration?.values?.['p(95)']?.toFixed(2) || 0;

    const successRate = (checkoutSuccess > 0)
        ? ((paymentSuccess / checkoutSuccess) * 100).toFixed(1)
        : '0';

    console.log('\n');
    console.log('╔══════════════════════════════════════════════════════════════╗');
    console.log('║        장애 시나리오 4: 결제 동시 처리 폭주                  ║');
    console.log('╠══════════════════════════════════════════════════════════════╣');
    console.log(`║  총 HTTP 요청:        ${total.toString().padStart(10)}                        ║`);
    console.log('╠══════════════════════════════════════════════════════════════╣');
    console.log('║  [결제 플로우]                                               ║');
    console.log(`║    체크아웃 성공:     ${checkoutSuccess.toString().padStart(10)}                        ║`);
    console.log(`║    결제 성공:         ${paymentSuccess.toString().padStart(10)}                        ║`);
    console.log(`║    결제 성공률:       ${successRate.toString().padStart(10)} %                    ║`);
    console.log(`║    포인트 충전:       ${chargeSuccess.toString().padStart(10)}                        ║`);
    console.log('╠══════════════════════════════════════════════════════════════╣');
    console.log('║  [실패 분석]                                                 ║');
    console.log(`║    포인트 부족:       ${insufficientPoints.toString().padStart(10)}  (정상)              ║`);
    console.log(`║    결제 실패:         ${paymentFail.toString().padStart(10)}                        ║`);
    console.log('╠══════════════════════════════════════════════════════════════╣');
    console.log('║  [장애 지표]                                                 ║');
    console.log(`║    분산락 타임아웃:   ${paymentLockTimeout.toString().padStart(10)}  ⚠️  장애           ║`);
    console.log(`║    Kafka 에러:        ${kafkaError.toString().padStart(10)}  ⚠️  장애           ║`);
    console.log(`║    연결 타임아웃:     ${connTimeout.toString().padStart(10)}  ⚠️  장애           ║`);
    console.log('╠══════════════════════════════════════════════════════════════╣');
    console.log('║  [응답시간]                                                  ║');
    console.log(`║    결제 p95:          ${paymentP95.toString().padStart(10)} ms                     ║`);
    console.log(`║    결제 p99:          ${paymentP99.toString().padStart(10)} ms                     ║`);
    console.log(`║    전체 플로우 p95:   ${e2eP95.toString().padStart(10)} ms                     ║`);
    console.log('╠══════════════════════════════════════════════════════════════╣');

    // 장애 판정
    const hasFailure = paymentLockTimeout > 0 || kafkaError > 0 || connTimeout > 0 ||
                       parseFloat(paymentP95) > 10000;

    if (hasFailure) {
        console.log('║  [결과] ❌ 장애 발생                                         ║');
        if (paymentLockTimeout > 0) console.log('║    - 결제 분산락 타임아웃: 동시 결제 처리 한계              ║');
        if (kafkaError > 0) console.log('║    - Kafka 이벤트 발행 실패: 메시지 큐 과부하               ║');
        if (connTimeout > 0) console.log('║    - 연결 타임아웃: 서버 과부하                             ║');
        if (parseFloat(paymentP95) > 10000) console.log('║    - 결제 p95 > 10초: 심각한 성능 저하                       ║');
    } else {
        console.log('║  [결과] ✅ 정상 처리                                         ║');
    }
    console.log('╚══════════════════════════════════════════════════════════════╝');
    console.log('\n');

    return {
        'stdout': '',
        './k6/results/payment-burst-result.json': JSON.stringify(data, null, 2),
    };
}
