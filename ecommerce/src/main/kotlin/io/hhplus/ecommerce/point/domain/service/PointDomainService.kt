package io.hhplus.ecommerce.point.domain.service

import io.hhplus.ecommerce.point.domain.entity.PointHistory
import io.hhplus.ecommerce.point.domain.entity.UserPoint
import io.hhplus.ecommerce.point.domain.repository.PointHistoryRepository
import io.hhplus.ecommerce.point.domain.repository.UserPointRepository
import io.hhplus.ecommerce.point.domain.vo.Balance
import io.hhplus.ecommerce.point.domain.vo.PointAmount
import io.hhplus.ecommerce.point.exception.PointException
import org.springframework.stereotype.Component

/**
 * 포인트 도메인 서비스 - 순수 도메인 로직
 *
 * 역할:
 * - 포인트 엔티티 생성 및 상태 관리
 * - 포인트 적립/사용/소멸 비즈니스 로직
 * - 포인트 이력 기록
 *
 * 책임:
 * - 포인트 도메인 불변식 보장
 * - Repository(Port)를 통한 영속성 위임
 *
 * 주의:
 * - 트랜잭션 관리는 UseCase에서 담당
 * - @Transactional 사용 금지
 * - 오케스트레이션은 UseCase에서 담당
 */
@Component
class PointDomainService(
    private val userPointRepository: UserPointRepository,
    private val pointHistoryRepository: PointHistoryRepository
) {

    // ========== UserPoint 관련 메서드 ==========

    /**
     * 사용자 포인트 조회
     *
     * @param userId 사용자 ID
     * @return 사용자 포인트 (없으면 null)
     */
    fun getUserPoint(userId: Long): UserPoint? {
        return userPointRepository.findByUserId(userId)
    }

    /**
     * 사용자 포인트 조회 (필수)
     *
     * @param userId 사용자 ID
     * @return 사용자 포인트
     * @throws PointException.PointNotFound 포인트 정보가 없는 경우
     */
    fun getUserPointOrThrow(userId: Long): UserPoint {
        return userPointRepository.findByUserId(userId)
            ?: throw PointException.PointNotFound(userId)
    }

    /**
     * 사용자 포인트와 이력을 함께 조회 (FETCH JOIN 활용)
     *
     * @param userId 사용자 ID
     * @return 포인트 이력과 함께 조회된 UserPoint
     */
    fun getUserPointWithHistories(userId: Long): UserPoint? {
        return userPointRepository.findUserPointWithHistoriesByUserId(userId)
    }

    /**
     * 사용자 포인트 생성
     *
     * @param userId 사용자 ID
     * @return 생성된 사용자 포인트
     */
    fun createUserPoint(userId: Long): UserPoint {
        val userPoint = UserPoint.create(userId)
        return userPointRepository.save(userPoint)
    }

    /**
     * 사용자 포인트 존재 여부 확인 및 생성
     *
     * @param userId 사용자 ID
     * @return 기존 또는 새로 생성된 사용자 포인트
     */
    fun getOrCreateUserPoint(userId: Long): UserPoint {
        return userPointRepository.findByUserId(userId)
            ?: createUserPoint(userId)
    }

    /**
     * 포인트 적립 (구매 시 자동 적립)
     *
     * @param userId 사용자 ID
     * @param amount 적립할 포인트 금액
     * @return 적립 완료된 사용자 포인트 정보
     * @throws PointException.PointNotFound 사용자 포인트 정보가 없는 경우
     * @throws PointException.MaxBalanceExceeded 최대 잔액 초과 시
     */
    fun earnPoint(userId: Long, amount: PointAmount): UserPoint {
        val userPoint = getUserPointOrThrow(userId)
        userPoint.earn(amount)
        return userPointRepository.save(userPoint)
    }

    /**
     * 포인트 사용 (할인 적용)
     *
     * @param userId 사용자 ID
     * @param amount 사용할 포인트 금액
     * @return 사용 완료된 사용자 포인트 정보
     * @throws PointException.PointNotFound 사용자 포인트 정보가 없는 경우
     * @throws PointException.InsufficientBalance 잔액 부족 시
     * @throws PointException.InvalidAmount 사용 금액이 0 이하인 경우
     */
    fun usePoint(userId: Long, amount: PointAmount): UserPoint {
        val userPoint = getUserPointOrThrow(userId)
        userPoint.use(amount)
        return userPointRepository.save(userPoint)
    }

    /**
     * 포인트 소멸 (만료 처리)
     *
     * @param userId 사용자 ID
     * @param amount 소멸할 포인트 금액
     * @return 소멸 완료된 사용자 포인트 정보
     * @throws PointException.PointNotFound 사용자 포인트 정보가 없는 경우
     * @throws PointException.InsufficientBalance 소멸 가능한 포인트 부족 시
     */
    fun expirePoint(userId: Long, amount: PointAmount): UserPoint {
        val userPoint = getUserPointOrThrow(userId)
        userPoint.expire(amount)
        return userPointRepository.save(userPoint)
    }

    // ========== PointHistory 관련 메서드 ==========

    /**
     * 포인트 적립 이력 기록
     *
     * @param userId 사용자 ID
     * @param amount 적립 금액
     * @param balanceBefore 적립 전 잔액
     * @param balanceAfter 적립 후 잔액
     * @param description 적립 설명 (선택)
     * @param orderId 주문 ID (선택)
     * @return 기록된 포인트 이력
     */
    fun recordEarnHistory(
        userId: Long,
        amount: PointAmount,
        balanceBefore: Balance,
        balanceAfter: Balance,
        description: String? = null,
        orderId: Long? = null
    ): PointHistory {
        val history = PointHistory.createEarnHistory(
            userId = userId,
            amount = amount,
            balanceBefore = balanceBefore.value,
            balanceAfter = balanceAfter.value,
            orderId = orderId,
            description = description
        )
        return pointHistoryRepository.save(history)
    }

    /**
     * 포인트 사용 이력 기록
     *
     * @param userId 사용자 ID
     * @param amount 사용 금액
     * @param balanceBefore 사용 전 잔액
     * @param balanceAfter 사용 후 잔액
     * @param description 사용 설명 (선택)
     * @param orderId 주문 ID (선택)
     * @return 기록된 포인트 이력
     */
    fun recordUseHistory(
        userId: Long,
        amount: PointAmount,
        balanceBefore: Balance,
        balanceAfter: Balance,
        description: String? = null,
        orderId: Long? = null
    ): PointHistory {
        val history = PointHistory.createUseHistory(
            userId = userId,
            amount = amount,
            balanceBefore = balanceBefore.value,
            balanceAfter = balanceAfter.value,
            orderId = orderId,
            description = description
        )
        return pointHistoryRepository.save(history)
    }

    /**
     * 포인트 소멸 이력 기록
     *
     * @param userId 사용자 ID
     * @param amount 소멸 금액
     * @param balanceBefore 소멸 전 잔액
     * @param balanceAfter 소멸 후 잔액
     * @param description 소멸 설명 (선택)
     * @return 기록된 포인트 이력
     */
    fun recordExpireHistory(
        userId: Long,
        amount: PointAmount,
        balanceBefore: Balance,
        balanceAfter: Balance,
        description: String? = null
    ): PointHistory {
        val history = PointHistory.createExpireHistory(
            userId = userId,
            amount = amount,
            balanceBefore = balanceBefore.value,
            balanceAfter = balanceAfter.value,
            description = description
        )
        return pointHistoryRepository.save(history)
    }

    /**
     * 사용자의 포인트 이력 조회 (최신순)
     *
     * @param userId 사용자 ID
     * @return 포인트 이력 목록
     */
    fun getPointHistories(userId: Long): List<PointHistory> {
        return pointHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId)
    }
}
