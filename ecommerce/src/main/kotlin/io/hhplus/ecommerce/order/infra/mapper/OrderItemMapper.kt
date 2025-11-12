package io.hhplus.ecommerce.order.infra.mapper

import io.hhplus.ecommerce.order.domain.entity.OrderItem
import io.hhplus.ecommerce.order.infra.persistence.entity.OrderItemJpaEntity
import org.springframework.stereotype.Component

/**
 * OrderItem 도메인 모델 <-> JPA 엔티티 Mapper
 *
 * 역할:
 * - 도메인 모델과 영속성 모델 간의 양방향 변환
 * - 도메인 계층과 인프라 계층의 분리 유지
 */
@Component
class OrderItemMapper {

    /**
     * JPA 엔티티 -> 도메인 모델 변환
     */
    fun toDomain(entity: OrderItemJpaEntity): OrderItem {
        return OrderItem(
            id = entity.id,
            orderId = entity.orderId,
            packageTypeId = entity.packageTypeId,
            packageTypeName = entity.packageTypeName,
            packageTypeDays = entity.packageTypeDays,
            dailyServing = entity.dailyServing,
            totalQuantity = entity.totalQuantity,
            giftWrap = entity.giftWrap,
            giftMessage = entity.giftMessage,
            quantity = entity.quantity,
            containerPrice = entity.containerPrice,
            teaPrice = entity.teaPrice,
            giftWrapPrice = entity.giftWrapPrice,
            totalPrice = entity.totalPrice,
            isActive = entity.isActive,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            createdBy = entity.createdBy ?: 0,
            updatedBy = entity.updatedBy ?: 0,
            deletedAt = entity.deletedAt
        )
    }

    /**
     * 도메인 모델 -> JPA 엔티티 변환
     */
    fun toEntity(domain: OrderItem): OrderItemJpaEntity {
        return OrderItemJpaEntity(
            id = domain.id,
            orderId = domain.orderId,
            packageTypeId = domain.packageTypeId,
            packageTypeName = domain.packageTypeName,
            packageTypeDays = domain.packageTypeDays,
            dailyServing = domain.dailyServing,
            totalQuantity = domain.totalQuantity,
            giftWrap = domain.giftWrap,
            giftMessage = domain.giftMessage,
            quantity = domain.quantity,
            containerPrice = domain.containerPrice,
            teaPrice = domain.teaPrice,
            giftWrapPrice = domain.giftWrapPrice,
            totalPrice = domain.totalPrice
        ).apply {
            isActive = domain.isActive
            createdAt = domain.createdAt
            updatedAt = domain.updatedAt
            createdBy = domain.createdBy
            updatedBy = domain.updatedBy
            deletedAt = domain.deletedAt
        }
    }

    /**
     * JPA 엔티티 리스트 -> 도메인 모델 리스트 변환
     */
    fun toDomainList(entities: List<OrderItemJpaEntity>): List<OrderItem> {
        return entities.map { toDomain(it) }
    }

    /**
     * 도메인 모델 리스트 -> JPA 엔티티 리스트 변환
     */
    fun toEntityList(domains: List<OrderItem>): List<OrderItemJpaEntity> {
        return domains.map { toEntity(it) }
    }
}

/**
 * OrderItem Mapper Extension Functions
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
fun OrderItemJpaEntity?.toDomain(mapper: OrderItemMapper): OrderItem? =
    this?.let { mapper.toDomain(it) }

fun OrderItem.toEntity(mapper: OrderItemMapper): OrderItemJpaEntity =
    mapper.toEntity(this)

fun List<OrderItemJpaEntity>.toDomain(mapper: OrderItemMapper): List<OrderItem> =
    map { mapper.toDomain(it) }
