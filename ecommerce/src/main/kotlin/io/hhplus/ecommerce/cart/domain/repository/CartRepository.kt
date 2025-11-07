package io.hhplus.ecommerce.cart.domain.repository

import io.hhplus.ecommerce.cart.domain.entity.Cart

interface CartRepository {
    fun save(cart: Cart): Cart
    fun findById(id: Long): Cart?
    fun findByUserId(userId: Long): Cart?
    fun findByUserIdWithItems(userId: Long): Cart?
    fun delete(cart: Cart)
    fun deleteById(id: Long)
}