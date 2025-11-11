package io.hhplus.ecommerce.cart.domain.repository

import io.hhplus.ecommerce.cart.domain.entity.CartItem

interface CartItemRepository {
    fun save(cartItem: CartItem): CartItem
    fun findById(id: Long): CartItem?
    fun findByCartId(cartId: Long): List<CartItem>
    fun findByCartIdAndProductId(cartId: Long, productId: Long): CartItem?
    fun findByCartIdAndProductIdAndBoxTypeId(cartId: Long, productId: Long, boxTypeId: Long): CartItem?
    fun deleteById(id: Long)
    fun deleteByCartId(cartId: Long)
}