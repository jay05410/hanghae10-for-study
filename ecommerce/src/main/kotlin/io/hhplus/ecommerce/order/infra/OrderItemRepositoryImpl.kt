package io.hhplus.ecommerce.order.infra

import io.hhplus.ecommerce.order.domain.entity.OrderItem
import io.hhplus.ecommerce.order.domain.repository.OrderItemRepository
import io.hhplus.ecommerce.order.infra.mapper.OrderItemMapper
import io.hhplus.ecommerce.order.infra.mapper.toDomain
import io.hhplus.ecommerce.order.infra.mapper.toEntity
import io.hhplus.ecommerce.order.infra.persistence.repository.OrderItemJpaRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository

/**
 * OrderItem Repository JPA 구현체
 *
 * 역할:
 * - 도메인 Repository 인터페이스 구현
 * - JPA Repository와 Mapper를 사용하여 영속성 처리
 * - 도메인 모델과 JPA 엔티티 간 변환
 *
 */
@Repository
class OrderItemRepositoryImpl(
    private val jpaRepository: OrderItemJpaRepository,
    private val mapper: OrderItemMapper
) : OrderItemRepository {

    /**
     * OrderItem 저장
     *
     * Dual Mapping Pattern:
     * - orderId만 사용하여 저장 (EntityManager 불필요)
     * - order 참조는 읽기 전용으로 자동 매핑됨
     */
    override fun save(orderItem: OrderItem): OrderItem {
        return jpaRepository.save(orderItem.toEntity(mapper)).toDomain(mapper)!!
    }

    override fun findById(id: Long): OrderItem? =
        jpaRepository.findById(id).orElse(null).toDomain(mapper)

    override fun findByOrderId(orderId: Long): List<OrderItem> =
        jpaRepository.findByOrderId(orderId).toDomain(mapper)

    override fun findByOrderIdAndProductId(orderId: Long, productId: Long): OrderItem? =
        jpaRepository.findByOrderIdAndProductId(orderId, productId).toDomain(mapper)

    override fun findByProductId(productId: Long): List<OrderItem> =
        jpaRepository.findByProductId(productId).toDomain(mapper)

    override fun deleteById(id: Long) {
        jpaRepository.deleteById(id)
    }

    override fun deleteByOrderId(orderId: Long) {
        jpaRepository.deleteByOrderId(orderId)
    }
}
