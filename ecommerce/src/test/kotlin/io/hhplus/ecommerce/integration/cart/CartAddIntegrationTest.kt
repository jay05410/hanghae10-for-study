package io.hhplus.ecommerce.integration.cart

import io.hhplus.ecommerce.support.KotestIntegrationTestBase

import io.hhplus.ecommerce.cart.usecase.CartCommandUseCase
import io.hhplus.ecommerce.cart.usecase.GetCartUseCase
import io.hhplus.ecommerce.cart.domain.repository.CartRepository
import io.hhplus.ecommerce.cart.dto.AddToCartRequest
import io.hhplus.ecommerce.product.dto.CreateProductRequest
import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.support.config.IntegrationTestFixtures
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * 장바구니 추가 통합 테스트
 *
 * TestContainers MySQL을 사용하여 장바구니 추가 전체 플로우를 검증합니다.
 * - 장바구니 아이템 추가 성공
 * - 최대 50개 제한 검증 (코드 기준, 정책은 10개)
 * - 동일 상품 중복 방지 검증
 * - 선물 포장 옵션 검증
 */
class CartAddIntegrationTest(
    private val cartCommandUseCase: CartCommandUseCase,
    private val cartRepository: CartRepository,
    private val productCommandUseCase: io.hhplus.ecommerce.product.usecase.ProductCommandUseCase,
    private val inventoryCommandUseCase: io.hhplus.ecommerce.inventory.usecase.InventoryCommandUseCase
) : KotestIntegrationTestBase({

    // 테스트용 상품 ID를 저장할 변수
    lateinit var product1: Product
    lateinit var product2: Product

    beforeEach {
        // 모든 테스트 전에 상품과 재고 생성
        product1 = productCommandUseCase.createProduct(
            CreateProductRequest(
                name = "테스트 상품 1",
                description = "장바구니 테스트용 상품",
                price = 10000L,
                categoryId = 1L,
                createdBy = 0L
            )
        )
        product2 = productCommandUseCase.createProduct(
            CreateProductRequest(
                name = "테스트 상품 2",
                description = "장바구니 테스트용 상품",
                price = 15000L,
                categoryId = 1L,
                createdBy = 0L
            )
        )

        inventoryCommandUseCase.createInventory(product1.id, 1000, 0L)
        inventoryCommandUseCase.createInventory(product2.id, 1000, 0L)
    }

    describe("장바구니 아이템 추가") {
        context("정상적인 추가 요청일 때") {
            it("아이템을 정상적으로 추가할 수 있다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(1)
                val productId = product1.id // 실제 생성한 상품 ID 사용

                // When
                val cart = cartCommandUseCase.addToCart(
                    userId = userId,
                    request = AddToCartRequest(
                        productId = productId,
                        quantity = 2,
                        giftWrap = false,
                        giftMessage = null
                    )
                )

                // Then
                cart shouldNotBe null
                cart.userId shouldBe userId
                cart.items.size shouldBe 1

                val cartItem = cart.items.first()
                cartItem.productId shouldBe productId
                cartItem.quantity shouldBe 2

                // 데이터베이스에서 확인
                val savedCart = cartRepository.findByUserId(userId)
                savedCart shouldNotBe null
                savedCart!!.items.size shouldBe 1
            }
        }

        context("여러 아이템 추가 시") {
            it("여러 아이템을 추가할 수 있다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(2)

                // When - 2개 아이템 추가
                cartCommandUseCase.addToCart(userId, AddToCartRequest(
                    productId = product1.id,
                    quantity = 1,
                    giftWrap = false,
                    giftMessage = null
                ))
                cartCommandUseCase.addToCart(userId, AddToCartRequest(
                    productId = product2.id,
                    quantity = 3,
                    giftWrap = false,
                    giftMessage = null
                ))

                // Then
                val savedCart = cartRepository.findByUserId(userId)
                savedCart shouldNotBe null
                savedCart!!.items.size shouldBe 2
            }
        }

        context("동일한 상품 추가 시") {
            it("기존 아이템 수량이 업데이트된다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(3)
                val productId = product1.id

                // 첫 번째 추가
                cartCommandUseCase.addToCart(userId, AddToCartRequest(
                    productId = productId,
                    quantity = 1,
                    giftWrap = false,
                    giftMessage = null
                ))

                // When - 동일 상품 다시 추가 (수량 업데이트)
                val cart = cartCommandUseCase.addToCart(userId, AddToCartRequest(
                    productId = productId,
                    quantity = 3,
                    giftWrap = true,
                    giftMessage = "선물입니다"
                ))

                // Then - 1개만 있어야 함 (수량 업데이트)
                cart.items.size shouldBe 1
                val item = cart.items.first()
                item.productId shouldBe productId
                item.quantity shouldBe 3
                item.giftWrap shouldBe true
                item.giftMessage shouldBe "선물입니다"
            }
        }

        context("선물 포장 옵션 추가 시") {
            it("선물 포장과 메시지를 포함할 수 있다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(4)
                val giftMessage = "생일 축하합니다!"

                // When
                val cart = cartCommandUseCase.addToCart(userId, AddToCartRequest(
                    productId = product1.id,
                    quantity = 2,
                    giftWrap = true,
                    giftMessage = giftMessage
                ))

                // Then
                val item = cart.items.first()
                item.giftWrap shouldBe true
                item.giftMessage shouldBe giftMessage
                item.productId shouldBe product1.id
                item.quantity shouldBe 2
            }
        }

        context("다른 상품 추가 시") {
            it("여러 상품을 장바구니에 담을 수 있다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(5)

                // When - 두 개의 다른 상품 추가
                cartCommandUseCase.addToCart(userId, AddToCartRequest(
                    productId = product1.id,
                    quantity = 1,
                    giftWrap = false,
                    giftMessage = null
                ))
                val cart = cartCommandUseCase.addToCart(userId, AddToCartRequest(
                    productId = product2.id,
                    quantity = 2,
                    giftWrap = true,
                    giftMessage = "선물"
                ))

                // Then
                cart shouldNotBe null
                cart.items.size shouldBe 2

                val product1Item = cart.items.find { it.productId == product1.id }
                val product2Item = cart.items.find { it.productId == product2.id }

                product1Item shouldNotBe null
                product1Item!!.quantity shouldBe 1

                product2Item shouldNotBe null
                product2Item!!.quantity shouldBe 2
                product2Item.giftWrap shouldBe true
            }
        }

        context("새 사용자의 장바구니 추가 시") {
            it("장바구니가 자동으로 생성된다") {
                // Given
                val newUserId = IntegrationTestFixtures.createTestUserId(999)

                // 장바구니가 없는 상태 확인
                val cartBeforeAdd = cartRepository.findByUserId(newUserId)
                cartBeforeAdd shouldBe null

                // When - 아이템 추가
                cartCommandUseCase.addToCart(newUserId, AddToCartRequest(
                    productId = product1.id,
                    quantity = 1,
                    giftWrap = false,
                    giftMessage = null
                ))

                // Then - 장바구니가 자동 생성되었는지 확인
                val cartAfterAdd = cartRepository.findByUserId(newUserId)
                cartAfterAdd shouldNotBe null
                cartAfterAdd!!.userId shouldBe newUserId
                cartAfterAdd.items.size shouldBe 1
            }
        }
    }

    describe("장바구니 아이템 수정") {
        context("수량 업데이트 시") {
            it("아이템의 수량을 업데이트할 수 있다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(6)
                val cart = cartCommandUseCase.addToCart(userId, AddToCartRequest(
                    productId = product1.id,
                    quantity = 1,
                    giftWrap = false,
                    giftMessage = null
                ))
                val cartItemId = cart.items.first().id

                // When - 수량 업데이트
                val updatedCart = cartCommandUseCase.updateCartItem(userId, cartItemId, 14)

                // Then
                val updatedItem = updatedCart.items.find { it.id == cartItemId }
                updatedItem shouldNotBe null
                updatedItem!!.quantity shouldBe 14
            }
        }
    }

    describe("장바구니 아이템 삭제") {
        context("아이템 삭제 시") {
            it("아이템을 삭제할 수 있다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(7)
                val cart = cartCommandUseCase.addToCart(userId, AddToCartRequest(
                    productId = product1.id,
                    quantity = 1,
                    giftWrap = false,
                    giftMessage = null
                ))
                val cartItemId = cart.items.first().id

                // When - 아이템 삭제
                val updatedCart = cartCommandUseCase.removeCartItem(userId, cartItemId)

                // Then
                updatedCart.items.size shouldBe 0
            }
        }

        context("장바구니 전체 비우기 시") {
            it("모든 아이템을 삭제할 수 있다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(8)

                // 2개 아이템 추가
                cartCommandUseCase.addToCart(userId, AddToCartRequest(productId = product1.id, quantity = 1, giftWrap = false, giftMessage = null))
                cartCommandUseCase.addToCart(userId, AddToCartRequest(productId = product2.id, quantity = 2, giftWrap = false, giftMessage = null))

                // When - 전체 비우기
                cartCommandUseCase.clearCart(userId)

                // Then
                val clearedCart = cartRepository.findByUserId(userId)
                clearedCart shouldNotBe null
                clearedCart!!.items.size shouldBe 0
            }
        }
    }
})
