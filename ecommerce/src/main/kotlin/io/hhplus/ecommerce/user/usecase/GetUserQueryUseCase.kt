package io.hhplus.ecommerce.user.usecase

import io.hhplus.ecommerce.user.application.UserService
import io.hhplus.ecommerce.user.domain.entity.User
import org.springframework.stereotype.Component

/**
 * 사용자 조회 UseCase
 *
 * 책임:
 * - 사용자 ID로 단일 사용자 조회
 * - 이메일로 사용자 조회
 * - 활성 사용자 전체 조회
 */
@Component
class GetUserQueryUseCase(
    private val userService: UserService
) {

    fun getUser(userId: Long): User? {
        return userService.getUser(userId)
    }

    fun getUserByEmail(email: String): User? {
        return userService.getUserByEmail(email)
    }

    fun getAllActiveUsers(): List<User> {
        return userService.getAllUsers()
    }
}