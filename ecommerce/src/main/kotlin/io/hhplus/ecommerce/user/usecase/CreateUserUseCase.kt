package io.hhplus.ecommerce.user.usecase

import io.hhplus.ecommerce.user.application.UserService
import io.hhplus.ecommerce.user.domain.entity.User
import io.hhplus.ecommerce.user.domain.constant.LoginType
import io.hhplus.ecommerce.user.dto.CreateUserRequest
import org.springframework.stereotype.Component

/**
 * 사용자 생성 유스케이스 - 애플리케이션 계층
 *
 * 역할:
 * - 새로운 사용자 회원가입 비즈니스 플로우 수행
 * - 사용자 정보 검증 및 등록 처리
 * - CQRS Command 패턴 구현
 *
 * 책임:
 * - 사용자 정보 유효성 검증
 * - 사용자 생성 트랜잭션 관리
 * - 사용자 생성 후 초기 설정 및 연동 서비스 처리
 */
@Component
class CreateUserUseCase(
    private val userService: UserService
) {
    /**
     * 새로운 사용자를 등록하고 생성한다
     *
     * @param request 사용자 생성 요청 데이터
     * @return 생성이 완료된 사용자 정보
     * @throws IllegalArgumentException 사용자 정보가 유효하지 않거나 중복되는 경우
     * @throws RuntimeException 사용자 생성 처리에 실패한 경우
     */
    fun execute(request: CreateUserRequest): User {
        return userService.createUser(
            loginType = LoginType.LOCAL,
            loginId = request.email,
            password = null,
            email = request.email,
            name = request.name,
            phone = "010-0000-0000",
            providerId = null,
            createdBy = 1L
        )
    }
}