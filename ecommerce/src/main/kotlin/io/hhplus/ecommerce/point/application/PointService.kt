package io.hhplus.ecommerce.point.application

import io.hhplus.ecommerce.point.domain.entity.PointHistory
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
 * - 포인트 충전 및 차감 관리
 * - 포인트 잔액 조회 및 검증
 *
 * 책임:
 * - 사용자 포인트 생성 및 조회
 * - 포인트 충전 및 사용 처리
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

    fun chargePoint(userId: Long, amount: PointAmount, chargedBy: Long, description: String? = null): UserPoint {
        val userPoint = userPointRepository.findByUserIdWithLock(userId)
            ?: throw IllegalArgumentException("사용자 포인트 정보를 찾을 수 없습니다: $userId")

        userPoint.charge(amount, chargedBy)
        return userPointRepository.save(userPoint)
    }

    fun deductPoint(userId: Long, amount: PointAmount, deductedBy: Long, description: String? = null): UserPoint {
        val userPoint = userPointRepository.findByUserIdWithLock(userId)
            ?: throw IllegalArgumentException("사용자 포인트 정보를 찾을 수 없습니다: $userId")

        userPoint.deduct(amount, deductedBy)
        return userPointRepository.save(userPoint)
    }

}