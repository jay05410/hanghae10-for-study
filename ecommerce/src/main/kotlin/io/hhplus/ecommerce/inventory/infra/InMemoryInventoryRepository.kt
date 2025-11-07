package io.hhplus.ecommerce.inventory.infra

import io.hhplus.ecommerce.inventory.domain.entity.Inventory
import io.hhplus.ecommerce.inventory.domain.repository.InventoryRepository
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 재고 리포지토리 인메모리 구현체 - 인프라 계층
 *
 * 역할:
 * - 제품 재고 데이터의 영속화 및 조회 기능 제공
 * - 테스트 환경에서 외부 의존성 제거
 * - 인메모리 저장소를 통한 빠른 데이터 접근
 *
 * 책임:
 * - InventoryRepository 인터페이스 구현
 * - 동시성 안전한 데이터 저장 및 조회
 * - 테스트용 샘플 재고 데이터 초기화
 */
@Repository
class InMemoryInventoryRepository : InventoryRepository {
    private val inventories = ConcurrentHashMap<Long, Inventory>()
    private val idGenerator = AtomicLong(1)

    init {
        initializeSampleData()
    }

    private fun initializeSampleData() {
        val inventory1 = Inventory(
            id = 1L,
            productId = 1L, // 제주 유기농 녹차
            quantity = 500,
            reservedQuantity = 0,
            version = 1
        )
        val inventory2 = Inventory(
            id = 2L,
            productId = 2L, // 전통 우롱차
            quantity = 200,
            reservedQuantity = 10,
            version = 1
        )
        val inventory3 = Inventory(
            id = 3L,
            productId = 3L, // 캐모마일 허브차
            quantity = 300,
            reservedQuantity = 5,
            version = 1
        )

        inventories[1L] = inventory1
        inventories[2L] = inventory2
        inventories[3L] = inventory3

        idGenerator.set(4L)
    }

    /**
     * 재고 정보를 저장하거나 업데이트한다
     *
     * @param inventory 저장할 재고 엔티티
     * @return 저장된 재고 엔티티 (ID가 할당된 상태)
     */
    override fun save(inventory: Inventory): Inventory {
        val savedInventory = if (inventory.id == 0L) {
            Inventory(
                id = idGenerator.getAndIncrement(),
                productId = inventory.productId,
                quantity = inventory.quantity,
                reservedQuantity = inventory.reservedQuantity,
                version = inventory.version
            )
        } else {
            inventory
        }
        inventories[savedInventory.id] = savedInventory
        return savedInventory
    }

    /**
     * 재고 ID로 재고 정보를 조회한다
     *
     * @param id 조회할 재고의 ID
     * @return 재고 엔티티 (존재하지 않을 경우 null)
     */
    override fun findById(id: Long): Inventory? {
        return inventories[id]
    }

    /**
     * 제품 ID로 재고 정보를 조회한다
     *
     * @param productId 조회할 제품의 ID
     * @return 재고 엔티티 (존재하지 않을 경우 null)
     */
    override fun findByProductId(productId: Long): Inventory? {
        return inventories.values.find { it.productId == productId }
    }

    /**
     * 제품 ID로 재고 정보를 업데이트 락과 함께 조회한다
     *
     * @param productId 조회할 제품의 ID
     * @return 재고 엔티티 (존재하지 않을 경우 null)
     */
    override fun findByProductIdWithLock(productId: Long): Inventory? {
        return findByProductId(productId)
    }

    /**
     * 모든 재고 정보를 조회한다
     *
     * @return 모든 재고 엔티티 목록
     */
    override fun findAll(): List<Inventory> {
        return inventories.values.toList()
    }

    /**
     * 재고 정보를 삭제한다
     *
     * @param inventory 삭제할 재고 엔티티
     */
    override fun delete(inventory: Inventory) {
        inventories.remove(inventory.id)
    }

    /**
     * 저장소를 초기화한다 (테스트 전용)
     */
    fun clear() {
        inventories.clear()
        idGenerator.set(1)
        initializeSampleData()
    }
}