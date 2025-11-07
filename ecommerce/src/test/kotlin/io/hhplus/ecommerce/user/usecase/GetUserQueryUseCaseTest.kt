package io.hhplus.ecommerce.user.usecase

import io.hhplus.ecommerce.user.application.UserService
import io.hhplus.ecommerce.user.domain.entity.User
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * GetUserQueryUseCase 단위 테스트
 *
 * 책임: 사용자 조회 요청을 UserService로 위임하는 역할 검증
 * - 단일 사용자 조회 기능의 UserService 호출 검증
 * - 전체 사용자 조회 기능의 UserService 호출 검증
 * - 파라미터 전달 및 결과 반환의 정확성 검증
 *
 * 검증 목표:
 * 1. getUser 메서드가 올바른 userId로 UserService를 호출하는가?
 * 2. getAllUsers 메서드가 UserService의 getAllUsers를 호출하는가?
 * 3. UserService의 결과가 그대로 반환되는가?
 * 4. 다양한 입력값에 대한 정확한 처리가 이루어지는가?
 * 5. null 결과도 올바르게 처리되는가?
 */
class GetUserQueryUseCaseTest : DescribeSpec({
    val mockUserService = mockk<UserService>()
    val sut = GetUserQueryUseCase(mockUserService)

    beforeEach {
        clearMocks(mockUserService)
    }

    describe("getUser") {
        context("존재하는 사용자 ID로 조회") {
            it("UserService.getUser를 호출하고 결과를 반환") {
                val userId = 1L
                val mockUser = mockk<User>()

                every { mockUserService.getUser(userId) } returns mockUser

                val result = sut.getUser(userId)

                result shouldBe mockUser
                verify(exactly = 1) { mockUserService.getUser(userId) }
            }
        }

        context("존재하지 않는 사용자 ID로 조회") {
            it("UserService.getUser를 호출하고 null을 반환") {
                val userId = 999L

                every { mockUserService.getUser(userId) } returns null

                val result = sut.getUser(userId)

                result shouldBe null
                verify(exactly = 1) { mockUserService.getUser(userId) }
            }
        }

        context("다양한 사용자 ID로 조회") {
            it("각 userId가 정확히 UserService에 전달") {
                val userIds = listOf(1L, 100L, 999L, Long.MAX_VALUE)

                userIds.forEach { userId ->
                    val mockUser = mockk<User>()
                    every { mockUserService.getUser(userId) } returns mockUser

                    val result = sut.getUser(userId)

                    result shouldBe mockUser
                    verify(exactly = 1) { mockUserService.getUser(userId) }
                    clearMocks(mockUserService)
                }
            }
        }

        context("경계값 테스트") {
            it("최소값과 최대값 userId로 조회") {
                val boundaryUserIds = listOf(1L, Long.MAX_VALUE)

                boundaryUserIds.forEach { userId ->
                    val mockUser = mockk<User>()
                    every { mockUserService.getUser(userId) } returns mockUser

                    val result = sut.getUser(userId)

                    result shouldBe mockUser
                    verify(exactly = 1) { mockUserService.getUser(userId) }
                    clearMocks(mockUserService)
                }
            }
        }
    }

    describe("getAllUsers") {
        context("활성 사용자들이 존재할 때") {
            it("UserService.getAllUsers를 호출하고 결과를 반환") {
                val mockUsers = listOf(mockk<User>(), mockk<User>(), mockk<User>())

                every { mockUserService.getAllUsers() } returns mockUsers

                val result = sut.getAllUsers()

                result shouldBe mockUsers
                verify(exactly = 1) { mockUserService.getAllUsers() }
            }
        }

        context("활성 사용자가 없을 때") {
            it("UserService.getAllUsers를 호출하고 빈 리스트를 반환") {
                every { mockUserService.getAllUsers() } returns emptyList()

                val result = sut.getAllUsers()

                result shouldBe emptyList()
                verify(exactly = 1) { mockUserService.getAllUsers() }
            }
        }

        context("단일 사용자만 있을 때") {
            it("UserService.getAllUsers를 호출하고 단일 요소 리스트를 반환") {
                val mockUsers = listOf(mockk<User>())

                every { mockUserService.getAllUsers() } returns mockUsers

                val result = sut.getAllUsers()

                result shouldBe mockUsers
                verify(exactly = 1) { mockUserService.getAllUsers() }
            }
        }

        context("대량의 사용자가 있을 때") {
            it("UserService.getAllUsers를 호출하고 모든 사용자를 반환") {
                val mockUsers = (1..100).map { mockk<User>() }

                every { mockUserService.getAllUsers() } returns mockUsers

                val result = sut.getAllUsers()

                result shouldBe mockUsers
                verify(exactly = 1) { mockUserService.getAllUsers() }
            }
        }
    }

    describe("메서드별 독립성 검증") {
        context("getUser와 getAllUsers 호출") {
            it("각 메서드가 독립적으로 UserService의 해당 메서드만 호출") {
                val userId = 1L
                val mockUser = mockk<User>()
                val mockUsers = listOf(mockUser)

                // getUser 호출 시 getAllUsers가 호출되지 않음을 확인
                every { mockUserService.getUser(userId) } returns mockUser
                every { mockUserService.getAllUsers() } returns mockUsers

                sut.getUser(userId)

                verify(exactly = 1) { mockUserService.getUser(userId) }
                verify(exactly = 0) { mockUserService.getAllUsers() }

                clearMocks(mockUserService)

                // getAllUsers 호출 시 getUser가 호출되지 않음을 확인
                every { mockUserService.getAllUsers() } returns mockUsers
                every { mockUserService.getUser(any()) } returns mockUser

                sut.getAllUsers()

                verify(exactly = 1) { mockUserService.getAllUsers() }
                verify(exactly = 0) { mockUserService.getUser(any()) }
            }
        }
    }

    describe("위임 패턴 검증") {
        context("UseCase의 역할") {
            it("비즈니스 로직 없이 UserService로 단순 위임") {
                val userId = 1L
                val mockUser = mockk<User>()

                every { mockUserService.getUser(userId) } returns mockUser

                val result = sut.getUser(userId)

                // 결과가 UserService에서 온 것과 동일한지 확인
                result shouldBe mockUser

                // UserService가 정확히 한 번만 호출되었는지 확인
                verify(exactly = 1) { mockUserService.getUser(userId) }

                // 다른 메서드는 호출되지 않았는지 확인
                verify(exactly = 0) { mockUserService.getAllUsers() }
            }
        }

        context("결과 변환 없음 확인") {
            it("UserService 결과를 변환 없이 그대로 반환") {
                val mockUsers = listOf(mockk<User>(), mockk<User>())

                every { mockUserService.getAllUsers() } returns mockUsers

                val result = sut.getAllUsers()

                // 참조가 동일한지 확인 (새로운 객체를 만들지 않았는지)
                result shouldBe mockUsers
                verify(exactly = 1) { mockUserService.getAllUsers() }
            }
        }
    }
})