import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

/**
 * K6 부하 테스트 스크립트 - 쿠폰 발급
 *
 * 시나리오: 10,000명이 100개 한정 쿠폰에 동시 발급 시도
 *
 * 실행 방법:
 * ```bash
 * # 설치 (Mac)
 * brew install k6
 *
 * # 실행
 * k6 run coupon-load-test.js
 *
 * # 결과를 JSON으로 저장
 * k6 run --out json=coupon-load-test-results.json coupon-load-test.js
 * ```
 *
 * 측정 지표:
 * - 타임아웃 비율
 * - 응답 시간 (평균, P95, P99)
 * - 에러율
 * - 초당 요청 수 (RPS)
 */

// 커스텀 메트릭 정의
const timeoutRate = new Rate('timeout_rate');
const errorRate = new Rate('error_rate');
const successRate = new Rate('success_rate');
const responseTime = new Trend('response_time');
const requestCount = new Counter('request_count');

// 테스트 옵션
export const options = {
    scenarios: {
        coupon_issuance: {
            executor: 'shared-iterations',
            vus: 100,  // 100개의 가상 사용자
            iterations: 10000,  // 총 10,000번 요청
            maxDuration: '5m',  // 최대 5분
        },
    },
    thresholds: {
        'http_req_duration': ['p(95)<5000', 'p(99)<10000'],  // 95%는 5초 이내, 99%는 10초 이내
        'timeout_rate': ['rate<0.5'],  // 타임아웃 비율 50% 미만
        'error_rate': ['rate<0.5'],  // 에러율 50% 미만
    },
};

// 환경 변수 설정
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const COUPON_ID = __ENV.COUPON_ID || '1';  // 테스트할 쿠폰 ID

// 테스트 시작 전 설정
export function setup() {
    console.log('🚀 쿠폰 발급 부하 테스트 시작');
    console.log(`   Base URL: ${BASE_URL}`);
    console.log(`   Coupon ID: ${COUPON_ID}`);
    console.log(`   가상 사용자: 100명`);
    console.log(`   총 요청: 10,000건`);

    return { couponId: COUPON_ID };
}

// 메인 테스트 로직
export default function(data) {
    const userId = __VU * 1000 + __ITER;  // 고유한 사용자 ID 생성

    const url = `${BASE_URL}/api/coupons/${data.couponId}/issue`;
    const payload = JSON.stringify({
        couponId: parseInt(data.couponId),
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'X-User-Id': userId.toString(),  // 인증 헤더 (실제 구현에 맞게 수정)
        },
        timeout: '30s',  // 30초 타임아웃
    };

    requestCount.add(1);
    const startTime = new Date().getTime();

    const response = http.post(url, payload, params);

    const duration = new Date().getTime() - startTime;
    responseTime.add(duration);

    // 응답 검증
    const isSuccess = check(response, {
        'status is 200 or 201': (r) => r.status === 200 || r.status === 201,
        'response time < 30s': (r) => r.timings.duration < 30000,
        'has response body': (r) => r.body.length > 0,
    });

    // 타임아웃 체크
    if (response.timings.duration >= 30000 || response.status === 0) {
        timeoutRate.add(1);
        console.error(`⏱️  타임아웃 발생 - User ${userId}`);
    } else {
        timeoutRate.add(0);
    }

    // 에러율 체크
    if (response.status >= 400) {
        errorRate.add(1);
        if (response.status !== 400) {  // 400은 중복 발급 등 예상된 에러
            console.error(`❌ 에러 발생 - Status: ${response.status}, User: ${userId}, Body: ${response.body}`);
        }
    } else {
        errorRate.add(0);
    }

    // 성공률 체크
    if (isSuccess) {
        successRate.add(1);
    } else {
        successRate.add(0);
    }

    // 짧은 대기 (서버 부하 완화)
    sleep(0.1);
}

// 테스트 종료 후 요약
export function teardown(data) {
    console.log('');
    console.log('=' .repeat(80));
    console.log('✅ 쿠폰 발급 부하 테스트 완료');
    console.log('=' .repeat(80));
    console.log('');
    console.log('📊 결과 파일: coupon-load-test-results.json');
    console.log('');
    console.log('분석 방법:');
    console.log('1. 타임아웃 비율 확인');
    console.log('2. 응답 시간 분포 (평균, P95, P99) 확인');
    console.log('3. 성공/실패 비율 확인');
    console.log('4. After 측정 결과와 비교');
    console.log('');
}
