package io.hhplus.ecommerce.user.usecase

import io.hhplus.ecommerce.user.domain.entity.User
import io.hhplus.ecommerce.user.domain.repository.UserRepository
import io.hhplus.ecommerce.user.domain.constant.LoginType
import io.hhplus.ecommerce.user.exception.UserException
import org.springframework.stereotype.Component

/**
 * 사용자 명령 UseCase
 *
 * 역할:
 * - 모든 사용자 변경 작업을 통합 관리
 * - 사용자 생성, 수정, 상태 변경 기능 제공
 *
 * 책임:
 * - 사용자 생성/수정 요청 검증 및 실행
 * - 사용자 데이터 무결성 보장
 */
@Component
class UserCommandUseCase(
    private val userRepository: UserRepository
) {

    /**
     * 새로운 사용자를 생성합니다.
     *
     * @param loginType 로그인 타입
     * @param loginId 로그인 ID
     * @param password 비밀번호
     * @param email 이메일
     * @param name 이름
     * @param phone 전화번호
     * @param providerId 외부 제공자 ID
     * @param createdBy 생성 요청자 ID
     * @return 생성된 사용자 정보
     * @throws UserException.EmailAlreadyExists 이메일이 이미 존재하는 경우
     */
    fun createUser(
        loginType: LoginType,
        loginId: String,
        password: String?,
        email: String,
        name: String,
        phone: String,
        providerId: String? = null,
        createdBy: Long = 1L
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

    /**
     * 사용자 정보를 업데이트합니다.
     *
     * @param userId 사용자 ID
     * @param name 이름
     * @param email 이메일
     * @param updatedBy 업데이트 요청자 ID
     * @return 업데이트된 사용자 정보
     * @throws UserException.UserNotFound 사용자를 찾을 수 없는 경우
     * @throws UserException.EmailAlreadyExists 이메일이 이미 존재하는 경우
     */
    fun updateUser(userId: Long, name: String?, email: String?, updatedBy: Long = 1L): User {
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

    /**
     * 사용자를 삭제합니다.
     *
     * @param userId 사용자 ID
     * @param deletedBy 삭제 요청자 ID
     * @return 삭제된 사용자 정보
     * @throws UserException.UserNotFound 사용자를 찾을 수 없는 경우
     */
    fun deleteUser(userId: Long, deletedBy: Long = 1L): User {
        val user = userRepository.findById(userId)
            ?: throw UserException.UserNotFound(userId)

        user.delete(deletedBy)
        return userRepository.save(user)
    }

    /**
     * 사용자를 복구합니다.
     *
     * @param userId 사용자 ID
     * @param restoredBy 복구 요청자 ID
     * @return 복구된 사용자 정보
     * @throws UserException.UserNotFound 사용자를 찾을 수 없는 경우
     */
    fun restoreUser(userId: Long, restoredBy: Long = 1L): User {
        val user = userRepository.findById(userId)
            ?: throw UserException.UserNotFound(userId)

        user.restore()
        user.updatedBy = restoredBy
        return userRepository.save(user)
    }
}