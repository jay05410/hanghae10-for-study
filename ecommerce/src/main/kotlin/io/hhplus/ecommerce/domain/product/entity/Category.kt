package io.hhplus.ecommerce.domain.product.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "categories")
class Category(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 50)
    val name: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,

    @Column(nullable = false)
    val displayOrder: Int = 0,

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
    fun isAvailable(): Boolean = isActive

    fun deactivate(updatedBy: Long) {
        this.updatedAt = LocalDateTime.now()
        this.updatedBy = updatedBy
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
                displayOrder = displayOrder,
                createdBy = createdBy,
                updatedBy = createdBy
            )
        }
    }
}