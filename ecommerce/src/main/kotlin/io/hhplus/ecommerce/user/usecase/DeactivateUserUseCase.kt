package io.hhplus.ecommerce.user.usecase

import io.hhplus.ecommerce.user.application.UserService
import io.hhplus.ecommerce.user.domain.entity.User
import org.springframework.stereotype.Component

/**
 * 사용자 비활성화 유스케이스 - 애플리케이션 계층
 *
 * 역할:
 * - 활성 상태의 사용자를 비활성 상태로 변경
 * - 사용자 상태 관리 및 접근 권한 제한
 * - CQRS Command 패턴 구현
 *
 * 책임:
 * - 사용자 비활성화 가능 상태 검증
 * - 사용자 상태 변경 트랜잭션 관리
 * - 비활성화 후 관련 데이터 정리 및 연동 서비스 처리
 */
@Component
class DeactivateUserUseCase(
    private val userService: UserService
) {
    /**
     * 사용자를 비활성 상태로 변경하여 서비스 이용을 제한한다
     *
     * @param userId 비활성화할 사용자 ID
     * @return 비활성화가 완료된 사용자 정보
     * @throws IllegalArgumentException 사용자를 찾을 수 없거나 이미 비활성화된 상태인 경우
     * @throws RuntimeException 사용자 비활성화 처리에 실패한 경우
     */
    fun execute(userId: Long): User {
        return userService.deactivateUser(userId, userId)
    }
}