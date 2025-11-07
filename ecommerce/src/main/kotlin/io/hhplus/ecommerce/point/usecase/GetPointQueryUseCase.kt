package io.hhplus.ecommerce.point.usecase

import io.hhplus.ecommerce.point.application.PointService
import io.hhplus.ecommerce.point.application.PointHistoryService
import io.hhplus.ecommerce.point.domain.entity.UserPoint
import io.hhplus.ecommerce.point.domain.entity.PointHistory
import org.springframework.stereotype.Component

/**
 * 포인트 조회 통합 유스케이스 - 애플리케이션 계층
 *
 * 역할:
 * - 포인트 관련 다양한 조회 작업 통합 처리
 * - 사용자별 포인트 정보 조회 및 비즈니스 로직 수행
 * - CQRS Query 패턴 구현
 *
 * 책임:
 * - 다양한 포인트 조회 사용 사례 통합 처리
 * - 포인트 데이터 반환 및 전달
 * - 읽기 전용 작업 처리
 */
@Component
class GetPointQueryUseCase(
    private val pointService: PointService,
    private val pointHistoryService: PointHistoryService
) {

    /**
     * 사용자의 현재 포인트 잔액을 조회한다
     *
     * @param userId 인증된 사용자 ID
     * @return 사용자의 포인트 정보
     * @throws IllegalArgumentException 사용자 포인트 정보가 없는 경우
     */
    fun getUserPoint(userId: Long): UserPoint {
        return pointService.getUserPoint(userId)
            ?: throw IllegalArgumentException("사용자 포인트 정보가 없습니다: $userId")
    }

    /**
     * 사용자의 모든 포인트 거래 내역을 조회한다
     *
     * @param userId 인증된 사용자 ID
     * @return 사용자의 포인트 거래 내역 목록 (충전과 차감 모두 포함)
     */
    fun getPointHistories(userId: Long): List<PointHistory> {
        return pointHistoryService.getPointHistories(userId)
    }
}