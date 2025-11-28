package io.hhplus.ecommerce.point.application

import io.hhplus.ecommerce.point.exception.PointException
import io.hhplus.ecommerce.point.domain.entity.UserPoint
import io.hhplus.ecommerce.point.domain.repository.UserPointRepository
import io.hhplus.ecommerce.point.domain.vo.PointAmount
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 포인트 서비스 - 애플리케이션 계층
 *
 * 역할:
 * - 사용자 포인트의 핵심 비즈니스 로직 처리
 * - 포인트 적립/사용/소멸 관리
 * - 포인트 잔액 조회 및 검증
 *
 * 책임:
 * - 사용자 포인트 생성 및 조회
 * - 포인트 적립 및 사용 처리
 *
 * 주의:
 * - 동시성 제어는 UseCase 레벨에서 분산락으로 처리
 * - Service는 순수한 비즈니스 로직만 담당
 */
@Service
@Transactional
class PointService(
    private val userPointRepository: UserPointRepository
) {

    fun getUserPoint(userId: Long): UserPoint? {
        return userPointRepository.findByUserId(userId)
    }

    /**
     * 사용자의 포인트와 이력을 함께 조회한다 (FETCH JOIN 활용)
     *
     * @param userId 조회할 사용자 ID
     * @return 포인트 이력과 함께 조회된 UserPoint
     */
    @Transactional(readOnly = true)
    fun getUserPointWithHistories(userId: Long): UserPoint? {
        return userPointRepository.findUserPointWithHistoriesByUserId(userId)
    }

    fun createUserPoint(userId: Long): UserPoint {
        val userPoint = UserPoint.create(userId)
        return userPointRepository.save(userPoint)
    }

    /**
     * 포인트 적립 (구매 시 자동 적립)
     *
     * @param userId 사용자 ID
     * @param amount 적립할 포인트 금액
     * @param description 적립 설명
     * @return 적립 완료된 사용자 포인트 정보
     * @throws PointException.PointNotFound 사용자 포인트 정보가 없는 경우
     * @throws PointException.MaxBalanceExceeded 최대 잔액 초과 시
     */
    fun earnPoint(userId: Long, amount: PointAmount, description: String? = null): UserPoint {
        val userPoint = userPointRepository.findByUserId(userId)
            ?: throw PointException.PointNotFound(userId)

        userPoint.earn(amount)
        return userPointRepository.save(userPoint)
    }

    /**
     * 포인트 사용 (할인 적용)
     *
     * @param userId 사용자 ID
     * @param amount 사용할 포인트 금액
     * @param description 사용 설명
     * @return 사용 완료된 사용자 포인트 정보
     * @throws PointException.PointNotFound 사용자 포인트 정보가 없는 경우
     * @throws PointException.InsufficientBalance 잔액 부족 시
     * @throws PointException.InvalidAmount 사용 금액이 0 이하인 경우
     */
    fun usePoint(userId: Long, amount: PointAmount, description: String? = null): UserPoint {
        val userPoint = userPointRepository.findByUserId(userId)
            ?: throw PointException.PointNotFound(userId)

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
     * @throws PointException.InvalidAmount 소멸 금액이 0 이하인 경우
     */
    fun expirePoint(userId: Long, amount: PointAmount): UserPoint {
        val userPoint = userPointRepository.findByUserId(userId)
            ?: throw PointException.PointNotFound(userId)

        userPoint.expire(amount)
        return userPointRepository.save(userPoint)
    }

}