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
 * - 재고 변경 작업 오케스트레이션
 *
 * 책임:
 * - 재고 생성, 차감, 보충 기능 제공
 * - 재고 예약 확정/해제 기능 제공
 * - InventoryDomainService에 도메인 로직 위임
 */
@Component
class InventoryCommandUseCase(
    private val inventoryDomainService: InventoryDomainService
) {

    /**
     * 새 재고를 생성합니다.
     */
    @DistributedTransaction
    fun createInventory(productId: Long, initialQuantity: Int): Inventory {
        return inventoryDomainService.createInventory(productId, initialQuantity)
    }

    /**
     * 재고를 차감합니다.
     */
    @DistributedLock(key = "'inventory:' + #productId")
    @DistributedTransaction
    fun deductStock(productId: Long, quantity: Int): Inventory {
        return inventoryDomainService.deductStock(productId, quantity)
    }

    /**
     * 재고를 보충합니다.
     */
    @DistributedLock(key = "'inventory:' + #productId")
    @DistributedTransaction
    fun restockInventory(productId: Long, quantity: Int): Inventory {
        return inventoryDomainService.restockInventory(productId, quantity)
    }

    /**
     * 재고를 예약합니다.
     */
    @DistributedLock(key = "'inventory:' + #productId")
    @DistributedTransaction
    fun reserveStock(productId: Long, quantity: Int): Inventory {
        return inventoryDomainService.reserveStock(productId, quantity)
    }

    /**
     * 예약을 해제합니다.
     */
    @DistributedLock(key = "'inventory:' + #productId")
    @DistributedTransaction
    fun releaseReservation(productId: Long, quantity: Int): Inventory {
        return inventoryDomainService.releaseReservation(productId, quantity)
    }

    /**
     * 예약을 확정합니다 (예약 수량을 실제 차감).
     */
    @DistributedLock(key = "'inventory:' + #productId")
    @DistributedTransaction
    fun confirmReservation(productId: Long, quantity: Int): Inventory {
        return inventoryDomainService.confirmReservation(productId, quantity)
    }
}
