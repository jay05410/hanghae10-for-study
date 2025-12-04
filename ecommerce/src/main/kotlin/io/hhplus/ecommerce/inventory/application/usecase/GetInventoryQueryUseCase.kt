package io.hhplus.ecommerce.inventory.application.usecase

import io.hhplus.ecommerce.inventory.domain.entity.Inventory
import io.hhplus.ecommerce.inventory.domain.service.InventoryDomainService
import org.springframework.stereotype.Component

/**
 * 재고 조회 UseCase - 애플리케이션 계층 (Query)
 *
 * 역할:
 * - 재고 관련 조회 작업 처리
 * - CQRS Query 패턴 구현
 *
 * 책임:
 * - 상품별 재고 정보 조회
 * - 재고 가용성 확인
 * - 가용 수량 조회
 */
@Component
class GetInventoryQueryUseCase(
    private val inventoryDomainService: InventoryDomainService
) {

    /**
     * 상품 ID로 재고 조회
     */
    fun getInventory(productId: Long): Inventory? {
        return inventoryDomainService.getInventory(productId)
    }

    /**
     * 재고 가용 여부 확인
     */
    fun checkStockAvailability(productId: Long, requestedQuantity: Int): Boolean {
        return inventoryDomainService.checkStockAvailability(productId, requestedQuantity)
    }

    /**
     * 가용 수량 조회
     */
    fun getAvailableQuantity(productId: Long): Int {
        return inventoryDomainService.getAvailableQuantity(productId)
    }
}
