package io.hhplus.ecommerce.cart.infra.mapper

import io.hhplus.ecommerce.cart.domain.entity.Cart
import io.hhplus.ecommerce.cart.infra.persistence.entity.CartJpaEntity
import org.springframework.stereotype.Component

/**
 * Cart 도메인 모델 ↔ JPA 엔티티 변환 Mapper
 *
 * 역할:
 * - 도메인 모델과 영속성 모델 간의 변환 담당
 * - 도메인 계층과 인프라 계층 간의 격리 유지
 *
 * 책임:
 * - toDomain: JPA 엔티티 → 도메인 모델
 * - toEntity: 도메인 모델 → JPA 엔티티
 *
 * 주의: Cart와 CartItem은 별도로 관리됩니다.
 *       items는 CartItemRepository를 통해 별도로 조회/저장합니다.
 */
@Component
class CartMapper {
    /**
     * JPA 엔티티를 도메인 모델로 변환
     *
     * @param entity JPA 엔티티
     * @param items 장바구니 아이템 리스트 (별도 조회 필요)
     * @return 도메인 모델
     */
    fun toDomain(entity: CartJpaEntity, items: List<io.hhplus.ecommerce.cart.domain.entity.CartItem> = emptyList()): Cart {
        return Cart(
            id = entity.id,
            userId = entity.userId,
            _items = items.toMutableList()
        )
    }

    /**
     * 도메인 모델을 JPA 엔티티로 변환
     *
     * @param domain 도메인 모델
     * @return JPA 엔티티
     */
    fun toEntity(domain: Cart): CartJpaEntity {
        return CartJpaEntity(
            id = domain.id,
            userId = domain.userId
        )
    }

    /**
     * JPA 엔티티 리스트를 도메인 모델 리스트로 변환
     */
    fun toDomainList(entities: List<CartJpaEntity>): List<Cart> {
        return entities.map { toDomain(it) }
    }

    /**
     * 도메인 모델 리스트를 JPA 엔티티 리스트로 변환
     */
    fun toEntityList(domains: List<Cart>): List<CartJpaEntity> {
        return domains.map { toEntity(it) }
    }
}

/**
 * Cart Mapper Extension Functions
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
fun CartJpaEntity?.toDomain(mapper: CartMapper, items: List<io.hhplus.ecommerce.cart.domain.entity.CartItem> = emptyList()): Cart? =
    this?.let { mapper.toDomain(it, items) }

fun Cart.toEntity(mapper: CartMapper): CartJpaEntity =
    mapper.toEntity(this)

fun List<CartJpaEntity>.toDomain(mapper: CartMapper): List<Cart> =
    map { mapper.toDomain(it) }
