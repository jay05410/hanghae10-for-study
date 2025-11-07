package io.hhplus.ecommerce.user.usecase

import io.hhplus.ecommerce.user.application.UserService
import io.hhplus.ecommerce.user.domain.entity.User
import io.hhplus.ecommerce.user.dto.UpdateUserRequest
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * UpdateUserUseCase 단위 테스트
 *
 * 책임: 사용자 정보 수정 요청 처리 및 UserService 호출 검증
 * - UpdateUserRequest를 UserService 파라미터로 변환
 * - updatedBy 파라미터가 userId와 동일하게 설정되는지 검증
 * - UserService와의 올바른 상호작용 검증
 *
 * 검증 목표:
 * 1. UpdateUserRequest가 올바른 UserService 파라미터로 변환되는가?
 * 2. updatedBy가 userId와 동일하게 설정되는가?
 * 3. name과 email이 요청에서 정확히 전달되는가?
 * 4. UserService의 updateUser 메서드가 정확한 파라미터로 호출되는가?
 * 5. UserService의 결과가 그대로 반환되는가?
 */
class UpdateUserUseCaseTest : DescribeSpec({
    val mockUserService = mockk<UserService>()
    val sut = UpdateUserUseCase(mockUserService)

    beforeEach {
        clearMocks(mockUserService)
    }

    describe("execute") {
        context("완전한 정보 수정 요청") {
            it("UserService.updateUser를 올바른 파라미터로 호출하고 결과를 반환") {
                val userId = 1L
                val request = UpdateUserRequest(
                    name = "수정된이름",
                    email = "updated@example.com"
                )
                val mockUser = mockk<User>()

                every {
                    mockUserService.updateUser(
                        userId = userId,
                        name = "수정된이름",
                        email = "updated@example.com",
                        updatedBy = userId
                    )
                } returns mockUser

                val result = sut.execute(userId, request)

                result shouldBe mockUser
                verify(exactly = 1) {
                    mockUserService.updateUser(
                        userId = userId,
                        name = "수정된이름",
                        email = "updated@example.com",
                        updatedBy = userId
                    )
                }
            }
        }

        context("이름만 수정하는 요청") {
            it("name은 전달하고 email은 null로 UserService에 전달") {
                val userId = 2L
                val request = UpdateUserRequest(
                    name = "새로운이름",
                    email = null
                )
                val mockUser = mockk<User>()

                every {
                    mockUserService.updateUser(
                        userId = userId,
                        name = "새로운이름",
                        email = null,
                        updatedBy = userId
                    )
                } returns mockUser

                val result = sut.execute(userId, request)

                result shouldBe mockUser
                verify(exactly = 1) {
                    mockUserService.updateUser(
                        userId = userId,
                        name = "새로운이름",
                        email = null,
                        updatedBy = userId
                    )
                }
            }
        }

        context("이메일만 수정하는 요청") {
            it("email은 전달하고 name은 null로 UserService에 전달") {
                val userId = 3L
                val request = UpdateUserRequest(
                    name = null,
                    email = "newemail@example.com"
                )
                val mockUser = mockk<User>()

                every {
                    mockUserService.updateUser(
                        userId = userId,
                        name = null,
                        email = "newemail@example.com",
                        updatedBy = userId
                    )
                } returns mockUser

                val result = sut.execute(userId, request)

                result shouldBe mockUser
                verify(exactly = 1) {
                    mockUserService.updateUser(
                        userId = userId,
                        name = null,
                        email = "newemail@example.com",
                        updatedBy = userId
                    )
                }
            }
        }

        context("모든 필드가 null인 요청") {
            it("모든 값을 null로 UserService에 전달") {
                val userId = 4L
                val request = UpdateUserRequest(
                    name = null,
                    email = null
                )
                val mockUser = mockk<User>()

                every {
                    mockUserService.updateUser(
                        userId = userId,
                        name = null,
                        email = null,
                        updatedBy = userId
                    )
                } returns mockUser

                val result = sut.execute(userId, request)

                result shouldBe mockUser
                verify(exactly = 1) {
                    mockUserService.updateUser(
                        userId = userId,
                        name = null,
                        email = null,
                        updatedBy = userId
                    )
                }
            }
        }

        context("다양한 userId로 수정 요청") {
            it("각 userId가 정확히 UserService에 전달되고 updatedBy도 동일하게 설정") {
                val testCases = listOf(
                    Pair(1L, UpdateUserRequest("사용자1", "user1@example.com")),
                    Pair(100L, UpdateUserRequest("사용자100", "user100@example.com")),
                    Pair(999L, UpdateUserRequest("사용자999", null))
                )

                testCases.forEach { (userId, request) ->
                    val mockUser = mockk<User>()
                    every {
                        mockUserService.updateUser(
                            userId = userId,
                            name = request.name,
                            email = request.email,
                            updatedBy = userId
                        )
                    } returns mockUser

                    val result = sut.execute(userId, request)

                    result shouldBe mockUser
                    verify(exactly = 1) {
                        mockUserService.updateUser(
                            userId = userId,
                            name = request.name,
                            email = request.email,
                            updatedBy = userId
                        )
                    }
                    clearMocks(mockUserService)
                }
            }
        }

        context("updatedBy 설정 검증") {
            it("updatedBy가 항상 userId와 동일하게 설정") {
                val userIds = listOf(1L, 50L, 999L)

                userIds.forEach { userId ->
                    val request = UpdateUserRequest("테스트", "test@example.com")
                    val mockUser = mockk<User>()

                    every {
                        mockUserService.updateUser(
                            userId = userId,
                            name = any(),
                            email = any(),
                            updatedBy = userId  // userId와 동일한지 확인
                        )
                    } returns mockUser

                    sut.execute(userId, request)

                    verify(exactly = 1) {
                        mockUserService.updateUser(
                            userId = userId,
                            name = any(),
                            email = any(),
                            updatedBy = userId
                        )
                    }
                    clearMocks(mockUserService)
                }
            }
        }
    }

    describe("파라미터 변환 검증") {
        context("요청 데이터 변환") {
            it("UpdateUserRequest의 모든 필드가 올바르게 UserService 파라미터로 매핑") {
                val userId = 123L
                val request = UpdateUserRequest(
                    name = "완전한수정테스트",
                    email = "complete.update@test.example.com"
                )
                val mockUser = mockk<User>()

                every { mockUserService.updateUser(any(), any(), any(), any()) } returns mockUser

                sut.execute(userId, request)

                verify(exactly = 1) {
                    mockUserService.updateUser(
                        userId = 123L,                                      // 파라미터 userId
                        name = "완전한수정테스트",                              // request.name
                        email = "complete.update@test.example.com",        // request.email
                        updatedBy = 123L                                    // userId와 동일
                    )
                }
            }
        }

        context("null 값 처리") {
            it("UpdateUserRequest의 null 값들이 그대로 UserService에 전달") {
                val userId = 456L
                val request = UpdateUserRequest(
                    name = null,
                    email = null
                )
                val mockUser = mockk<User>()

                every { mockUserService.updateUser(any(), any(), any(), any()) } returns mockUser

                sut.execute(userId, request)

                verify(exactly = 1) {
                    mockUserService.updateUser(
                        userId = 456L,     // 파라미터 userId
                        name = null,       // request.name (null)
                        email = null,      // request.email (null)
                        updatedBy = 456L   // userId와 동일
                    )
                }
            }
        }
    }

    describe("비즈니스 로직 검증") {
        context("UseCase의 책임") {
            it("요청 변환 외에 추가적인 비즈니스 로직이 없음을 확인") {
                val userId = 789L
                val request = UpdateUserRequest("테스트", "test@example.com")
                val mockUser = mockk<User>()

                every { mockUserService.updateUser(any(), any(), any(), any()) } returns mockUser

                val result = sut.execute(userId, request)

                // 결과가 UserService에서 온 것과 동일한지 확인
                result shouldBe mockUser

                // UserService가 정확히 한 번만 호출되었는지 확인
                verify(exactly = 1) { mockUserService.updateUser(any(), any(), any(), any()) }
            }
        }
    }
})