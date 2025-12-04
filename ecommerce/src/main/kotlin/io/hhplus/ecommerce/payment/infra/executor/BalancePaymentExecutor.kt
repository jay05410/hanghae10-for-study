package io.hhplus.ecommerce.payment.infra.executor

import io.hhplus.ecommerce.common.util.IdPrefix
import io.hhplus.ecommerce.common.util.SnowflakeGenerator
import io.hhplus.ecommerce.payment.application.port.out.PaymentExecutorPort
import io.hhplus.ecommerce.payment.domain.constant.PaymentMethod
import io.hhplus.ecommerce.payment.domain.model.PaymentContext
import io.hhplus.ecommerce.payment.domain.model.PaymentResult
import io.hhplus.ecommerce.payment.domain.model.RefundResult
import io.hhplus.ecommerce.point.application.PointService
import io.hhplus.ecommerce.point.domain.vo.PointAmount
import io.hhplus.ecommerce.point.exception.PointException
import mu.KotlinLogging
import org.springframework.stereotype.Component

/**
 * 포인트/잔액 결제 실행기 - 인프라 계층 구현체
 *
 * 역할:
 * - PaymentExecutorPort의 BALANCE 결제 수단 구현
 * - PointService와 협력하여 실제 포인트 차감/환불 수행
 *
 * 책임:
 * - 포인트 잔액 검증
 * - 포인트 차감 실행
 * - 포인트 환불 실행
 *
 * 트랜잭션:
 * - 호출하는 UseCase의 트랜잭션에 참여
 */
@Component
class BalancePaymentExecutor(
    private val pointService: PointService,
    private val snowflakeGenerator: SnowflakeGenerator
) : PaymentExecutorPort {

    private val logger = KotlinLogging.logger {}

    override fun supportedMethod(): PaymentMethod = PaymentMethod.BALANCE

    override fun canExecute(context: PaymentContext): Boolean {
        return try {
            val userPoint = pointService.getUserPoint(context.userId)
            userPoint != null && userPoint.balance.value >= context.amount
        } catch (e: Exception) {
            logger.warn("포인트 잔액 확인 실패: userId=${context.userId}, error=${e.message}")
            false
        }
    }

    override fun execute(context: PaymentContext): PaymentResult {
        return try {
            val description = context.description ?: "주문 결제 (주문번호: ${context.orderId})"

            pointService.usePoint(
                userId = context.userId,
                amount = PointAmount.of(context.amount),
                description = description
            )

            val transactionId = snowflakeGenerator.generateNumberWithPrefix(IdPrefix.TRANSACTION)

            logger.info("포인트 결제 완료: userId=${context.userId}, amount=${context.amount}, orderId=${context.orderId}, txId=$transactionId")

            PaymentResult.success(
                externalTransactionId = transactionId,
                message = "포인트 결제가 완료되었습니다"
            )
        } catch (e: PointException.InsufficientBalance) {
            logger.warn("포인트 잔액 부족: userId=${context.userId}, amount=${context.amount}")
            PaymentResult.failure("포인트 잔액이 부족합니다")
        } catch (e: PointException.PointNotFound) {
            logger.warn("포인트 정보 없음: userId=${context.userId}")
            PaymentResult.failure("사용자 포인트 정보가 없습니다")
        } catch (e: Exception) {
            logger.error("포인트 결제 실패: userId=${context.userId}, error=${e.message}", e)
            PaymentResult.failure("포인트 결제 처리 중 오류가 발생했습니다: ${e.message}")
        }
    }

    override fun refund(context: PaymentContext): RefundResult {
        return try {
            val description = context.description ?: "주문 취소 환불 (주문번호: ${context.orderId})"

            pointService.earnPoint(
                userId = context.userId,
                amount = PointAmount.of(context.amount),
                description = description
            )

            logger.info("포인트 환불 완료: userId=${context.userId}, amount=${context.amount}, orderId=${context.orderId}")

            RefundResult.success(
                refundedAmount = context.amount,
                message = "포인트 환불이 완료되었습니다"
            )
        } catch (e: PointException.PointNotFound) {
            logger.warn("환불 대상 포인트 정보 없음: userId=${context.userId}")
            RefundResult.failure("사용자 포인트 정보가 없습니다")
        } catch (e: Exception) {
            logger.error("포인트 환불 실패: userId=${context.userId}, error=${e.message}", e)
            RefundResult.failure("포인트 환불 처리 중 오류가 발생했습니다: ${e.message}")
        }
    }
}