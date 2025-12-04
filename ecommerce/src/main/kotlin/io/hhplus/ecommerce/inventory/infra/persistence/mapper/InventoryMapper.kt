package io.hhplus.ecommerce.inventory.infra.persistence.mapper

import io.hhplus.ecommerce.inventory.domain.entity.Inventory
import io.hhplus.ecommerce.inventory.infra.persistence.entity.InventoryJpaEntity
import org.springframework.stereotype.Component

/**
 * Inventory 도메인 모델 ↔ JPA 엔티티 변환 Mapper
 *
 * 역할:
 * - 도메인 모델과 영속성 모델 간의 변환 담당
 * - 도메인 계층과 인프라 계층 간의 격리 유지
 *
 * 책임:
 * - toDomain: JPA 엔티티 → 도메인 모델
 * - toEntity: 도메인 모델 → JPA 엔티티
 */
@Component
class InventoryMapper {
    /**
     * JPA 엔티티를 도메인 모델로 변환
     *
     * @param entity JPA 엔티티
     * @return 도메인 모델
     */
    fun toDomain(entity: InventoryJpaEntity): Inventory {
        return Inventory(
            id = entity.id,
            productId = entity.productId,
            quantity = entity.quantity,
            reservedQuantity = entity.reservedQuantity,
            version = entity.version
        )
    }

    /**
     * 도메인 모델을 JPA 엔티티로 변환
     *
     * @param domain 도메인 모델
     * @return JPA 엔티티
     */
    fun toEntity(domain: Inventory): InventoryJpaEntity {
        return InventoryJpaEntity(
            id = domain.id,
            productId = domain.productId,
            quantity = domain.quantity,
            reservedQuantity = domain.reservedQuantity,
            version = domain.version
        )
    }

    /**
     * JPA 엔티티 리스트를 도메인 모델 리스트로 변환
     */
    fun toDomainList(entities: List<InventoryJpaEntity>): List<Inventory> {
        return entities.map { toDomain(it) }
    }

    /**
     * 도메인 모델 리스트를 JPA 엔티티 리스트로 변환
     */
    fun toEntityList(domains: List<Inventory>): List<InventoryJpaEntity> {
        return domains.map { toEntity(it) }
    }
}

/**
 * Inventory Mapper Extension Functions
 *
 * 역할:
 * - Mapper 호출을 간결하게 만들어 가독성 향상
 * - Nullable 처리를 자동화
 *
 * 사용법:
 * - entity.toDomain(mapper)  // JPA Entity → Domain
 * - domain.toEntity(mapper)   // Domain → JPA Entity
 * - entities.toDomain(mapper) // List 변환
 */
fun InventoryJpaEntity?.toDomain(mapper: InventoryMapper): Inventory? =
    this?.let { mapper.toDomain(it) }

fun Inventory.toEntity(mapper: InventoryMapper): InventoryJpaEntity =
    mapper.toEntity(this)

fun List<InventoryJpaEntity>.toDomain(mapper: InventoryMapper): List<Inventory> =
    map { mapper.toDomain(it) }
