package io.hhplus.ecommerce.product.application

import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.product.domain.repository.ProductRepository
import io.hhplus.ecommerce.product.exception.ProductException
import io.hhplus.ecommerce.common.cache.CacheNames
import io.hhplus.ecommerce.common.response.Cursor
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.CacheEvict
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
 *
 * 주의:
 * - Product는 불변 객체이므로 상태 변경 메서드는 새로운 인스턴스를 반환
 * - 반환된 인스턴스를 반드시 save()로 저장해야 함
 */
@Service
class ProductService(
    private val productRepository: ProductRepository
) {

    /**
     * 상품 목록 조회 (커서 기반 페이징) - Redis 캐시 적용
     * TTL: 5분 (상품 목록 일관성 유지)
     */
    @Cacheable(value = [CacheNames.PRODUCT_LIST], key = "'cursor:' + (#lastId ?: 'first') + ':' + #size", cacheManager = "redisCacheManager")
    fun getProductsWithCursor(lastId: Long?, size: Int): Cursor<Product> {
        val products = productRepository.findActiveProductsWithCursor(lastId, size + 1) // 다음 페이지 존재 확인을 위해 +1

        val hasNext = products.size > size
        val contents = if (hasNext) products.take(size) else products
        val nextLastId = if (hasNext && contents.isNotEmpty()) contents.last().id else null

        return Cursor.from(contents, nextLastId)
    }

    /**
     * 상품 상세 조회 - 로컬 캐시 적용
     * TTL: 10분 (빠른 응답, 서버별 독립적 캐시)
     */
    @Cacheable(value = [CacheNames.PRODUCT_DETAIL], key = "#productId")
    fun getProduct(productId: Long): Product {
        return productRepository.findByIdAndIsActive(productId)
            ?: throw ProductException.ProductNotFound(productId)
    }

    /**
     * 상품 생성
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

    /**
     * 상품 정보 업데이트
     *
     * 주의: 불변 객체이므로 updateInfo()의 반환값을 save()로 저장
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
     * 상품 업데이트 (범용)
     */
    fun updateProduct(product: Product): Product {
        return productRepository.save(product)
    }

    /**
     * 상품 품절 처리
     *
     * 주의: 가변 객체이므로 markOutOfStock() 호출 후 저장
     */
    fun markProductOutOfStock(productId: Long): Product {
        val product = getProduct(productId)
        product.markOutOfStock()
        return productRepository.save(product)
    }

    /**
     * 상품 단종 처리
     *
     * 주의: 가변 객체이므로 markDiscontinued() 호출 후 저장
     */
    fun discontinueProduct(productId: Long): Product {
        val product = getProduct(productId)
        product.markDiscontinued()
        return productRepository.save(product)
    }

    /**
     * 상품 숨김 처리
     *
     * 주의: 가변 객체이므로 hide() 호출 후 저장
     */
    fun hideProduct(productId: Long): Product {
        val product = getProduct(productId)
        product.hide()
        return productRepository.save(product)
    }

    /**
     * 상품 복구 처리
     *
     * 주의: 가변 객체이므로 restore() 호출 후 저장
     */
    fun restoreProduct(productId: Long): Product {
        val product = getProduct(productId)
        product.restore()
        return productRepository.save(product)
    }

    /**
     * 카테고리별 상품 조회 (커서 기반 페이징) - Redis 캐시 적용
     * TTL: 5분 (카테고리별 목록 일관성 유지)
     */
    @Cacheable(value = [CacheNames.PRODUCT_CATEGORY_LIST], key = "#categoryId + ':cursor:' + (#lastId ?: 'first') + ':' + #size", cacheManager = "redisCacheManager")
    fun getProductsByCategoryWithCursor(categoryId: Long, lastId: Long?, size: Int): Cursor<Product> {
        val products = productRepository.findCategoryProductsWithCursor(categoryId, lastId, size + 1)

        val hasNext = products.size > size
        val contents = if (hasNext) products.take(size) else products
        val nextLastId = if (hasNext && contents.isNotEmpty()) contents.last().id else null

        return Cursor.from(contents, nextLastId)
    }
}