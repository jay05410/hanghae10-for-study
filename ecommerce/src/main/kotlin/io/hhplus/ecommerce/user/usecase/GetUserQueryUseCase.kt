package io.hhplus.ecommerce.user.usecase

import io.hhplus.ecommerce.user.application.UserService
import io.hhplus.ecommerce.user.domain.entity.User
import org.springframework.stereotype.Component

/**
 * 사용자 조회 통합 유스케이스 - 애플리케이션 계층
 *
 * 역할:
 * - 사용자 관련 다양한 조회 작업 통합 처리
 * - 사용자 정보 조회 및 비즈니스 로직 수행
 * - CQRS Query 패턴 구현
 *
 * 책임:
 * - 다양한 사용자 조회 사용 사례 통합 처리
 * - 사용자 데이터 반환 및 전달
 * - 읽기 전용 작업 처리
 */
@Component
class GetUserQueryUseCase(
    private val userService: UserService
) {

    /**
     * 사용자 ID로 특정 사용자를 조회한다
     *
     * @param userId 조회할 사용자 ID
     * @return 사용자 정보 (존재하지 않으면 null 반환)
     */
    fun getUser(userId: Long): User? {
        return userService.getUser(userId)
    }

    /**
     * 시스템에 등록된 모든 사용자 목록을 조회한다
     *
     * @return 전체 사용자 목록 (모든 상태 포함)
     */
    fun getAllUsers(): List<User> {
        return userService.getAllUsers()
    }
}