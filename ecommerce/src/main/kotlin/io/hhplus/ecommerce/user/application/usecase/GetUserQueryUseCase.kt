package io.hhplus.ecommerce.user.application.usecase

import io.hhplus.ecommerce.user.domain.entity.User
import io.hhplus.ecommerce.user.domain.service.UserDomainService
import org.springframework.stereotype.Component

/**
 * 사용자 조회 UseCase - Application Layer
 *
 * 역할:
 * - 사용자 조회 비즈니스 흐름 오케스트레이션
 *
 * 책임:
 * - 사용자 ID로 단일 사용자 조회
 * - 이메일로 사용자 조회
 * - 활성 사용자 전체 조회
 */
@Component
class GetUserQueryUseCase(
    private val userDomainService: UserDomainService
) {

    fun getUser(userId: Long): User? {
        return userDomainService.getUser(userId)
    }

    fun getUserByEmail(email: String): User? {
        return userDomainService.getUserByEmail(email)
    }

    fun getAllActiveUsers(): List<User> {
        return userDomainService.getAllActiveUsers()
    }
}