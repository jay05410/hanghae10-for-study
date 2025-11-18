package io.hhplus.ecommerce.cart.infra.mapper

import io.hhplus.ecommerce.cart.domain.entity.CartItem
import io.hhplus.ecommerce.cart.infra.persistence.entity.CartItemJpaEntity
import org.springframework.stereotype.Component

/**
 * CartItem 도메인 모델 ↔ JPA 엔티티 변환 Mapper
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
class CartItemMapper {
    /**
     * JPA 엔티티를 도메인 모델로 변환
     *
     * @param entity JPA 엔티티
     * @return 도메인 모델
     */
    fun toDomain(entity: CartItemJpaEntity): CartItem {
        return CartItem(
            id = entity.id,
            cartId = entity.cartId,
            productId = entity.productId,
            quantity = entity.quantity,
            giftWrap = entity.giftWrap,
            giftMessage = entity.giftMessage,
            isActive = !entity.isDeleted(),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            createdBy = entity.createdBy ?: 0,
            updatedBy = entity.updatedBy ?: 0
        )
    }

    /**
     * 도메인 모델을 JPA 엔티티로 변환
     *
     * @param domain 도메인 모델
     * @return JPA 엔티티
     */
    fun toEntity(domain: CartItem): CartItemJpaEntity {
        return CartItemJpaEntity(
            id = domain.id,
            cartId = domain.cartId,
            productId = domain.productId,
            quantity = domain.quantity,
            giftWrap = domain.giftWrap,
            giftMessage = domain.giftMessage
        ).apply {
            if (!domain.isActive) {
                delete()
            }
            createdAt = domain.createdAt
            updatedAt = domain.updatedAt
            createdBy = domain.createdBy
            updatedBy = domain.updatedBy
        }
    }

    /**
     * JPA 엔티티 리스트를 도메인 모델 리스트로 변환
     */
    fun toDomainList(entities: List<CartItemJpaEntity>): List<CartItem> {
        return entities.map { toDomain(it) }
    }

    /**
     * 도메인 모델 리스트를 JPA 엔티티 리스트로 변환
     */
    fun toEntityList(domains: List<CartItem>): List<CartItemJpaEntity> {
        return domains.map { toEntity(it) }
    }
}

/**
 * CartItem Mapper Extension Functions
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
fun CartItemJpaEntity?.toDomain(mapper: CartItemMapper): CartItem? =
    this?.let { mapper.toDomain(it) }

fun CartItem.toEntity(mapper: CartItemMapper): CartItemJpaEntity =
    mapper.toEntity(this)

fun List<CartItemJpaEntity>.toDomain(mapper: CartItemMapper): List<CartItem> =
    map { mapper.toDomain(it) }
