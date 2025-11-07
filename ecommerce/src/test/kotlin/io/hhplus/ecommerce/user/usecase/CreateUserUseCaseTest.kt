package io.hhplus.ecommerce.user.usecase

import io.hhplus.ecommerce.user.application.UserService
import io.hhplus.ecommerce.user.domain.entity.User
import io.hhplus.ecommerce.user.domain.constant.LoginType
import io.hhplus.ecommerce.user.dto.CreateUserRequest
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * CreateUserUseCase 단위 테스트
 *
 * 책임: 사용자 생성 요청 처리 및 UserService 호출 검증
 * - CreateUserRequest를 UserService 파라미터로 변환
 * - 고정된 기본값들의 적절한 설정 검증
 * - UserService와의 올바른 상호작용 검증
 *
 * 검증 목표:
 * 1. CreateUserRequest가 올바른 UserService 파라미터로 변환되는가?
 * 2. LoginType이 LOCAL로 고정되어 전달되는가?
 * 3. 기본값들(phone, providerId, createdBy)이 올바르게 설정되는가?
 * 4. UserService의 createUser 메서드가 정확한 파라미터로 호출되는가?
 * 5. UserService의 결과가 그대로 반환되는가?
 */
class CreateUserUseCaseTest : DescribeSpec({
    val mockUserService = mockk<UserService>()
    val sut = CreateUserUseCase(mockUserService)

    beforeEach {
        clearMocks(mockUserService)
    }

    describe("execute") {
        context("올바른 CreateUserRequest가 주어졌을 때") {
            it("UserService.createUser를 적절한 파라미터로 호출하고 결과를 반환") {
                val request = CreateUserRequest(
                    name = "테스트사용자",
                    email = "test@example.com"
                )
                val mockUser = mockk<User>()

                every {
                    mockUserService.createUser(
                        loginType = LoginType.LOCAL,
                        loginId = "test@example.com",
                        password = null,
                        email = "test@example.com",
                        name = "테스트사용자",
                        phone = "010-0000-0000",
                        providerId = null,
                        createdBy = 1L
                    )
                } returns mockUser

                val result = sut.execute(request)

                result shouldBe mockUser
                verify(exactly = 1) {
                    mockUserService.createUser(
                        loginType = LoginType.LOCAL,
                        loginId = "test@example.com",
                        password = null,
                        email = "test@example.com",
                        name = "테스트사용자",
                        phone = "010-0000-0000",
                        providerId = null,
                        createdBy = 1L
                    )
                }
            }
        }

        context("다양한 사용자 정보로 요청") {
            it("각 요청의 name과 email이 정확히 UserService에 전달") {
                val testCases = listOf(
                    CreateUserRequest("사용자1", "user1@example.com"),
                    CreateUserRequest("김철수", "kimchulsoo@example.com"),
                    CreateUserRequest("홍길동", "hong@test.com")
                )

                testCases.forEach { request ->
                    val mockUser = mockk<User>()
                    every {
                        mockUserService.createUser(
                            loginType = LoginType.LOCAL,
                            loginId = request.email,
                            password = null,
                            email = request.email,
                            name = request.name,
                            phone = "010-0000-0000",
                            providerId = null,
                            createdBy = 1L
                        )
                    } returns mockUser

                    val result = sut.execute(request)

                    result shouldBe mockUser
                    verify(exactly = 1) {
                        mockUserService.createUser(
                            loginType = LoginType.LOCAL,
                            loginId = request.email,
                            password = null,
                            email = request.email,
                            name = request.name,
                            phone = "010-0000-0000",
                            providerId = null,
                            createdBy = 1L
                        )
                    }
                    clearMocks(mockUserService)
                }
            }
        }

        context("기본값 설정 검증") {
            it("LoginType이 LOCAL로 고정되어 전달") {
                val request = CreateUserRequest("테스트", "test@example.com")
                val mockUser = mockk<User>()

                every {
                    mockUserService.createUser(
                        loginType = LoginType.LOCAL,
                        loginId = any(),
                        password = any(),
                        email = any(),
                        name = any(),
                        phone = any(),
                        providerId = any(),
                        createdBy = any()
                    )
                } returns mockUser

                sut.execute(request)

                verify(exactly = 1) {
                    mockUserService.createUser(
                        loginType = LoginType.LOCAL,
                        loginId = any(),
                        password = any(),
                        email = any(),
                        name = any(),
                        phone = any(),
                        providerId = any(),
                        createdBy = any()
                    )
                }
            }
        }

        context("loginId와 email 매핑 검증") {
            it("loginId가 email과 동일하게 설정") {
                val request = CreateUserRequest("테스트", "unique@example.com")
                val mockUser = mockk<User>()

                every {
                    mockUserService.createUser(
                        loginType = any(),
                        loginId = "unique@example.com",
                        password = any(),
                        email = "unique@example.com",
                        name = any(),
                        phone = any(),
                        providerId = any(),
                        createdBy = any()
                    )
                } returns mockUser

                sut.execute(request)

                verify(exactly = 1) {
                    mockUserService.createUser(
                        loginType = any(),
                        loginId = "unique@example.com",
                        password = any(),
                        email = "unique@example.com",
                        name = any(),
                        phone = any(),
                        providerId = any(),
                        createdBy = any()
                    )
                }
            }
        }

        context("null 값 설정 검증") {
            it("password와 providerId가 null로 설정") {
                val request = CreateUserRequest("테스트", "test@example.com")
                val mockUser = mockk<User>()

                every {
                    mockUserService.createUser(
                        loginType = any(),
                        loginId = any(),
                        password = null,
                        email = any(),
                        name = any(),
                        phone = any(),
                        providerId = null,
                        createdBy = any()
                    )
                } returns mockUser

                sut.execute(request)

                verify(exactly = 1) {
                    mockUserService.createUser(
                        loginType = any(),
                        loginId = any(),
                        password = null,
                        email = any(),
                        name = any(),
                        phone = any(),
                        providerId = null,
                        createdBy = any()
                    )
                }
            }
        }

        context("고정값 설정 검증") {
            it("phone이 '010-0000-0000'으로, createdBy가 1L로 고정") {
                val request = CreateUserRequest("테스트", "test@example.com")
                val mockUser = mockk<User>()

                every {
                    mockUserService.createUser(
                        loginType = any(),
                        loginId = any(),
                        password = any(),
                        email = any(),
                        name = any(),
                        phone = "010-0000-0000",
                        providerId = any(),
                        createdBy = 1L
                    )
                } returns mockUser

                sut.execute(request)

                verify(exactly = 1) {
                    mockUserService.createUser(
                        loginType = any(),
                        loginId = any(),
                        password = any(),
                        email = any(),
                        name = any(),
                        phone = "010-0000-0000",
                        providerId = any(),
                        createdBy = 1L
                    )
                }
            }
        }
    }

    describe("파라미터 변환 검증") {
        context("요청 데이터 변환") {
            it("CreateUserRequest의 모든 필드가 올바르게 UserService 파라미터로 매핑") {
                val request = CreateUserRequest(
                    name = "완전한테스트사용자",
                    email = "complete@test.example.com"
                )
                val mockUser = mockk<User>()

                every { mockUserService.createUser(any(), any(), any(), any(), any(), any(), any(), any()) } returns mockUser

                sut.execute(request)

                verify(exactly = 1) {
                    mockUserService.createUser(
                        loginType = LoginType.LOCAL,           // 고정값
                        loginId = "complete@test.example.com", // request.email
                        password = null,                       // 고정값
                        email = "complete@test.example.com",   // request.email
                        name = "완전한테스트사용자",                // request.name
                        phone = "010-0000-0000",              // 고정값
                        providerId = null,                     // 고정값
                        createdBy = 1L                         // 고정값
                    )
                }
            }
        }
    }
})