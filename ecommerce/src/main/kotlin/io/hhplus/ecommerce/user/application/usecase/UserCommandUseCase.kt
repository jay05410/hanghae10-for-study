package io.hhplus.ecommerce.user.application.usecase

import io.hhplus.ecommerce.user.domain.constant.LoginType
import io.hhplus.ecommerce.user.domain.entity.User
import io.hhplus.ecommerce.user.domain.service.UserDomainService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 사용자 명령 UseCase - Application Layer
 *
 * 역할:
 * - 사용자 변경 작업 비즈니스 흐름 오케스트레이션
 * - 트랜잭션 경계 관리
 *
 * 책임:
 * - 사용자 생성/수정 요청 검증 및 실행
 * - 사용자 데이터 무결성 보장
 */
@Component
class UserCommandUseCase(
    private val userDomainService: UserDomainService
) {

    /**
     * 새로운 사용자를 생성
     *
     * @param loginType 로그인 타입
     * @param loginId 로그인 ID
     * @param password 비밀번호
     * @param email 이메일
     * @param name 이름
     * @param phone 전화번호
     * @param providerId 외부 제공자 ID
     * @return 생성된 사용자 정보
     * @throws UserException.EmailAlreadyExists 이메일이 이미 존재하는 경우
     */
    @Transactional
    fun createUser(
        loginType: LoginType,
        loginId: String,
        password: String?,
        email: String,
        name: String,
        phone: String,
        providerId: String? = null
    ): User {
        // 1. 이메일 중복 검증
        userDomainService.validateNoDuplicateEmail(email)

        // 2. 사용자 생성
        return userDomainService.createUser(
            loginType = loginType,
            loginId = loginId,
            password = password,
            email = email,
            name = name,
            phone = phone,
            providerId = providerId
        )
    }

    /**
     * 사용자 정보를 업데이트
     *
     * @param userId 사용자 ID
     * @param name 이름
     * @param email 이메일
     * @return 업데이트된 사용자 정보
     * @throws UserException.UserNotFound 사용자를 찾을 수 없는 경우
     * @throws UserException.EmailAlreadyExists 이메일이 이미 존재하는 경우
     */
    @Transactional
    fun updateUser(userId: Long, name: String?, email: String?): User {
        // 1. 사용자 조회
        val user = userDomainService.getUserOrThrow(userId)

        // 2. 이메일 변경 시 중복 검증
        val newEmail = email ?: user.email
        userDomainService.validateEmailChangeAllowed(userId, newEmail, user.email)

        // 3. 사용자 정보 업데이트
        return userDomainService.updateUser(
            user = user,
            name = name ?: user.name,
            email = newEmail
        )
    }

    /**
     * 사용자를 삭제 (비활성화)
     *
     * @param userId 사용자 ID
     * @return 삭제된 사용자 정보
     * @throws UserException.UserNotFound 사용자를 찾을 수 없는 경우
     */
    @Transactional
    fun deleteUser(userId: Long): User {
        val user = userDomainService.getUserOrThrow(userId)
        return userDomainService.deactivateUser(user)
    }

    /**
     * 사용자를 복구 (활성화)
     *
     * @param userId 사용자 ID
     * @return 복구된 사용자 정보
     * @throws UserException.UserNotFound 사용자를 찾을 수 없는 경우
     */
    @Transactional
    fun restoreUser(userId: Long): User {
        val user = userDomainService.getUserOrThrow(userId)
        return userDomainService.activateUser(user)
    }
}
