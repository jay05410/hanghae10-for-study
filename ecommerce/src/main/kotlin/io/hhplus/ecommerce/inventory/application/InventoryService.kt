package io.hhplus.ecommerce.inventory.application

import io.hhplus.ecommerce.inventory.domain.entity.Inventory
import io.hhplus.ecommerce.inventory.domain.repository.InventoryRepository
import io.hhplus.ecommerce.inventory.exception.InventoryException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 재고 관리 서비스 - 애플리케이션 계층
 *
 * 역할:
 * - 상품 재고의 핵심 비즈니스 로직 처리
 * - 재고 수량 변경 및 상태 관리
 * - 재고 예약 및 확정 프로세스 관리
 *
 * 책임:
 * - 재고 생성, 차감, 충당, 예약 관리
 * - 재고 가용성 검증 및 조회
 * - 동시성 제어를 통한 안전한 재고 관리
 */
@Service
class InventoryService(
    private val inventoryRepository: InventoryRepository
) {

    fun getInventory(productId: Long): Inventory? {
        return inventoryRepository.findByProductId(productId)
    }

    @Transactional
    fun createInventory(productId: Long, initialQuantity: Int): Inventory {
        val existingInventory = inventoryRepository.findByProductId(productId)
        if (existingInventory != null) {
            throw InventoryException.InventoryAlreadyExists(productId)
        }

        val inventory = Inventory.create(productId, initialQuantity)
        return inventoryRepository.save(inventory)
    }

    @Transactional
    fun deductStock(productId: Long, quantity: Int): Inventory {
        val inventory = inventoryRepository.findByProductIdWithLock(productId)
            ?: throw InventoryException.InventoryNotFound(productId)

        inventory.deduct(quantity)
        return inventoryRepository.save(inventory)
    }

    @Transactional
    fun restockInventory(productId: Long, quantity: Int): Inventory {
        val inventory = inventoryRepository.findByProductIdWithLock(productId)
            ?: throw InventoryException.InventoryNotFound(productId)

        inventory.restock(quantity)
        return inventoryRepository.save(inventory)
    }

    @Transactional
    fun reserveStock(productId: Long, quantity: Int): Inventory {
        val inventory = inventoryRepository.findByProductIdWithLock(productId)
            ?: throw InventoryException.InventoryNotFound(productId)

        inventory.reserve(quantity)
        return inventoryRepository.save(inventory)
    }

    @Transactional
    fun releaseReservation(productId: Long, quantity: Int): Inventory {
        val inventory = inventoryRepository.findByProductIdWithLock(productId)
            ?: throw InventoryException.InventoryNotFound(productId)

        inventory.releaseReservation(quantity)
        return inventoryRepository.save(inventory)
    }

    @Transactional
    fun confirmReservation(productId: Long, quantity: Int): Inventory {
        val inventory = inventoryRepository.findByProductIdWithLock(productId)
            ?: throw InventoryException.InventoryNotFound(productId)

        inventory.confirmReservation(quantity)
        return inventoryRepository.save(inventory)
    }

    fun checkStockAvailability(productId: Long, requestedQuantity: Int): Boolean {
        val inventory = inventoryRepository.findByProductId(productId)
            ?: return false
        return inventory.isStockAvailable(requestedQuantity)
    }

    fun getAvailableQuantity(productId: Long): Int {
        val inventory = inventoryRepository.findByProductId(productId)
            ?: return 0
        return inventory.getAvailableQuantity()
    }
}