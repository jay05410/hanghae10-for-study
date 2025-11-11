package io.hhplus.ecommerce.inventory.domain.entity

import io.hhplus.ecommerce.common.baseentity.ActiveJpaEntity
import io.hhplus.ecommerce.common.exception.inventory.InventoryException
//import jakarta.persistence.*

//@Entity
//@Table(name = "inventory")
class Inventory(
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

//    @Column(nullable = false, unique = true)
    val productId: Long,

//    @Column(nullable = false)
    var quantity: Int = 0,

//    @Column(nullable = false)
    var reservedQuantity: Int = 0,

//    @Version
    var version: Int = 0
) : ActiveJpaEntity() {
    fun getAvailableQuantity(): Int = quantity - reservedQuantity

    fun isStockAvailable(requestedQuantity: Int): Boolean = getAvailableQuantity() >= requestedQuantity

    fun deduct(requestedQuantity: Int, deductedBy: Long): Int {
        if (!isStockAvailable(requestedQuantity)) {
            throw InventoryException.InsufficientStock(productId, getAvailableQuantity(), requestedQuantity)
        }

        val oldQuantity = this.quantity
        this.quantity -= requestedQuantity

        return oldQuantity
    }

    fun restock(additionalQuantity: Int, restockedBy: Long): Int {
        require(additionalQuantity > 0) { "추가할 재고 수량은 0보다 커야 합니다" }

        val oldQuantity = this.quantity
        this.quantity += additionalQuantity

        return oldQuantity
    }

    fun reserve(requestedQuantity: Int, reservedBy: Long): Int {
        if (!isStockAvailable(requestedQuantity)) {
            throw InventoryException.InsufficientStock(productId, getAvailableQuantity(), requestedQuantity)
        }

        val oldReservedQuantity = this.reservedQuantity
        this.reservedQuantity += requestedQuantity
        updateAuditInfo(reservedBy)

        return oldReservedQuantity
    }

    fun releaseReservation(releaseQuantity: Int, releasedBy: Long): Int {
        require(releaseQuantity > 0) { "해제할 예약 수량은 0보다 커야 합니다" }
        require(releaseQuantity <= reservedQuantity) { "해제할 수량이 예약된 수량보다 클 수 없습니다" }

        val oldReservedQuantity = this.reservedQuantity
        this.reservedQuantity -= releaseQuantity
        updateAuditInfo(releasedBy)

        return oldReservedQuantity
    }

    fun confirmReservation(confirmQuantity: Int, confirmedBy: Long): Int {
        require(confirmQuantity > 0) { "확정할 예약 수량은 0보다 커야 합니다" }
        require(confirmQuantity <= reservedQuantity) { "확정할 수량이 예약된 수량보다 클 수 없습니다" }

        val oldQuantity = this.quantity
        this.quantity -= confirmQuantity
        this.reservedQuantity -= confirmQuantity
        updateAuditInfo(confirmedBy)

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
                quantity = initialQuantity
            )
        }
    }
}