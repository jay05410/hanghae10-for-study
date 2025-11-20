package io.hhplus.ecommerce.coupon.domain.constant

/**
 * 쿠폰 발급 Queue 상태
 *
 * 역할:
 * - 쿠폰 발급 요청의 처리 상태를 추적
 *
 * 상태 전이:
 * - WAITING → PROCESSING → COMPLETED
 * - WAITING → PROCESSING → FAILED
 * - WAITING → EXPIRED (대기 시간 초과)
 */
enum class QueueStatus {
    /**
     * 대기 중
     * - Redis Queue에 등록되어 처리 대기 중인 상태
     */
    WAITING,

    /**
     * 처리 중
     * - Worker가 쿠폰 발급 처리를 진행 중인 상태
     */
    PROCESSING,

    /**
     * 완료
     * - 쿠폰 발급이 성공적으로 완료된 상태
     */
    COMPLETED,

    /**
     * 실패
     * - 쿠폰 발급 처리 중 오류가 발생한 상태
     * - 예: 쿠폰 품절, 이미 발급받음, 시스템 오류 등
     */
    FAILED,

    /**
     * 만료
     * - 대기 시간 초과로 만료된 상태
     * - 일정 시간(예: 10분) 이내에 처리되지 않으면 자동 만료
     */
    EXPIRED
}
