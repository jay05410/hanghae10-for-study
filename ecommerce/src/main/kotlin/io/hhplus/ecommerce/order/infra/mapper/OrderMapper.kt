package io.hhplus.ecommerce.order.infra.mapper

import io.hhplus.ecommerce.order.domain.entity.Order
import io.hhplus.ecommerce.order.infra.persistence.entity.OrderJpaEntity
import org.springframework.stereotype.Component

/**
 * Order 도메인 모델 <-> JPA 엔티티 Mapper
 *
 * 역할:
 * - 도메인 모델과 영속성 모델 간의 양방향 변환
 * - 도메인 계층과 인프라 계층의 분리 유지
 */
@Component
class OrderMapper {

    /**
     * JPA 엔티티 -> 도메인 모델 변환
     */
    fun toDomain(entity: OrderJpaEntity): Order {
        return Order(
            id = entity.id,
            orderNumber = entity.orderNumber,
            userId = entity.userId,
            totalAmount = entity.totalAmount,
            discountAmount = entity.discountAmount,
            finalAmount = entity.finalAmount,
            usedCouponId = entity.usedCouponId,
            status = entity.status,
            isActive = !entity.isDeleted(),
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
    fun toEntity(domain: Order): OrderJpaEntity {
        return OrderJpaEntity(
            id = domain.id,
            orderNumber = domain.orderNumber,
            userId = domain.userId,
            totalAmount = domain.totalAmount,
            discountAmount = domain.discountAmount,
            finalAmount = domain.finalAmount,
            usedCouponId = domain.usedCouponId,
            status = domain.status
        ).apply {
            if (!domain.isActive) { delete() }
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
    fun toDomainList(entities: List<OrderJpaEntity>): List<Order> {
        return entities.map { toDomain(it) }
    }

    /**
     * 도메인 모델 리스트 -> JPA 엔티티 리스트 변환
     */
    fun toEntityList(domains: List<Order>): List<OrderJpaEntity> {
        return domains.map { toEntity(it) }
    }
}

/**
 * Order Mapper Extension Functions
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
fun OrderJpaEntity?.toDomain(mapper: OrderMapper): Order? =
    this?.let { mapper.toDomain(it) }

fun Order.toEntity(mapper: OrderMapper): OrderJpaEntity =
    mapper.toEntity(this)

fun List<OrderJpaEntity>.toDomain(mapper: OrderMapper): List<Order> =
    map { mapper.toDomain(it) }
