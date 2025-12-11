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
     * 이미 처리된 키인지 확인
     */
    fun isProcessed(key: String): Boolean

    /**
     * 처리 완료로 마킹
     */
    fun markAsProcessed(key: String, ttl: Duration = Duration.ofDays(7))
}
