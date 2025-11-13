package io.hhplus.ecommerce.inventory.usecase

import io.hhplus.ecommerce.inventory.application.InventoryService
import io.hhplus.ecommerce.inventory.domain.entity.Inventory
import org.springframework.stereotype.Component

/**
 * 재고 조회 UseCase
 *
 * 책임:
 * - 상품별 재고 정보 조회
 * - 재고 가용성 확인
 * - 가용 수량 조회
 */
@Component
class GetInventoryQueryUseCase(
    private val inventoryService: InventoryService
) {

    fun getInventory(productId: Long): Inventory? {
        return inventoryService.getInventory(productId)
    }

    fun checkStockAvailability(productId: Long, requestedQuantity: Int): Boolean {
        return inventoryService.checkStockAvailability(productId, requestedQuantity)
    }

    fun getAvailableQuantity(productId: Long): Int {
        return inventoryService.getAvailableQuantity(productId)
    }
}