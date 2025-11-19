package io.hhplus.ecommerce.unit.cart.application

import io.hhplus.ecommerce.cart.application.CartService
import io.hhplus.ecommerce.cart.domain.entity.Cart
import io.hhplus.ecommerce.cart.domain.entity.CartItem
import io.hhplus.ecommerce.cart.domain.repository.CartRepository
import io.hhplus.ecommerce.cart.exception.CartException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * CartService 단위 테스트
 *
 * 책임: 장바구니 도메인 서비스의 핵심 비즈니스 로직 검증
 * - 장바구니 생성, 아이템 추가/수정/삭제, 장바구니 비우기 기능의 Repository 호출 검증
 * - 도메인 객체와의 상호작용 검증
 *
 * 검증 목표:
 * 1. 각 메서드가 적절한 Repository 메서드를 호출하는가?
 * 2. 도메인 객체의 비즈니스 메서드가 올바르게 호출되는가?
 * 3. 예외 상황에서 적절한 예외가 발생하는가?
 */
class CartServiceTest : DescribeSpec({
    val mockCartRepository = mockk<CartRepository>()
    val sut = CartService(mockCartRepository)

    beforeEach {
        clearMocks(mockCartRepository)
    }

    describe("getOrCreateCart") {
        context("기존 장바구니가 있는 경우") {
            it("Repository에서 조회한 장바구니를 반환") {
                val userId = 1L
                val existingCart = mockk<Cart>()
                every { mockCartRepository.findByUserIdWithItems(userId) } returns existingCart

                val result = sut.getOrCreateCart(userId)

                result shouldBe existingCart
                verify(exactly = 1) { mockCartRepository.findByUserIdWithItems(userId) }
                verify(exactly = 0) { mockCartRepository.save(any()) }
            }
        }

        context("기존 장바구니가 없는 경우") {
            it("새 장바구니를 생성하고 저장한 후 반환") {
                val userId = 1L
                val newCart = mockk<Cart>()

                every { mockCartRepository.findByUserIdWithItems(userId) } returns null
                mockkObject(Cart.Companion)
                every { Cart.create(userId = userId) } returns newCart
                every { mockCartRepository.save(newCart) } returns newCart

                val result = sut.getOrCreateCart(userId)

                result shouldBe newCart
                verify(exactly = 1) { mockCartRepository.findByUserIdWithItems(userId) }
                verify(exactly = 1) { Cart.create(userId = userId) }
                verify(exactly = 1) { mockCartRepository.save(newCart) }
            }
        }
    }

    describe("addToCart") {
        context("새로운 상품 추가") {
            it("새 아이템을 추가하고 장바구니를 저장") {
                val userId = 1L
                val productId = 1L
                val quantity = 2
                val giftWrap = false
                val giftMessage: String? = null
                val mockCart = mockk<Cart>(relaxed = true)
                val mockSavedCart = mockk<Cart>(relaxed = true)
                val realCartItem = CartItem(
                    id = 1L,
                    cartId = 1L,
                    productId = productId,
                    quantity = quantity,
                    giftWrap = giftWrap,
                    giftMessage = giftMessage
                )
                val mockNewCartItem = spyk(realCartItem)

                every { mockCart.items } returns emptyList()
                every { mockCartRepository.findByUserIdWithItems(userId) } returns mockCart
                every { mockCart.addItem(
                    productId = productId,
                    quantity = quantity,
                    giftWrap = giftWrap,
                    giftMessage = giftMessage
                ) } returns mockNewCartItem
                every { mockCartRepository.save(mockCart) } returns mockSavedCart

                val result = sut.addToCart(userId, productId, quantity, giftWrap, giftMessage)

                result shouldBe mockSavedCart
                verify(exactly = 1) { mockCart.addItem(
                    productId = productId,
                    quantity = quantity,
                    giftWrap = giftWrap,
                    giftMessage = giftMessage
                ) }
                verify(exactly = 1) { mockCartRepository.save(mockCart) }
            }
        }

        context("기존 동일 상품이 있는 경우") {
            it("기존 아이템 수량을 업데이트") {
                val userId = 1L
                val productId = 1L
                val quantity = 3
                val giftWrap = false
                val giftMessage: String? = null
                val mockCart = mockk<Cart>(relaxed = true)
                val mockExistingItem = CartItem(
                    id = 1L,
                    cartId = 1L,
                    productId = 1L,
                    quantity = 1,
                    giftWrap = false,
                    giftMessage = null
                )
                val mockSavedCart = mockk<Cart>(relaxed = true)

                every { mockCart.items } returns listOf(mockExistingItem)
                every { mockCartRepository.findByUserIdWithItems(userId) } returns mockCart
                every { mockCart.updateItem(1L, quantity, giftWrap, giftMessage) } just Runs
                every { mockCartRepository.save(mockCart) } returns mockSavedCart

                val result = sut.addToCart(userId, productId, quantity, giftWrap, giftMessage)

                result shouldBe mockSavedCart
                verify(exactly = 1) { mockCart.updateItem(1L, quantity, giftWrap, giftMessage) }
                verify(exactly = 1) { mockCartRepository.save(mockCart) }
            }
        }

        context("다른 상품들이 장바구니에 있는 경우") {
            it("다른 productId인 경우 새 아이템으로 추가") {
                val userId = 1L
                val productId = 2L
                val quantity = 1
                val giftWrap = false
                val giftMessage: String? = null
                val mockCart = mockk<Cart>(relaxed = true)
                val mockSavedCart = mockk<Cart>(relaxed = true)
                val mockExistingItem = CartItem(
                    id = 1L,
                    cartId = 1L,
                    productId = 1L,
                    quantity = 2,
                    giftWrap = false,
                    giftMessage = null
                )
                val realNewCartItem = CartItem(
                    id = 2L,
                    cartId = 1L,
                    productId = productId,
                    quantity = quantity,
                    giftWrap = giftWrap,
                    giftMessage = giftMessage
                )
                val mockNewCartItem = spyk(realNewCartItem)

                every { mockCart.items } returns listOf(mockExistingItem)
                every { mockCartRepository.findByUserIdWithItems(userId) } returns mockCart
                every { mockCart.addItem(
                    productId = productId,
                    quantity = quantity,
                    giftWrap = giftWrap,
                    giftMessage = giftMessage
                ) } returns mockNewCartItem
                every { mockCartRepository.save(mockCart) } returns mockSavedCart

                val result = sut.addToCart(userId, productId, quantity, giftWrap, giftMessage)

                result shouldBe mockSavedCart
                verify(exactly = 1) { mockCart.addItem(
                    productId = productId,
                    quantity = quantity,
                    giftWrap = giftWrap,
                    giftMessage = giftMessage
                ) }
                verify(exactly = 1) { mockCartRepository.save(mockCart) }
            }
        }
    }

    describe("updateCartItem") {
        context("수량을 양수로 변경하는 경우") {
            it("아이템 수량을 업데이트하고 장바구니를 저장") {
                val userId = 1L
                val cartItemId = 1L
                val quantity = 5
                val mockCart = mockk<Cart>(relaxed = true)

                every { mockCartRepository.findByUserIdWithItems(userId) } returns mockCart
                every { mockCart.updateItemQuantity(cartItemId, quantity) } just Runs
                every { mockCartRepository.save(mockCart) } returns mockCart

                val result = sut.updateCartItem(userId, cartItemId, quantity)

                result shouldBe mockCart
                verify(exactly = 1) { mockCartRepository.findByUserIdWithItems(userId) }
                verify(exactly = 1) { mockCart.updateItemQuantity(cartItemId, quantity) }
                verify(exactly = 1) { mockCartRepository.save(mockCart) }
                verify(exactly = 0) { mockCart.removeItem(any()) }
            }
        }

        context("수량을 0 이하로 변경하는 경우") {
            it("아이템을 제거하고 장바구니를 저장") {
                val userId = 1L
                val cartItemId = 1L
                val quantity = 0
                val mockCart = mockk<Cart>(relaxed = true)

                every { mockCartRepository.findByUserIdWithItems(userId) } returns mockCart
                every { mockCart.removeItem(cartItemId) } just Runs
                every { mockCartRepository.save(mockCart) } returns mockCart

                val result = sut.updateCartItem(userId, cartItemId, quantity)

                result shouldBe mockCart
                verify(exactly = 1) { mockCartRepository.findByUserIdWithItems(userId) }
                verify(exactly = 1) { mockCart.removeItem(cartItemId) }
                verify(exactly = 1) { mockCartRepository.save(mockCart) }
                verify(exactly = 0) { mockCart.updateItemQuantity(any(), any()) }
            }
        }

        context("장바구니가 존재하지 않는 경우") {
            it("CartException.CartNotFound를 발생") {
                val userId = 999L
                val cartItemId = 1L
                val quantity = 5

                every { mockCartRepository.findByUserIdWithItems(userId) } returns null

                shouldThrow<CartException.CartNotFound> {
                    sut.updateCartItem(userId, cartItemId, quantity)
                }

                verify(exactly = 1) { mockCartRepository.findByUserIdWithItems(userId) }
                verify(exactly = 0) { mockCartRepository.save(any()) }
            }
        }
    }

    describe("removeCartItem") {
        context("정상적인 아이템 제거") {
            it("아이템을 제거하고 장바구니를 저장") {
                val userId = 1L
                val cartItemId = 1L
                val mockCart = mockk<Cart>(relaxed = true)

                every { mockCartRepository.findByUserIdWithItems(userId) } returns mockCart
                every { mockCart.removeItem(cartItemId) } just Runs
                every { mockCartRepository.save(mockCart) } returns mockCart

                val result = sut.removeCartItem(userId, cartItemId)

                result shouldBe mockCart
                verify(exactly = 1) { mockCartRepository.findByUserIdWithItems(userId) }
                verify(exactly = 1) { mockCart.removeItem(cartItemId) }
                verify(exactly = 1) { mockCartRepository.save(mockCart) }
            }
        }

        context("장바구니가 존재하지 않는 경우") {
            it("CartException.CartNotFound를 발생") {
                val userId = 999L
                val cartItemId = 1L

                every { mockCartRepository.findByUserIdWithItems(userId) } returns null

                shouldThrow<CartException.CartNotFound> {
                    sut.removeCartItem(userId, cartItemId)
                }

                verify(exactly = 1) { mockCartRepository.findByUserIdWithItems(userId) }
                verify(exactly = 0) { mockCartRepository.save(any()) }
            }
        }
    }

    describe("clearCart") {
        context("정상적인 장바구니 비우기") {
            it("장바구니를 비우고 저장") {
                val userId = 1L
                val mockCart = mockk<Cart>(relaxed = true)

                every { mockCartRepository.findByUserIdWithItems(userId) } returns mockCart
                every { mockCart.clear() } just Runs
                every { mockCartRepository.save(mockCart) } returns mockCart

                val result = sut.clearCart(userId)

                result shouldBe mockCart
                verify(exactly = 1) { mockCartRepository.findByUserIdWithItems(userId) }
                verify(exactly = 1) { mockCart.clear() }
                verify(exactly = 1) { mockCartRepository.save(mockCart) }
            }
        }

        context("장바구니가 존재하지 않는 경우") {
            it("CartException.CartNotFound를 발생") {
                val userId = 999L

                every { mockCartRepository.findByUserIdWithItems(userId) } returns null

                shouldThrow<CartException.CartNotFound> {
                    sut.clearCart(userId)
                }

                verify(exactly = 1) { mockCartRepository.findByUserIdWithItems(userId) }
                verify(exactly = 0) { mockCartRepository.save(any()) }
            }
        }
    }

    describe("getCartByUser") {
        context("사용자의 장바구니 조회") {
            it("Repository에 조회를 지시하고 결과를 반환") {
                val userId = 1L
                val expectedCart = mockk<Cart>()

                every { mockCartRepository.findByUserIdWithItems(userId) } returns expectedCart

                val result = sut.getCartByUser(userId)

                result shouldBe expectedCart
                verify(exactly = 1) { mockCartRepository.findByUserIdWithItems(userId) }
            }
        }

        context("장바구니가 없는 사용자 조회") {
            it("null을 반환") {
                val userId = 999L

                every { mockCartRepository.findByUserIdWithItems(userId) } returns null

                val result = sut.getCartByUser(userId)

                result shouldBe null
                verify(exactly = 1) { mockCartRepository.findByUserIdWithItems(userId) }
            }
        }
    }

    describe("removeOrderedItems") {
        context("주문된 상품들이 장바구니에서 제거되는 경우") {
            it("주문된 상품들만 제거하고 장바구니를 저장") {
                val userId = 1L
                val orderedProductIds = listOf(1L, 3L)
                val mockCart = mockk<Cart>(relaxed = true)
                val mockCartItem1 = mockk<CartItem> { every { id } returns 11L; every { productId } returns 1L }
                val mockCartItem2 = mockk<CartItem> { every { id } returns 12L; every { productId } returns 2L }
                val mockCartItem3 = mockk<CartItem> { every { id } returns 13L; every { productId } returns 3L }

                every { mockCartRepository.findByUserIdWithItems(userId) } returns mockCart
                every { mockCart.items } returns listOf(mockCartItem1, mockCartItem2, mockCartItem3)
                every { mockCart.removeItem(11L) } just Runs
                every { mockCart.removeItem(13L) } just Runs
                every { mockCart.isEmpty() } returns false
                every { mockCartRepository.save(mockCart) } returns mockCart

                sut.removeOrderedItems(userId, orderedProductIds)

                verify(exactly = 1) { mockCartRepository.findByUserIdWithItems(userId) }
                verify(exactly = 1) { mockCart.removeItem(11L) }
                verify(exactly = 1) { mockCart.removeItem(13L) }
                verify(exactly = 0) { mockCart.removeItem(12L) }
                verify(exactly = 1) { mockCartRepository.save(mockCart) }
                verify(exactly = 0) { mockCartRepository.delete(any()) }
            }
        }

        context("주문 후 장바구니가 완전히 비워지는 경우") {
            it("장바구니 자체를 물리 삭제") {
                val userId = 1L
                val orderedProductIds = listOf(1L, 2L)
                val mockCart = mockk<Cart>(relaxed = true)
                val mockCartItem1 = mockk<CartItem> { every { id } returns 11L; every { productId } returns 1L }
                val mockCartItem2 = mockk<CartItem> { every { id } returns 12L; every { productId } returns 2L }

                every { mockCartRepository.findByUserIdWithItems(userId) } returns mockCart
                every { mockCart.id } returns 10L
                every { mockCart.items } returns listOf(mockCartItem1, mockCartItem2)
                every { mockCart.removeItem(11L) } just Runs
                every { mockCart.removeItem(12L) } just Runs
                every { mockCart.isEmpty() } returns true
                every { mockCartRepository.deleteById(10L) } just Runs

                sut.removeOrderedItems(userId, orderedProductIds)

                verify(exactly = 1) { mockCartRepository.findByUserIdWithItems(userId) }
                verify(exactly = 1) { mockCart.removeItem(11L) }
                verify(exactly = 1) { mockCart.removeItem(12L) }
                verify(exactly = 1) { mockCartRepository.deleteById(10L) }
                verify(exactly = 0) { mockCartRepository.save(any()) }
            }
        }

        context("장바구니가 존재하지 않는 경우") {
            it("아무 작업도 하지 않고 정상 종료") {
                val userId = 999L
                val orderedProductIds = listOf(1L, 2L)

                every { mockCartRepository.findByUserIdWithItems(userId) } returns null

                sut.removeOrderedItems(userId, orderedProductIds)

                verify(exactly = 1) { mockCartRepository.findByUserIdWithItems(userId) }
                verify(exactly = 0) { mockCartRepository.save(any()) }
                verify(exactly = 0) { mockCartRepository.delete(any()) }
            }
        }
    }

    describe("deleteCart (완전 삭제)") {
        context("장바구니 전체 삭제") {
            it("장바구니를 물리 삭제") {
                val userId = 1L
                val mockCart = mockk<Cart>()

                every { mockCartRepository.findByUserIdWithItems(userId) } returns mockCart
                every { mockCart.id } returns 10L
                every { mockCartRepository.deleteById(10L) } just Runs

                sut.deleteCart(userId)

                verify(exactly = 1) { mockCartRepository.findByUserIdWithItems(userId) }
                verify(exactly = 1) { mockCartRepository.deleteById(10L) }
            }
        }

        context("장바구니가 존재하지 않는 경우") {
            it("CartException.CartNotFound를 발생") {
                val userId = 999L

                every { mockCartRepository.findByUserIdWithItems(userId) } returns null

                shouldThrow<CartException.CartNotFound> {
                    sut.deleteCart(userId)
                }

                verify(exactly = 1) { mockCartRepository.findByUserIdWithItems(userId) }
                verify(exactly = 0) { mockCartRepository.delete(any()) }
            }
        }
    }
})