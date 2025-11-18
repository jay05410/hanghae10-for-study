package io.hhplus.ecommerce.order.infra

import io.hhplus.ecommerce.order.domain.constant.OrderStatus
import io.hhplus.ecommerce.order.domain.entity.Order
import io.hhplus.ecommerce.order.domain.repository.OrderRepository
import io.hhplus.ecommerce.order.infra.mapper.OrderMapper
import io.hhplus.ecommerce.order.infra.mapper.toDomain
import io.hhplus.ecommerce.order.infra.mapper.toEntity
import io.hhplus.ecommerce.order.infra.persistence.repository.OrderJpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Order Repository JPA 구현체
 *
 * 역할:
 * - 도메인 Repository 인터페이스 구현
 * - JPA Repository와 Mapper를 사용하여 영속성 처리
 * - 도메인 모델과 JPA 엔티티 간 변환
 *
 */
@Repository
class OrderRepositoryImpl(
    private val jpaRepository: OrderJpaRepository,
    private val mapper: OrderMapper
) : OrderRepository {

    override fun save(order: Order): Order =
        jpaRepository.save(order.toEntity(mapper)).toDomain(mapper)!!

    override fun findById(id: Long): Order? =
        jpaRepository.findById(id).orElse(null).toDomain(mapper)

    override fun findByOrderNumber(orderNumber: String): Order? =
        jpaRepository.findByOrderNumber(orderNumber).toDomain(mapper)

    override fun findByUserId(userId: Long): List<Order> =
        jpaRepository.findByUserId(userId).toDomain(mapper)


    override fun findByUserIdAndStatus(userId: Long, status: OrderStatus): List<Order> =
        jpaRepository.findByUserIdAndStatus(userId, status).toDomain(mapper)

    override fun findByStatus(status: OrderStatus): List<Order> =
        jpaRepository.findByStatus(status).toDomain(mapper)

    override fun findByCreatedAtBetween(startDate: LocalDateTime, endDate: LocalDateTime): List<Order> =
        jpaRepository.findByCreatedAtBetween(startDate, endDate).toDomain(mapper)

    override fun countByUserIdAndStatus(userId: Long, status: OrderStatus): Long =
        jpaRepository.countByUserIdAndStatus(userId, status)

    override fun findOrdersWithItemsByUserId(userId: Long): List<Order> {
        return jpaRepository.findOrdersWithItemsByUserId(userId).toDomain(mapper)
    }

    override fun findOrderWithItemsById(orderId: Long): Order? {
        return jpaRepository.findOrderWithItemsById(orderId).toDomain(mapper)
    }
}
