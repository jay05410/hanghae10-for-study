package io.hhplus.ecommerce.coupon.usecase

import io.hhplus.ecommerce.coupon.application.CouponQueueService
import io.hhplus.ecommerce.coupon.application.CouponService
import io.hhplus.ecommerce.coupon.domain.entity.CouponQueueRequest
import io.hhplus.ecommerce.coupon.domain.repository.CouponRepository
import io.hhplus.ecommerce.coupon.dto.IssueCouponRequest
import io.hhplus.ecommerce.coupon.dto.UseCouponRequest
import io.hhplus.ecommerce.coupon.exception.CouponException
import org.springframework.stereotype.Component

/**
 * 쿠폰 명령 UseCase
 *
 * 역할:
 * - 모든 쿠폰 변경 작업을 통합 관리
 * - 쿠폰 발급 Queue 등록, 사용 기능 제공
 *
 * 책임:
 * - 쿠폰 발급 Queue 등록 (직접 발급 대신)
 * - 쿠폰 사용 요청 검증 및 실행
 * - 쿠폰 데이터 무결성 보장
 */
@Component
class CouponCommandUseCase(
    private val couponService: CouponService,
    private val couponQueueService: CouponQueueService,
    private val couponRepository: CouponRepository
) {

    /**
     * 사용자의 쿠폰 발급 요청을 Queue에 등록한다
     *
     * @param userId 인증된 사용자 ID
     * @param request 쿠폰 발급 요청 데이터
     * @return Queue에 등록된 요청 정보 (대기 순번, 예상 시간 포함)
     * @throws CouponException.CouponNotFound 쿠폰을 찾을 수 없는 경우
     * @throws CouponException.AlreadyInQueue 이미 Queue에 등록된 경우
     */
    fun issueCoupon(userId: Long, request: IssueCouponRequest): CouponQueueRequest {
        val coupon = couponRepository.findById(request.couponId)
            ?: throw CouponException.CouponNotFound(request.couponId)

        return couponQueueService.enqueue(
            userId = userId,
            couponId = coupon.id,
            couponName = coupon.name
        )
    }

    /**
     * 주문에 쿠폰을 실제로 적용하고 할인 금액을 계산한다
     *
     * @param userId 인증된 사용자 ID
     * @param request 쿠폰 사용 요청 데이터
     * @param orderId 쿠폰이 적용될 주문 ID
     * @return 계산된 할인 금액
     * @throws IllegalArgumentException 쿠폰 사용 조건을 만족하지 않는 경우
     * @throws RuntimeException 쿠폰 사용 처리에 실패한 경우
     */
    fun applyCoupon(userId: Long, request: UseCouponRequest, orderId: Long): Long {
        return couponService.applyCoupon(
            userId = userId,
            userCouponId = request.userCouponId,
            orderId = orderId,
            orderAmount = request.orderAmount
        )
    }
}