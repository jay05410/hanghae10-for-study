package io.hhplus.ecommerce.user.domain.service

import io.hhplus.ecommerce.user.domain.constant.LoginType
import io.hhplus.ecommerce.user.domain.entity.User
import io.hhplus.ecommerce.user.domain.repository.UserRepository
import io.hhplus.ecommerce.user.exception.UserException
import org.springframework.stereotype.Component

/**
 * 사용자 도메인 서비스 - 순수 도메인 로직
 *
 * 역할:
 * - 사용자 엔티티 생성 및 상태 관리
 * - 이메일 중복 검증
 * - 사용자 정보 업데이트
 *
 * 책임:
 * - 사용자 도메인 불변식 보장
 * - Repository(Port)를 통한 영속성 위임
 *
 * 주의:
 * - 트랜잭션 관리는 UseCase에서 담당
 * - @Transactional 사용 금지
 */
@Component
class UserDomainService(
    private val userRepository: UserRepository
) {

    /**
     * 이메일 중복 검증
     *
     * @param email 검증할 이메일
     * @throws UserException.EmailAlreadyExists 이메일이 이미 존재하는 경우
     */
    fun validateNoDuplicateEmail(email: String) {
        val existingUser = userRepository.findByEmail(email)
        if (existingUser != null) {
            throw UserException.EmailAlreadyExists(email)
        }
    }

    /**
     * 사용자 생성
     *
     * @param loginType 로그인 타입
     * @param loginId 로그인 ID
     * @param password 비밀번호
     * @param email 이메일
     * @param name 이름
     * @param phone 전화번호
     * @param providerId 외부 제공자 ID
     * @return 생성된 사용자
     */
    fun createUser(
        loginType: LoginType,
        loginId: String,
        password: String?,
        email: String,
        name: String,
        phone: String,
        providerId: String?
    ): User {
        val user = User.create(
            loginType = loginType,
            loginId = loginId,
            password = password,
            email = email,
            name = name,
            phone = phone,
            providerId = providerId
        )
        return userRepository.save(user)
    }

    /**
     * 사용자 ID로 조회
     *
     * @param userId 사용자 ID
     * @return 사용자 또는 null
     */
    fun getUser(userId: Long): User? {
        return userRepository.findById(userId)
    }

    /**
     * 사용자 ID로 조회 (필수)
     *
     * @param userId 사용자 ID
     * @return 사용자
     * @throws UserException.UserNotFound 사용자가 없는 경우
     */
    fun getUserOrThrow(userId: Long): User {
        return userRepository.findById(userId)
            ?: throw UserException.UserNotFound(userId)
    }

    /**
     * 이메일로 사용자 조회
     *
     * @param email 이메일
     * @return 사용자 또는 null
     */
    fun getUserByEmail(email: String): User? {
        return userRepository.findByEmail(email)
    }

    /**
     * 이메일 변경 시 중복 검증
     *
     * @param userId 현재 사용자 ID
     * @param newEmail 변경할 이메일
     * @param currentEmail 현재 이메일
     * @throws UserException.EmailAlreadyExists 다른 사용자가 해당 이메일을 사용 중인 경우
     */
    fun validateEmailChangeAllowed(userId: Long, newEmail: String, currentEmail: String) {
        if (newEmail != currentEmail) {
            val existingUser = userRepository.findByEmail(newEmail)
            if (existingUser != null && existingUser.id != userId) {
                throw UserException.EmailAlreadyExists(newEmail)
            }
        }
    }

    /**
     * 사용자 정보 업데이트
     *
     * @param user 사용자
     * @param name 변경할 이름
     * @param email 변경할 이메일
     * @return 업데이트된 사용자
     */
    fun updateUser(user: User, name: String, email: String): User {
        user.update(name = name, email = email)
        return userRepository.save(user)
    }

    /**
     * 사용자 비활성화
     *
     * @param user 사용자
     * @return 비활성화된 사용자
     */
    fun deactivateUser(user: User): User {
        user.deactivate()
        return userRepository.save(user)
    }

    /**
     * 사용자 활성화
     *
     * @param user 사용자
     * @return 활성화된 사용자
     */
    fun activateUser(user: User): User {
        user.activate()
        return userRepository.save(user)
    }

    /**
     * 모든 활성 사용자 조회
     *
     * @return 활성 사용자 목록
     */
    fun getAllActiveUsers(): List<User> {
        return userRepository.findActiveUsers()
    }
}