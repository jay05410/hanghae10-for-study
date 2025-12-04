package io.hhplus.ecommerce.payment.application.usecase

import io.hhplus.ecommerce.payment.domain.entity.Payment
import io.hhplus.ecommerce.payment.domain.service.PaymentDomainService
import org.springframework.stereotype.Component

/**
 * 결제 조회 유스케이스 - Application Layer (Query)
 *
 * 역할:
 * - 결제 관련 조회 작업 처리
 * - CQRS Query 패턴 구현
 *
 * 책임:
 * - 다양한 결제 조회 사용 사례 처리
 * - 읽기 전용 작업만 수행
 */
@Component
class GetPaymentQueryUseCase(
    private val paymentDomainService: PaymentDomainService
) {

    /**
     * 결제 ID로 결제 조회
     *
     * @param paymentId 결제 ID
     * @return 결제 정보 (없으면 null)
     */
    fun getPayment(paymentId: Long): Payment? {
        return paymentDomainService.getPaymentOrNull(paymentId)
    }

    /**
     * 사용자의 결제 목록 조회
     *
     * @param userId 사용자 ID
     * @return 결제 목록
     */
    fun getUserPayments(userId: Long): List<Payment> {
        return paymentDomainService.getPaymentsByUser(userId)
    }

    /**
     * 주문 ID로 결제 조회
     *
     * @param orderId 주문 ID
     * @return 결제 정보 (없으면 null)
     */
    fun getPaymentByOrderId(orderId: Long): Payment? {
        return paymentDomainService.getPaymentByOrderId(orderId)
    }

    /**
     * 결제번호로 결제 조회
     *
     * @param paymentNumber 결제번호
     * @return 결제 정보 (없으면 null)
     */
    fun getPaymentByNumber(paymentNumber: String): Payment? {
        return paymentDomainService.getPaymentByNumber(paymentNumber)
    }
}
