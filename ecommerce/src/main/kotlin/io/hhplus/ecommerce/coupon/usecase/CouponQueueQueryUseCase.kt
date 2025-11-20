package io.hhplus.ecommerce.coupon.usecase

import io.hhplus.ecommerce.coupon.application.CouponQueueService
import io.hhplus.ecommerce.coupon.domain.entity.CouponQueueRequest
import org.springframework.stereotype.Component

/**
 * 쿠폰 Queue 조회 UseCase
 *
 * 역할:
 * - 쿠폰 발급 Queue 상태 조회 기능 제공
 * - 사용자별 Queue 요청 조회
 *
 * 책임:
 * - Queue 요청 상태 확인
 * - 대기 순번 및 처리 결과 조회
 */
@Component
class CouponQueueQueryUseCase(
    private val couponQueueService: CouponQueueService
) {

    /**
     * Queue ID로 요청 상태를 조회한다
     *
     * @param queueId Queue ID
     * @return Queue 요청 정보 (없으면 null)
     */
    fun getQueueStatus(queueId: String): CouponQueueRequest? {
        return couponQueueService.getQueueRequest(queueId)
    }

    /**
     * 사용자의 특정 쿠폰에 대한 Queue 요청을 조회한다
     *
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     * @return Queue 요청 정보 (없으면 null)
     */
    fun getUserQueueRequest(userId: Long, couponId: Long): CouponQueueRequest? {
        return couponQueueService.getUserQueueRequest(userId, couponId)
    }

    /**
     * 특정 쿠폰의 현재 대기열 크기를 조회한다
     *
     * @param couponId 쿠폰 ID
     * @return 대기 중인 요청 수
     */
    fun getQueueSize(couponId: Long): Long {
        return couponQueueService.getQueueSize(couponId)
    }
}
