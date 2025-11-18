package io.hhplus.ecommerce.user.application

import io.hhplus.ecommerce.user.domain.entity.User
import io.hhplus.ecommerce.user.domain.repository.UserRepository
import io.hhplus.ecommerce.user.domain.constant.LoginType
import io.hhplus.ecommerce.user.exception.UserException
import org.springframework.stereotype.Service

/**
 * 사용자 서비스 - 애플리케이션 계층
 *
 * 역할:
 * - 사용자 도메인의 핵심 비즈니스 로직 처리
 * - 사용자 생명주기 및 상태 관리
 * - 사용자 인증 및 권한 관리
 *
 * 책임:
 * - 사용자 등록, 수정, 비활성화 처리
 * - 사용자 정보 조회 및 검증
 * - 이메일 중복 및 데이터 무결성 검증
 */
@Service
class UserService(
    private val userRepository: UserRepository
) {

    fun createUser(
        loginType: LoginType,
        loginId: String,
        password: String?,
        email: String,
        name: String,
        phone: String,
        providerId: String?,
        createdBy: Long
    ): User {
        // 이메일 중복 검증
        val existingUser = userRepository.findByEmail(email)
        if (existingUser != null) {
            throw UserException.EmailAlreadyExists(email)
        }

        val user = User.create(
            loginType = loginType,
            loginId = loginId,
            password = password,
            email = email,
            name = name,
            phone = phone,
            providerId = providerId,
            createdBy = createdBy
        )

        return userRepository.save(user)
    }

    fun getUser(userId: Long): User? {
        return userRepository.findById(userId)
    }

    fun getUserByEmail(email: String): User? {
        return userRepository.findByEmail(email)
    }

    fun updateUser(userId: Long, name: String?, email: String?, updatedBy: Long): User {
        val user = userRepository.findById(userId)
            ?: throw UserException.UserNotFound(userId)

        // 이메일 변경 시 중복 검증
        if (email != null && email != user.email) {
            val existingUser = userRepository.findByEmail(email)
            if (existingUser != null && existingUser.id != userId) {
                throw UserException.EmailAlreadyExists(email)
            }
        }

        // 가변 모델: update 메서드 호출 후 저장
        user.update(
            name = name ?: user.name,
            email = email ?: user.email,
            updatedBy = updatedBy
        )

        return userRepository.save(user)
    }

    fun deactivateUser(userId: Long, deactivatedBy: Long): User {
        val user = userRepository.findById(userId)
            ?: throw UserException.UserNotFound(userId)

        // 가변 모델: delete 메서드 호출 후 저장 (soft delete)
        user.delete(deactivatedBy)
        return userRepository.save(user)
    }

    fun activateUser(userId: Long, activatedBy: Long): User {
        val user = userRepository.findById(userId)
            ?: throw UserException.UserNotFound(userId)

        // 가변 모델: deletedAt을 null로 설정하여 복원
        user.deletedAt = null
        user.updatedBy = activatedBy
        user.updatedAt = java.time.LocalDateTime.now()
        return userRepository.save(user)
    }

    fun getAllUsers(): List<User> {
        return userRepository.findByIsActiveTrue()
    }
}