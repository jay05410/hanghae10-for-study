package io.hhplus.ecommerce.user.usecase

import io.hhplus.ecommerce.user.application.UserService
import io.hhplus.ecommerce.user.domain.entity.User
import io.hhplus.ecommerce.user.dto.UpdateUserRequest
import org.springframework.stereotype.Component

/**
 * 사용자 정보 수정 유스케이스 - 애플리케이션 계층
 *
 * 역할:
 * - 기존 사용자 정보 수정 비즈니스 플로우 수행
 * - 사용자 정보 검증 및 업데이트 처리
 * - CQRS Command 패턴 구현
 *
 * 책임:
 * - 사용자 수정 권한 및 정보 유효성 검증
 * - 사용자 정보 업데이트 트랜잭션 관리
 * - 수정 후 관련 데이터 일관성 보장
 */
@Component
class UpdateUserUseCase(
    private val userService: UserService
) {
    /**
     * 지정된 사용자의 정보를 수정하고 업데이트한다
     *
     * @param userId 수정할 사용자 ID
     * @param request 사용자 정보 수정 요청 데이터
     * @return 수정이 완료된 사용자 정보
     * @throws IllegalArgumentException 사용자를 찾을 수 없거나 수정 정보가 잘못된 경우
     * @throws RuntimeException 사용자 정보 수정 처리에 실패한 경우
     */
    fun execute(userId: Long, request: UpdateUserRequest): User {
        return userService.updateUser(
            userId = userId,
            name = request.name,
            email = request.email,
            updatedBy = userId
        )
    }
}