package io.hhplus.ecommerce.cart.domain.repository

import io.hhplus.ecommerce.cart.domain.entity.CartItemTea

interface CartItemTeaRepository {
    fun save(cartItemTea: CartItemTea): CartItemTea
    fun findById(id: Long): CartItemTea?
    fun findByCartItemId(cartItemId: Long): List<CartItemTea>
    fun deleteById(id: Long)
    fun deleteByCartItemId(cartItemId: Long)
}