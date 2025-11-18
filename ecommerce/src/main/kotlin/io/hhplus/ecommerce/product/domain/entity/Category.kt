package io.hhplus.ecommerce.product.domain.entity

import io.hhplus.ecommerce.common.baseentity.BaseJpaEntity
// import jakarta.persistence.*

// @Entity
// @Table(name = "categories")
class Category(
    // @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // @Column(nullable = false, unique = true, length = 50)
    val name: String,

    // @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,

    // @Column(nullable = false)
    val displayOrder: Int = 0
) : BaseJpaEntity() {
    fun isAvailable(): Boolean = isActive

    fun deactivateCategory() {
        this.deactivate()
    }

    companion object {
        fun create(
            name: String,
            description: String,
            displayOrder: Int = 0,
            createdBy: Long
        ): Category {
            require(name.isNotBlank()) { "카테고리명은 필수입니다" }
            require(description.isNotBlank()) { "카테고리 설명은 필수입니다" }

            return Category(
                name = name,
                description = description,
                displayOrder = displayOrder
            )
        }
    }
}