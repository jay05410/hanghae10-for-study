package io.hhplus.ecommerce.delivery.infra.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.hhplus.ecommerce.delivery.domain.entity.Delivery
import io.hhplus.ecommerce.delivery.domain.vo.DeliveryAddress
import io.hhplus.ecommerce.delivery.infra.persistence.entity.DeliveryJpaEntity
import org.springframework.stereotype.Component

/**
 * Delivery 도메인 모델 ↔ JPA 엔티티 변환 Mapper
 *
 * 역할:
 * - 도메인 모델과 영속성 모델 간의 변환 담당
 * - 도메인 계층과 인프라 계층 간의 격리 유지
 *
 * 책임:
 * - toDomain: JPA 엔티티 → 도메인 모델
 * - toEntity: 도메인 모델 → JPA 엔티티
 * - JSON 직렬화/역직렬화 (DeliveryAddress)
 */
@Component
class DeliveryMapper(
    private val objectMapper: ObjectMapper
) {
    /**
     * JPA 엔티티를 도메인 모델로 변환
     *
     * @param entity JPA 엔티티
     * @return 도메인 모델
     */
    fun toDomain(entity: DeliveryJpaEntity): Delivery {
        return Delivery(
            id = entity.id,
            orderId = entity.orderId,
            deliveryAddress = deserializeAddress(entity.deliveryAddress),
            trackingNumber = entity.trackingNumber,
            carrier = entity.carrier,
            status = entity.status,
            shippedAt = entity.shippedAt,
            deliveredAt = entity.deliveredAt,
            deliveryMemo = entity.deliveryMemo
        )
    }

    /**
     * 도메인 모델을 JPA 엔티티로 변환
     *
     * @param domain 도메인 모델
     * @return JPA 엔티티
     */
    fun toEntity(domain: Delivery): DeliveryJpaEntity {
        return DeliveryJpaEntity(
            id = domain.id,
            orderId = domain.orderId,
            deliveryAddress = serializeAddress(domain.deliveryAddress),
            trackingNumber = domain.trackingNumber,
            carrier = domain.carrier,
            status = domain.status,
            shippedAt = domain.shippedAt,
            deliveredAt = domain.deliveredAt,
            deliveryMemo = domain.deliveryMemo
        )
    }

    /**
     * DeliveryAddress를 JSON 문자열로 직렬화
     */
    private fun serializeAddress(address: DeliveryAddress): String {
        return objectMapper.writeValueAsString(address)
    }

    /**
     * JSON 문자열을 DeliveryAddress로 역직렬화
     */
    private fun deserializeAddress(json: String): DeliveryAddress {
        return objectMapper.readValue(json)
    }

    /**
     * JPA 엔티티 리스트를 도메인 모델 리스트로 변환
     */
    fun toDomainList(entities: List<DeliveryJpaEntity>): List<Delivery> {
        return entities.map { toDomain(it) }
    }

    /**
     * 도메인 모델 리스트를 JPA 엔티티 리스트로 변환
     */
    fun toEntityList(domains: List<Delivery>): List<DeliveryJpaEntity> {
        return domains.map { toEntity(it) }
    }
}

/**
 * Delivery Mapper Extension Functions
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
fun DeliveryJpaEntity?.toDomain(mapper: DeliveryMapper): Delivery? =
    this?.let { mapper.toDomain(it) }

fun Delivery.toEntity(mapper: DeliveryMapper): DeliveryJpaEntity =
    mapper.toEntity(this)

fun List<DeliveryJpaEntity>.toDomain(mapper: DeliveryMapper): List<Delivery> =
    map { mapper.toDomain(it) }
