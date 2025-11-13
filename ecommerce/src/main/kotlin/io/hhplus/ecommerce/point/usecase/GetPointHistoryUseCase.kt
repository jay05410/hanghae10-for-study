package io.hhplus.ecommerce.point.usecase

import io.hhplus.ecommerce.point.application.PointHistoryService
import io.hhplus.ecommerce.point.domain.entity.PointHistory
import org.springframework.stereotype.Component

/**
 * 포인트 이력 조회 UseCase
 *
 * 역할:
 * - 포인트 이력 조회 요청 처리
 * - 비즈니스 로직을 PointHistoryService에 위임
 *
 * 책임:
 * - 포인트 이력 조회 요청 검증 및 실행
 * - 조회된 이력 정보 반환
 */
@Component
class GetPointHistoryUseCase(
    private val pointHistoryService: PointHistoryService
) {

    /**
     * 사용자의 포인트 이력을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 사용자의 포인트 이력 목록 (최신순)
     */
    fun execute(userId: Long): List<PointHistory> {
        return pointHistoryService.getPointHistories(userId)
    }
}