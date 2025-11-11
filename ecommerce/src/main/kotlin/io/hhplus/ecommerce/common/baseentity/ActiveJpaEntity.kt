package io.hhplus.ecommerce.common.baseentity

// import jakarta.persistence.Column
// import jakarta.persistence.MappedSuperclass

// @MappedSuperclass
abstract class ActiveJpaEntity(
    // @Column(nullable = false)
    var isActive: Boolean = true
) : BaseJpaEntity() {

    open fun activate() {
        this.isActive = true
    }

    open fun deactivate() {
        this.isActive = false
    }

    fun isDeactivated(): Boolean = !isActive
}