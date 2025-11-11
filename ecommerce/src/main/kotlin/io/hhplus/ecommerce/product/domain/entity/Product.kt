package io.hhplus.ecommerce.product.domain.entity

import io.hhplus.ecommerce.common.baseentity.ActiveJpaEntity
import io.hhplus.ecommerce.product.domain.constant.ProductStatus
// import jakarta.persistence.*

// @Entity
// @Table(name = "items")
class Product(
    // @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // @Column(nullable = false)
    val categoryId: Long,

    // @Column(nullable = false, length = 100)
    var name: String,

    // @Column(nullable = false, columnDefinition = "TEXT")
    var description: String,

    // @Column(nullable = false, length = 20)
    val caffeineType: String,

    // @Column(nullable = false, length = 100)
    val tasteProfile: String,

    // @Column(nullable = false, length = 100)
    val aromaProfile: String,

    // @Column(nullable = false, length = 100)
    val colorProfile: String,

    // @Column(nullable = false)
    val bagPerWeight: Int,

    // @Column(nullable = false)
    var pricePer100g: Int,

    // @Column(nullable = false, columnDefinition = "TEXT")
    val ingredients: String,

    // @Column(nullable = false, length = 100)
    val origin: String,

    // @Column(nullable = false, length = 20)
    // @Enumerated(EnumType.STRING)
    var status: ProductStatus = ProductStatus.ACTIVE
) : ActiveJpaEntity() {
    val price: Long get() = pricePer100g.toLong()

    fun isAvailable(): Boolean = status == ProductStatus.ACTIVE && isActive

    fun markOutOfStock(updatedBy: Long) {
        this.status = ProductStatus.OUT_OF_STOCK
    }

    fun markDiscontinued(updatedBy: Long) {
        this.status = ProductStatus.DISCONTINUED
    }

    fun hide(updatedBy: Long) {
        this.status = ProductStatus.HIDDEN
    }

    fun restore(updatedBy: Long) {
        this.status = ProductStatus.ACTIVE
    }

    fun updateInfo(
        name: String,
        description: String,
        price: Long,
        updatedBy: Long
    ) {
        require(name.isNotBlank()) { "상품명은 필수입니다" }
        require(price > 0) { "가격은 0보다 커야 합니다" }

        this.name = name
        this.description = description
        this.pricePer100g = price.toInt()
        this.updateAuditInfo(updatedBy)
    }

    companion object {
        fun create(
            name: String,
            description: String,
            price: Long,
            categoryId: Long,
            createdBy: Long
        ): Product {
            require(name.isNotBlank()) { "상품명은 필수입니다" }
            require(price > 0) { "가격은 0보다 커야 합니다" }

            return Product(
                categoryId = categoryId,
                name = name,
                description = description,
                caffeineType = "MEDIUM", // 기본값
                tasteProfile = "MILD",    // 기본값
                aromaProfile = "FRESH",   // 기본값
                colorProfile = "GOLDEN",  // 기본값
                bagPerWeight = 3,        // 기본값
                pricePer100g = price.toInt(),
                ingredients = "차 잎 100%", // 기본값
                origin = "한국"          // 기본값
            )
        }

        fun createDetailed(
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
                origin = origin
            )
        }
    }
}

