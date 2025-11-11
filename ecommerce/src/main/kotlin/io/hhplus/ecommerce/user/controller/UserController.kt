package io.hhplus.ecommerce.user.controller

import io.hhplus.ecommerce.user.application.UserService
import io.hhplus.ecommerce.user.dto.*
import io.hhplus.ecommerce.user.domain.constant.LoginType
import io.hhplus.ecommerce.common.response.ApiResponse
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
@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService
) {

    /**
     * 새로운 사용자를 생성한다
     *
     * @param request 사용자 생성 요청 데이터
     * @return 생성된 사용자 정보를 포함한 API 응답
     */
    /**
     * 새로운 사용자를 생성한다
     *
     * @param request 사용자 생성 요청 데이터
     * @return 생성된 사용자 정보를 포함한 API 응답
     */
    @PostMapping
    fun createUser(@RequestBody request: CreateUserRequest): ApiResponse<UserResponse> {
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

    /**
     * 사용자 ID로 단일 사용자를 조회한다
     *
     * @param userId 조회할 사용자의 ID
     * @return 사용자 정보를 포함한 API 응답
     */
    /**
     * 사용자 ID로 단일 사용자를 조회한다
     *
     * @param userId 조회할 사용자의 ID
     * @return 사용자 정보를 포함한 API 응답
     */
    @GetMapping("/{userId}")
    fun getUser(@PathVariable userId: Long): ApiResponse<UserResponse?> {
        val user = userService.getUser(userId)
        return ApiResponse.success(user?.toResponse())
    }

    /**
     * 기존 사용자 정보를 업데이트한다
     *
     * @param userId 업데이트할 사용자의 ID
     * @param request 사용자 업데이트 요청 데이터
     * @return 업데이트된 사용자 정보를 포함한 API 응답
     */
    /**
     * 기존 사용자 정보를 업데이트한다
     *
     * @param userId 업데이트할 사용자의 ID
     * @param request 사용자 업데이트 요청 데이터
     * @return 업데이트된 사용자 정보를 포함한 API 응답
     */
    @PutMapping("/{userId}")
    fun updateUser(
        @PathVariable userId: Long,
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

    /**
     * 모든 사용자 목록을 조회한다
     *
     * @return 사용자 목록을 포함한 API 응답
     */
    /**
     * 모든 사용자 목록을 조회한다
     *
     * @return 사용자 목록을 포함한 API 응답
     */
    @GetMapping
    fun getAllUsers(): ApiResponse<List<UserResponse>> {
        val users = userService.getAllUsers()
        return ApiResponse.success(users.map { it.toResponse() })
    }


    /**
     * 사용자 계정을 비활성화한다
     *
     * @param userId 비활성화할 사용자의 ID
     * @return 비활성화된 사용자 정보를 포함한 API 응답
     */
    /**
     * 사용자 계정을 비활성화한다
     *
     * @param userId 비활성화할 사용자의 ID
     * @return 비활성화된 사용자 정보를 포함한 API 응답
     */
    @PostMapping("/{userId}/deactivate")
    fun deactivateUser(@PathVariable userId: Long): ApiResponse<UserResponse> {
        val user = userService.deactivateUser(userId, userId)
        return ApiResponse.success(user.toResponse())
    }

    /**
     * 사용자 계정을 활성화한다
     *
     * @param userId 활성화할 사용자의 ID
     * @return 활성화된 사용자 정보를 포함한 API 응답
     */
    /**
     * 사용자 계정을 활성화한다
     *
     * @param userId 활성화할 사용자의 ID
     * @return 활성화된 사용자 정보를 포함한 API 응답
     */
    @PostMapping("/{userId}/activate")
    fun activateUser(@PathVariable userId: Long): ApiResponse<UserResponse> {
        val user = userService.activateUser(userId, userId)
        return ApiResponse.success(user.toResponse())
    }
}