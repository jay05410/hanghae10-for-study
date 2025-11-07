package io.hhplus.ecommerce.product.domain.repository

import io.hhplus.ecommerce.product.domain.constant.ProductStatus
import io.hhplus.ecommerce.product.domain.entity.Product

/**
 * Product Repository Interface - 도메인 계층
 *
 * 책임:
 * - Product 도메인의 영속성 인터페이스 정의
 * - 구현체는 infra 계층에서 담당
 */
interface ProductRepository {
    fun save(product: Product): Product
    fun findById(id: Long): Product?
    fun findByIdAndIsActive(id: Long, isActive: Boolean): Product?
    fun findAllByIsActive(isActive: Boolean): List<Product>
    fun findByStatus(status: ProductStatus): List<Product>
    fun findByCategoryIdAndIsActive(categoryId: Long, isActive: Boolean): List<Product>
    fun findByNameContaining(keyword: String): List<Product>
}