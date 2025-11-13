package io.hhplus.ecommerce.integration.user

import io.hhplus.ecommerce.support.KotestIntegrationTestBase
import io.hhplus.ecommerce.common.exception.user.UserException
import io.hhplus.ecommerce.user.usecase.UserCommandUseCase
import io.hhplus.ecommerce.user.usecase.GetUserQueryUseCase
import io.hhplus.ecommerce.user.domain.constant.LoginType
import io.hhplus.ecommerce.user.domain.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * 사용자 검증 통합 테스트
 *
 * TestContainers MySQL을 사용하여 사용자 검증 전체 플로우를 검증합니다.
 * - 사용자 생성
 * - 이메일 중복 검증
 * - 전화번호 형식 검증
 * - 사용자 수정 및 활성화/비활성화
 */
class UserValidationIntegrationTest(
    private val userCommandUseCase: UserCommandUseCase,
    private val getUserQueryUseCase: GetUserQueryUseCase,
    private val userRepository: UserRepository
) : KotestIntegrationTestBase({

    describe("사용자 생성") {
        context("정상적인 사용자 생성 요청일 때") {
            it("사용자를 정상적으로 생성할 수 있다") {
                // Given
                val loginType = LoginType.LOCAL
                val loginId = "testuser@example.com"
                val password = "password123"
                val email = "testuser@example.com"
                val name = "테스트 사용자"
                val phone = "010-1234-5678"
                val createdBy = 1L

                // When
                val user = userCommandUseCase.createUser(
                    loginType = loginType,
                    loginId = loginId,
                    password = password,
                    email = email,
                    name = name,
                    phone = phone,
                    providerId = null,
                    createdBy = createdBy
                )

                // Then
                user shouldNotBe null
                user.email shouldBe email
                user.name shouldBe name
                user.phone shouldBe phone
                user.isActive shouldBe true
            }
        }

        context("이메일이 중복될 때") {
            it("예외가 발생한다") {
                // Given
                val email = "duplicate@example.com"

                // 첫 번째 사용자 생성
                userCommandUseCase.createUser(
                    loginType = LoginType.LOCAL,
                    loginId = email,
                    password = "password123",
                    email = email,
                    name = "첫 번째 사용자",
                    phone = "010-1111-1111",
                    providerId = null,
                    createdBy = 1L
                )

                // When & Then - 동일 이메일로 두 번째 사용자 생성 시도
                shouldThrow<UserException.EmailAlreadyExists> {
                    userCommandUseCase.createUser(
                        loginType = LoginType.LOCAL,
                        loginId = email,
                        password = "password456",
                        email = email,
                        name = "두 번째 사용자",
                        phone = "010-2222-2222",
                        providerId = null,
                        createdBy = 1L
                    )
                }
            }
        }

        context("올바른 전화번호 형식일 때") {
            it("사용자를 생성할 수 있다") {
                // Given
                val validPhones = listOf(
                    "010-1234-5678",
                    "011-9999-8888",
                    "019-5555-4444"
                )

                // When & Then
                validPhones.forEachIndexed { index, phone ->
                    val user = userCommandUseCase.createUser(
                        loginType = LoginType.LOCAL,
                        loginId = "user$index@example.com",
                        password = "password123",
                        email = "user$index@example.com",
                        name = "사용자 $index",
                        phone = phone,
                        providerId = null,
                        createdBy = 1L
                    )

                    user.phone shouldBe phone
                    user.validatePhoneFormat() // 검증 통과
                }
            }
        }

        context("잘못된 전화번호 형식일 때") {
            it("사용자 생성 시 예외가 발생한다") {
                // Given
                val invalidPhones = listOf(
                    "01012345678",           // 하이픈 없음
                    "010-123-5678",          // 중간 자리수 부족
                    "010-12345-678",         // 마지막 자리수 부족
                    "02-1234-5678",          // 010으로 시작하지 않음
                    "010-abcd-5678"          // 숫자 아님
                )

                // When & Then
                invalidPhones.forEachIndexed { index, phone ->
                    // User.create()가 자동으로 validatePhoneFormat()을 호출하므로 생성 시점에 예외 발생
                    shouldThrow<IllegalArgumentException> {
                        userCommandUseCase.createUser(
                            loginType = LoginType.LOCAL,
                            loginId = "invalid$index@example.com",
                            password = "password123",
                            email = "invalid$index@example.com",
                            name = "잘못된 전화번호 $index",
                            phone = phone,
                            providerId = null,
                            createdBy = 1L
                        )
                    }
                }
            }
        }
    }

    describe("사용자 조회") {
        context("존재하는 사용자 ID로 조회할 때") {
            it("사용자를 조회할 수 있다") {
                // Given
                val createdUser = userCommandUseCase.createUser(
                    loginType = LoginType.LOCAL,
                    loginId = "lookup@example.com",
                    password = "password123",
                    email = "lookup@example.com",
                    name = "조회 테스트",
                    phone = "010-5555-6666",
                    providerId = null,
                    createdBy = 1L
                )

                // When
                val foundUser = getUserQueryUseCase.getUser(createdUser.id)

                // Then
                foundUser shouldNotBe null
                foundUser!!.id shouldBe createdUser.id
                foundUser.email shouldBe createdUser.email
            }
        }

        context("존재하지 않는 사용자 ID로 조회할 때") {
            it("null을 반환한다") {
                // When
                val foundUser = getUserQueryUseCase.getUser(99999L)

                // Then
                foundUser shouldBe null
            }
        }

        context("이메일로 사용자를 조회할 때") {
            it("사용자를 찾을 수 있다") {
                // Given
                val email = "email-search@example.com"
                userCommandUseCase.createUser(
                    loginType = LoginType.LOCAL,
                    loginId = email,
                    password = "password123",
                    email = email,
                    name = "이메일 검색",
                    phone = "010-7777-8888",
                    providerId = null,
                    createdBy = 1L
                )

                // When
                val foundUser = getUserQueryUseCase.getUserByEmail(email)

                // Then
                foundUser shouldNotBe null
                foundUser!!.email shouldBe email
            }
        }
    }

    describe("사용자 수정") {
        context("정상적인 수정 요청일 때") {
            it("사용자 정보를 수정할 수 있다") {
                // Given
                val originalEmail = "original@example.com"
                val user = userCommandUseCase.createUser(
                    loginType = LoginType.LOCAL,
                    loginId = originalEmail,
                    password = "password123",
                    email = originalEmail,
                    name = "원래 이름",
                    phone = "010-1111-2222",
                    providerId = null,
                    createdBy = 1L
                )

                // When
                val updatedName = "변경된 이름"
                val updatedEmail = "updated@example.com"
                val updatedUser = userCommandUseCase.updateUser(
                    userId = user.id,
                    name = updatedName,
                    email = updatedEmail,
                    updatedBy = 1L
                )

                // Then
                updatedUser.name shouldBe updatedName
                updatedUser.email shouldBe updatedEmail
            }
        }

        context("이메일을 다른 사용자의 이메일로 변경하려 할 때") {
            it("예외가 발생한다") {
                // Given
                val existingEmail = "existing@example.com"

                // 첫 번째 사용자
                userCommandUseCase.createUser(
                    loginType = LoginType.LOCAL,
                    loginId = existingEmail,
                    password = "password123",
                    email = existingEmail,
                    name = "기존 사용자",
                    phone = "010-3333-4444",
                    providerId = null,
                    createdBy = 1L
                )

                // 두 번째 사용자
                val secondUser = userCommandUseCase.createUser(
                    loginType = LoginType.LOCAL,
                    loginId = "second@example.com",
                    password = "password123",
                    email = "second@example.com",
                    name = "두 번째 사용자",
                    phone = "010-5555-6666",
                    providerId = null,
                    createdBy = 1L
                )

                // When & Then - 두 번째 사용자의 이메일을 첫 번째 사용자 이메일로 변경 시도
                shouldThrow<UserException.EmailAlreadyExists> {
                    userCommandUseCase.updateUser(
                        userId = secondUser.id,
                        name = secondUser.name,
                        email = existingEmail,
                        updatedBy = 1L
                    )
                }
            }
        }
    }

    describe("사용자 활성화/비활성화") {
        context("사용자를 비활성화할 때") {
            it("isActive가 false로 변경된다") {
                // Given
                val user = userCommandUseCase.createUser(
                    loginType = LoginType.LOCAL,
                    loginId = "deactivate@example.com",
                    password = "password123",
                    email = "deactivate@example.com",
                    name = "비활성화 테스트",
                    phone = "010-9999-0000",
                    providerId = null,
                    createdBy = 1L
                )

                // When
                val deactivatedUser = userCommandUseCase.deactivateUser(user.id, 1L)

                // Then
                deactivatedUser.isActive shouldBe false
            }
        }

        context("비활성화된 사용자를 다시 활성화할 때") {
            it("isActive가 true로 변경된다") {
                // Given
                val user = userCommandUseCase.createUser(
                    loginType = LoginType.LOCAL,
                    loginId = "reactivate@example.com",
                    password = "password123",
                    email = "reactivate@example.com",
                    name = "재활성화 테스트",
                    phone = "010-8888-7777",
                    providerId = null,
                    createdBy = 1L
                )
                userCommandUseCase.deactivateUser(user.id, 1L)

                // When
                val activatedUser = userCommandUseCase.activateUser(user.id, 1L)

                // Then
                activatedUser.isActive shouldBe true
            }
        }
    }
})
