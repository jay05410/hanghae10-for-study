import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

/**
 * K6 부하 테스트 스크립트 - 주문 생성
 *
 * 시나리오: 100명이 동시에 주문 생성
 *
 * 실행 방법:
 * ```bash
 * # 설치 (Mac)
 * brew install k6
 *
 * # 실행
 * k6 run order-load-test.js
 *
 * # 결과를 JSON으로 저장
 * k6 run --out json=order-load-test-results.json order-load-test.js
 * ```
 *
 * 측정 지표:
 * - 응답 시간 (평균, P95, P99)
 * - 초당 처리량 (TPS)
 * - 에러율
 * - 커넥션 타임아웃 비율
 */

// 커스텀 메트릭 정의
const errorRate = new Rate('error_rate');
const successRate = new Rate('success_rate');
const responseTime = new Trend('response_time');
const requestCount = new Counter('request_count');

// 테스트 옵션
export const options = {
    scenarios: {
        order_creation: {
            executor: 'constant-vus',
            vus: 100,  // 100명의 가상 사용자
            duration: '1m',  // 1분간 지속
        },
    },
    thresholds: {
        'http_req_duration': ['p(95)<1000', 'p(99)<3000'],  // 95%는 1초 이내, 99%는 3초 이내
        'error_rate': ['rate<0.1'],  // 에러율 10% 미만
    },
};

// 환경 변수 설정
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PRODUCT_ID = __ENV.PRODUCT_ID || '1';  // 테스트할 상품 ID

// 테스트 시작 전 설정
export function setup() {
    console.log('🚀 주문 생성 부하 테스트 시작');
    console.log(`   Base URL: ${BASE_URL}`);
    console.log(`   Product ID: ${PRODUCT_ID}`);
    console.log(`   가상 사용자: 100명`);
    console.log(`   테스트 시간: 1분`);

    return { productId: PRODUCT_ID };
}

// 메인 테스트 로직
export default function(data) {
    const userId = __VU * 1000 + __ITER;  // 고유한 사용자 ID 생성

    const url = `${BASE_URL}/api/orders`;
    const payload = JSON.stringify({
        userId: userId,
        items: [
            {
                productId: parseInt(data.productId),
                quantity: 1,
                giftWrap: false,
                giftMessage: null,
            },
        ],
        usedCouponId: null,
        deliveryAddress: {
            recipientName: `테스트사용자${userId}`,
            recipientPhone: '010-1234-5678',
            zipCode: '12345',
            address: '서울특별시 강남구 테헤란로 123',
            addressDetail: `${userId}호`,
            deliveryMessage: '문 앞에 놓아주세요',
        },
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'X-User-Id': userId.toString(),  // 인증 헤더 (실제 구현에 맞게 수정)
        },
        timeout: '10s',  // 10초 타임아웃
    };

    requestCount.add(1);
    const startTime = new Date().getTime();

    const response = http.post(url, payload, params);

    const duration = new Date().getTime() - startTime;
    responseTime.add(duration);

    // 응답 검증
    const isSuccess = check(response, {
        'status is 200 or 201': (r) => r.status === 200 || r.status === 201,
        'response time < 3s': (r) => r.timings.duration < 3000,
        'has orderId in response': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.orderId || body.id;
            } catch (e) {
                return false;
            }
        },
    });

    // 에러율 체크
    if (response.status >= 400) {
        errorRate.add(1);
        console.error(`❌ 주문 실패 - Status: ${response.status}, User: ${userId}, Body: ${response.body}`);
    } else {
        errorRate.add(0);
    }

    // 성공률 체크
    if (isSuccess) {
        successRate.add(1);
    } else {
        successRate.add(0);
    }

    // 짧은 대기 (실제 사용자 행동 시뮬레이션)
    sleep(1);
}

// 테스트 종료 후 요약
export function teardown(data) {
    console.log('');
    console.log('=' .repeat(80));
    console.log('✅ 주문 생성 부하 테스트 완료');
    console.log('=' .repeat(80));
    console.log('');
    console.log('📊 결과 파일: order-load-test-results.json');
    console.log('');
    console.log('분석 방법:');
    console.log('1. 평균 응답 시간 확인 (Before: ~700ms, After: ~200ms 예상)');
    console.log('2. TPS 확인 (Before: ~10-15, After: ~35-50 예상)');
    console.log('3. 에러율 확인');
    console.log('4. P95, P99 응답 시간 확인');
    console.log('');
}
