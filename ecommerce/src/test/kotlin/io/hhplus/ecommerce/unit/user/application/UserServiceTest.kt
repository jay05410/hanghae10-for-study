package io.hhplus.ecommerce.unit.user.application

import io.hhplus.ecommerce.user.domain.entity.User
import io.hhplus.ecommerce.user.domain.repository.UserRepository
import io.hhplus.ecommerce.user.domain.constant.LoginType
import io.hhplus.ecommerce.user.exception.UserException
import io.hhplus.ecommerce.user.application.UserService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.*

/**
 * UserService 단위 테스트
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
class UserServiceTest : DescribeSpec({
    val mockUserRepository = mockk<UserRepository>()
    val sut = UserService(mockUserRepository)

    beforeEach {
        clearMocks(mockUserRepository)
    }

    describe("createUser") {
        context("새로운 사용자 생성 요청") {
            it("이메일 중복 검증 후 사용자를 생성하고 저장") {
                val email = "test@example.com"
                val name = "테스트사용자"
                val phone = "010-1234-5678"
                val createdBy = 1L
                val mockUser = mockk<User>()

                every { mockUserRepository.findByEmail(email) } returns null
                every { mockUserRepository.save(any()) } returns mockUser

                val result = sut.createUser(
                    loginType = LoginType.LOCAL,
                    loginId = "testId",
                    password = "password",
                    email = email,
                    name = name,
                    phone = phone,
                    providerId = null,
                    createdBy = createdBy
                )

                result shouldBe mockUser
                verify(exactly = 1) { mockUserRepository.findByEmail(email) }
                verify(exactly = 1) { mockUserRepository.save(any()) }
            }
        }

        context("중복된 이메일로 사용자 생성 요청") {
            it("EmailAlreadyExists 예외를 발생") {
                val email = "duplicate@example.com"
                val existingUser = mockk<User>()

                every { mockUserRepository.findByEmail(email) } returns existingUser

                shouldThrow<UserException.EmailAlreadyExists> {
                    sut.createUser(
                        loginType = LoginType.LOCAL,
                        loginId = "testId",
                        password = "password",
                        email = email,
                        name = "테스트사용자",
                        phone = "010-1234-5678",
                        providerId = null,
                        createdBy = 1L
                    )
                }

                verify(exactly = 1) { mockUserRepository.findByEmail(email) }
                verify(exactly = 0) { mockUserRepository.save(any()) }
            }
        }

        context("다양한 LoginType으로 사용자 생성") {
            it("각 LoginType에 맞게 사용자를 생성") {
                val loginTypes = listOf(LoginType.LOCAL, LoginType.KAKAO)

                loginTypes.forEach { loginType ->
                    val email = "test_${loginType.name.lowercase()}@example.com"
                    val mockUser = mockk<User>()

                    every { mockUserRepository.findByEmail(email) } returns null
                    every { mockUserRepository.save(any()) } returns mockUser

                    val result = sut.createUser(
                        loginType = loginType,
                        loginId = "testId",
                        password = if (loginType == LoginType.LOCAL) "password" else null,
                        email = email,
                        name = "테스트사용자",
                        phone = "010-1234-5678",
                        providerId = if (loginType == LoginType.KAKAO) "provider123" else null,
                        createdBy = 1L
                    )

                    result shouldBe mockUser
                    verify(exactly = 1) { mockUserRepository.findByEmail(email) }
                    verify(exactly = 1) { mockUserRepository.save(any()) }
                    clearMocks(mockUserRepository)
                }
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

        context("다양한 사용자 ID로 조회") {
            it("각 ID에 대해 정확한 Repository 호출") {
                val userIds = listOf(1L, 100L, 999L)

                userIds.forEach { userId ->
                    val mockUser = mockk<User>()
                    every { mockUserRepository.findById(userId) } returns mockUser

                    val result = sut.getUser(userId)

                    result shouldBe mockUser
                    verify(exactly = 1) { mockUserRepository.findById(userId) }
                    clearMocks(mockUserRepository)
                }
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

    describe("updateUser") {
        context("존재하는 사용자 정보 수정") {
            it("이메일 중복 검증 후 사용자 정보를 수정") {
                val userId = 1L
                val newName = "수정된이름"
                val newEmail = "new@example.com"
                val updatedBy = 1L
                val mockUser = mockk<User> {
                    every { id } returns userId
                    every { name } returns "기존이름"
                    every { email } returns "old@example.com"
                }

                every { mockUser.update(any(), any(), any()) } just runs
                every { mockUserRepository.findById(userId) } returns mockUser
                every { mockUserRepository.findByEmail(newEmail) } returns null
                every { mockUserRepository.save(mockUser) } returns mockUser

                val result = sut.updateUser(userId, newName, newEmail, updatedBy)

                result shouldBe mockUser
                verify(exactly = 1) { mockUserRepository.findById(userId) }
                verify(exactly = 1) { mockUserRepository.findByEmail(newEmail) }
                verify(exactly = 1) { mockUser.update(newName, newEmail, updatedBy) }
                verify(exactly = 1) { mockUserRepository.save(mockUser) }
            }
        }

        context("존재하지 않는 사용자 정보 수정 시도") {
            it("UserNotFound 예외를 발생") {
                val userId = 999L

                every { mockUserRepository.findById(userId) } returns null

                shouldThrow<UserException.UserNotFound> {
                    sut.updateUser(userId, "새이름", "new@example.com", 1L)
                }

                verify(exactly = 1) { mockUserRepository.findById(userId) }
                verify(exactly = 0) { mockUserRepository.findByEmail(any()) }
            }
        }

        context("이미 사용 중인 이메일로 수정 시도") {
            it("EmailAlreadyExists 예외를 발생") {
                val userId = 1L
                val newEmail = "duplicate@example.com"
                val mockUser = mockk<User> {
                    every { id } returns userId
                    every { email } returns "old@example.com"
                }
                val existingUser = mockk<User> {
                    every { id } returns 2L
                }

                every { mockUserRepository.findById(userId) } returns mockUser
                every { mockUserRepository.findByEmail(newEmail) } returns existingUser

                shouldThrow<UserException.EmailAlreadyExists> {
                    sut.updateUser(userId, "새이름", newEmail, 1L)
                }

                verify(exactly = 1) { mockUserRepository.findById(userId) }
                verify(exactly = 1) { mockUserRepository.findByEmail(newEmail) }
            }
        }

        context("동일한 이메일로 수정") {
            it("이메일 중복 검증을 통과하고 정보를 수정") {
                val userId = 1L
                val sameEmail = "same@example.com"
                val mockUser = mockk<User> {
                    every { id } returns userId
                    every { name } returns "기존이름"
                    every { email } returns sameEmail
                }

                every { mockUser.update(any(), any(), any()) } just runs
                every { mockUserRepository.findById(userId) } returns mockUser
                every { mockUserRepository.save(mockUser) } returns mockUser

                val result = sut.updateUser(userId, "새이름", sameEmail, 1L)

                result shouldBe mockUser
                verify(exactly = 1) { mockUserRepository.findById(userId) }
                verify(exactly = 0) { mockUserRepository.findByEmail(any()) }
                verify(exactly = 1) { mockUser.update("새이름", sameEmail, 1L) }
                verify(exactly = 1) { mockUserRepository.save(mockUser) }
            }
        }

        context("null 값으로 사용자 정보 수정") {
            it("name이 null인 경우 기존 name을 유지") {
                val userId = 1L
                val newEmail = "new@example.com"
                val mockUser = mockk<User> {
                    every { id } returns userId
                    every { name } returns "기존이름"
                    every { email } returns "old@example.com"
                }

                every { mockUser.update(any(), any(), any()) } just runs
                every { mockUserRepository.findById(userId) } returns mockUser
                every { mockUserRepository.findByEmail(newEmail) } returns null
                every { mockUserRepository.save(mockUser) } returns mockUser

                val result = sut.updateUser(userId, null, newEmail, 1L)

                result shouldBe mockUser
                verify(exactly = 1) { mockUserRepository.findById(userId) }
                verify(exactly = 1) { mockUserRepository.findByEmail(newEmail) }
                verify(exactly = 1) { mockUser.update("기존이름", newEmail, 1L) }
                verify(exactly = 1) { mockUserRepository.save(mockUser) }
            }

            it("email이 null인 경우 기존 email을 유지") {
                val userId = 1L
                val newName = "새이름"
                val mockUser = mockk<User> {
                    every { id } returns userId
                    every { name } returns "기존이름"
                    every { email } returns "existing@example.com"
                }

                every { mockUser.update(any(), any(), any()) } just runs
                every { mockUserRepository.findById(userId) } returns mockUser
                every { mockUserRepository.save(mockUser) } returns mockUser

                val result = sut.updateUser(userId, newName, null, 1L)

                result shouldBe mockUser
                verify(exactly = 1) { mockUserRepository.findById(userId) }
                verify(exactly = 0) { mockUserRepository.findByEmail(any()) }
                verify(exactly = 1) { mockUser.update(newName, "existing@example.com", 1L) }
                verify(exactly = 1) { mockUserRepository.save(mockUser) }
            }

            it("name과 email이 모두 null인 경우 기존 값들을 유지") {
                val userId = 1L
                val mockUser = mockk<User> {
                    every { id } returns userId
                    every { name } returns "기존이름"
                    every { email } returns "existing@example.com"
                }

                every { mockUser.update(any(), any(), any()) } just runs
                every { mockUserRepository.findById(userId) } returns mockUser
                every { mockUserRepository.save(mockUser) } returns mockUser

                val result = sut.updateUser(userId, null, null, 1L)

                result shouldBe mockUser
                verify(exactly = 1) { mockUserRepository.findById(userId) }
                verify(exactly = 0) { mockUserRepository.findByEmail(any()) }
                verify(exactly = 1) { mockUser.update("기존이름", "existing@example.com", 1L) }
                verify(exactly = 1) { mockUserRepository.save(mockUser) }
            }
        }

        context("동일한 사용자가 같은 이메일로 수정 시도") {
            it("기존 사용자가 자신의 이메일로 수정하는 경우 성공") {
                val userId = 1L
                val currentEmail = "current@example.com"
                val mockUser = mockk<User> {
                    every { id } returns userId
                    every { name } returns "기존이름"
                    every { email } returns "old@example.com"
                }
                val existingUserWithSameEmail = mockk<User> {
                    every { id } returns userId  // 동일한 사용자
                }

                every { mockUser.update(any(), any(), any()) } just runs
                every { mockUserRepository.findById(userId) } returns mockUser
                every { mockUserRepository.findByEmail(currentEmail) } returns existingUserWithSameEmail
                every { mockUserRepository.save(mockUser) } returns mockUser

                val result = sut.updateUser(userId, "새이름", currentEmail, 1L)

                result shouldBe mockUser
                verify(exactly = 1) { mockUserRepository.findById(userId) }
                verify(exactly = 1) { mockUserRepository.findByEmail(currentEmail) }
                verify(exactly = 1) { mockUser.update("새이름", currentEmail, 1L) }
                verify(exactly = 1) { mockUserRepository.save(mockUser) }
            }
        }
    }

    describe("deleteUser") {
        context("존재하는 사용자 삭제") {
            it("사용자를 삭제하고 저장") {
                val userId = 1L
                val deactivatedBy = 1L
                val mockUser = mockk<User> {
                    every { delete(any()) } just runs
                }

                every { mockUserRepository.findById(userId) } returns mockUser
                every { mockUserRepository.save(mockUser) } returns mockUser

                val result = sut.deleteUser(userId, deactivatedBy)

                result shouldBe mockUser
                verify(exactly = 1) { mockUserRepository.findById(userId) }
                verify(exactly = 1) { mockUser.delete(any()) }
                verify(exactly = 1) { mockUserRepository.save(any()) }
            }
        }

        context("존재하지 않는 사용자 비활성화 시도") {
            it("UserNotFound 예외를 발생") {
                val userId = 999L

                every { mockUserRepository.findById(userId) } returns null

                shouldThrow<UserException.UserNotFound> {
                    sut.deleteUser(userId, 1L)
                }

                verify(exactly = 1) { mockUserRepository.findById(userId) }
                verify(exactly = 0) { mockUserRepository.save(any()) }
            }
        }
    }

    describe("restoreUser") {
        context("존재하는 사용자 복구") {
            it("사용자를 복구하고 저장") {
                val userId = 1L
                val activatedBy = 1L
                val mockUser = mockk<User> {
                    every { restore() } just runs
                    every { updatedBy = any() } just runs
                }

                every { mockUserRepository.findById(userId) } returns mockUser
                every { mockUserRepository.save(mockUser) } returns mockUser

                val result = sut.restoreUser(userId, activatedBy)

                result shouldBe mockUser
                verify(exactly = 1) { mockUserRepository.findById(userId) }
                verify(exactly = 1) { mockUser.restore() }
                verify(exactly = 1) { mockUserRepository.save(any()) }
            }
        }

        context("존재하지 않는 사용자 활성화 시도") {
            it("UserNotFound 예외를 발생") {
                val userId = 999L

                every { mockUserRepository.findById(userId) } returns null

                shouldThrow<UserException.UserNotFound> {
                    sut.restoreUser(userId, 1L)
                }

                verify(exactly = 1) { mockUserRepository.findById(userId) }
                verify(exactly = 0) { mockUserRepository.save(any()) }
            }
        }
    }

    describe("getAllUsers") {
        context("모든 활성 사용자 조회") {
            it("UserRepository에서 활성 사용자 목록을 조회하고 반환") {
                val mockUsers = listOf(mockk<User>(), mockk<User>(), mockk<User>())

                every { mockUserRepository.findActiveUsers() } returns mockUsers

                val result = sut.getAllUsers()

                result shouldBe mockUsers
                verify(exactly = 1) { mockUserRepository.findActiveUsers() }
            }
        }

        context("활성 사용자가 없는 경우") {
            it("빈 리스트를 반환") {
                every { mockUserRepository.findActiveUsers() } returns emptyList()

                val result = sut.getAllUsers()

                result shouldBe emptyList()
                verify(exactly = 1) { mockUserRepository.findActiveUsers() }
            }
        }
    }

    describe("비즈니스 로직 검증") {
        context("Repository 호출 순서 검증") {
            it("각 메서드가 올바른 순서로 Repository를 호출") {
                val email = "test@example.com"
                val mockUser = mockk<User>()

                every { mockUserRepository.findByEmail(email) } returns null
                every { mockUserRepository.save(any()) } returns mockUser

                sut.createUser(
                    loginType = LoginType.LOCAL,
                    loginId = "testId",
                    password = "password",
                    email = email,
                    name = "테스트사용자",
                    phone = "010-1234-5678",
                    providerId = null,
                    createdBy = 1L
                )

                verifyOrder {
                    mockUserRepository.findByEmail(email)  // 먼저 중복 검증
                    mockUserRepository.save(any())         // 그 다음 저장
                }
            }
        }
    }
})