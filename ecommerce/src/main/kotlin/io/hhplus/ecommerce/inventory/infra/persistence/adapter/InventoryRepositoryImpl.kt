package io.hhplus.ecommerce.inventory.infra.persistence.adapter

import io.hhplus.ecommerce.inventory.domain.entity.Inventory
import io.hhplus.ecommerce.inventory.domain.repository.InventoryRepository
import io.hhplus.ecommerce.inventory.infra.persistence.mapper.InventoryMapper
import io.hhplus.ecommerce.inventory.infra.persistence.mapper.toDomain
import io.hhplus.ecommerce.inventory.infra.persistence.mapper.toEntity
import io.hhplus.ecommerce.inventory.infra.persistence.repository.InventoryJpaRepository
import org.springframework.stereotype.Repository

/**
 * Inventory Repository JPA 구현체 - 인프라 계층
 *
 * 역할:
 * - 도메인 Repository 인터페이스 구현
 * - JPA Repository와 Mapper를 사용하여 영속성 처리
 * - 도메인 모델과 JPA 엔티티 간 변환
 */
@Repository
class InventoryRepositoryImpl(
    private val jpaRepository: InventoryJpaRepository,
    private val mapper: InventoryMapper
) : InventoryRepository {

    override fun save(inventory: Inventory): Inventory =
        jpaRepository.save(inventory.toEntity(mapper)).toDomain(mapper)!!

    override fun findById(id: Long): Inventory? =
        jpaRepository.findById(id).orElse(null).toDomain(mapper)

    override fun findByProductId(productId: Long): Inventory? =
        jpaRepository.findByProductId(productId).toDomain(mapper)

    override fun findByProductIdWithLock(productId: Long): Inventory? =
        jpaRepository.findByProductIdWithLock(productId).toDomain(mapper)

    override fun findAll(): List<Inventory> =
        jpaRepository.findAll().toDomain(mapper)

    override fun delete(inventory: Inventory) {
        jpaRepository.delete(inventory.toEntity(mapper))
    }
}
