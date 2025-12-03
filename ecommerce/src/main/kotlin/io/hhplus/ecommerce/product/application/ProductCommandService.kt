package io.hhplus.ecommerce.product.application

import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.product.domain.repository.ProductRepository
import io.hhplus.ecommerce.common.cache.CacheNames
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Caching
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 상품 명령 서비스 - CQRS Command 담당
 *
 * 역할:
 * - 모든 상품 변경 로직 담당 (생성, 수정, 상태 변경)
 * - 쓰기 작업만 수행
 * - 변경 시 관련 캐시 무효화 처리
 *
 * 책임:
 * - 상품 생성 및 수정
 * - 상품 상태 변경 (품절, 단종, 숨김, 복구)
 * - 캐시 무효화 관리
 */
@Service
class ProductCommandService(
    private val productRepository: ProductRepository,
    private val productQueryService: ProductQueryService
) {

    /**
     * 상품 생성
     */
    @Transactional
    @Caching(evict = [
        CacheEvict(value = [CacheNames.PRODUCT_LIST], allEntries = true, cacheManager = "redisCacheManager"),
        CacheEvict(value = [CacheNames.PRODUCT_POPULAR], allEntries = true, cacheManager = "redisCacheManager")
    ])
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

        val savedProduct = productRepository.save(product)

        // 카테고리별 캐시도 무효화
        evictCategoryCache(categoryId)

        return savedProduct
    }

    /**
     * 상품 정보 업데이트
     *
     * 주의: 불변 객체이므로 updateInfo()의 반환값을 save()로 저장
     */
    @Transactional
    @Caching(evict = [
        CacheEvict(value = [CacheNames.PRODUCT_LIST], allEntries = true, cacheManager = "redisCacheManager"),
        CacheEvict(value = [CacheNames.PRODUCT_POPULAR], allEntries = true, cacheManager = "redisCacheManager")
    ])
    fun updateProductInfo(
        productId: Long,
        name: String,
        description: String,
        price: Long
    ): Product {
        val product = productQueryService.getProduct(productId) // 외부 서비스 호출로 캐시 작동
        product.updateInfo(name, description, price)
        val savedProduct = productRepository.save(product)

        // 관련 캐시 무효화
        evictProductDetailCache(productId)
        evictCategoryCache(product.categoryId)

        return savedProduct
    }

    /**
     * 상품 업데이트 (범용)
     */
    @Transactional
    fun updateProduct(product: Product): Product {
        val savedProduct = productRepository.save(product)

        // 관련 캐시 무효화
        evictProductDetailCache(product.id)
        evictCategoryCache(product.categoryId)
        evictListCaches()

        return savedProduct
    }

    /**
     * 상품 품절 처리
     *
     * 주의: 가변 객체이므로 markOutOfStock() 호출 후 저장
     */
    @Transactional
    fun markProductOutOfStock(productId: Long): Product {
        val product = productQueryService.getProduct(productId) // 외부 서비스 호출로 캐시 작동
        product.markOutOfStock()
        val savedProduct = productRepository.save(product)

        // 관련 캐시 무효화
        evictProductDetailCache(productId)
        evictCategoryCache(product.categoryId)
        evictListCaches()

        return savedProduct
    }

    /**
     * 상품 단종 처리
     *
     * 주의: 가변 객체이므로 markDiscontinued() 호출 후 저장
     */
    @Transactional
    fun discontinueProduct(productId: Long): Product {
        val product = productQueryService.getProduct(productId) // 외부 서비스 호출로 캐시 작동
        product.markDiscontinued()
        val savedProduct = productRepository.save(product)

        // 관련 캐시 무효화
        evictProductDetailCache(productId)
        evictCategoryCache(product.categoryId)
        evictListCaches()

        return savedProduct
    }

    /**
     * 상품 숨김 처리
     *
     * 주의: 가변 객체이므로 hide() 호출 후 저장
     */
    @Transactional
    fun hideProduct(productId: Long): Product {
        val product = productQueryService.getProduct(productId) // 외부 서비스 호출로 캐시 작동
        product.hide()
        val savedProduct = productRepository.save(product)

        // 관련 캐시 무효화
        evictProductDetailCache(productId)
        evictCategoryCache(product.categoryId)
        evictListCaches()

        return savedProduct
    }

    /**
     * 상품 복구 처리
     *
     * 주의: 가변 객체이므로 restore() 호출 후 저장
     */
    @Transactional
    fun restoreProduct(productId: Long): Product {
        val product = productQueryService.getProduct(productId) // 외부 서비스 호출로 캐시 작동
        product.restore()
        val savedProduct = productRepository.save(product)

        // 관련 캐시 무효화
        evictProductDetailCache(productId)
        evictCategoryCache(product.categoryId)
        evictListCaches()

        return savedProduct
    }

    // 캐시 무효화 헬퍼 메서드들
    @CacheEvict(value = [CacheNames.PRODUCT_DETAIL], key = "#productId")
    private fun evictProductDetailCache(productId: Long) {
        // 캐시 무효화는 어노테이션이 처리
    }

    @CacheEvict(value = [CacheNames.PRODUCT_CATEGORY_LIST], allEntries = true, cacheManager = "redisCacheManager")
    private fun evictCategoryCache(categoryId: Long) {
        // 해당 카테고리의 모든 캐시 무효화 (정확한 키 매칭이 어려우므로 전체 삭제)
    }

    @Caching(evict = [
        CacheEvict(value = [CacheNames.PRODUCT_LIST], allEntries = true, cacheManager = "redisCacheManager"),
        CacheEvict(value = [CacheNames.PRODUCT_POPULAR], allEntries = true, cacheManager = "redisCacheManager")
    ])
    private fun evictListCaches() {
        // 목록 관련 캐시 무효화
    }
}