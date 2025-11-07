package io.hhplus.ecommerce.product.application

import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.product.domain.repository.ProductRepository
import io.hhplus.ecommerce.common.exception.product.ProductException
import org.springframework.stereotype.Service

/**
 * 상품 서비스 - 애플리케이션 계층
 *
 * 역할:
 * - 상품 도메인의 핵심 비즈니스 로직 처리
 * - 상품 생명주기 관리 및 상태 변경
 * - 상품 조회 및 검색 기능 제공
 *
 * 책임:
 * - 상품 생성, 수정, 조회, 삭제 로직
 * - 카테고리별 상품 관리
 * - 상품 활성화 상태 관리
 */
@Service
class ProductService(
    private val productRepository: ProductRepository
) {

    /**
     * 상품 목록 조회 (페이징)
     */
    fun getProducts(page: Int): List<Product> {
        // TODO: 실제 페이징 로직 구현
        return productRepository.findAllByIsActive(true)
    }

    fun getProduct(productId: Long): Product {
        return productRepository.findByIdAndIsActive(productId, true)
            ?: throw ProductException.ProductNotFound(productId)
    }

    /**
     * 상품 생성
     */
    fun createProduct(
        name: String,
        description: String,
        price: Long,
        categoryId: Long,
        createdBy: Long
    ): Product {
        val product = Product.create(
            name = name,
            description = description,
            price = price,
            categoryId = categoryId,
            createdBy = createdBy
        )

        return productRepository.save(product)
    }

    /**
     * 상품 업데이트
     */
    fun updateProduct(product: Product): Product {
        return productRepository.save(product)
    }

    /**
     * 카테고리별 상품 조회
     */
    fun getProductsByCategory(categoryId: Long): List<Product> {
        return productRepository.findByCategoryIdAndIsActive(categoryId, true)
    }


}