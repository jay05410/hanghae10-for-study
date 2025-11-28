package io.hhplus.ecommerce.point.usecase

import io.hhplus.ecommerce.point.application.PointService
import io.hhplus.ecommerce.point.application.PointHistoryService
import io.hhplus.ecommerce.point.domain.entity.UserPoint
import io.hhplus.ecommerce.point.domain.vo.PointAmount
import io.hhplus.ecommerce.common.annotation.DistributedLock
import io.hhplus.ecommerce.common.lock.DistributedLockKeys
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 포인트 명령 UseCase
 *
 * 역할:
 * - 모든 포인트 변경 작업을 통합 관리
 * - 포인트 충전, 사용, 차감 기능 제공
 * - 히스토리 기록 및 트랜잭션 관리
 *
 * 책임:
 * - 포인트 변경 요청 검증 및 실행
 * - 히스토리 기록 관리
 * - 트랜잭션 무결성 보장
 */
@Component
class PointCommandUseCase(
    private val pointService: PointService,
    private val pointHistoryService: PointHistoryService
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 사용자에게 포인트를 충전하고 이력을 기록한다 (재시도 로직 포함)
     *
     * @param userId 인증된 사용자 ID
     * @param amount 충전할 포인트 금액 (양수)
     * @param description 충전 사유 및 설명 (선택적)
     * @param orderId 주문 ID (구매 적립인 경우)
     * @return 충전 처리가 완료된 사용자 포인트 정보
     * @throws IllegalArgumentException 충전 금액이 잘못된 경우
     * @throws RuntimeException 최대 재시도 후에도 실패한 경우
     */
    @DistributedLock(key = DistributedLockKeys.Point.CHARGE, waitTime = 10L, leaseTime = 60L)
    fun chargePoint(userId: Long, amount: Long, description: String? = null, orderId: Long? = null): UserPoint {
        return processPointOperationWithRetry(
            operation = "충전",
            userId = userId,
            amount = amount,
            description = description,
            orderId = orderId
        ) { pointAmount ->
            chargePointCore(userId, pointAmount, description, orderId)
        }
    }

    /**
     * 포인트 충전 핵심 로직
     */
    @Transactional
    private fun chargePointCore(userId: Long, pointAmount: PointAmount, description: String?, orderId: Long?): UserPoint {
        // 사용자 포인트가 없는 경우 새로 생성
        val existingUserPoint = pointService.getUserPoint(userId)
        if (existingUserPoint == null) {
            pointService.createUserPoint(userId)
        }

        // 충전 전 잔액 조회
        val userPointBefore = pointService.getUserPoint(userId)!!
        val balanceBefore = userPointBefore.balance

        // 포인트 충전
        val updatedUserPoint = pointService.earnPoint(userId, pointAmount, description)

        // 히스토리 기록
        pointHistoryService.recordEarnHistory(
            userId = userId,
            amount = pointAmount,
            balanceBefore = balanceBefore,
            balanceAfter = updatedUserPoint.balance,
            description = description,
            orderId = orderId
        )

        return updatedUserPoint
    }

    /**
     * 사용자의 포인트를 사용하고 이력을 기록한다 (재시도 로직 포함)
     *
     * @param userId 인증된 사용자 ID
     * @param amount 사용할 포인트 금액 (양수)
     * @param description 사용 사유 및 설명 (선택적)
     * @param orderId 주문 ID (주문 할인인 경우)
     * @return 사용 처리가 완료된 사용자 포인트 정보
     * @throws IllegalArgumentException 사용 금액이 잘못되거나 잔액이 부족한 경우
     * @throws RuntimeException 최대 재시도 후에도 실패한 경우
     */
    @DistributedLock(key = DistributedLockKeys.Point.USE, waitTime = 10L, leaseTime = 60L)
    fun usePoint(userId: Long, amount: Long, description: String? = null, orderId: Long? = null): UserPoint {
        return processPointOperationWithRetry(
            operation = "사용",
            userId = userId,
            amount = amount,
            description = description,
            orderId = orderId
        ) { pointAmount ->
            usePointCore(userId, pointAmount, description, orderId)
        }
    }

    /**
     * 포인트 사용 핵심 로직
     */
    @Transactional
    private fun usePointCore(userId: Long, pointAmount: PointAmount, description: String?, orderId: Long?): UserPoint {
        // 사용 전 잔액 조회
        val userPointBefore = pointService.getUserPoint(userId)
            ?: throw IllegalArgumentException("사용자 포인트 정보가 없습니다: $userId")
        val balanceBefore = userPointBefore.balance

        // 포인트 사용
        val updatedUserPoint = pointService.usePoint(userId, pointAmount, description)

        // 히스토리 기록
        pointHistoryService.recordUseHistory(
            userId = userId,
            amount = pointAmount,
            balanceBefore = balanceBefore,
            balanceAfter = updatedUserPoint.balance,
            description = description,
            orderId = orderId
        )

        return updatedUserPoint
    }

    /**
     * 포인트를 차감합니다 (히스토리 없이 직접 차감)
     *
     * @param userId 사용자 ID
     * @param amount 차감할 포인트 금액
     * @param description 차감 설명
     * @return 차감 완료된 사용자 포인트 정보
     * @throws PointException.PointNotFound 사용자 포인트 정보가 없는 경우
     * @throws PointException.InsufficientBalance 잔액 부족 시
     * @throws PointException.InvalidAmount 차감 금액이 0 이하인 경우
     */
    @DistributedLock(key = DistributedLockKeys.Point.DEDUCT, waitTime = 5L, leaseTime = 30L)
    fun deductPoint(userId: Long, amount: PointAmount, description: String? = null): UserPoint {
        return pointService.usePoint(userId, amount, description)
    }

    /**
     * 새 사용자 포인트를 생성합니다.
     *
     * @param userId 사용자 ID
     * @return 생성된 사용자 포인트 정보
     */
    @Transactional
    fun createUserPoint(userId: Long): UserPoint {
        return pointService.createUserPoint(userId)
    }

    /**
     * 포인트를 적립합니다 (히스토리 없이 직접 적립)
     *
     * @param userId 사용자 ID
     * @param amount 적립할 포인트 금액
     * @param description 적립 설명
     * @return 적립 완료된 사용자 포인트 정보
     * @throws PointException.PointNotFound 사용자 포인트 정보가 없는 경우
     * @throws PointException.MaxBalanceExceeded 최대 잔액 초과 시
     */
    @DistributedLock(key = DistributedLockKeys.Point.EARN, waitTime = 5L, leaseTime = 30L)
    fun earnPoint(userId: Long, amount: PointAmount, description: String? = null): UserPoint {
        return pointService.earnPoint(userId, amount, description)
    }

    /**
     * 포인트를 소멸시킵니다.
     *
     * @param userId 사용자 ID
     * @param amount 소멸할 포인트 금액
     * @return 소멸 완료된 사용자 포인트 정보
     * @throws PointException.PointNotFound 사용자 포인트 정보가 없는 경우
     * @throws PointException.InsufficientBalance 소멸 가능한 포인트 부족 시
     * @throws PointException.InvalidAmount 소멸 금액이 0 이하인 경우
     */
    @DistributedLock(key = DistributedLockKeys.Point.EXPIRE, waitTime = 5L, leaseTime = 30L)
    fun expirePoint(userId: Long, amount: PointAmount): UserPoint {
        return pointService.expirePoint(userId, amount)
    }

    /**
     * 재시도 로직이 포함된 포인트 연산 처리
     *
     * @param operation 연산 타입 (로그용)
     * @param userId 사용자 ID
     * @param amount 포인트 금액
     * @param description 설명
     * @param orderId 주문 ID
     * @param maxRetries 최대 재시도 횟수
     * @param baseDelayMs 기본 지연 시간
     * @param processor 실제 처리 로직
     */
    private fun processPointOperationWithRetry(
        operation: String,
        userId: Long,
        amount: Long,
        description: String?,
        orderId: Long?,
        maxRetries: Int = 3,
        baseDelayMs: Long = 100L,
        processor: (PointAmount) -> UserPoint
    ): UserPoint {
        val pointAmount = PointAmount.of(amount)
        var lastException: Exception? = null

        repeat(maxRetries + 1) { attempt ->
            try {
                return processor(pointAmount)
            } catch (e: Exception) {
                // 비즈니스 예외는 재시도하지 않고 즉시 전파
                if (isBusinessException(e)) {
                    logger.debug("포인트 $operation 비즈니스 예외 (재시도 안함): ${e.message} - userId: $userId")
                    throw e
                }

                lastException = e

                if (attempt < maxRetries) {
                    val delayMs = baseDelayMs * (1L shl attempt) // 지수 백오프
                    logger.warn("포인트 $operation 실패 (재시도 ${attempt + 1}/$maxRetries): ${e.message} - userId: $userId")

                    try {
                        Thread.sleep(delayMs)
                    } catch (ie: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw ie
                    }
                } else {
                    logger.error("포인트 $operation 최종 실패 (최대 재시도 초과): ${e.message} - userId: $userId", e)
                }
            }
        }

        throw RuntimeException("포인트 $operation 실패: 최대 재시도 횟수($maxRetries)를 초과했습니다 - userId: $userId", lastException)
    }

    /**
     * 비즈니스 예외 여부 판단 (재시도하지 않아야 할 예외들)
     */
    private fun isBusinessException(exception: Exception): Boolean {
        return when (exception) {
            is io.hhplus.ecommerce.point.exception.PointException -> true
            is IllegalArgumentException -> true
            else -> false
        }
    }
}