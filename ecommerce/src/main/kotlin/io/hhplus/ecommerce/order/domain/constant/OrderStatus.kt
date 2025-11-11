package io.hhplus.ecommerce.order.domain.constant

enum class OrderStatus {
    PENDING, CONFIRMED, COMPLETED, CANCELLED, FAILED;

    fun canBeCancelled(): Boolean = this in listOf(PENDING, CONFIRMED)

    fun isPaid(): Boolean = this in listOf(CONFIRMED, COMPLETED)

    fun isCompleted(): Boolean = this == COMPLETED

    fun canTransitionTo(newStatus: OrderStatus): Boolean {
        val validTransitions = when (this) {
            PENDING -> listOf(CONFIRMED, CANCELLED, FAILED)
            CONFIRMED -> listOf(COMPLETED, CANCELLED)
            COMPLETED -> emptyList()
            CANCELLED -> emptyList()
            FAILED -> emptyList()
        }
        return newStatus in validTransitions
    }
}