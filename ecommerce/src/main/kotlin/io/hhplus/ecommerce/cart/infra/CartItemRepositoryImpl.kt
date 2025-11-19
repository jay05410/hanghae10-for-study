package io.hhplus.ecommerce.cart.infra

import io.hhplus.ecommerce.cart.domain.entity.CartItem
import io.hhplus.ecommerce.cart.domain.repository.CartItemRepository
import io.hhplus.ecommerce.cart.infra.mapper.CartItemMapper
import io.hhplus.ecommerce.cart.infra.mapper.toDomain
import io.hhplus.ecommerce.cart.infra.mapper.toEntity
import io.hhplus.ecommerce.cart.infra.persistence.repository.CartItemJpaRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository

/**
 * CartItem Repository JPA 구현체
 *
 * 역할:
 * - 도메인 Repository 인터페이스 구현
 * - JPA Repository와 Mapper를 사용하여 영속성 처리
 * - 도메인 모델과 JPA 엔티티 간 변환
 *
 */
@Repository
class CartItemRepositoryImpl(
    private val jpaRepository: CartItemJpaRepository,
    private val mapper: CartItemMapper
) : CartItemRepository {

    /**
     * CartItem 저장
     */
    override fun save(cartItem: CartItem): CartItem {
        return jpaRepository.save(cartItem.toEntity(mapper)).toDomain(mapper)!!
    }

    override fun findById(id: Long): CartItem? =
        jpaRepository.findById(id).orElse(null).toDomain(mapper)

    override fun findByCartId(cartId: Long): List<CartItem> =
        jpaRepository.findActiveByCartId(cartId).toDomain(mapper)

    override fun findByCartIdAndProductId(cartId: Long, productId: Long): CartItem? =
        jpaRepository.findByCartIdAndProductId(cartId, productId).toDomain(mapper)

    override fun deleteById(id: Long) {
        jpaRepository.deleteById(id)
    }

    override fun deleteByCartId(cartId: Long) {
        jpaRepository.deleteByCartId(cartId)
    }
}
