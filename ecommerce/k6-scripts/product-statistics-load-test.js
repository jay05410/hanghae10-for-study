import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

/**
 * K6 부하 테스트 스크립트 - 상품 통계(조회수)
 *
 * 시나리오: 10,000명이 인기 상품(Top 10)을 동시 조회
 *
 * 실행 방법:
 * ```bash
 * # 설치 (Mac)
 * brew install k6
 *
 * # 실행
 * k6 run product-statistics-load-test.js
 *
 * # 결과를 JSON으로 저장
 * k6 run --out json=product-statistics-load-test-results.json product-statistics-load-test.js
 * ```
 *
 * 측정 지표:
 * - 초당 처리량 (TPS)
 * - 응답 시간 (평균, P95, P99)
 * - 에러율
 * - 락 경합으로 인한 대기 시간
 */

// 커스텀 메트릭 정의
const errorRate = new Rate('error_rate');
const successRate = new Rate('success_rate');
const responseTime = new Trend('response_time');
const requestCount = new Counter('request_count');

// 테스트 옵션
export const options = {
    scenarios: {
        product_view: {
            executor: 'constant-arrival-rate',
            rate: 1000,  // 초당 1000개 요청
            timeUnit: '1s',
            duration: '30s',  // 30초간 지속
            preAllocatedVUs: 500,  // 사전 할당 VU
            maxVUs: 1000,  // 최대 VU
        },
    },
    thresholds: {
        'http_req_duration': ['p(95)<500', 'p(99)<1000'],  // 95%는 500ms 이내, 99%는 1초 이내
        'error_rate': ['rate<0.05'],  // 에러율 5% 미만
    },
};

// 환경 변수 설정
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TOP_PRODUCT_IDS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];  // Top 10 상품 ID

// 테스트 시작 전 설정
export function setup() {
    console.log('🚀 상품 통계(조회수) 부하 테스트 시작');
    console.log(`   Base URL: ${BASE_URL}`);
    console.log(`   Top 10 Products: ${TOP_PRODUCT_IDS.join(', ')}`);
    console.log(`   요청률: 1000 req/s`);
    console.log(`   테스트 시간: 30초`);
    console.log(`   예상 총 요청: 30,000건`);

    return { topProductIds: TOP_PRODUCT_IDS };
}

// 메인 테스트 로직
export default function(data) {
    // 랜덤하게 Top 10 상품 중 하나 선택
    const randomIndex = Math.floor(Math.random() * data.topProductIds.length);
    const productId = data.topProductIds[randomIndex];

    // 고유한 사용자 ID 생성
    const userId = __VU * 1000 + __ITER;

    const url = `${BASE_URL}/api/v1/products/${productId}`;

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'User-Id': userId.toString(),  // 상품 조회 통계를 위한 사용자 ID 헤더
        },
        timeout: '5s',  // 5초 타임아웃
    };

    requestCount.add(1);
    const startTime = new Date().getTime();

    const response = http.get(url, params);

    const duration = new Date().getTime() - startTime;
    responseTime.add(duration);

    // 응답 검증
    const isSuccess = check(response, {
        'status is 200': (r) => r.status === 200,
        'response time < 1s': (r) => r.timings.duration < 1000,
        'has product data': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.data && body.data.id && body.data.name;
            } catch (e) {
                return false;
            }
        },
    });

    // 에러율 체크
    if (response.status >= 400) {
        errorRate.add(1);
        console.error(`❌ 조회 실패 - Status: ${response.status}, Product: ${productId}`);
    } else {
        errorRate.add(0);
    }

    // 성공률 체크
    if (isSuccess) {
        successRate.add(1);
    } else {
        successRate.add(0);
    }

    // 대기 없음 (최대 부하 테스트)
}

// 테스트 종료 후 요약
export function teardown(data) {
    console.log('');
    console.log('=' .repeat(80));
    console.log('✅ 상품 통계(조회수) 부하 테스트 완료');
    console.log('=' .repeat(80));
    console.log('');
    console.log('📊 결과 파일: product-statistics-load-test-results.json');
    console.log('');
    console.log('분석 방법:');
    console.log('1. TPS 확인 (Before: ~50, After: ~10,000 예상)');
    console.log('2. 평균 응답 시간 확인 (Before: ~100ms, After: ~5ms 예상)');
    console.log('3. 락 경합으로 인한 대기 시간 확인 (P95, P99)');
    console.log('4. 에러율 확인');
    console.log('');
}
