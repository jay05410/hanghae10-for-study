package io.hhplus.ecommerce.common.idempotency

import java.time.Duration

/**
 * 멱등성 서비스 인터페이스
 *
 * 중복 처리 방지를 위한 추상화
 * - Producer: 중복 발행 방지
 * - Consumer: 중복 소비 방지 (2차 방어)
 */
interface IdempotencyService {

    /**
     * 원자적으로 멱등성 키 획득 시도 (SETNX 기반)
     *
     * 체크와 마킹을 원자적으로 수행하여 경쟁 조건 방지
     * - true 반환: 처리 권한 획득 (처음 요청)
     * - false 반환: 이미 다른 요청이 처리 중이거나 처리 완료
     *
     * @param key 멱등성 키
     * @param ttl 키 만료 시간 (기본값: 7일)
     * @return 처리 권한 획득 여부
     */
    fun tryAcquire(key: String, ttl: Duration = Duration.ofDays(7)): Boolean
}
