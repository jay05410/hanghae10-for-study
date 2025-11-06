package io.hhplus.ecommerce.domain.cart.entity

import io.hhplus.ecommerce.domain.cart.validator.CartValidator
import io.hhplus.ecommerce.domain.cart.vo.Quantity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "carts")
class Cart(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val userId: Long,

    @Column(nullable = false)
    val isActive: Boolean = true,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val createdBy: Long,

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedBy: Long,

    @OneToMany(mappedBy = "cart", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    private val _items: MutableList<CartItem> = mutableListOf()
) {
    val items: List<CartItem> get() = _items.toList()

    fun addItem(productId: Long, boxTypeId: Long, quantity: Quantity, addedBy: Long): CartItem {
        CartValidator.validateMaxItems(_items.size)
        CartValidator.validateDuplicateBoxType(_items, boxTypeId)

        val cartItem = CartItem.create(
            cart = this,
            productId = productId,
            boxTypeId = boxTypeId,
            quantity = quantity,
            createdBy = addedBy
        )

        _items.add(cartItem)
        this.updatedAt = LocalDateTime.now()
        this.updatedBy = addedBy

        return cartItem
    }

    fun updateItemQuantity(cartItemId: Long, newQuantity: Quantity, updatedBy: Long): CartItem {
        val item = findItem(cartItemId)
        item.updateQuantity(newQuantity, updatedBy)

        this.updatedAt = LocalDateTime.now()
        this.updatedBy = updatedBy

        return item
    }

    fun removeItem(cartItemId: Long, removedBy: Long) {
        val item = findItem(cartItemId)
        _items.remove(item)

        this.updatedAt = LocalDateTime.now()
        this.updatedBy = removedBy
    }

    fun clear(clearedBy: Long) {
        _items.clear()
        this.updatedAt = LocalDateTime.now()
        this.updatedBy = clearedBy
    }

    fun isEmpty(): Boolean = _items.isEmpty()

    fun getTotalItemCount(): Int = _items.size

    private fun findItem(cartItemId: Long): CartItem {
        return _items.find { it.id == cartItemId }
            ?: throw CartException.CartItemNotFound(cartItemId)
    }

    companion object {

        fun create(userId: Long, createdBy: Long): Cart {
            require(userId > 0) { "사용자 ID는 유효해야 합니다" }

            return Cart(
                userId = userId,
                createdBy = createdBy,
                updatedBy = createdBy
            )
        }
    }
}