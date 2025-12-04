package io.hhplus.ecommerce.product.domain.service

import io.hhplus.ecommerce.common.response.Cursor
import io.hhplus.ecommerce.product.domain.constant.ProductStatus
import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.product.domain.repository.ProductRepository
import io.hhplus.ecommerce.product.exception.ProductException
import org.springframework.stereotype.Component

/**
 * 상품 도메인 서비스 - 순수 도메인 로직
 *
 * 역할:
 * - 상품 엔티티 생성 및 상태 관리
 * - 상품 조회 및 검증
 * - 상품 정보 업데이트
 *
 * 책임:
 * - 상품 도메인 불변식 보장
 * - Repository(Port)를 통한 영속성 위임
 *
 * 주의:
 * - 트랜잭션 관리는 UseCase에서 담당
 * - 캐시 관리는 UseCase에서 담당
 * - 오케스트레이션은 UseCase에서 담당
 */
@Component
class ProductDomainService(
    private val productRepository: ProductRepository
) {

    // ========== 조회 메서드 ==========

    /**
     * 상품 ID로 조회
     *
     * @param productId 상품 ID
     * @return 상품 엔티티
     * @throws ProductException.ProductNotFound 상품이 없는 경우
     */
    fun getProduct(productId: Long): Product {
        return productRepository.findByIdAndIsActive(productId)
            ?: throw ProductException.ProductNotFound(productId)
    }

    /**
     * 상품 ID로 조회 (nullable)
     *
     * @param productId 상품 ID
     * @return 상품 엔티티 또는 null
     */
    fun getProductOrNull(productId: Long): Product? {
        return productRepository.findByIdAndIsActive(productId)
    }

    /**
     * 여러 상품 조회
     *
     * @param productIds 조회할 상품 ID 목록
     * @return 조회된 상품 목록 (존재하지 않는 ID는 제외)
     */
    fun getProducts(productIds: List<Long>): List<Product> {
        return productIds.mapNotNull { productId ->
            getProductOrNull(productId)
        }
    }

    /**
     * 상품 목록 커서 기반 페이징 조회
     *
     * @param lastId 마지막 상품 ID (커서)
     * @param size 조회할 상품 수
     * @return 커서 기반 상품 목록
     */
    fun getProductsWithCursor(lastId: Long?, size: Int): Cursor<Product> {
        val products = productRepository.findActiveProductsWithCursor(lastId, size + 1)

        val hasNext = products.size > size
        val contents = if (hasNext) products.take(size) else products
        val nextLastId = if (hasNext && contents.isNotEmpty()) contents.last().id else null

        return Cursor.from(contents, nextLastId)
    }

    /**
     * 카테고리별 상품 목록 커서 기반 페이징 조회
     *
     * @param categoryId 카테고리 ID
     * @param lastId 마지막 상품 ID (커서)
     * @param size 조회할 상품 수
     * @return 커서 기반 상품 목록
     */
    fun getProductsByCategoryWithCursor(categoryId: Long, lastId: Long?, size: Int): Cursor<Product> {
        val products = productRepository.findCategoryProductsWithCursor(categoryId, lastId, size + 1)

        val hasNext = products.size > size
        val contents = if (hasNext) products.take(size) else products
        val nextLastId = if (hasNext && contents.isNotEmpty()) contents.last().id else null

        return Cursor.from(contents, nextLastId)
    }

    /**
     * 특정 카테고리의 모든 활성 상품 조회
     *
     * @param categoryId 카테고리 ID
     * @return 활성 상품 목록
     */
    fun getActiveProductsByCategory(categoryId: Long): List<Product> {
        return productRepository.findActiveProductsByCategory(categoryId)
    }

    /**
     * 모든 활성 상품 조회
     *
     * @return 활성 상품 목록
     */
    fun getAllActiveProducts(): List<Product> {
        return productRepository.findAllByIsActive()
    }

    // ========== 생성 메서드 ==========

    /**
     * 상품 생성
     *
     * @param name 상품명
     * @param description 상품 설명
     * @param price 가격
     * @param categoryId 카테고리 ID
     * @return 생성된 상품 엔티티 (저장됨)
     */
    fun createProduct(
        name: String,
        description: String,
        price: Long,
        categoryId: Long
    ): Product {
        val product = Product.create(
            name = name,
            description = description,
            price = price,
            categoryId = categoryId
        )
        return productRepository.save(product)
    }

    // ========== 수정 메서드 ==========

    /**
     * 상품 정보 업데이트
     *
     * @param productId 상품 ID
     * @param name 변경할 상품명
     * @param description 변경할 상품 설명
     * @param price 변경할 가격
     * @return 수정된 상품 엔티티 (저장됨)
     */
    fun updateProductInfo(
        productId: Long,
        name: String,
        description: String,
        price: Long
    ): Product {
        val product = getProduct(productId)
        product.updateInfo(name, description, price)
        return productRepository.save(product)
    }

    /**
     * 상품 저장 (범용)
     *
     * @param product 저장할 상품 엔티티
     * @return 저장된 상품 엔티티
     */
    fun saveProduct(product: Product): Product {
        return productRepository.save(product)
    }

    // ========== 상태 변경 메서드 ==========

    /**
     * 상품 품절 처리
     *
     * @param productId 상품 ID
     * @return 품절 처리된 상품 엔티티
     */
    fun markProductOutOfStock(productId: Long): Product {
        val product = getProduct(productId)
        product.markOutOfStock()
        return productRepository.save(product)
    }

    /**
     * 상품 단종 처리
     *
     * @param productId 상품 ID
     * @return 단종 처리된 상품 엔티티
     */
    fun discontinueProduct(productId: Long): Product {
        val product = getProduct(productId)
        product.markDiscontinued()
        return productRepository.save(product)
    }

    /**
     * 상품 숨김 처리
     *
     * @param productId 상품 ID
     * @return 숨김 처리된 상품 엔티티
     */
    fun hideProduct(productId: Long): Product {
        val product = getProduct(productId)
        product.hide()
        return productRepository.save(product)
    }

    /**
     * 상품 복구 처리
     *
     * @param productId 상품 ID
     * @return 복구된 상품 엔티티
     */
    fun restoreProduct(productId: Long): Product {
        val product = getProduct(productId)
        product.restore()
        return productRepository.save(product)
    }
}
