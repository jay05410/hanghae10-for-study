package io.hhplus.ecommerce.cart.application

import io.hhplus.ecommerce.cart.domain.entity.Cart
import io.hhplus.ecommerce.cart.domain.entity.CartItem
import io.hhplus.ecommerce.cart.domain.entity.CartItemTea
import io.hhplus.ecommerce.cart.domain.repository.CartRepository
import io.hhplus.ecommerce.cart.dto.TeaItemRequest
import io.hhplus.ecommerce.common.exception.cart.CartException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.time.LocalDateTime

/**
 * CartService 단위 테스트
 *
 * 책임: 장바구니 도메인 서비스의 핵심 비즈니스 로직 검증
 * - 장바구니 생성, 아이템 추가/수정/삭제, 장바구니 비우기 기능의 Repository 호출 검증
 * - CartItemTeaService와의 상호작용 검증
 * - 도메인 객체와의 상호작용 검증
 *
 * 검증 목표:
 * 1. 각 메서드가 적절한 Repository 및 의존 서비스 메서드를 호출하는가?
 * 2. 도메인 객체의 비즈니스 메서드가 올바르게 호출되는가?
 * 3. 예외 상황에서 적절한 예외가 발생하는가?
 * 4. 차 구성 관련 로직이 올바르게 처리되는가?
 */
class CartServiceTest : DescribeSpec({
    val mockCartRepository = mockk<CartRepository>()
    val mockCartItemTeaService = mockk<CartItemTeaService>()
    val sut = CartService(mockCartRepository, mockCartItemTeaService)

    beforeEach {
        clearMocks(mockCartRepository, mockCartItemTeaService)
    }

    describe("getOrCreateCart") {
        context("기존 장바구니가 있는 경우") {
            it("Repository에서 조회한 장바구니를 반환") {
                val userId = 1L
                val existingCart = mockk<Cart>()
                every { mockCartRepository.findByUserId(userId) } returns existingCart

                val result = sut.getOrCreateCart(userId)

                result shouldBe existingCart
                verify(exactly = 1) { mockCartRepository.findByUserId(userId) }
                verify(exactly = 0) { mockCartRepository.save(any()) }
            }
        }

        context("기존 장바구니가 없는 경우") {
            it("새 장바구니를 생성하고 저장한 후 반환") {
                val userId = 1L
                val newCart = mockk<Cart>()

                every { mockCartRepository.findByUserId(userId) } returns null
                mockkObject(Cart.Companion)
                every { Cart.create(userId = userId, createdBy = userId) } returns newCart
                every { mockCartRepository.save(newCart) } returns newCart

                val result = sut.getOrCreateCart(userId)

                result shouldBe newCart
                verify(exactly = 1) { mockCartRepository.findByUserId(userId) }
                verify(exactly = 1) { Cart.create(userId = userId, createdBy = userId) }
                verify(exactly = 1) { mockCartRepository.save(newCart) }
            }
        }
    }

    describe("addToCart") {
        context("새로운 상품 추가") {
            it("차 구성을 검증하고 새 아이템을 추가한 후 차 구성을 저장") {
                val userId = 1L
                val packageTypeId = 1L
                val packageTypeName = "30일 패키지"
                val packageTypeDays = 30
                val dailyServing = 2
                val totalQuantity = 300.0
                val giftWrap = false
                val giftMessage: String? = null
                val teaItems = listOf(TeaItemRequest(productId = 2L, selectionOrder = 1, ratioPercent = 100))
                val mockCart = mockk<Cart>(relaxed = true)
                val mockSavedCart = mockk<Cart>(relaxed = true)
                val mockNewCartItem = mockk<CartItem> {
                    every { id } returns 1L
                }

                every { mockCart.items } returns emptyList()
                every { mockCartItemTeaService.validateTeaItems(teaItems) } just Runs
                every { mockCartRepository.findByUserId(userId) } returns mockCart
                every { mockCart.addItem(packageTypeId, packageTypeName, packageTypeDays, dailyServing, totalQuantity, giftWrap, giftMessage, userId) } returns mockNewCartItem
                every { mockCartRepository.save(mockCart) } returns mockSavedCart
                every { mockSavedCart.items } returns listOf(mockNewCartItem)
                every { mockCartItemTeaService.saveCartItemTeas(1L, teaItems) } returns emptyList()

                val result = sut.addToCart(userId, packageTypeId, packageTypeName, packageTypeDays, dailyServing, totalQuantity, giftWrap, giftMessage, teaItems)

                result shouldBe mockSavedCart
                verify(exactly = 1) { mockCartItemTeaService.validateTeaItems(teaItems) }
                verify(exactly = 1) { mockCart.addItem(packageTypeId, packageTypeName, packageTypeDays, dailyServing, totalQuantity, giftWrap, giftMessage, userId) }
                verify(exactly = 1) { mockCartRepository.save(mockCart) }
                verify(exactly = 1) { mockCartItemTeaService.saveCartItemTeas(1L, teaItems) }
            }
        }

        context("기존 동일 패키지가 있는 경우") {
            it("기존 아이템을 제거하고 새 아이템을 추가한 후 차 구성을 저장") {
                val userId = 1L
                val packageTypeId = 1L
                val packageTypeName = "30일 패키지"
                val packageTypeDays = 30
                val dailyServing = 2
                val totalQuantity = 300.0
                val giftWrap = false
                val giftMessage: String? = null
                val teaItems = listOf(TeaItemRequest(productId = 2L, selectionOrder = 1, ratioPercent = 100))
                val mockCart = mockk<Cart>(relaxed = true)
                val mockExistingItem = mockk<CartItem>(relaxed = true) {
                    every { this@mockk.packageTypeId } returns 1L
                    every { this@mockk.id } returns 1L
                    every { isActive } returns true
                    every { createdAt } returns LocalDateTime.now()
                    every { updatedAt } returns LocalDateTime.now()
                    every { createdBy } returns 1L
                    every { updatedBy } returns 1L
                    every { deletedAt } returns null
                }
                val mockNewCartItem = mockk<CartItem> {
                    every { id } returns 2L
                }

                every { mockCart.items } returns listOf(mockExistingItem)
                every { mockCartItemTeaService.validateTeaItems(teaItems) } just Runs
                every { mockCartRepository.findByUserId(userId) } returns mockCart
                every { mockCart.removeItem(1L, userId) } just Runs
                every { mockCart.addItem(packageTypeId, packageTypeName, packageTypeDays, dailyServing, totalQuantity, giftWrap, giftMessage, userId) } returns mockNewCartItem
                every { mockCartRepository.save(mockCart) } returns mockCart
                every { mockCartItemTeaService.saveCartItemTeas(2L, teaItems) } returns emptyList()

                val result = sut.addToCart(userId, packageTypeId, packageTypeName, packageTypeDays, dailyServing, totalQuantity, giftWrap, giftMessage, teaItems)

                result shouldBe mockCart
                verify(exactly = 1) { mockCartItemTeaService.validateTeaItems(teaItems) }
                verify(exactly = 1) { mockCart.removeItem(1L, userId) }
                verify(exactly = 1) { mockCart.addItem(packageTypeId, packageTypeName, packageTypeDays, dailyServing, totalQuantity, giftWrap, giftMessage, userId) }
                verify(exactly = 1) { mockCartRepository.save(mockCart) }
                verify(exactly = 1) { mockCartItemTeaService.saveCartItemTeas(2L, teaItems) }
            }
        }

        context("다른 패키지들이 장바구니에 있는 경우") {
            it("다른 packageTypeId인 경우 새 아이템으로 추가") {
                val userId = 1L
                val packageTypeId = 2L
                val packageTypeName = "15일 패키지"
                val packageTypeDays = 15
                val dailyServing = 1
                val totalQuantity = 150.0
                val giftWrap = false
                val giftMessage: String? = null
                val teaItems = listOf(TeaItemRequest(productId = 3L, selectionOrder = 1, ratioPercent = 50))
                val mockCart = mockk<Cart>(relaxed = true)
                val mockSavedCart = mockk<Cart>(relaxed = true)
                val mockExistingItem = mockk<CartItem>(relaxed = true) {
                    every { this@mockk.packageTypeId } returns 1L
                }
                val mockNewCartItem = mockk<CartItem> {
                    every { id } returns 2L
                }

                every { mockCart.items } returns listOf(mockExistingItem)
                every { mockCartItemTeaService.validateTeaItems(teaItems) } just Runs
                every { mockCartRepository.findByUserId(userId) } returns mockCart
                every { mockCart.addItem(packageTypeId, packageTypeName, packageTypeDays, dailyServing, totalQuantity, giftWrap, giftMessage, userId) } returns mockNewCartItem
                every { mockCartRepository.save(mockCart) } returns mockSavedCart
                every { mockSavedCart.items } returns listOf(mockExistingItem, mockNewCartItem)
                every { mockCartItemTeaService.saveCartItemTeas(2L, teaItems) } returns emptyList()

                val result = sut.addToCart(userId, packageTypeId, packageTypeName, packageTypeDays, dailyServing, totalQuantity, giftWrap, giftMessage, teaItems)

                result shouldBe mockSavedCart
                verify(exactly = 1) { mockCartItemTeaService.validateTeaItems(teaItems) }
                verify(exactly = 1) { mockCart.addItem(packageTypeId, packageTypeName, packageTypeDays, dailyServing, totalQuantity, giftWrap, giftMessage, userId) }
                verify(exactly = 1) { mockCartRepository.save(mockCart) }
                verify(exactly = 1) { mockCartItemTeaService.saveCartItemTeas(2L, teaItems) }
            }

        }
    }

    describe("updateCartItem") {
        context("수량을 양수로 변경하는 경우") {
            it("아이템 수량을 업데이트하고 장바구니를 저장") {
                val userId = 1L
                val cartItemId = 1L
                val totalQuantity = 500.0
                val updatedBy = 1L
                val mockCart = mockk<Cart>(relaxed = true)

                every { mockCartRepository.findByUserId(userId) } returns mockCart
                every { mockCartRepository.save(mockCart) } returns mockCart

                val result = sut.updateCartItem(userId, cartItemId, totalQuantity, updatedBy)

                result shouldBe mockCart
                verify(exactly = 1) { mockCartRepository.findByUserId(userId) }
                verify(exactly = 1) { mockCart.updateItemQuantity(cartItemId, totalQuantity, updatedBy) }
                verify(exactly = 1) { mockCartRepository.save(mockCart) }
                verify(exactly = 0) { mockCart.removeItem(any(), any()) }
            }
        }

        context("수량을 0 이하로 변경하는 경우") {
            it("아이템을 제거하고 장바구니를 저장") {
                val userId = 1L
                val cartItemId = 1L
                val totalQuantity = 0.0
                val updatedBy = 1L
                val mockCart = mockk<Cart>(relaxed = true)

                every { mockCartRepository.findByUserId(userId) } returns mockCart
                every { mockCartRepository.save(mockCart) } returns mockCart

                val result = sut.updateCartItem(userId, cartItemId, totalQuantity, updatedBy)

                result shouldBe mockCart
                verify(exactly = 1) { mockCartRepository.findByUserId(userId) }
                verify(exactly = 1) { mockCart.removeItem(cartItemId, updatedBy) }
                verify(exactly = 1) { mockCartRepository.save(mockCart) }
                verify(exactly = 0) { mockCart.updateItemQuantity(any(), any(), any()) }
            }
        }

        context("장바구니가 존재하지 않는 경우") {
            it("CartException.CartNotFound를 발생") {
                val userId = 999L
                val cartItemId = 1L
                val totalQuantity = 500.0
                val updatedBy = 1L

                every { mockCartRepository.findByUserId(userId) } returns null

                shouldThrow<CartException.CartNotFound> {
                    sut.updateCartItem(userId, cartItemId, totalQuantity, updatedBy)
                }

                verify(exactly = 1) { mockCartRepository.findByUserId(userId) }
                verify(exactly = 0) { mockCartRepository.save(any()) }
            }
        }
    }

    describe("removeCartItem") {
        context("정상적인 아이템 제거") {
            it("차 구성을 먼저 삭제한 후 아이템을 제거하고 장바구니를 저장") {
                val userId = 1L
                val cartItemId = 1L
                val mockCart = mockk<Cart>(relaxed = true)

                every { mockCartRepository.findByUserId(userId) } returns mockCart
                every { mockCartItemTeaService.deleteCartItemTeas(cartItemId) } just Runs
                every { mockCartRepository.save(mockCart) } returns mockCart

                val result = sut.removeCartItem(userId, cartItemId)

                result shouldBe mockCart
                verifyOrder {
                    mockCartRepository.findByUserId(userId)
                    mockCartItemTeaService.deleteCartItemTeas(cartItemId)
                    mockCart.removeItem(cartItemId, userId)
                    mockCartRepository.save(mockCart)
                }
            }
        }

        context("장바구니가 존재하지 않는 경우") {
            it("CartException.CartNotFound를 발생") {
                val userId = 999L
                val cartItemId = 1L

                every { mockCartRepository.findByUserId(userId) } returns null

                shouldThrow<CartException.CartNotFound> {
                    sut.removeCartItem(userId, cartItemId)
                }

                verify(exactly = 1) { mockCartRepository.findByUserId(userId) }
                verify(exactly = 0) { mockCartItemTeaService.deleteCartItemTeas(any()) }
                verify(exactly = 0) { mockCartRepository.save(any()) }
            }
        }
    }

    describe("clearCart") {
        context("정상적인 장바구니 비우기") {
            it("모든 차 구성을 삭제한 후 장바구니를 비우고 저장") {
                val userId = 1L
                val mockCart = mockk<Cart>(relaxed = true)
                val mockCartItem1 = mockk<CartItem> { every { id } returns 1L }
                val mockCartItem2 = mockk<CartItem> { every { id } returns 2L }

                every { mockCartRepository.findByUserId(userId) } returns mockCart
                every { mockCart.items } returns listOf(mockCartItem1, mockCartItem2)
                every { mockCartItemTeaService.deleteCartItemTeas(any()) } just Runs
                every { mockCartRepository.save(mockCart) } returns mockCart

                val result = sut.clearCart(userId)

                result shouldBe mockCart
                verify(exactly = 1) { mockCartRepository.findByUserId(userId) }
                verify(exactly = 1) { mockCartItemTeaService.deleteCartItemTeas(1L) }
                verify(exactly = 1) { mockCartItemTeaService.deleteCartItemTeas(2L) }
                verify(exactly = 1) { mockCart.clear(userId) }
                verify(exactly = 1) { mockCartRepository.save(mockCart) }
            }
        }

        context("장바구니가 존재하지 않는 경우") {
            it("CartException.CartNotFound를 발생") {
                val userId = 999L

                every { mockCartRepository.findByUserId(userId) } returns null

                shouldThrow<CartException.CartNotFound> {
                    sut.clearCart(userId)
                }

                verify(exactly = 1) { mockCartRepository.findByUserId(userId) }
                verify(exactly = 0) { mockCartItemTeaService.deleteCartItemTeas(any()) }
                verify(exactly = 0) { mockCartRepository.save(any()) }
            }
        }
    }

    describe("getCartItemTeas") {
        context("장바구니 아이템 차 구성 조회") {
            it("CartItemTeaService에 조회를 위임하고 결과를 반환") {
                val cartItemId = 1L
                val expectedTeas = listOf(mockk<CartItemTea>(), mockk<CartItemTea>())

                every { mockCartItemTeaService.getCartItemTeas(cartItemId) } returns expectedTeas

                val result = sut.getCartItemTeas(cartItemId)

                result shouldBe expectedTeas
                verify(exactly = 1) { mockCartItemTeaService.getCartItemTeas(cartItemId) }
            }
        }
    }

    describe("getCartByUser") {
        context("사용자의 장바구니 조회") {
            it("Repository에 조회를 지시하고 결과를 반환") {
                val userId = 1L
                val expectedCart = mockk<Cart>()

                every { mockCartRepository.findByUserId(userId) } returns expectedCart

                val result = sut.getCartByUser(userId)

                result shouldBe expectedCart
                verify(exactly = 1) { mockCartRepository.findByUserId(userId) }
            }
        }

        context("장바구니가 없는 사용자 조회") {
            it("null을 반환") {
                val userId = 999L

                every { mockCartRepository.findByUserId(userId) } returns null

                val result = sut.getCartByUser(userId)

                result shouldBe null
                verify(exactly = 1) { mockCartRepository.findByUserId(userId) }
            }
        }
    }
})