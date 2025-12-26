/**
 * 장애 시나리오 1: 쿠폰 이벤트 트래픽 폭주
 *
 * 시나리오:
 *   - 선착순 100개 한정 쿠폰 3종에 500명이 동시 발급 시도
 *   - 실제 이벤트 오픈 시 발생하는 트래픽 패턴 시뮬레이션
 *
 * 예상 장애 지점:
 *   1. 분산락 타임아웃 (waitTime 10초 초과)
 *   2. Redis SADD 경합으로 인한 지연
 *   3. DB 커넥션 풀 고갈 (HikariCP 기본 10개)
 *   4. 응답시간 급증으로 인한 타임아웃
 *
 * 사용법:
 *   k6 run stress-coupon-rush.js
 *   k6 run --vus 500 --duration 1m stress-coupon-rush.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// ===== 커스텀 메트릭 =====
const successCounter = new Counter('coupon_issue_success');
const failCounter = new Counter('coupon_issue_fail');
const duplicateCounter = new Counter('coupon_duplicate_reject');
const soldOutCounter = new Counter('coupon_sold_out');
const lockTimeoutCounter = new Counter('coupon_lock_timeout');
const connectionTimeoutCounter = new Counter('connection_timeout');
const issueDuration = new Trend('coupon_issue_duration');
const errorRate = new Rate('errors');

// ===== 테스트 옵션 =====
export const options = {
    scenarios: {
        // 시나리오 1: 이벤트 오픈 순간 (스파이크)
        event_open_spike: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '5s', target: 300 },   // 5초만에 300명 폭증
                { duration: '30s', target: 500 },  // 500명까지 증가
                { duration: '1m', target: 500 },   // 500명 유지 (메인 부하)
                { duration: '30s', target: 300 },  // 점진적 감소
                { duration: '10s', target: 0 },    // 종료
            ],
            gracefulRampDown: '10s',
        },
    },

    thresholds: {
        // 장애 탐지용 임계치 (일부러 낮게 설정)
        'http_req_duration': ['p(95)<3000'],       // p95 < 3초 (위반 예상)
        'http_req_failed': ['rate<0.3'],           // 에러율 30% 이내
        'coupon_issue_duration': ['p(95)<5000'],   // 쿠폰 발급 p95 < 5초
        'errors': ['rate<0.5'],                    // 전체 에러율 50% 이내
    },
};

// 선착순 쿠폰 ID (100개 한정) - 02-test-data.sql 기준
const LIMITED_COUPON_IDS = [1, 2, 3];  // NEW10, FIRST15, VIP20

// 극소량 쿠폰 (추가 경합)
const SUPER_LIMITED_COUPON_IDS = [9, 10];  // PREMIUM50 (10개), LUCKY100 (5개)

export default function () {
    const userId = randomIntBetween(1, 1000);

    // 80% 확률로 100개 한정 쿠폰, 20% 확률로 극소량 쿠폰
    const couponId = randomIntBetween(1, 100) <= 80
        ? LIMITED_COUPON_IDS[randomIntBetween(0, 2)]
        : SUPER_LIMITED_COUPON_IDS[randomIntBetween(0, 1)];

    const payload = JSON.stringify({
        couponId: couponId,
    });

    const startTime = Date.now();

    const response = http.post(
        `${BASE_URL}/api/v1/coupons/issue?userId=${userId}`,
        payload,
        {
            headers: { 'Content-Type': 'application/json' },
            timeout: '30s',  // 타임아웃 30초
        }
    );

    const duration = Date.now() - startTime;
    issueDuration.add(duration);

    // 응답 분석 및 메트릭 수집
    if (response.status === 200) {
        successCounter.add(1);
        check(response, { 'coupon_issued': () => true });
    } else if (response.status === 409) {
        // 중복 발급 거절 (정상 동작)
        duplicateCounter.add(1);
        check(response, { 'duplicate_rejected': () => true });
    } else if (response.status === 400) {
        // 수량 소진 또는 유효성 실패
        const body = response.body || '';
        if (body.includes('소진') || body.includes('exhausted') || body.includes('sold out')) {
            soldOutCounter.add(1);
            check(response, { 'sold_out': () => true });
        } else {
            failCounter.add(1);
            errorRate.add(1);
        }
    } else if (response.status === 500 || response.status === 503) {
        // 서버 에러 (분산락 타임아웃 등)
        const body = response.body || '';
        if (body.includes('분산락') || body.includes('lock')) {
            lockTimeoutCounter.add(1);
        }
        failCounter.add(1);
        errorRate.add(1);
        console.log(`[ERROR] Status: ${response.status}, Body: ${body.substring(0, 200)}`);
    } else if (response.status === 0) {
        // 연결 타임아웃
        connectionTimeoutCounter.add(1);
        failCounter.add(1);
        errorRate.add(1);
        console.log(`[TIMEOUT] Connection timeout for userId: ${userId}`);
    } else {
        failCounter.add(1);
        errorRate.add(1);
        console.log(`[UNEXPECTED] Status: ${response.status}`);
    }

    // 매우 짧은 간격 (실제 이벤트 러시 시뮬레이션)
    sleep(randomIntBetween(0, 2) * 0.05);  // 0~100ms
}

// ===== 요약 리포트 =====
export function handleSummary(data) {
    const success = data.metrics.coupon_issue_success?.values?.count || 0;
    const duplicate = data.metrics.coupon_duplicate_reject?.values?.count || 0;
    const soldOut = data.metrics.coupon_sold_out?.values?.count || 0;
    const lockTimeout = data.metrics.coupon_lock_timeout?.values?.count || 0;
    const connTimeout = data.metrics.connection_timeout?.values?.count || 0;
    const fail = data.metrics.coupon_issue_fail?.values?.count || 0;
    const total = data.metrics.http_reqs?.values?.count || 0;

    const p95 = data.metrics.coupon_issue_duration?.values?.['p(95)']?.toFixed(2) || 0;
    const p99 = data.metrics.coupon_issue_duration?.values?.['p(99)']?.toFixed(2) || 0;
    const avg = data.metrics.coupon_issue_duration?.values?.avg?.toFixed(2) || 0;

    console.log('\n');
    console.log('╔══════════════════════════════════════════════════════════════╗');
    console.log('║        장애 시나리오 1: 쿠폰 이벤트 트래픽 폭주              ║');
    console.log('╠══════════════════════════════════════════════════════════════╣');
    console.log(`║  총 요청 수:          ${total.toString().padStart(10)}                        ║`);
    console.log('╠══════════════════════════════════════════════════════════════╣');
    console.log('║  [성공/실패 분석]                                            ║');
    console.log(`║    발급 성공:         ${success.toString().padStart(10)}                        ║`);
    console.log(`║    중복 거절:         ${duplicate.toString().padStart(10)}  (정상)              ║`);
    console.log(`║    수량 소진:         ${soldOut.toString().padStart(10)}  (정상)              ║`);
    console.log(`║    분산락 타임아웃:   ${lockTimeout.toString().padStart(10)}  ⚠️  장애           ║`);
    console.log(`║    연결 타임아웃:     ${connTimeout.toString().padStart(10)}  ⚠️  장애           ║`);
    console.log(`║    기타 실패:         ${fail.toString().padStart(10)}                        ║`);
    console.log('╠══════════════════════════════════════════════════════════════╣');
    console.log('║  [응답시간]                                                  ║');
    console.log(`║    평균:              ${avg.toString().padStart(10)} ms                     ║`);
    console.log(`║    p95:               ${p95.toString().padStart(10)} ms                     ║`);
    console.log(`║    p99:               ${p99.toString().padStart(10)} ms                     ║`);
    console.log('╠══════════════════════════════════════════════════════════════╣');

    // 장애 판정
    const hasFailure = lockTimeout > 0 || connTimeout > 0 || parseFloat(p95) > 3000;
    if (hasFailure) {
        console.log('║  [결과] ❌ 장애 발생                                         ║');
        if (lockTimeout > 0) console.log('║    - 분산락 획득 실패로 인한 서비스 거부                     ║');
        if (connTimeout > 0) console.log('║    - 서버 과부하로 인한 연결 타임아웃                        ║');
        if (parseFloat(p95) > 3000) console.log('║    - SLA 위반: p95 응답시간 3초 초과                         ║');
    } else {
        console.log('║  [결과] ✅ 정상 처리 (장애 미발생)                            ║');
    }
    console.log('╚══════════════════════════════════════════════════════════════╝');
    console.log('\n');

    return {
        'stdout': '',
        './k6/results/coupon-rush-result.json': JSON.stringify(data, null, 2),
    };
}
