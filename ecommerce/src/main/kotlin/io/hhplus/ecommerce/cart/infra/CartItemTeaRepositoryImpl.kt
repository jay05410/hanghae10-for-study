package io.hhplus.ecommerce.cart.infra

import io.hhplus.ecommerce.cart.domain.entity.CartItemTea
import io.hhplus.ecommerce.cart.domain.repository.CartItemTeaRepository
import io.hhplus.ecommerce.cart.infra.persistence.repository.CartItemTeaJpaRepository
import org.springframework.stereotype.Repository

/**
 * CartItemTea Repository JPA 구현체
 *
 * 역할:
 * - 도메인 Repository 인터페이스 구현
 * - JPA Repository를 사용하여 영속성 처리
 */
@Repository
class CartItemTeaRepositoryImpl(
    private val jpaRepository: CartItemTeaJpaRepository
) : CartItemTeaRepository {

    override fun save(cartItemTea: CartItemTea): CartItemTea =
        jpaRepository.save(cartItemTea)

    override fun findById(id: Long): CartItemTea? =
        jpaRepository.findById(id).orElse(null)

    override fun findByCartItemId(cartItemId: Long): List<CartItemTea> =
        jpaRepository.findByCartItemId(cartItemId)

    override fun deleteById(id: Long) {
        jpaRepository.deleteById(id)
    }

    override fun deleteByCartItemId(cartItemId: Long) {
        jpaRepository.deleteByCartItemId(cartItemId)
    }
}