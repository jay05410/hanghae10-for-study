package io.hhplus.ecommerce.inventory.application.usecase

import io.hhplus.ecommerce.common.annotation.DistributedLock
import io.hhplus.ecommerce.common.annotation.DistributedTransaction
import io.hhplus.ecommerce.inventory.domain.entity.Inventory
import io.hhplus.ecommerce.inventory.domain.service.InventoryDomainService
import org.springframework.stereotype.Component

/**
 * 재고 명령 UseCase - 애플리케이션 계층
 *
 * 역할:
 * - 트랜잭션 경계 관리
 * - 분산락을 통한 동시성 제어
 * - 재고 수량 변경 오케스트레이션
 *
 * 책임:
 * - 재고 차감, 보충 기능 제공
 * - InventoryDomainService에 도메인 로직 위임
 *
 * 참고:
 * - 예약 관련 기능은 StockReservationCommandUseCase에서 담당
 */
@Component
class InventoryCommandUseCase(
    private val inventoryDomainService: InventoryDomainService
) {

    /**
     * 재고 차감
     */
    @DistributedLock(key = "'inventory:' + #productId")
    @DistributedTransaction
    fun deductStock(productId: Long, quantity: Int): Inventory {
        return inventoryDomainService.deductStock(productId, quantity)
    }

    /**
     * 재고 보충 (upsert: 없으면 생성, 있으면 추가)
     */
    @DistributedLock(key = "'inventory:' + #productId")
    @DistributedTransaction
    fun restockInventory(productId: Long, quantity: Int): Inventory {
        return inventoryDomainService.restockInventory(productId, quantity)
    }
}
