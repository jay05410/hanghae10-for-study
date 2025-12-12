package io.hhplus.ecommerce.inventory.domain.service

import io.hhplus.ecommerce.inventory.domain.entity.Inventory
import io.hhplus.ecommerce.inventory.domain.repository.InventoryRepository
import io.hhplus.ecommerce.inventory.exception.InventoryException
import org.springframework.stereotype.Component

/**
 * 재고 도메인 서비스 - 순수 도메인 로직
 *
 * 역할:
 * - 재고 엔티티 생성 및 상태 관리
 * - 재고 수량 변경 (차감, 보충, 예약)
 * - 재고 가용성 검증
 *
 * 책임:
 * - 재고 도메인 불변식 보장
 * - Repository(Port)를 통한 영속성 위임
 *
 * 주의:
 * - @Transactional 사용 금지 (UseCase에서 관리)
 * - 오케스트레이션은 UseCase에서 담당
 */
@Component
class InventoryDomainService(
    private val inventoryRepository: InventoryRepository
) {

    /**
     * 상품 ID로 재고 조회
     */
    fun getInventory(productId: Long): Inventory? {
        return inventoryRepository.findByProductId(productId)
    }

    /**
     * 상품 ID로 재고 조회 (없으면 예외)
     */
    fun getInventoryOrThrow(productId: Long): Inventory {
        return inventoryRepository.findByProductId(productId)
            ?: throw InventoryException.InventoryNotFound(productId)
    }

    /**
     * 상품 ID로 재고 조회 (비관적 락 적용)
     */
    fun getInventoryWithLock(productId: Long): Inventory {
        return inventoryRepository.findByProductIdWithLock(productId)
            ?: throw InventoryException.InventoryNotFound(productId)
    }

    /**
     * 새 재고 생성
     */
    fun createInventory(productId: Long, initialQuantity: Int): Inventory {
        val existingInventory = inventoryRepository.findByProductId(productId)
        if (existingInventory != null) {
            throw InventoryException.InventoryAlreadyExists(productId)
        }

        val inventory = Inventory.create(productId, initialQuantity)
        return inventoryRepository.save(inventory)
    }

    /**
     * 재고 차감
     */
    fun deductStock(productId: Long, quantity: Int): Inventory {
        val inventory = getInventoryWithLock(productId)
        inventory.deduct(quantity)
        return inventoryRepository.save(inventory)
    }

    /**
     * 재고 보충 (upsert)
     * 재고가 없으면 생성, 있으면 보충
     */
    fun restockInventory(productId: Long, quantity: Int): Inventory {
        val inventory = inventoryRepository.findByProductIdWithLock(productId)

        return if (inventory != null) {
            inventory.restock(quantity)
            inventoryRepository.save(inventory)
        } else {
            val newInventory = Inventory.create(productId, quantity)
            inventoryRepository.save(newInventory)
        }
    }

    /**
     * 재고 예약
     */
    fun reserveStock(productId: Long, quantity: Int): Inventory {
        val inventory = getInventoryWithLock(productId)
        inventory.reserve(quantity)
        return inventoryRepository.save(inventory)
    }

    /**
     * 예약 해제
     */
    fun releaseReservation(productId: Long, quantity: Int): Inventory {
        val inventory = getInventoryWithLock(productId)
        inventory.releaseReservation(quantity)
        return inventoryRepository.save(inventory)
    }

    /**
     * 예약 확정 (예약된 수량을 실제 차감)
     */
    fun confirmReservation(productId: Long, quantity: Int): Inventory {
        val inventory = getInventoryWithLock(productId)
        inventory.confirmReservation(quantity)
        return inventoryRepository.save(inventory)
    }

    /**
     * 재고 가용 여부 확인
     */
    fun checkStockAvailability(productId: Long, requestedQuantity: Int): Boolean {
        val inventory = inventoryRepository.findByProductId(productId)
            ?: return false
        return inventory.isStockAvailable(requestedQuantity)
    }

    /**
     * 가용 수량 조회
     */
    fun getAvailableQuantity(productId: Long): Int {
        val inventory = inventoryRepository.findByProductId(productId)
            ?: return 0
        return inventory.getAvailableQuantity()
    }
}
