package io.hhplus.ecommerce.integration.inventory

import io.hhplus.ecommerce.support.KotestIntegrationTestBase

import io.hhplus.ecommerce.inventory.exception.InventoryException
import io.hhplus.ecommerce.support.config.IntegrationTestFixtures
import io.hhplus.ecommerce.inventory.application.usecase.InventoryCommandUseCase
import io.hhplus.ecommerce.inventory.application.usecase.StockReservationCommandUseCase
import io.hhplus.ecommerce.inventory.application.usecase.GetInventoryQueryUseCase
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * 재고 차감 통합 테스트
 *
 * TestContainers MySQL을 사용하여 재고 차감 전체 플로우를 검증합니다.
 * - 재고 차감 성공
 * - 재고 부족 검증
 * - 가용 재고 계산
 */
class InventoryDeductIntegrationTest(
    private val inventoryCommandUseCase: InventoryCommandUseCase,
    private val stockReservationCommandUseCase: StockReservationCommandUseCase,
    private val getInventoryQueryUseCase: GetInventoryQueryUseCase
) : KotestIntegrationTestBase({

    describe("재고 차감") {
        context("정상적인 차감 요청일 때") {
            it("재고를 정상적으로 차감할 수 있다") {
                // Given
                val productId = IntegrationTestFixtures.createTestProductId(1)
                val initialQuantity = 100
                val deductQuantity = 30

                // 재고 생성
                inventoryCommandUseCase.restockInventory(productId, initialQuantity)

                // When
                val updatedInventory = inventoryCommandUseCase.deductStock(productId, deductQuantity)

                // Then
                updatedInventory shouldNotBe null
                updatedInventory.quantity shouldBe 70 // 100 - 30
                updatedInventory.reservedQuantity shouldBe 0
                updatedInventory.getAvailableQuantity() shouldBe 70

                // 데이터베이스에서 확인
                val savedInventory = getInventoryQueryUseCase.getInventory(productId)
                savedInventory shouldNotBe null
                savedInventory!!.quantity shouldBe 70
            }
        }

        context("전액 차감 시") {
            it("재고를 전액 차감할 수 있다") {
                // Given
                val productId = IntegrationTestFixtures.createTestProductId(2)
                val initialQuantity = 50

                inventoryCommandUseCase.restockInventory(productId, initialQuantity)

                // When - 전액 차감
                val updatedInventory = inventoryCommandUseCase.deductStock(productId, initialQuantity)

                // Then
                updatedInventory.quantity shouldBe 0
                updatedInventory.getAvailableQuantity() shouldBe 0
            }
        }

        context("재고가 부족할 때") {
            it("예외가 발생한다") {
                // Given
                val productId = IntegrationTestFixtures.createTestProductId(3)
                val initialQuantity = 20
                val deductQuantity = 30 // 재고보다 많은 수량

                inventoryCommandUseCase.restockInventory(productId, initialQuantity)

                // When & Then
                shouldThrow<InventoryException.InsufficientStock> {
                    inventoryCommandUseCase.deductStock(productId, deductQuantity)
                }

                // 재고가 변경되지 않았는지 확인
                val inventory = getInventoryQueryUseCase.getInventory(productId)
                inventory?.quantity shouldBe 20
            }
        }

        context("연속으로 차감할 때") {
            it("여러 번 재고를 차감할 수 있다") {
                // Given
                val productId = IntegrationTestFixtures.createTestProductId(4)
                val initialQuantity = 100
                val firstDeduct = 20
                val secondDeduct = 30
                val thirdDeduct = 10

                inventoryCommandUseCase.restockInventory(productId, initialQuantity)

                // When - 연속 차감
                inventoryCommandUseCase.deductStock(productId, firstDeduct)
                inventoryCommandUseCase.deductStock(productId, secondDeduct)
                inventoryCommandUseCase.deductStock(productId, thirdDeduct)

                // Then
                val finalInventory = getInventoryQueryUseCase.getInventory(productId)
                finalInventory shouldNotBe null
                finalInventory!!.quantity shouldBe 40 // 100 - 20 - 30 - 10
            }
        }

        context("예약된 재고가 있을 때") {
            it("가용 재고만 차감할 수 있다") {
                // Given
                val productId = IntegrationTestFixtures.createTestProductId(5)
                val userId = IntegrationTestFixtures.createTestUserId(5)
                val initialQuantity = 100
                val reserveQuantity = 30
                val deductQuantity = 70 // 가용 재고 딱 맞게

                // 재고 생성 및 예약
                inventoryCommandUseCase.restockInventory(productId, initialQuantity)
                stockReservationCommandUseCase.reserveStock(productId, userId, reserveQuantity)

                // When - 가용 재고만큼 차감
                val updatedInventory = inventoryCommandUseCase.deductStock(productId, deductQuantity)

                // Then
                updatedInventory.quantity shouldBe 30 // 100 - 70
                updatedInventory.reservedQuantity shouldBe 30 // 예약은 그대로
                updatedInventory.getAvailableQuantity() shouldBe 0 // 가용 재고 = 30 - 30
            }

            it("가용 재고를 초과하면 예외가 발생한다") {
                // Given
                val productId = IntegrationTestFixtures.createTestProductId(6)
                val userId = IntegrationTestFixtures.createTestUserId(6)
                val initialQuantity = 100
                val reserveQuantity = 40
                val deductQuantity = 80 // 가용 재고(60)보다 많음

                // 재고 생성 및 예약
                inventoryCommandUseCase.restockInventory(productId, initialQuantity)
                stockReservationCommandUseCase.reserveStock(productId, userId, reserveQuantity)

                // When & Then
                shouldThrow<InventoryException.InsufficientStock> {
                    inventoryCommandUseCase.deductStock(productId, deductQuantity)
                }

                // 재고가 변경되지 않았는지 확인
                val inventory = getInventoryQueryUseCase.getInventory(productId)
                inventory?.quantity shouldBe 100
                inventory?.reservedQuantity shouldBe 40
            }
        }

        context("재고 보충 후") {
            it("차감이 정상 동작한다") {
                // Given
                val productId = IntegrationTestFixtures.createTestProductId(7)
                val initialQuantity = 50
                val restockQuantity = 30
                val deductQuantity = 70

                // 재고 생성
                inventoryCommandUseCase.restockInventory(productId, initialQuantity)

                // When - 보충 후 차감
                inventoryCommandUseCase.restockInventory(productId, restockQuantity)
                val updatedInventory = inventoryCommandUseCase.deductStock(productId, deductQuantity)

                // Then
                updatedInventory.quantity shouldBe 10 // (50 + 30) - 70
            }
        }

        context("존재하지 않는 상품의 재고 차감 시") {
            it("예외가 발생한다") {
                // Given
                val productId = 999999L
                val deductQuantity = 10

                // When & Then
                shouldThrow<InventoryException.InventoryNotFound> {
                    inventoryCommandUseCase.deductStock(productId, deductQuantity)
                }
            }
        }
    }
})
