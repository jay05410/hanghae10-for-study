package io.hhplus.ecommerce.user.usecase

import io.hhplus.ecommerce.user.application.UserService
import io.hhplus.ecommerce.user.domain.entity.User
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * ActivateUserUseCase 단위 테스트
 *
 * 책임: 사용자 활성화 요청 처리 및 UserService 호출 검증
 * - userId를 UserService의 activateUser 메서드 파라미터로 전달
 * - activatedBy 파라미터가 userId와 동일하게 설정되는지 검증
 * - UserService와의 올바른 상호작용 검증
 *
 * 검증 목표:
 * 1. userId가 올바르게 UserService의 activateUser 메서드에 전달되는가?
 * 2. activatedBy가 userId와 동일하게 설정되는가?
 * 3. UserService의 결과가 그대로 반환되는가?
 * 4. 다양한 userId에 대한 정확한 처리가 이루어지는가?
 * 5. UseCase가 단순 위임 역할만 수행하는가?
 */
class ActivateUserUseCaseTest : DescribeSpec({
    val mockUserService = mockk<UserService>()
    val sut = ActivateUserUseCase(mockUserService)

    beforeEach {
        clearMocks(mockUserService)
    }

    describe("execute") {
        context("사용자 활성화 요청") {
            it("UserService.activateUser를 올바른 파라미터로 호출하고 결과를 반환") {
                val userId = 1L
                val mockUser = mockk<User>()

                every {
                    mockUserService.activateUser(
                        userId = userId,
                        activatedBy = userId
                    )
                } returns mockUser

                val result = sut.execute(userId)

                result shouldBe mockUser
                verify(exactly = 1) {
                    mockUserService.activateUser(
                        userId = userId,
                        activatedBy = userId
                    )
                }
            }
        }

        context("다양한 userId로 활성화 요청") {
            it("각 userId가 정확히 UserService에 전달되고 activatedBy도 동일하게 설정") {
                val userIds = listOf(1L, 100L, 999L, Long.MAX_VALUE)

                userIds.forEach { userId ->
                    val mockUser = mockk<User>()
                    every {
                        mockUserService.activateUser(
                            userId = userId,
                            activatedBy = userId
                        )
                    } returns mockUser

                    val result = sut.execute(userId)

                    result shouldBe mockUser
                    verify(exactly = 1) {
                        mockUserService.activateUser(
                            userId = userId,
                            activatedBy = userId
                        )
                    }
                    clearMocks(mockUserService)
                }
            }
        }

        context("activatedBy 설정 검증") {
            it("activatedBy가 항상 userId와 동일하게 설정") {
                val testUserIds = listOf(1L, 50L, 999L)

                testUserIds.forEach { userId ->
                    val mockUser = mockk<User>()

                    every {
                        mockUserService.activateUser(
                            userId = userId,
                            activatedBy = userId  // userId와 동일한지 확인
                        )
                    } returns mockUser

                    sut.execute(userId)

                    verify(exactly = 1) {
                        mockUserService.activateUser(
                            userId = userId,
                            activatedBy = userId
                        )
                    }
                    clearMocks(mockUserService)
                }
            }
        }

        context("경계값 테스트") {
            it("최소값과 최대값 userId로 활성화") {
                val boundaryUserIds = listOf(1L, Long.MAX_VALUE)

                boundaryUserIds.forEach { userId ->
                    val mockUser = mockk<User>()
                    every {
                        mockUserService.activateUser(
                            userId = userId,
                            activatedBy = userId
                        )
                    } returns mockUser

                    val result = sut.execute(userId)

                    result shouldBe mockUser
                    verify(exactly = 1) {
                        mockUserService.activateUser(
                            userId = userId,
                            activatedBy = userId
                        )
                    }
                    clearMocks(mockUserService)
                }
            }
        }
    }

    describe("위임 패턴 검증") {
        context("UseCase의 역할") {
            it("추가적인 비즈니스 로직 없이 UserService로 단순 위임") {
                val userId = 123L
                val mockUser = mockk<User>()

                every { mockUserService.activateUser(any(), any()) } returns mockUser

                val result = sut.execute(userId)

                // 결과가 UserService에서 온 것과 동일한지 확인
                result shouldBe mockUser

                // UserService가 정확히 한 번만 호출되었는지 확인
                verify(exactly = 1) { mockUserService.activateUser(userId, userId) }
            }
        }

        context("파라미터 변환") {
            it("userId를 userId와 activatedBy 두 파라미터로 전달") {
                val userId = 456L
                val mockUser = mockk<User>()

                every { mockUserService.activateUser(any(), any()) } returns mockUser

                sut.execute(userId)

                verify(exactly = 1) {
                    mockUserService.activateUser(
                        userId = 456L,        // 첫 번째 파라미터: userId
                        activatedBy = 456L    // 두 번째 파라미터: activatedBy (동일한 값)
                    )
                }
            }
        }
    }

    describe("메서드 호출 독립성") {
        context("다른 UserService 메서드 호출 확인") {
            it("activateUser만 호출하고 다른 메서드는 호출하지 않음") {
                val userId = 789L
                val mockUser = mockk<User>()

                every { mockUserService.activateUser(any(), any()) } returns mockUser
                every { mockUserService.deactivateUser(any(), any()) } returns mockUser
                every { mockUserService.getUser(any()) } returns mockUser

                sut.execute(userId)

                verify(exactly = 1) { mockUserService.activateUser(userId, userId) }
                verify(exactly = 0) { mockUserService.deactivateUser(any(), any()) }
                verify(exactly = 0) { mockUserService.getUser(any()) }
                verify(exactly = 0) { mockUserService.createUser(any(), any(), any(), any(), any(), any(), any(), any()) }
                verify(exactly = 0) { mockUserService.updateUser(any(), any(), any(), any()) }
                verify(exactly = 0) { mockUserService.getAllUsers() }
            }
        }
    }

    describe("결과 처리 검증") {
        context("UserService 결과 반환") {
            it("UserService의 결과를 변환 없이 그대로 반환") {
                val userId = 999L
                val mockUser = mockk<User>()

                every { mockUserService.activateUser(userId, userId) } returns mockUser

                val result = sut.execute(userId)

                // 참조가 동일한지 확인 (새로운 객체를 만들지 않았는지)
                result shouldBe mockUser
                verify(exactly = 1) { mockUserService.activateUser(userId, userId) }
            }
        }
    }
})