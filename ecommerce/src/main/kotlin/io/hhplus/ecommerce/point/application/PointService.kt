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
 * - 동시성 제어를 통한 안전한 포인트 관리
 */
@Service
@Transactional
class PointService(
    private val userPointRepository: UserPointRepository
) {

    fun getUserPoint(userId: Long): UserPoint? {
        return userPointRepository.findByUserId(userId)
    }

    fun createUserPoint(userId: Long, createdBy: Long): UserPoint {
        val userPoint = UserPoint.create(userId, createdBy)
        return userPointRepository.save(userPoint)
    }

    /**
     * 포인트 적립 (구매 시 자동 적립)
     *
     * @throws PointException.PointNotFound 사용자 포인트 정보가 없는 경우
     * @throws PointException.MaxBalanceExceeded 최대 잔액 초과 시
     */
    fun earnPoint(userId: Long, amount: PointAmount, earnedBy: Long, description: String? = null): UserPoint {
        val userPoint = userPointRepository.findByUserIdWithLock(userId)
            ?: throw PointException.PointNotFound(userId)

        userPoint.earn(amount, earnedBy)
        return userPointRepository.save(userPoint)
    }

    /**
     * 포인트 사용 (할인 적용)
     *
     * @throws PointException.PointNotFound 사용자 포인트 정보가 없는 경우
     * @throws PointException.InsufficientBalance 잔액 부족 시
     * @throws PointException.InvalidAmount 사용 금액이 0 이하인 경우
     */
    fun usePoint(userId: Long, amount: PointAmount, usedBy: Long, description: String? = null): UserPoint {
        val userPoint = userPointRepository.findByUserIdWithLock(userId)
            ?: throw PointException.PointNotFound(userId)

        userPoint.use(amount, usedBy)
        return userPointRepository.save(userPoint)
    }

    /**
     * 포인트 소멸 (만료 처리)
     *
     * @throws PointException.PointNotFound 사용자 포인트 정보가 없는 경우
     * @throws PointException.InsufficientBalance 소멸 가능한 포인트 부족 시
     * @throws PointException.InvalidAmount 소멸 금액이 0 이하인 경우
     */
    fun expirePoint(userId: Long, amount: PointAmount): UserPoint {
        val userPoint = userPointRepository.findByUserIdWithLock(userId)
            ?: throw PointException.PointNotFound(userId)

        userPoint.expire(amount)
        return userPointRepository.save(userPoint)
    }

}