package io.hhplus.ecommerce.domain.product.entity

import io.hhplus.ecommerce.common.exception.product.ProductException
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "inventory")
class Inventory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val productId: Long,

    @Column(nullable = false)
    var quantity: Int = 0,

    @Column(nullable = false)
    val reservedQuantity: Int = 0,

    @Version
    var version: Int = 0,

    @Column(nullable = false)
    val isActive: Boolean = true,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val createdBy: Long,

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedBy: Long
) {
    fun getAvailableQuantity(): Int = quantity - reservedQuantity

    fun isStockAvailable(requestedQuantity: Int): Boolean = getAvailableQuantity() >= requestedQuantity

    fun deduct(requestedQuantity: Int, deductedBy: Long): Int {
        if (!isStockAvailable(requestedQuantity)) {
            throw ProductException.InsufficientStock(productId, getAvailableQuantity(), requestedQuantity)
        }

        val oldQuantity = this.quantity
        this.quantity -= requestedQuantity
        this.updatedAt = LocalDateTime.now()
        this.updatedBy = deductedBy

        return oldQuantity
    }

    fun restock(additionalQuantity: Int, restockedBy: Long): Int {
        require(additionalQuantity > 0) { "추가할 재고 수량은 0보다 커야 합니다" }

        val oldQuantity = this.quantity
        this.quantity += additionalQuantity
        this.updatedAt = LocalDateTime.now()
        this.updatedBy = restockedBy

        return oldQuantity
    }

    companion object {
        fun create(
            productId: Long,
            initialQuantity: Int = 0,
            createdBy: Long
        ): Inventory {
            require(productId > 0) { "상품 ID는 유효해야 합니다" }
            require(initialQuantity >= 0) { "초기 재고는 0 이상이어야 합니다" }

            return Inventory(
                productId = productId,
                quantity = initialQuantity,
                createdBy = createdBy,
                updatedBy = createdBy
            )
        }
    }
}