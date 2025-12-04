package io.hhplus.ecommerce.payment.infra.executor

import io.hhplus.ecommerce.common.util.IdPrefix
import io.hhplus.ecommerce.common.util.SnowflakeGenerator
import io.hhplus.ecommerce.payment.application.port.out.PaymentExecutorPort
import io.hhplus.ecommerce.payment.domain.constant.PaymentMethod
import io.hhplus.ecommerce.payment.domain.model.PaymentContext
import io.hhplus.ecommerce.payment.domain.model.PaymentResult
import io.hhplus.ecommerce.payment.domain.model.RefundResult
import io.hhplus.ecommerce.point.domain.service.PointDomainService
import io.hhplus.ecommerce.point.domain.vo.PointAmount
import io.hhplus.ecommerce.point.exception.PointException
import mu.KotlinLogging
import org.springframework.stereotype.Component

/**
 * 포인트/잔액 결제 실행기 - 인프라 계층 구현체
 *
 * 역할:
 * - PaymentExecutorPort의 BALANCE 결제 수단 구현
 * - 결제 가능 여부 검증만 수행
 *
 * 책임:
 * - 포인트 잔액 검증
 * - 결제 기록 생성 (실제 포인트 차감은 PointEventHandler가 담당)
 *
 * Saga 흐름:
 * - PaymentEventHandler → BalancePaymentExecutor (검증 + 결제 기록)
 * - PaymentCompleted → PointEventHandler (실제 포인트 차감)
 *
 * 트랜잭션:
 * - 호출하는 UseCase의 트랜잭션에 참여
 */
@Component
class BalancePaymentExecutor(
    private val pointDomainService: PointDomainService,
    private val snowflakeGenerator: SnowflakeGenerator
) : PaymentExecutorPort {

    private val logger = KotlinLogging.logger {}

    override fun supportedMethod(): PaymentMethod = PaymentMethod.BALANCE

    override fun canExecute(context: PaymentContext): Boolean {
        return try {
            val userPoint = pointDomainService.getUserPoint(context.userId)
            userPoint != null && userPoint.balance.value >= context.amount
        } catch (e: Exception) {
            logger.warn("포인트 잔액 확인 실패: userId=${context.userId}, error=${e.message}")
            false
        }
    }

    /**
     * 결제 실행 - 검증 및 결제 기록 생성
     *
     * 실제 포인트 차감은 PointEventHandler에서 PaymentCompleted 이벤트 처리 시 수행
     * 여기서는 잔액 검증 후 결제 기록만 생성
     */
    override fun execute(context: PaymentContext): PaymentResult {
        return try {
            // 잔액 검증
            val userPoint = pointDomainService.getUserPoint(context.userId)
            if (userPoint == null || userPoint.balance.value < context.amount) {
                return PaymentResult.failure("포인트 잔액이 부족합니다")
            }

            val transactionId = snowflakeGenerator.generateNumberWithPrefix(IdPrefix.TRANSACTION)

            logger.info("포인트 결제 승인: userId=${context.userId}, amount=${context.amount}, orderId=${context.orderId}, txId=$transactionId")

            PaymentResult.success(
                externalTransactionId = transactionId,
                message = "포인트 결제가 승인되었습니다"
            )
        } catch (e: PointException.PointNotFound) {
            logger.warn("포인트 정보 없음: userId=${context.userId}")
            PaymentResult.failure("사용자 포인트 정보가 없습니다")
        } catch (e: Exception) {
            logger.error("포인트 결제 실패: userId=${context.userId}, error=${e.message}", e)
            PaymentResult.failure("포인트 결제 처리 중 오류가 발생했습니다: ${e.message}")
        }
    }

    /**
     * 환불 실행 - 환불 기록 생성
     *
     * 실제 포인트 환불은 PointEventHandler에서 OrderCancelled 이벤트 처리 시 수행
     */
    override fun refund(context: PaymentContext): RefundResult {
        return try {
            logger.info("포인트 환불 승인: userId=${context.userId}, amount=${context.amount}, orderId=${context.orderId}")

            RefundResult.success(
                refundedAmount = context.amount,
                message = "포인트 환불이 승인되었습니다"
            )
        } catch (e: Exception) {
            logger.error("포인트 환불 실패: userId=${context.userId}, error=${e.message}", e)
            RefundResult.failure("포인트 환불 처리 중 오류가 발생했습니다: ${e.message}")
        }
    }
}