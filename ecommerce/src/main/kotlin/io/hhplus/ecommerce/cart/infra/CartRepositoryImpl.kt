package io.hhplus.ecommerce.cart.infra

import io.hhplus.ecommerce.cart.domain.entity.Cart
import io.hhplus.ecommerce.cart.domain.repository.CartRepository
import io.hhplus.ecommerce.cart.infra.mapper.CartItemMapper
import io.hhplus.ecommerce.cart.infra.mapper.CartMapper
import io.hhplus.ecommerce.cart.infra.mapper.toDomain
import io.hhplus.ecommerce.cart.infra.mapper.toEntity
import io.hhplus.ecommerce.cart.infra.persistence.repository.CartItemJpaRepository
import io.hhplus.ecommerce.cart.infra.persistence.repository.CartJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/**
 * Cart Repository JPA 구현체
 *
 * 역할:
 * - 도메인 Repository 인터페이스 구현
 * - JPA Repository와 Mapper를 사용하여 영속성 처리
 * - 도메인 모델과 JPA 엔티티 간 변환
 * - Cart와 CartItem을 별도로 저장/조회
 */
@Repository
class CartRepositoryImpl(
    private val cartJpaRepository: CartJpaRepository,
    private val cartItemJpaRepository: CartItemJpaRepository,
    private val cartMapper: CartMapper,
    private val cartItemMapper: CartItemMapper
) : CartRepository {

    @Transactional
    override fun save(cart: Cart): Cart {
        // 1. Cart 엔티티 저장
        val savedCartEntity = cartJpaRepository.save(cart.toEntity(cartMapper))

        // 2. 기존 CartItem들 조회
        val existingItems = if (cart.id > 0) {
            cartItemJpaRepository.findByCartIdAndIsActive(cart.id, true)
        } else {
            emptyList()
        }

        // 3. 삭제된 아이템들 제거 (기존에는 있었지만 cart.items에는 없는 것들)
        val currentItemIds = cart.items.map { it.id }.toSet()
        val itemsToDelete = existingItems.filter { it.id !in currentItemIds }
        itemsToDelete.forEach { cartItemJpaRepository.deleteById(it.id) }

        // 4. CartItem 엔티티들 저장
        val savedItems = cart.items.map { item ->
            val itemEntity = item.toEntity(cartItemMapper)
            cartItemJpaRepository.save(itemEntity).toDomain(cartItemMapper)!!
        }

        // 5. 저장된 Cart와 Items를 조합하여 도메인 모델 반환
        return cartMapper.toDomain(savedCartEntity, savedItems)
    }

    override fun findById(id: Long): Cart? {
        val cartEntity = cartJpaRepository.findById(id).orElse(null) ?: return null
        val items = cartItemJpaRepository.findByCartIdAndIsActive(id, true).toDomain(cartItemMapper)
        return cartMapper.toDomain(cartEntity, items)
    }

    override fun findByUserId(userId: Long): Cart? {
        val cartEntity = cartJpaRepository.findByUserIdAndIsActive(userId, true) ?: return null
        val items = cartItemJpaRepository.findByCartIdAndIsActive(cartEntity.id, true).toDomain(cartItemMapper)
        return cartMapper.toDomain(cartEntity, items)
    }

    override fun findByUserIdWithItems(userId: Long): Cart? {
        val cartEntity = cartJpaRepository.findByUserIdWithItems(userId) ?: return null
        val items = cartItemJpaRepository.findByCartIdAndIsActive(cartEntity.id, true).toDomain(cartItemMapper)
        return cartMapper.toDomain(cartEntity, items)
    }

    @Transactional
    override fun delete(cart: Cart) {
        // 1. 관련 CartItem들 먼저 삭제
        cartItemJpaRepository.deleteByCartId(cart.id)
        // 2. Cart 삭제
        cartJpaRepository.deleteById(cart.id)
    }

    @Transactional
    override fun deleteById(id: Long) {
        // 1. 관련 CartItem들 먼저 삭제
        cartItemJpaRepository.deleteByCartId(id)
        // 2. Cart 삭제
        cartJpaRepository.deleteById(id)
    }
}
