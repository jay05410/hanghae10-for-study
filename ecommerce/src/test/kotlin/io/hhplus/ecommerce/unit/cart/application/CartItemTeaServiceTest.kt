package io.hhplus.ecommerce.unit.cart.application

import io.hhplus.ecommerce.cart.domain.entity.CartItemTea
import io.hhplus.ecommerce.cart.domain.repository.CartItemTeaRepository
import io.hhplus.ecommerce.cart.dto.TeaItemRequest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * CartItemTeaService 단위 테스트
 *
 * 책임: 장바구니 아이템 차 구성 관리 서비스의 핵심 기능 검증
 * - 차 구성 저장, 수정, 삭제, 조회 기능의 Repository 호출 검증
 * - 차 구성 검증 로직 검증
 * - 도메인 객체 생성 메서드 호출 검증
 *
 * 검증 목표:
 * 1. 각 CRUD 메서드가 적절한 Repository 메서드를 호출하는가?
 * 2. 도메인 객체 생성 메서드가 올바르게 호출되는가?
 * 3. 차 구성 검증 로직이 올바르게 동작하는가?
 * 4. 트랜잭션 경계 내에서 업데이트 로직이 순서대로 실행되는가?
 */
class CartItemTeaServiceTest : DescribeSpec({
    val mockCartItemTeaRepository = mockk<CartItemTeaRepository>()
    val sut = CartItemTeaService(mockCartItemTeaRepository)

    beforeEach {
        clearMocks(mockCartItemTeaRepository)
    }

    describe("saveCartItemTeas") {
        context("차 구성 목록을 저장할 때") {
            it("각 TeaItemRequest에 대해 CartItemTea를 생성하고 Repository에 저장") {
                val cartItemId = 1L
                val teaItems = listOf(
                    TeaItemRequest(productId = 1L, selectionOrder = 1, ratioPercent = 60),
                    TeaItemRequest(productId = 2L, selectionOrder = 2, ratioPercent = 40)
                )
                val mockCartItemTea1 = mockk<CartItemTea>()
                val mockCartItemTea2 = mockk<CartItemTea>()

                mockkObject(CartItemTea.Companion)
                every { CartItemTea.create(cartItemId, 1L, 1, 60) } returns mockCartItemTea1
                every { CartItemTea.create(cartItemId, 2L, 2, 40) } returns mockCartItemTea2
                every { mockCartItemTeaRepository.save(mockCartItemTea1) } returns mockCartItemTea1
                every { mockCartItemTeaRepository.save(mockCartItemTea2) } returns mockCartItemTea2

                val result = sut.saveCartItemTeas(cartItemId, teaItems)

                result shouldBe listOf(mockCartItemTea1, mockCartItemTea2)
                verify(exactly = 1) { CartItemTea.create(cartItemId, 1L, 1, 60) }
                verify(exactly = 1) { CartItemTea.create(cartItemId, 2L, 2, 40) }
                verify(exactly = 1) { mockCartItemTeaRepository.save(mockCartItemTea1) }
                verify(exactly = 1) { mockCartItemTeaRepository.save(mockCartItemTea2) }
            }
        }

        context("빈 차 구성 목록을 저장할 때") {
            it("빈 리스트를 반환하고 저장 작업을 수행하지 않음") {
                val cartItemId = 1L
                val teaItems = emptyList<TeaItemRequest>()

                val result = sut.saveCartItemTeas(cartItemId, teaItems)

                result shouldBe emptyList()
                verify(exactly = 0) { mockCartItemTeaRepository.save(any()) }
            }
        }
    }

    describe("updateCartItemTeas") {
        context("차 구성을 업데이트할 때") {
            it("기존 차 구성을 삭제한 후 새로운 차 구성을 저장") {
                val cartItemId = 1L
                val teaItems = listOf(
                    TeaItemRequest(productId = 1L, selectionOrder = 1, ratioPercent = 100)
                )
                val mockCartItemTea = mockk<CartItemTea>()

                every { mockCartItemTeaRepository.deleteByCartItemId(cartItemId) } just Runs
                mockkObject(CartItemTea.Companion)
                every { CartItemTea.create(cartItemId, 1L, 1, 100) } returns mockCartItemTea
                every { mockCartItemTeaRepository.save(mockCartItemTea) } returns mockCartItemTea

                val result = sut.updateCartItemTeas(cartItemId, teaItems)

                result shouldBe listOf(mockCartItemTea)
                verifyOrder {
                    mockCartItemTeaRepository.deleteByCartItemId(cartItemId)
                    CartItemTea.create(cartItemId, 1L, 1, 100)
                    mockCartItemTeaRepository.save(mockCartItemTea)
                }
            }
        }
    }

    describe("deleteCartItemTeas") {
        context("차 구성 삭제") {
            it("Repository에 삭제를 지시") {
                val cartItemId = 1L

                every { mockCartItemTeaRepository.deleteByCartItemId(cartItemId) } just Runs

                sut.deleteCartItemTeas(cartItemId)

                verify(exactly = 1) { mockCartItemTeaRepository.deleteByCartItemId(cartItemId) }
            }
        }
    }

    describe("getCartItemTeas") {
        context("차 구성 목록 조회") {
            it("Repository에 조회를 지시하고 결과를 반환") {
                val cartItemId = 1L
                val expectedTeas = listOf(mockk<CartItemTea>(), mockk<CartItemTea>())

                every { mockCartItemTeaRepository.findByCartItemId(cartItemId) } returns expectedTeas

                val result = sut.getCartItemTeas(cartItemId)

                result shouldBe expectedTeas
                verify(exactly = 1) { mockCartItemTeaRepository.findByCartItemId(cartItemId) }
            }
        }

        context("차 구성이 없는 아이템 조회") {
            it("빈 리스트를 반환") {
                val cartItemId = 999L
                val emptyTeas = emptyList<CartItemTea>()

                every { mockCartItemTeaRepository.findByCartItemId(cartItemId) } returns emptyTeas

                val result = sut.getCartItemTeas(cartItemId)

                result shouldBe emptyTeas
                verify(exactly = 1) { mockCartItemTeaRepository.findByCartItemId(cartItemId) }
            }
        }
    }

    describe("getCartItemTea") {
        context("특정 차 구성 조회") {
            it("Repository에 조회를 지시하고 결과를 반환") {
                val id = 1L
                val expectedTea = mockk<CartItemTea>()

                every { mockCartItemTeaRepository.findById(id) } returns expectedTea

                val result = sut.getCartItemTea(id)

                result shouldBe expectedTea
                verify(exactly = 1) { mockCartItemTeaRepository.findById(id) }
            }
        }

        context("존재하지 않는 차 구성 조회") {
            it("null을 반환") {
                val id = 999L

                every { mockCartItemTeaRepository.findById(id) } returns null

                val result = sut.getCartItemTea(id)

                result shouldBe null
                verify(exactly = 1) { mockCartItemTeaRepository.findById(id) }
            }
        }
    }

    describe("validateTeaItems") {
        context("유효한 차 구성") {
            it("검증을 통과") {
                val validTeaItems = listOf(
                    TeaItemRequest(productId = 1L, selectionOrder = 1, ratioPercent = 60),
                    TeaItemRequest(productId = 2L, selectionOrder = 2, ratioPercent = 40)
                )

                // 예외가 발생하지 않아야 함
                sut.validateTeaItems(validTeaItems)
            }
        }

        context("빈 차 구성") {
            it("IllegalArgumentException을 발생") {
                val emptyTeaItems = emptyList<TeaItemRequest>()

                shouldThrow<IllegalArgumentException> {
                    sut.validateTeaItems(emptyTeaItems)
                }.message shouldBe "차 구성은 최소 1개 이상이어야 합니다"
            }
        }

        context("총 배합 비율이 100%가 아닌 차 구성") {
            it("IllegalArgumentException을 발생") {
                val invalidRatioTeaItems = listOf(
                    TeaItemRequest(productId = 1L, selectionOrder = 1, ratioPercent = 30),
                    TeaItemRequest(productId = 2L, selectionOrder = 2, ratioPercent = 30)
                )

                shouldThrow<IllegalArgumentException> {
                    sut.validateTeaItems(invalidRatioTeaItems)
                }.message shouldBe "총 배합 비율은 100%가 되어야 합니다. 현재: 60%"
            }
        }

        context("중복된 상품이 포함된 차 구성") {
            it("IllegalArgumentException을 발생") {
                val duplicateProductTeaItems = listOf(
                    TeaItemRequest(productId = 1L, selectionOrder = 1, ratioPercent = 60),
                    TeaItemRequest(productId = 1L, selectionOrder = 2, ratioPercent = 40)
                )

                shouldThrow<IllegalArgumentException> {
                    sut.validateTeaItems(duplicateProductTeaItems)
                }.message shouldBe "중복된 차 상품이 있습니다"
            }
        }

        context("잘못된 선택 순서가 포함된 차 구성") {
            it("선택 순서가 연속적이지 않으면 IllegalArgumentException을 발생") {
                val invalidOrderTeaItems = listOf(
                    TeaItemRequest(productId = 1L, selectionOrder = 1, ratioPercent = 50),
                    TeaItemRequest(productId = 2L, selectionOrder = 3, ratioPercent = 50)
                )

                shouldThrow<IllegalArgumentException> {
                    sut.validateTeaItems(invalidOrderTeaItems)
                }.message shouldBe "선택 순서는 1부터 연속적이어야 합니다"
            }
        }
    }
})