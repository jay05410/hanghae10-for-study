package io.hhplus.ecommerce.integration.inventory

import io.hhplus.ecommerce.support.KotestIntegrationTestBase

import io.hhplus.ecommerce.common.exception.inventory.InventoryException
import io.hhplus.ecommerce.support.config.IntegrationTestFixtures
import io.hhplus.ecommerce.inventory.application.InventoryService
import io.hhplus.ecommerce.inventory.domain.repository.InventoryRepository
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
    private val inventoryService: InventoryService,
    private val inventoryRepository: InventoryRepository
) : KotestIntegrationTestBase({

    describe("재고 차감") {
        context("정상적인 차감 요청일 때") {
            it("재고를 정상적으로 차감할 수 있다") {
                // Given
                val productId = IntegrationTestFixtures.createTestProductId(1)
                val initialQuantity = 100
                val deductQuantity = 30
                val createdBy = 1L

                // 재고 생성
                inventoryService.createInventory(productId, initialQuantity, createdBy)

                // When
                val updatedInventory = inventoryService.deductStock(productId, deductQuantity, createdBy)

                // Then
                updatedInventory shouldNotBe null
                updatedInventory.quantity shouldBe 70 // 100 - 30
                updatedInventory.reservedQuantity shouldBe 0
                updatedInventory.getAvailableQuantity() shouldBe 70

                // 데이터베이스에서 확인
                val savedInventory = inventoryRepository.findByProductId(productId)
                savedInventory shouldNotBe null
                savedInventory!!.quantity shouldBe 70
            }
        }

        context("전액 차감 시") {
            it("재고를 전액 차감할 수 있다") {
                // Given
                val productId = IntegrationTestFixtures.createTestProductId(2)
                val initialQuantity = 50
                val createdBy = 1L

                inventoryService.createInventory(productId, initialQuantity, createdBy)

                // When - 전액 차감
                val updatedInventory = inventoryService.deductStock(productId, initialQuantity, createdBy)

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
                val createdBy = 1L

                inventoryService.createInventory(productId, initialQuantity, createdBy)

                // When & Then
                shouldThrow<InventoryException.InsufficientStock> {
                    inventoryService.deductStock(productId, deductQuantity, createdBy)
                }

                // 재고가 변경되지 않았는지 확인
                val inventory = inventoryRepository.findByProductId(productId)
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
                val createdBy = 1L

                inventoryService.createInventory(productId, initialQuantity, createdBy)

                // When - 연속 차감
                inventoryService.deductStock(productId, firstDeduct, createdBy)
                inventoryService.deductStock(productId, secondDeduct, createdBy)
                inventoryService.deductStock(productId, thirdDeduct, createdBy)

                // Then
                val finalInventory = inventoryRepository.findByProductId(productId)
                finalInventory shouldNotBe null
                finalInventory!!.quantity shouldBe 40 // 100 - 20 - 30 - 10
            }
        }

        context("예약된 재고가 있을 때") {
            it("가용 재고만 차감할 수 있다") {
                // Given
                val productId = IntegrationTestFixtures.createTestProductId(5)
                val initialQuantity = 100
                val reserveQuantity = 30
                val deductQuantity = 70 // 가용 재고 딱 맞게
                val createdBy = 1L

                // 재고 생성 및 예약
                inventoryService.createInventory(productId, initialQuantity, createdBy)
                inventoryService.reserveStock(productId, reserveQuantity, createdBy)

                // When - 가용 재고만큼 차감
                val updatedInventory = inventoryService.deductStock(productId, deductQuantity, createdBy)

                // Then
                updatedInventory.quantity shouldBe 30 // 100 - 70
                updatedInventory.reservedQuantity shouldBe 30 // 예약은 그대로
                updatedInventory.getAvailableQuantity() shouldBe 0 // 가용 재고 = 30 - 30
            }

            it("가용 재고를 초과하면 예외가 발생한다") {
                // Given
                val productId = IntegrationTestFixtures.createTestProductId(6)
                val initialQuantity = 100
                val reserveQuantity = 40
                val deductQuantity = 80 // 가용 재고(60)보다 많음
                val createdBy = 1L

                // 재고 생성 및 예약
                inventoryService.createInventory(productId, initialQuantity, createdBy)
                inventoryService.reserveStock(productId, reserveQuantity, createdBy)

                // When & Then
                shouldThrow<InventoryException.InsufficientStock> {
                    inventoryService.deductStock(productId, deductQuantity, createdBy)
                }

                // 재고가 변경되지 않았는지 확인
                val inventory = inventoryRepository.findByProductId(productId)
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
                val createdBy = 1L

                // 재고 생성
                inventoryService.createInventory(productId, initialQuantity, createdBy)

                // When - 보충 후 차감
                inventoryService.restockInventory(productId, restockQuantity, createdBy)
                val updatedInventory = inventoryService.deductStock(productId, deductQuantity, createdBy)

                // Then
                updatedInventory.quantity shouldBe 10 // (50 + 30) - 70
            }
        }

        context("존재하지 않는 상품의 재고 차감 시") {
            it("예외가 발생한다") {
                // Given
                val productId = 999999L
                val deductQuantity = 10
                val createdBy = 1L

                // When & Then
                shouldThrow<InventoryException.InventoryNotFound> {
                    inventoryService.deductStock(productId, deductQuantity, createdBy)
                }
            }
        }
    }
})
