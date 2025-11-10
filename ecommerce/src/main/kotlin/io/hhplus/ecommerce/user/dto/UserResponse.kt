package io.hhplus.ecommerce.user.dto

import io.hhplus.ecommerce.user.domain.entity.User
import io.hhplus.ecommerce.user.domain.constant.LoginType
import java.time.LocalDateTime

/**
 * 사용자 정보 응답 DTO - 프레젠테이션 계층
 *
 * 역할:
 * - User 도메인 엔티티의 API 응답 형태로 변환
 * - 클라이언트에게 필요한 정보만 노출 (비밀번호 등 민감정보 제외)
 * - 도메인 객체와 API 스펙 간의 격리
 */
data class UserResponse(
    val id: Long,
    val loginType: LoginType,
    val loginId: String,
    val email: String,
    val name: String,
    val phone: String,
    val providerId: String?,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun User.toResponse(): UserResponse = UserResponse(
            id = this.id,
            loginType = this.loginType,
            loginId = this.loginId,
            email = this.email,
            name = this.name,
            phone = this.phone,
            providerId = this.providerId,
            isActive = this.isActive,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}
}