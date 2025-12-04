package io.hhplus.ecommerce.unit.user.domain.service

import io.hhplus.ecommerce.user.domain.entity.User
import io.hhplus.ecommerce.user.domain.repository.UserRepository
import io.hhplus.ecommerce.user.domain.constant.LoginType
import io.hhplus.ecommerce.user.exception.UserException
import io.hhplus.ecommerce.user.domain.service.UserDomainService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.*

/**
 * UserDomainService 단위 테스트
 *
 * 책임: 사용자 도메인 비즈니스 로직 처리 검증
 * - 사용자 생성/조회/수정/활성화/비활성화 로직 검증
 * - 이메일 중복 검증 로직 검증
 * - UserRepository와의 올바른 상호작용 검증
 *
 * 검증 목표:
 * 1. 각 비즈니스 메서드가 적절한 Repository 메서드를 호출하는가?
 * 2. 이메일 중복 검증이 올바르게 동작하는가?
 * 3. 존재하지 않는 사용자 접근 시 적절한 예외가 발생하는가?
 * 4. 사용자 생성/수정 시 도메인 객체가 올바르게 생성되는가?
 * 5. 각 메서드의 반환값이 올바른가?
 */
class UserDomainServiceTest : DescribeSpec({
    val mockUserRepository = mockk<UserRepository>()
    val sut = UserDomainService(mockUserRepository)

    beforeEach {
        clearMocks(mockUserRepository)
    }

    describe("createUser") {
        context("새로운 사용자 생성 요청") {
            it("사용자를 생성하고 저장") {
                val email = "test@example.com"
                val name = "테스트사용자"
                val phone = "010-1234-5678"
                val mockUser = mockk<User>()

                every { mockUserRepository.save(any()) } returns mockUser

                val result = sut.createUser(
                    loginType = LoginType.LOCAL,
                    loginId = "testId",
                    password = "password",
                    email = email,
                    name = name,
                    phone = phone,
                    providerId = null
                )

                result shouldBe mockUser
                verify(exactly = 1) { mockUserRepository.save(any()) }
            }
        }

        context("다양한 LoginType으로 사용자 생성") {
            it("각 LoginType에 맞게 사용자를 생성") {
                val loginTypes = listOf(LoginType.LOCAL, LoginType.KAKAO)

                loginTypes.forEach { loginType ->
                    val email = "test_${loginType.name.lowercase()}@example.com"
                    val mockUser = mockk<User>()

                    every { mockUserRepository.save(any()) } returns mockUser

                    val result = sut.createUser(
                        loginType = loginType,
                        loginId = "testId",
                        password = if (loginType == LoginType.LOCAL) "password" else null,
                        email = email,
                        name = "테스트사용자",
                        phone = "010-1234-5678",
                        providerId = if (loginType == LoginType.KAKAO) "provider123" else null
                    )

                    result shouldBe mockUser
                    verify(exactly = 1) { mockUserRepository.save(any()) }
                    clearMocks(mockUserRepository)
                }
            }
        }
    }

    describe("validateNoDuplicateEmail") {
        context("중복되지 않은 이메일") {
            it("예외 없이 통과") {
                val email = "new@example.com"

                every { mockUserRepository.findByEmail(email) } returns null

                sut.validateNoDuplicateEmail(email)

                verify(exactly = 1) { mockUserRepository.findByEmail(email) }
            }
        }

        context("중복된 이메일") {
            it("EmailAlreadyExists 예외를 발생") {
                val email = "duplicate@example.com"
                val existingUser = mockk<User>()

                every { mockUserRepository.findByEmail(email) } returns existingUser

                shouldThrow<UserException.EmailAlreadyExists> {
                    sut.validateNoDuplicateEmail(email)
                }

                verify(exactly = 1) { mockUserRepository.findByEmail(email) }
            }
        }
    }

    describe("getUser") {
        context("존재하는 사용자 ID로 조회") {
            it("UserRepository에서 사용자를 조회하고 반환") {
                val userId = 1L
                val mockUser = mockk<User>()

                every { mockUserRepository.findById(userId) } returns mockUser

                val result = sut.getUser(userId)

                result shouldBe mockUser
                verify(exactly = 1) { mockUserRepository.findById(userId) }
            }
        }

        context("존재하지 않는 사용자 ID로 조회") {
            it("null을 반환") {
                val userId = 999L

                every { mockUserRepository.findById(userId) } returns null

                val result = sut.getUser(userId)

                result shouldBe null
                verify(exactly = 1) { mockUserRepository.findById(userId) }
            }
        }
    }

    describe("getUserOrThrow") {
        context("존재하는 사용자 ID로 조회") {
            it("사용자를 반환") {
                val userId = 1L
                val mockUser = mockk<User>()

                every { mockUserRepository.findById(userId) } returns mockUser

                val result = sut.getUserOrThrow(userId)

                result shouldBe mockUser
                verify(exactly = 1) { mockUserRepository.findById(userId) }
            }
        }

        context("존재하지 않는 사용자 ID로 조회") {
            it("UserNotFound 예외를 발생") {
                val userId = 999L

                every { mockUserRepository.findById(userId) } returns null

                shouldThrow<UserException.UserNotFound> {
                    sut.getUserOrThrow(userId)
                }

                verify(exactly = 1) { mockUserRepository.findById(userId) }
            }
        }
    }

    describe("getUserByEmail") {
        context("존재하는 이메일로 조회") {
            it("UserRepository에서 사용자를 조회하고 반환") {
                val email = "test@example.com"
                val mockUser = mockk<User>()

                every { mockUserRepository.findByEmail(email) } returns mockUser

                val result = sut.getUserByEmail(email)

                result shouldBe mockUser
                verify(exactly = 1) { mockUserRepository.findByEmail(email) }
            }
        }

        context("존재하지 않는 이메일로 조회") {
            it("null을 반환") {
                val email = "notfound@example.com"

                every { mockUserRepository.findByEmail(email) } returns null

                val result = sut.getUserByEmail(email)

                result shouldBe null
                verify(exactly = 1) { mockUserRepository.findByEmail(email) }
            }
        }
    }

    describe("validateEmailChangeAllowed") {
        context("이메일을 변경하지 않는 경우") {
            it("검증을 건너뛰고 통과") {
                val userId = 1L
                val currentEmail = "same@example.com"

                sut.validateEmailChangeAllowed(userId, currentEmail, currentEmail)

                verify(exactly = 0) { mockUserRepository.findByEmail(any()) }
            }
        }

        context("새 이메일이 사용 중이 아닌 경우") {
            it("예외 없이 통과") {
                val userId = 1L
                val newEmail = "new@example.com"
                val currentEmail = "old@example.com"

                every { mockUserRepository.findByEmail(newEmail) } returns null

                sut.validateEmailChangeAllowed(userId, newEmail, currentEmail)

                verify(exactly = 1) { mockUserRepository.findByEmail(newEmail) }
            }
        }

        context("새 이메일이 다른 사용자가 사용 중인 경우") {
            it("EmailAlreadyExists 예외를 발생") {
                val userId = 1L
                val newEmail = "duplicate@example.com"
                val currentEmail = "old@example.com"
                val existingUser = mockk<User> {
                    every { id } returns 2L
                }

                every { mockUserRepository.findByEmail(newEmail) } returns existingUser

                shouldThrow<UserException.EmailAlreadyExists> {
                    sut.validateEmailChangeAllowed(userId, newEmail, currentEmail)
                }

                verify(exactly = 1) { mockUserRepository.findByEmail(newEmail) }
            }
        }

        context("동일한 사용자가 이메일을 변경하려는 경우") {
            it("예외 없이 통과") {
                val userId = 1L
                val newEmail = "new@example.com"
                val currentEmail = "old@example.com"
                val sameUser = mockk<User> {
                    every { id } returns userId
                }

                every { mockUserRepository.findByEmail(newEmail) } returns sameUser

                sut.validateEmailChangeAllowed(userId, newEmail, currentEmail)

                verify(exactly = 1) { mockUserRepository.findByEmail(newEmail) }
            }
        }
    }

    describe("updateUser") {
        context("사용자 정보 수정") {
            it("사용자 정보를 수정하고 저장") {
                val mockUser = mockk<User> {
                    every { update(any(), any()) } just runs
                }

                every { mockUserRepository.save(mockUser) } returns mockUser

                val result = sut.updateUser(mockUser, "새이름", "new@example.com")

                result shouldBe mockUser
                verify(exactly = 1) { mockUser.update("새이름", "new@example.com") }
                verify(exactly = 1) { mockUserRepository.save(mockUser) }
            }
        }
    }

    describe("deactivateUser") {
        context("사용자 비활성화") {
            it("사용자를 비활성화하고 저장") {
                val mockUser = mockk<User> {
                    every { deactivate() } just runs
                }

                every { mockUserRepository.save(mockUser) } returns mockUser

                val result = sut.deactivateUser(mockUser)

                result shouldBe mockUser
                verify(exactly = 1) { mockUser.deactivate() }
                verify(exactly = 1) { mockUserRepository.save(mockUser) }
            }
        }
    }

    describe("activateUser") {
        context("사용자 활성화") {
            it("사용자를 활성화하고 저장") {
                val mockUser = mockk<User> {
                    every { activate() } just runs
                }

                every { mockUserRepository.save(mockUser) } returns mockUser

                val result = sut.activateUser(mockUser)

                result shouldBe mockUser
                verify(exactly = 1) { mockUser.activate() }
                verify(exactly = 1) { mockUserRepository.save(mockUser) }
            }
        }
    }

    describe("getAllActiveUsers") {
        context("모든 활성 사용자 조회") {
            it("UserRepository에서 활성 사용자 목록을 조회하고 반환") {
                val mockUsers = listOf(mockk<User>(), mockk<User>(), mockk<User>())

                every { mockUserRepository.findActiveUsers() } returns mockUsers

                val result = sut.getAllActiveUsers()

                result shouldBe mockUsers
                verify(exactly = 1) { mockUserRepository.findActiveUsers() }
            }
        }

        context("활성 사용자가 없는 경우") {
            it("빈 리스트를 반환") {
                every { mockUserRepository.findActiveUsers() } returns emptyList()

                val result = sut.getAllActiveUsers()

                result shouldBe emptyList()
                verify(exactly = 1) { mockUserRepository.findActiveUsers() }
            }
        }
    }
})
