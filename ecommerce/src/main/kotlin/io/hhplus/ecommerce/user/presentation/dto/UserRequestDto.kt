package io.hhplus.ecommerce.user.presentation.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사용자 생성 요청")
data class CreateUserRequest(
    @Schema(description = "사용자 이름", example = "홍길동", required = true)
    val name: String,

    @Schema(description = "이메일 주소", example = "hong@example.com", required = true)
    val email: String
)

@Schema(description = "사용자 정보 수정 요청")
data class UpdateUserRequest(
    @Schema(description = "변경할 이름 (선택)", example = "김철수")
    val name: String?,

    @Schema(description = "변경할 이메일 (선택)", example = "kim@example.com")
    val email: String?
)
