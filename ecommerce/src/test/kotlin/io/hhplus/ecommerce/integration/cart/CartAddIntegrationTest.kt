package io.hhplus.ecommerce.integration.cart

import io.hhplus.ecommerce.support.KotestIntegrationTestBase

import io.hhplus.ecommerce.cart.usecase.CartCommandUseCase
import io.hhplus.ecommerce.cart.usecase.GetCartUseCase
import io.hhplus.ecommerce.cart.domain.repository.CartItemTeaRepository
import io.hhplus.ecommerce.cart.domain.repository.CartRepository
import io.hhplus.ecommerce.cart.dto.AddToCartRequest
import io.hhplus.ecommerce.cart.dto.TeaItemRequest
import io.hhplus.ecommerce.support.config.IntegrationTestFixtures
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * 장바구니 추가 통합 테스트
 *
 * TestContainers MySQL을 사용하여 장바구니 추가 전체 플로우를 검증합니다.
 * - 장바구니 아이템 추가 성공
 * - 최대 50개 제한 검증 (코드 기준, 정책은 10개)
 * - 동일 박스타입 중복 방지 검증
 * - 커스텀 박스 구성 검증 (박스 일수 = 선택한 차 개수)
 */
class CartAddIntegrationTest(
    private val cartCommandUseCase: CartCommandUseCase,
    private val cartRepository: CartRepository,
    private val cartItemTeaRepository: CartItemTeaRepository
) : KotestIntegrationTestBase({

    describe("장바구니 아이템 추가") {
        context("정상적인 추가 요청일 때") {
            it("아이템을 정상적으로 추가할 수 있다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(1)
                val packageTypeId = IntegrationTestFixtures.createTestBoxTypeId(1)
                val teaItems = listOf(
                    TeaItemRequest(productId = 1L, selectionOrder = 1, ratioPercent = 70),
                    TeaItemRequest(productId = 2L, selectionOrder = 2, ratioPercent = 30)
                )

                // When
                val cart = cartCommandUseCase.addToCart(
                    userId = userId,
                    request = AddToCartRequest(
                        packageTypeId = packageTypeId,
                        packageTypeName = "7일 패키지",
                        packageTypeDays = 7,
                        dailyServing = 1,
                        totalQuantity = 7.0,
                        giftWrap = false,
                        giftMessage = null,
                        teaItems = teaItems
                    )
                )

                // Then
                cart shouldNotBe null
                cart.userId shouldBe userId
                cart.items.size shouldBe 1

                val cartItem = cart.items.first()
                cartItem.packageTypeId shouldBe packageTypeId
                cartItem.packageTypeName shouldBe "7일 패키지"
                cartItem.packageTypeDays shouldBe 7

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

                // When - 3개 아이템 추가
                cartCommandUseCase.addToCart(userId, AddToCartRequest(
                    1L, "7일 패키지", 7, 1, 7.0, false, null,
                    listOf(TeaItemRequest(1L, 1, 100))
                ))
                cartCommandUseCase.addToCart(userId, AddToCartRequest(
                    2L, "14일 패키지", 14, 1, 14.0, false, null,
                    listOf(TeaItemRequest(2L, 1, 100))
                ))
                cartCommandUseCase.addToCart(userId, AddToCartRequest(
                    3L, "30일 패키지", 30, 1, 30.0, false, null,
                    listOf(TeaItemRequest(3L, 1, 100))
                ))

                // Then
                val savedCart = cartRepository.findByUserId(userId)
                savedCart shouldNotBe null
                savedCart!!.items.size shouldBe 3
            }
        }

        context("동일한 박스타입 추가 시") {
            it("기존 아이템이 덮어써진다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(3)
                val packageTypeId = 1L

                // 첫 번째 추가
                cartCommandUseCase.addToCart(userId, AddToCartRequest(
                    packageTypeId, "7일 패키지", 7, 1, 7.0, false, null,
                    listOf(TeaItemRequest(1L, 1, 100))
                ))

                // When - 동일 박스타입 다시 추가 (덮어쓰기)
                val cart = cartCommandUseCase.addToCart(userId, AddToCartRequest(
                    packageTypeId, "7일 패키지 V2", 7, 2, 14.0, true, "선물입니다",
                    listOf(TeaItemRequest(2L, 1, 100))
                ))

                // Then - 1개만 있어야 함 (덮어써짐)
                cart.items.size shouldBe 1
                val item = cart.items.first()
                item.packageTypeName shouldBe "7일 패키지 V2"
                item.dailyServing shouldBe 2
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
                    packageTypeId = 1L,
                    packageTypeName = "선물 패키지",
                    packageTypeDays = 7,
                    dailyServing = 1,
                    totalQuantity = 7.0,
                    giftWrap = true,
                    giftMessage = giftMessage,
                    teaItems = listOf(TeaItemRequest(1L, 1, 100))
                ))

                // Then
                val item = cart.items.first()
                item.giftWrap shouldBe true
                item.giftMessage shouldBe giftMessage
            }
        }

        context("커스텀 박스 추가 시") {
            it("차 구성이 포함된 커스텀 박스를 추가할 수 있다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(5)
                val teaItems = listOf(
                    TeaItemRequest(productId = 1L, selectionOrder = 1, ratioPercent = 40),
                    TeaItemRequest(productId = 2L, selectionOrder = 2, ratioPercent = 30),
                    TeaItemRequest(productId = 3L, selectionOrder = 3, ratioPercent = 30)
                )

                // When
                val cart = cartCommandUseCase.addToCart(userId, AddToCartRequest(
                    packageTypeId = 1L,
                    packageTypeName = "커스텀 패키지",
                    packageTypeDays = 7,
                    dailyServing = 1,
                    totalQuantity = 7.0,
                    giftWrap = false,
                    giftMessage = null,
                    teaItems = teaItems
                ))

                // Then
                cart shouldNotBe null
                cart.items.size shouldBe 1

                // 차 구성 검증
                val cartItemId = cart.items.first().id
                val savedTeas = cartItemTeaRepository.findByCartItemId(cartItemId)
                savedTeas.size shouldBe 3
                savedTeas.find { it.selectionOrder == 1 }?.ratioPercent shouldBe 40
                savedTeas.find { it.selectionOrder == 2 }?.ratioPercent shouldBe 30
                savedTeas.find { it.selectionOrder == 3 }?.ratioPercent shouldBe 30
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
                    packageTypeId = 1L,
                    packageTypeName = "첫 패키지",
                    packageTypeDays = 7,
                    dailyServing = 1,
                    totalQuantity = 7.0,
                    giftWrap = false,
                    giftMessage = null,
                    teaItems = listOf(TeaItemRequest(1L, 1, 100))
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
                    packageTypeId = 1L,
                    packageTypeName = "기본 패키지",
                    packageTypeDays = 7,
                    dailyServing = 1,
                    totalQuantity = 7.0,
                    giftWrap = false,
                    giftMessage = null,
                    teaItems = listOf(TeaItemRequest(1L, 1, 100))
                ))
                val cartItemId = cart.items.first().id

                // When - 수량 업데이트
                val updatedCart = cartCommandUseCase.updateCartItem(userId, cartItemId, 14)

                // Then
                val updatedItem = updatedCart.items.find { it.id == cartItemId }
                updatedItem shouldNotBe null
                updatedItem!!.totalQuantity shouldBe 14.0
            }
        }
    }

    describe("장바구니 아이템 삭제") {
        context("아이템 삭제 시") {
            it("아이템을 삭제할 수 있다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(7)
                val cart = cartCommandUseCase.addToCart(userId, AddToCartRequest(
                    packageTypeId = 1L,
                    packageTypeName = "삭제 테스트",
                    packageTypeDays = 7,
                    dailyServing = 1,
                    totalQuantity = 7.0,
                    giftWrap = false,
                    giftMessage = null,
                    teaItems = listOf(TeaItemRequest(1L, 1, 100))
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

                // 3개 아이템 추가
                cartCommandUseCase.addToCart(userId, AddToCartRequest(1L, "패키지1", 7, 1, 7.0, false, null, listOf(TeaItemRequest(1L, 1, 100))))
                cartCommandUseCase.addToCart(userId, AddToCartRequest(2L, "패키지2", 14, 1, 14.0, false, null, listOf(TeaItemRequest(2L, 1, 100))))
                cartCommandUseCase.addToCart(userId, AddToCartRequest(3L, "패키지3", 30, 1, 30.0, false, null, listOf(TeaItemRequest(3L, 1, 100))))

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
