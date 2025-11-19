package io.hhplus.ecommerce.inventory.usecase

import io.hhplus.ecommerce.inventory.application.InventoryService
import io.hhplus.ecommerce.inventory.domain.entity.Inventory
import org.springframework.stereotype.Component

/**
 * 재고 명령 UseCase
 *
 * 역할:
 * - 모든 재고 변경 작업을 통합 관리
 * - 재고 생성, 차감, 보충 기능 제공
 * - 비즈니스 로직을 InventoryService에 위임
 *
 * 책임:
 * - 재고 생성/수정/차감 요청 검증 및 실행
 * - 처리 결과 반환
 */
@Component
class InventoryCommandUseCase(
    private val inventoryService: InventoryService
) {

    /**
     * 새 재고를 생성합니다.
     *
     * @param productId 상품 ID
     * @param initialQuantity 초기 재고 수량
     * @param createdBy 생성 요청자 ID
     * @return 생성된 재고 정보
     * @throws InventoryException.InventoryAlreadyExists 이미 재고가 존재하는 경우
     */
    fun createInventory(productId: Long, initialQuantity: Int, createdBy: Long): Inventory {
        return inventoryService.createInventory(productId, initialQuantity)
    }

    /**
     * 재고를 차감합니다.
     *
     * @param productId 상품 ID
     * @param quantity 차감할 수량
     * @param deductedBy 차감 요청자 ID
     * @return 차감 완료된 재고 정보
     * @throws InventoryException.InventoryNotFound 재고를 찾을 수 없는 경우
     * @throws InventoryException.InsufficientStock 재고가 부족한 경우
     */
    fun deductStock(productId: Long, quantity: Int, deductedBy: Long): Inventory {
        return inventoryService.deductStock(productId, quantity)
    }

    /**
     * 재고를 보충합니다.
     *
     * @param productId 상품 ID
     * @param quantity 보충할 수량
     * @param restockedBy 보충 요청자 ID
     * @return 보충 완료된 재고 정보
     * @throws InventoryException.InventoryNotFound 재고를 찾을 수 없는 경우
     */
    fun restockInventory(productId: Long, quantity: Int, restockedBy: Long): Inventory {
        return inventoryService.restockInventory(productId, quantity)
    }
}