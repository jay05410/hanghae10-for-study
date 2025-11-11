package io.hhplus.ecommerce.payment.usecase

import io.hhplus.ecommerce.payment.application.PaymentService
import io.hhplus.ecommerce.payment.domain.entity.Payment
import org.springframework.stereotype.Component

/**
 * 결제 조회 통합 유스케이스 - 애플리케이션 계층
 *
 * 역할:
 * - 결제 관련 다양한 조회 작업 통합 처리
 * - 사용자별 결제 정보 조회 및 비즈니스 로직 수행
 * - CQRS Query 패턴 구현
 *
 * 책임:
 * - 다양한 결제 조회 사용 사례 통합 처리
 * - 결제 데이터 반환 및 전달
 * - 읽기 전용 작업 처리
 */
@Component
class GetPaymentQueryUseCase(
    private val paymentService: PaymentService
) {

    /**
     * 결제 ID로 특정 결제를 조회한다
     *
     * @param paymentId 조회할 결제 ID
     * @return 결제 정보 (존재하지 않으면 null 반환)
     */
    fun getPayment(paymentId: Long): Payment? {
        return paymentService.getPayment(paymentId)
    }

    /**
     * 사용자가 진행한 모든 결제 목록을 조회한다
     *
     * @param userId 인증된 사용자 ID
     * @return 사용자의 결제 목록 (모든 상태 포함)
     */
    fun getUserPayments(userId: Long): List<Payment> {
        return paymentService.getPaymentsByUser(userId)
    }
}