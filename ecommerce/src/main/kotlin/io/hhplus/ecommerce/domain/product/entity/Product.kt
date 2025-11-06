package io.hhplus.ecommerce.domain.product.entity

import io.hhplus.ecommerce.domain.product.vo.ProductPrice
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "items")
class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val categoryId: Long,

    @Column(nullable = false, length = 100)
    val name: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,

    @Column(nullable = false, length = 20)
    val caffeineType: String,

    @Column(nullable = false, length = 100)
    val tasteProfile: String,

    @Column(nullable = false, length = 100)
    val aromaProfile: String,

    @Column(nullable = false, length = 100)
    val colorProfile: String,

    @Column(nullable = false)
    val bagPerWeight: Int,

    @Column(nullable = false)
    val pricePer100g: Int,

    @Column(nullable = false, columnDefinition = "TEXT")
    val ingredients: String,

    @Column(nullable = false, length = 100)
    val origin: String,

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var status: ProductStatus = ProductStatus.ACTIVE,

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
    fun getPrice(): ProductPrice = ProductPrice(pricePer100g)

    fun isAvailable(): Boolean = status == ProductStatus.ACTIVE && isActive

    fun markOutOfStock(updatedBy: Long) {
        this.status = ProductStatus.OUT_OF_STOCK
        this.updatedAt = LocalDateTime.now()
        this.updatedBy = updatedBy
    }

    fun markDiscontinued(updatedBy: Long) {
        this.status = ProductStatus.DISCONTINUED
        this.updatedAt = LocalDateTime.now()
        this.updatedBy = updatedBy
    }

    fun hide(updatedBy: Long) {
        this.status = ProductStatus.HIDDEN
        this.updatedAt = LocalDateTime.now()
        this.updatedBy = updatedBy
    }

    fun restore(updatedBy: Long) {
        this.status = ProductStatus.ACTIVE
        this.updatedAt = LocalDateTime.now()
        this.updatedBy = updatedBy
    }

    companion object {
        fun create(
            categoryId: Long,
            name: String,
            description: String,
            caffeineType: String,
            tasteProfile: String,
            aromaProfile: String,
            colorProfile: String,
            bagPerWeight: Int,
            pricePer100g: Int,
            ingredients: String,
            origin: String,
            createdBy: Long
        ): Product {
            require(name.isNotBlank()) { "상품명은 필수입니다" }
            require(pricePer100g > 0) { "가격은 0보다 커야 합니다" }
            require(bagPerWeight > 0) { "티백 용량은 0보다 커야 합니다" }

            return Product(
                categoryId = categoryId,
                name = name,
                description = description,
                caffeineType = caffeineType,
                tasteProfile = tasteProfile,
                aromaProfile = aromaProfile,
                colorProfile = colorProfile,
                bagPerWeight = bagPerWeight,
                pricePer100g = pricePer100g,
                ingredients = ingredients,
                origin = origin,
                createdBy = createdBy,
                updatedBy = createdBy
            )
        }
    }
}

enum class ProductStatus {
    ACTIVE, OUT_OF_STOCK, DISCONTINUED, HIDDEN
}