package io.hhplus.ecommerce.cart.infra.persistence.repository

import io.hhplus.ecommerce.cart.domain.entity.CartItemTea
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface CartItemTeaJpaRepository : JpaRepository<CartItemTea, Long> {

    fun findByCartItemId(cartItemId: Long): List<CartItemTea>

    @Modifying
    @Query("DELETE FROM CartItemTea c WHERE c.id = :id")
    override fun deleteById(id: Long)

    @Modifying
    @Query("DELETE FROM CartItemTea c WHERE c.cartItemId = :cartItemId")
    fun deleteByCartItemId(cartItemId: Long)
}