package io.hhplus.ecommerce.delivery.infra.persistence.repository

import io.hhplus.ecommerce.delivery.domain.constant.DeliveryStatus
import io.hhplus.ecommerce.delivery.infra.persistence.entity.DeliveryJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Delivery Spring Data JPA Repository
 *
 * 역할:
 * - Spring Data JPA를 사용한 데이터베이스 접근
 * - 기본 CRUD 및 쿼리 메서드 제공
 */
@Repository
interface DeliveryJpaRepository : JpaRepository<DeliveryJpaEntity, Long> {
    fun findByOrderId(orderId: Long): DeliveryJpaEntity?
    fun findByOrderIdIn(orderIds: List<Long>): List<DeliveryJpaEntity>
    fun findByTrackingNumber(trackingNumber: String): DeliveryJpaEntity?
    fun findByStatus(status: DeliveryStatus): List<DeliveryJpaEntity>
    fun findByCarrier(carrier: String): List<DeliveryJpaEntity>
}
