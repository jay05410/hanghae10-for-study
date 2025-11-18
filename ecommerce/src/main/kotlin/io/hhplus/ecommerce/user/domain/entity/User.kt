package io.hhplus.ecommerce.user.domain.entity

import io.hhplus.ecommerce.user.domain.constant.LoginType
import java.time.LocalDateTime

/**
 * 사용자 도메인 모델 (순수 비즈니스 로직)
 *
 * 역할:
 * - 사용자 정보 관리
 * - 사용자 상태 전환 및 검증
 * - 사용자 정보 업데이트 및 활성화/비활성화
 *
 * 비즈니스 규칙:
 * - 이메일과 로그인 ID는 유일해야 함
 * - 휴대폰 번호는 올바른 형식이어야 함 (010-1234-5678)
 * - 사용자 정보는 가변 객체로 관리 (직접 필드 수정)
 *
 * 주의: 이 클래스는 순수 도메인 모델이며 JPA 어노테이션이 없습니다.
 *       영속성은 infra/persistence/entity/UserJpaEntity에서 처리됩니다.
 */
data class User(
    val id: Long = 0,
    val loginType: LoginType,
    val loginId: String,
    val password: String?,
    var email: String,
    var name: String,
    val phone: String,
    val providerId: String?,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    val createdBy: Long = 0,
    var updatedBy: Long = 0,
    var deletedAt: LocalDateTime? = null
) {

    /**
     * 사용자 정보 업데이트
     *
     * @param name 변경할 이름
     * @param email 변경할 이메일
     * @param updatedBy 변경자 ID
     * @throws IllegalArgumentException 필수 정보 누락 시
     */
    fun update(name: String, email: String, updatedBy: Long) {
        require(name.isNotBlank()) { "이름은 필수입니다" }
        require(email.isNotBlank()) { "이메일은 필수입니다" }

        this.name = name
        this.email = email
        this.updatedBy = updatedBy
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 휴대폰 번호 형식 검증
     *
     * @throws IllegalArgumentException 형식이 올바르지 않을 때
     */
    fun validatePhoneFormat() {
        val phoneRegex = Regex("^01[0-9]-\\d{4}-\\d{4}$")
        if (!phone.matches(phoneRegex)) {
            throw IllegalArgumentException("올바른 휴대폰 번호 형식이 아닙니다: $phone")
        }
    }


    /**
     * 소프트 삭제
     *
     * @param deletedBy 삭제 처리자 ID
     */
    fun delete(deletedBy: Long) {
        this.deletedAt = LocalDateTime.now()
        this.updatedBy = deletedBy
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 삭제 상태 확인
     *
     * @return 삭제 여부
     */
    fun isDeleted(): Boolean = deletedAt != null

    /**
     * 소프트 삭제 복구
     */
    fun restore() {
        this.deletedAt = null
        this.updatedAt = LocalDateTime.now()
    }


    companion object {
        /**
         * 사용자 생성 팩토리 메서드
         *
         * @param loginType 로그인 타입
         * @param loginId 로그인 ID
         * @param password 비밀번호 (소셜 로그인의 경우 null)
         * @param email 이메일
         * @param name 이름
         * @param phone 휴대폰 번호
         * @param providerId 소셜 Provider ID (로컬 로그인의 경우 null)
         * @param createdBy 생성자 ID
         * @return 생성된 User 도메인 모델
         * @throws IllegalArgumentException 휴대폰 번호 형식이 올바르지 않을 때
         */
        fun create(
            loginType: LoginType,
            loginId: String,
            password: String?,
            email: String,
            name: String,
            phone: String,
            providerId: String?,
            createdBy: Long
        ): User {
            val now = LocalDateTime.now()
            return User(
                loginType = loginType,
                loginId = loginId,
                password = password,
                email = email,
                name = name,
                phone = phone,
                providerId = providerId,
                createdBy = createdBy,
                updatedBy = createdBy,
                createdAt = now,
                updatedAt = now
            ).also { it.validatePhoneFormat() }
        }
    }
}

