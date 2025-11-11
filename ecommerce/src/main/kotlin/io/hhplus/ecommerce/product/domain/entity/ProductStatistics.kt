package io.hhplus.ecommerce.product.domain.entity

import io.hhplus.ecommerce.common.baseentity.ActiveJpaEntity
// import jakarta.persistence.*

// @Entity
// @Table(name = "product_statistics")
class ProductStatistics(
    // @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // @Column(nullable = false, unique = true)
    val productId: Long,

    // @Column(nullable = false)
    var viewCount: Long = 0,

    // @Column(nullable = false)
    var salesCount: Long = 0,

    // @Version
    var version: Int = 0
) : ActiveJpaEntity() {

    fun incrementViewCount(incrementedBy: Long): Long {
        val oldViewCount = this.viewCount
        this.viewCount += 1
        updateAuditInfo(incrementedBy)
        return oldViewCount
    }

    fun incrementSalesCount(quantity: Int, incrementedBy: Long): Long {
        val oldSalesCount = this.salesCount
        this.salesCount += quantity
        updateAuditInfo(incrementedBy)
        return oldSalesCount
    }

    fun getPopularityScore(): Long = (viewCount * 0.3 + salesCount * 0.7).toLong()

    companion object {
        fun create(
            productId: Long,
            createdBy: Long
        ): ProductStatistics {
            require(productId > 0) { "상품 ID는 유효해야 합니다" }

            return ProductStatistics(
                productId = productId
            )
        }
    }
}