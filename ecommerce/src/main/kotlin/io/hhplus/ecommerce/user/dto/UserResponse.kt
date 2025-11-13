package io.hhplus.ecommerce.user.dto

import io.hhplus.ecommerce.user.domain.entity.User
import io.hhplus.ecommerce.user.domain.constant.LoginType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * 사용자 정보 응답 DTO - 프레젠테이션 계층
 *
 * 역할:
 * - User 도메인 엔티티의 API 응답 형태로 변환
 * - 클라이언트에게 필요한 정보만 노출 (비밀번호 등 민감정보 제외)
 * - 도메인 객체와 API 스펙 간의 격리
 */
@Schema(description = "사용자 정보")
data class UserResponse(
    @Schema(description = "사용자 ID", example = "1")
    val id: Long,

    @Schema(description = "로그인 타입", example = "LOCAL", allowableValues = ["LOCAL", "KAKAO", "NAVER", "GOOGLE"])
    val loginType: LoginType,

    @Schema(description = "로그인 ID", example = "hong@example.com")
    val loginId: String,

    @Schema(description = "이메일", example = "hong@example.com")
    val email: String,

    @Schema(description = "이름", example = "홍길동")
    val name: String,

    @Schema(description = "전화번호", example = "010-1234-5678")
    val phone: String,

    @Schema(description = "OAuth Provider ID (선택)", example = "kakao_123456")
    val providerId: String?,

    @Schema(description = "활성화 상태", example = "true")
    val isActive: Boolean,

    @Schema(description = "생성 일시", example = "2025-01-01T10:00:00")
    val createdAt: LocalDateTime,

    @Schema(description = "수정 일시", example = "2025-01-15T14:30:00")
    val updatedAt: LocalDateTime
)

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