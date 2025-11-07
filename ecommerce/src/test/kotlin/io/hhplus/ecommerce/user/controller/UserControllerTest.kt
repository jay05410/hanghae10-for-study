package io.hhplus.ecommerce.user.controller

import io.hhplus.ecommerce.user.usecase.*
import io.hhplus.ecommerce.user.dto.*
import io.hhplus.ecommerce.user.domain.entity.User
import io.hhplus.ecommerce.common.response.ApiResponse
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * UserController 단위 테스트
 *
 * 책임: 사용자 관련 HTTP 요청 처리 검증
 * - REST API 엔드포인트의 요청/응답 처리 검증
 * - UseCase 계층과의 올바른 상호작용 검증
 * - 요청 데이터 변환 및 응답 형식 검증
 *
 * 검증 목표:
 * 1. 각 엔드포인트가 적절한 UseCase를 호출하는가?
 * 2. 요청 파라미터와 Body가 올바르게 UseCase에 전달되는가?
 * 3. UseCase 결과가 적절한 ApiResponse로 변환되는가?
 * 4. HTTP 메서드와 경로 매핑이 올바른가?
 * 5. 다양한 요청 형태에 대한 처리가 올바른가?
 */
class UserControllerTest : DescribeSpec({
    val mockCreateUserUseCase = mockk<CreateUserUseCase>()
    val mockGetUserQueryUseCase = mockk<GetUserQueryUseCase>()
    val mockUpdateUserUseCase = mockk<UpdateUserUseCase>()
    val mockDeactivateUserUseCase = mockk<DeactivateUserUseCase>()
    val mockActivateUserUseCase = mockk<ActivateUserUseCase>()

    val sut = UserController(
        createUserUseCase = mockCreateUserUseCase,
        getUserQueryUseCase = mockGetUserQueryUseCase,
        updateUserUseCase = mockUpdateUserUseCase,
        deactivateUserUseCase = mockDeactivateUserUseCase,
        activateUserUseCase = mockActivateUserUseCase
    )

    beforeEach {
        clearMocks(
            mockCreateUserUseCase,
            mockGetUserQueryUseCase,
            mockUpdateUserUseCase,
            mockDeactivateUserUseCase,
            mockActivateUserUseCase
        )
    }

    describe("createUser") {
        context("POST /api/v1/users 요청") {
            it("CreateUserRequest를 CreateUserUseCase에 전달하고 ApiResponse로 반환") {
                val request = CreateUserRequest(
                    name = "테스트사용자",
                    email = "test@example.com"
                )
                val mockUser = mockk<User>()

                every { mockCreateUserUseCase.execute(request) } returns mockUser

                val result = sut.createUser(request)

                result shouldBe ApiResponse.success(mockUser)
                verify(exactly = 1) { mockCreateUserUseCase.execute(request) }
            }
        }

        context("다양한 사용자 정보로 생성 요청") {
            it("각 요청이 정확히 UseCase에 전달") {
                val requests = listOf(
                    CreateUserRequest("사용자1", "user1@example.com"),
                    CreateUserRequest("사용자2", "user2@example.com"),
                    CreateUserRequest("테스트사용자", "test@test.com")
                )

                requests.forEach { request ->
                    val mockUser = mockk<User>()
                    every { mockCreateUserUseCase.execute(request) } returns mockUser

                    val result = sut.createUser(request)

                    result shouldBe ApiResponse.success(mockUser)
                    verify(exactly = 1) { mockCreateUserUseCase.execute(request) }
                    clearMocks(mockCreateUserUseCase)
                }
            }
        }
    }

    describe("getUser") {
        context("GET /api/v1/users/{userId} 요청") {
            it("userId를 GetUserQueryUseCase에 전달하고 ApiResponse로 반환") {
                val userId = 1L
                val mockUser = mockk<User>()

                every { mockGetUserQueryUseCase.getUser(userId) } returns mockUser

                val result = sut.getUser(userId)

                result shouldBe ApiResponse.success(mockUser)
                verify(exactly = 1) { mockGetUserQueryUseCase.getUser(userId) }
            }
        }

        context("존재하지 않는 사용자 조회") {
            it("UseCase에서 반환된 null을 ApiResponse로 감싸서 반환") {
                val userId = 999L

                every { mockGetUserQueryUseCase.getUser(userId) } returns null

                val result = sut.getUser(userId)

                result shouldBe ApiResponse.success(null)
                verify(exactly = 1) { mockGetUserQueryUseCase.getUser(userId) }
            }
        }

        context("다양한 사용자 ID로 조회") {
            it("요청된 userId를 정확히 UseCase에 전달") {
                val userIds = listOf(1L, 100L, 999L)

                userIds.forEach { userId ->
                    val mockUser = mockk<User>()
                    every { mockGetUserQueryUseCase.getUser(userId) } returns mockUser

                    val result = sut.getUser(userId)

                    result shouldBe ApiResponse.success(mockUser)
                    verify(exactly = 1) { mockGetUserQueryUseCase.getUser(userId) }
                    clearMocks(mockGetUserQueryUseCase)
                }
            }
        }
    }

    describe("updateUser") {
        context("PUT /api/v1/users/{userId} 요청") {
            it("파라미터들을 UpdateUserUseCase에 전달하고 ApiResponse로 반환") {
                val userId = 1L
                val request = UpdateUserRequest(
                    name = "수정된이름",
                    email = "updated@example.com"
                )
                val mockUser = mockk<User>()

                every { mockUpdateUserUseCase.execute(userId, request) } returns mockUser

                val result = sut.updateUser(userId, request)

                result shouldBe ApiResponse.success(mockUser)
                verify(exactly = 1) { mockUpdateUserUseCase.execute(userId, request) }
            }
        }

        context("일부 정보만 수정하는 요청") {
            it("null 값을 포함한 요청을 UseCase에 전달") {
                val userId = 2L
                val request = UpdateUserRequest(
                    name = "수정된이름만",
                    email = null
                )
                val mockUser = mockk<User>()

                every { mockUpdateUserUseCase.execute(userId, request) } returns mockUser

                val result = sut.updateUser(userId, request)

                result shouldBe ApiResponse.success(mockUser)
                verify(exactly = 1) { mockUpdateUserUseCase.execute(userId, request) }
            }
        }

        context("다양한 파라미터 조합") {
            it("모든 파라미터가 정확히 UseCase에 전달되는지 확인") {
                val testCases = listOf(
                    Pair(1L, UpdateUserRequest("새이름1", "new1@example.com")),
                    Pair(100L, UpdateUserRequest("새이름2", null)),
                    Pair(999L, UpdateUserRequest(null, "new3@example.com"))
                )

                testCases.forEach { (userId, request) ->
                    val mockUser = mockk<User>()
                    every { mockUpdateUserUseCase.execute(userId, request) } returns mockUser

                    val result = sut.updateUser(userId, request)

                    result shouldBe ApiResponse.success(mockUser)
                    verify(exactly = 1) { mockUpdateUserUseCase.execute(userId, request) }
                    clearMocks(mockUpdateUserUseCase)
                }
            }
        }
    }

    describe("getAllUsers") {
        context("GET /api/v1/users 요청") {
            it("GetUserQueryUseCase를 호출하고 ApiResponse로 반환") {
                val mockUsers = listOf(mockk<User>(), mockk<User>(), mockk<User>())

                every { mockGetUserQueryUseCase.getAllUsers() } returns mockUsers

                val result = sut.getAllUsers()

                result shouldBe ApiResponse.success(mockUsers)
                verify(exactly = 1) { mockGetUserQueryUseCase.getAllUsers() }
            }
        }

        context("사용자가 없는 경우") {
            it("빈 리스트를 ApiResponse로 감싸서 반환") {
                every { mockGetUserQueryUseCase.getAllUsers() } returns emptyList()

                val result = sut.getAllUsers()

                result shouldBe ApiResponse.success(emptyList<User>())
                verify(exactly = 1) { mockGetUserQueryUseCase.getAllUsers() }
            }
        }
    }

    describe("deactivateUser") {
        context("POST /api/v1/users/{userId}/deactivate 요청") {
            it("userId를 DeactivateUserUseCase에 전달하고 ApiResponse로 반환") {
                val userId = 1L
                val mockUser = mockk<User>()

                every { mockDeactivateUserUseCase.execute(userId) } returns mockUser

                val result = sut.deactivateUser(userId)

                result shouldBe ApiResponse.success(mockUser)
                verify(exactly = 1) { mockDeactivateUserUseCase.execute(userId) }
            }
        }

        context("다양한 사용자 비활성화") {
            it("각각의 userId가 정확히 UseCase에 전달되는지 확인") {
                val userIds = listOf(1L, 100L, 999L)

                userIds.forEach { userId ->
                    val mockUser = mockk<User>()
                    every { mockDeactivateUserUseCase.execute(userId) } returns mockUser

                    val result = sut.deactivateUser(userId)

                    result shouldBe ApiResponse.success(mockUser)
                    verify(exactly = 1) { mockDeactivateUserUseCase.execute(userId) }
                    clearMocks(mockDeactivateUserUseCase)
                }
            }
        }
    }

    describe("activateUser") {
        context("POST /api/v1/users/{userId}/activate 요청") {
            it("userId를 ActivateUserUseCase에 전달하고 ApiResponse로 반환") {
                val userId = 1L
                val mockUser = mockk<User>()

                every { mockActivateUserUseCase.execute(userId) } returns mockUser

                val result = sut.activateUser(userId)

                result shouldBe ApiResponse.success(mockUser)
                verify(exactly = 1) { mockActivateUserUseCase.execute(userId) }
            }
        }

        context("다양한 사용자 활성화") {
            it("각각의 userId가 정확히 UseCase에 전달되는지 확인") {
                val userIds = listOf(1L, 100L, 999L)

                userIds.forEach { userId ->
                    val mockUser = mockk<User>()
                    every { mockActivateUserUseCase.execute(userId) } returns mockUser

                    val result = sut.activateUser(userId)

                    result shouldBe ApiResponse.success(mockUser)
                    verify(exactly = 1) { mockActivateUserUseCase.execute(userId) }
                    clearMocks(mockActivateUserUseCase)
                }
            }
        }
    }

    describe("API 경로 및 메서드 검증") {
        context("모든 엔드포인트") {
            it("적절한 UseCase만 호출하고 다른 UseCase는 호출하지 않음") {
                // createUser 테스트
                val createRequest = CreateUserRequest("테스트", "test@example.com")
                every { mockCreateUserUseCase.execute(createRequest) } returns mockk()
                sut.createUser(createRequest)
                verify(exactly = 1) { mockCreateUserUseCase.execute(createRequest) }
                verify(exactly = 0) { mockGetUserQueryUseCase.getUser(any()) }
                verify(exactly = 0) { mockUpdateUserUseCase.execute(any(), any()) }

                clearMocks(mockCreateUserUseCase, mockGetUserQueryUseCase, mockUpdateUserUseCase, mockDeactivateUserUseCase, mockActivateUserUseCase)

                // getUser 테스트
                every { mockGetUserQueryUseCase.getUser(1L) } returns mockk()
                sut.getUser(1L)
                verify(exactly = 1) { mockGetUserQueryUseCase.getUser(1L) }
                verify(exactly = 0) { mockCreateUserUseCase.execute(any()) }
                verify(exactly = 0) { mockUpdateUserUseCase.execute(any(), any()) }

                clearMocks(mockCreateUserUseCase, mockGetUserQueryUseCase, mockUpdateUserUseCase, mockDeactivateUserUseCase, mockActivateUserUseCase)

                // deactivateUser 테스트
                every { mockDeactivateUserUseCase.execute(1L) } returns mockk()
                sut.deactivateUser(1L)
                verify(exactly = 1) { mockDeactivateUserUseCase.execute(1L) }
                verify(exactly = 0) { mockActivateUserUseCase.execute(any()) }
                verify(exactly = 0) { mockCreateUserUseCase.execute(any()) }
            }
        }
    }

    describe("API 응답 형식 검증") {
        context("모든 성공 응답") {
            it("일관된 ApiResponse.success 형식으로 반환") {
                val mockUser = mockk<User>()

                // 각 엔드포인트의 응답이 ApiResponse.success로 감싸져 있는지 확인
                every { mockCreateUserUseCase.execute(any()) } returns mockUser
                every { mockGetUserQueryUseCase.getUser(any()) } returns mockUser
                every { mockUpdateUserUseCase.execute(any(), any()) } returns mockUser
                every { mockDeactivateUserUseCase.execute(any()) } returns mockUser
                every { mockActivateUserUseCase.execute(any()) } returns mockUser
                every { mockGetUserQueryUseCase.getAllUsers() } returns listOf(mockUser)

                val createResult = sut.createUser(CreateUserRequest("테스트", "test@example.com"))
                val getUserResult = sut.getUser(1L)
                val updateResult = sut.updateUser(1L, UpdateUserRequest("새이름", "new@example.com"))
                val deactivateResult = sut.deactivateUser(1L)
                val activateResult = sut.activateUser(1L)
                val getAllResult = sut.getAllUsers()

                // 모든 결과가 ApiResponse.success 형태인지 확인
                createResult::class shouldBe ApiResponse.success(mockUser)::class
                getUserResult::class shouldBe ApiResponse.success(mockUser)::class
                updateResult::class shouldBe ApiResponse.success(mockUser)::class
                deactivateResult::class shouldBe ApiResponse.success(mockUser)::class
                activateResult::class shouldBe ApiResponse.success(mockUser)::class
                getAllResult::class shouldBe ApiResponse.success(listOf(mockUser))::class
            }
        }
    }
})