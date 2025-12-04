package io.hhplus.ecommerce.delivery.infra.persistence.adapter

import io.hhplus.ecommerce.delivery.domain.constant.DeliveryStatus
import io.hhplus.ecommerce.delivery.domain.entity.Delivery
import io.hhplus.ecommerce.delivery.domain.repository.DeliveryRepository
import io.hhplus.ecommerce.delivery.infra.persistence.mapper.DeliveryMapper
import io.hhplus.ecommerce.delivery.infra.persistence.mapper.toDomain
import io.hhplus.ecommerce.delivery.infra.persistence.mapper.toEntity
import io.hhplus.ecommerce.delivery.infra.persistence.repository.DeliveryJpaRepository
import org.springframework.stereotype.Repository

/**
 * Delivery Repository JPA 구현체 - 인프라 계층 (Adapter)
 *
 * 역할:
 * - 도메인 Repository 인터페이스 구현
 * - JPA Repository와 Mapper를 사용하여 영속성 처리
 * - 도메인 모델과 JPA 엔티티 간 변환
 */
@Repository
class DeliveryRepositoryImpl(
    private val jpaRepository: DeliveryJpaRepository,
    private val mapper: DeliveryMapper
) : DeliveryRepository {

    override fun save(delivery: Delivery): Delivery =
        jpaRepository.save(delivery.toEntity(mapper)).toDomain(mapper)!!

    override fun findById(id: Long): Delivery? =
        jpaRepository.findById(id).orElse(null).toDomain(mapper)

    override fun findByOrderId(orderId: Long): Delivery? =
        jpaRepository.findByOrderId(orderId).toDomain(mapper)

    override fun findByOrderIdIn(orderIds: List<Long>): List<Delivery> =
        jpaRepository.findByOrderIdIn(orderIds).toDomain(mapper)

    override fun findByTrackingNumber(trackingNumber: String): Delivery? =
        jpaRepository.findByTrackingNumber(trackingNumber).toDomain(mapper)

    override fun findByStatus(status: DeliveryStatus): List<Delivery> =
        jpaRepository.findByStatus(status).toDomain(mapper)

    override fun findByCarrier(carrier: String): List<Delivery> =
        jpaRepository.findByCarrier(carrier).toDomain(mapper)
}
