package io.hhplus.ecommerce.user.controller

import io.hhplus.ecommerce.user.application.UserService
import io.hhplus.ecommerce.user.dto.*
import io.hhplus.ecommerce.user.domain.constant.LoginType
import io.hhplus.ecommerce.common.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

/**
 * 사용자 API 컨트롤러 - 프레젠테이션 계층
 *
 * 역할:
 * - 사용자 관련 REST API 엔드포인트 제공
 * - HTTP 요청/응답 처리 및 데이터 변환
 * - 비즈니스 로직은 Service에 직접 위임
 *
 * 책임:
 * - 요청 데이터 검증 및 응답 형식 통일
 * - UserService로 비즈니스 로직 위임
 * - HTTP 상태 코드 및 에러 처리
 */
@Tag(name = "사용자 관리", description = "사용자 CRUD 및 계정 활성화/비활성화 API")
@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService
) {

    @Operation(summary = "사용자 생성", description = "새로운 사용자를 생성합니다.")
    @PostMapping
    fun createUser(
        @Parameter(description = "사용자 생성 정보", required = true)
        @RequestBody request: CreateUserRequest
    ): ApiResponse<UserResponse> {
        val user = userService.createUser(
            loginType = LoginType.LOCAL,
            loginId = request.email,
            password = null,
            email = request.email,
            name = request.name,
            phone = "010-0000-0000",
            providerId = null,
            createdBy = 1L
        )
        return ApiResponse.success(user.toResponse())
    }

    @Operation(summary = "사용자 조회", description = "사용자 ID로 단일 사용자를 조회합니다.")
    @GetMapping("/{userId}")
    fun getUser(
        @Parameter(description = "조회할 사용자 ID", required = true)
        @PathVariable userId: Long
    ): ApiResponse<UserResponse?> {
        val user = userService.getUser(userId)
        return ApiResponse.success(user?.toResponse())
    }

    @Operation(summary = "사용자 정보 수정", description = "기존 사용자 정보를 업데이트합니다.")
    @PutMapping("/{userId}")
    fun updateUser(
        @Parameter(description = "수정할 사용자 ID", required = true)
        @PathVariable userId: Long,
        @Parameter(description = "사용자 수정 정보", required = true)
        @RequestBody request: UpdateUserRequest
    ): ApiResponse<UserResponse> {
        val user = userService.updateUser(
            userId = userId,
            name = request.name,
            email = request.email,
            updatedBy = userId
        )
        return ApiResponse.success(user.toResponse())
    }

    @Operation(summary = "사용자 목록 조회", description = "모든 사용자 목록을 조회합니다.")
    @GetMapping
    fun getAllUsers(): ApiResponse<List<UserResponse>> {
        val users = userService.getAllUsers()
        return ApiResponse.success(users.map { it.toResponse() })
    }


    @Operation(summary = "사용자 비활성화", description = "사용자 계정을 비활성화합니다.")
    @PostMapping("/{userId}/deactivate")
    fun deactivateUser(
        @Parameter(description = "비활성화할 사용자 ID", required = true)
        @PathVariable userId: Long
    ): ApiResponse<UserResponse> {
        val user = userService.deactivateUser(userId, userId)
        return ApiResponse.success(user.toResponse())
    }

    @Operation(summary = "사용자 활성화", description = "사용자 계정을 활성화합니다.")
    @PostMapping("/{userId}/activate")
    fun activateUser(
        @Parameter(description = "활성화할 사용자 ID", required = true)
        @PathVariable userId: Long
    ): ApiResponse<UserResponse> {
        val user = userService.activateUser(userId, userId)
        return ApiResponse.success(user.toResponse())
    }
}