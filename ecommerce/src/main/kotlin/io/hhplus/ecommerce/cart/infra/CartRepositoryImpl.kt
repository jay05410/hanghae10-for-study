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
        // 1. 기존 CartItem들 조회 (재조회 없이 데이터베이스에서 직접 조회)
        val existingItemIds = if (cart.id > 0) {
            cartItemJpaRepository.findActiveByCartId(cart.id).map { it.id }.toSet()
        } else {
            emptySet()
        }

        // 2. Cart 엔티티 저장
        val savedCartEntity = cartJpaRepository.save(cart.toEntity(cartMapper))

        // 3. 삭제된 아이템들 제거 (기존에는 있었지만 cart.items에는 없는 것들)
        val currentItemIds = cart.items.map { it.id }.toSet()
        val itemIdsToDelete = existingItemIds - currentItemIds
        if (itemIdsToDelete.isNotEmpty()) {
            cartItemJpaRepository.deleteByIdIn(itemIdsToDelete)
        }

        // 4. CartItem 엔티티들 저장
        val savedItems = cart.items.map { item ->
            val itemEntity = item.toEntity(cartItemMapper)
            cartItemJpaRepository.save(itemEntity).toDomain(cartItemMapper)!!
        }

        // 5. 저장된 Cart와 Items를 조합하여 도메인 모델 반환
        return cartMapper.toDomain(savedCartEntity, savedItems)
    }

    override fun findById(id: Long): Cart? {
        // FETCH JOIN으로 Cart와 CartItem을 함께 조회하여 N+1 문제 방지
        val cartEntity = cartJpaRepository.findCartWithItemsById(id) ?: return null

        // FETCH JOIN으로 가져온 cartItems 직접 활용
        val items = cartEntity.cartItems.toDomain(cartItemMapper)
        return cartMapper.toDomain(cartEntity, items)
    }

    override fun findByUserId(userId: Long): Cart? {
        // FETCH JOIN이 있는 findByUserIdWithItems를 활용하여 N+1 문제 방지
        return findByUserIdWithItems(userId)
    }

    override fun findByUserIdWithItems(userId: Long): Cart? {
        val cartEntity = cartJpaRepository.findByUserIdWithItems(userId) ?: return null

        // FETCH JOIN으로 가져온 cartItems 직접 활용 (N+1 문제 해결)
        val items = cartEntity.cartItems.toDomain(cartItemMapper)

        return cartMapper.toDomain(cartEntity, items)
    }

    @Transactional
    override fun delete(cart: Cart) {
        // Cart는 임시성 데이터이므로 물리 삭제
        deleteById(cart.id)
    }

    @Transactional
    override fun deleteById(id: Long) {
        // Cart는 임시성 데이터이므로 물리 삭제
        // Cart가 존재하는지 확인
        if (!cartJpaRepository.existsById(id)) {
            return
        }

        // 1. 관련 CartItem들 먼저 물리 삭제
        cartItemJpaRepository.deleteByCartId(id)

        // 2. Cart 물리 삭제
        cartJpaRepository.deleteById(id)
    }
}
