package io.hhplus.ecommerce.payment.usecase

import io.hhplus.ecommerce.payment.application.PaymentService
import io.hhplus.ecommerce.payment.domain.entity.Payment
import io.hhplus.ecommerce.payment.dto.ProcessPaymentRequest
import io.hhplus.ecommerce.common.annotation.DistributedLock
import io.hhplus.ecommerce.common.annotation.DistributedTransaction
import io.hhplus.ecommerce.common.lock.DistributedLockKeys
import org.springframework.stereotype.Component

/**
 * 결제 처리 유스케이스 - 애플리케이션 계층
 *
 * 역할:
 * - 결제 처리 비즈니스 플로우 수행
 * - 분산락을 통한 중복 결제 방지
 * - CQRS Command 패턴 구현
 *
 * 책임:
 * - 결제 수단 유효성 및 결제 가능 금액 검증
 * - 결제 처리 트랜잭션 관리
 * - 결제 성공 및 실패 후처리 수행
 *
 * 동시성 제어:
 * - orderId 기반 분산락으로 중복 결제 방지
 * - 분산 트랜잭션으로 데이터 정합성 보장
 */
@Component
class ProcessPaymentUseCase(
    private val paymentService: PaymentService
) {

    /**
     * 주문에 대한 결제를 처리하고 결과를 반환한다
     *
     * 동시성 제어:
     * - orderId 기반 분산락으로 동일 주문 중복 결제 방지
     * - 락 대기 시간: 5초, 보유 시간: 30초
     * - 분산 트랜잭션으로 데이터 정합성 보장
     *
     * @param request 결제 처리 요청 데이터
     * @return 처리가 완료된 결제 정보
     * @throws IllegalArgumentException 결제 수단이 유효하지 않거나 결제 금액이 잘못된 경우
     * @throws RuntimeException 결제 처리에 실패한 경우
     */
    @DistributedLock(key = "${DistributedLockKeys.Payment.DUPLICATE_PREVENTION}:#{#request.orderId}", waitTime = 5L, leaseTime = 30L)
    @DistributedTransaction
    fun execute(request: ProcessPaymentRequest): Payment {
        return paymentService.processPayment(
            userId = request.userId,
            orderId = request.orderId,
            amount = request.amount,
            paymentMethod = request.paymentMethod
        )
    }
}