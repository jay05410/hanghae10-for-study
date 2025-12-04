package io.hhplus.ecommerce.product.application.usecase

import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.product.domain.service.ProductDomainService
import io.hhplus.ecommerce.product.presentation.dto.CreateProductRequest
import io.hhplus.ecommerce.product.presentation.dto.UpdateProductRequest
import io.hhplus.ecommerce.common.cache.CacheNames
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Caching
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 상품 명령 UseCase - 애플리케이션 계층
 *
 * 역할:
 * - 모든 상품 변경 작업을 통합 관리
 * - 상품 생성, 수정 기능 제공
 * - 트랜잭션 경계 관리
 *
 * 책임:
 * - 상품 생성/수정 요청 검증 및 실행
 * - 상품 데이터 무결성 보장
 * - 캐시 무효화 관리
 */
@Component
class ProductCommandUseCase(
    private val productDomainService: ProductDomainService
) {

    /**
     * 새로운 상품을 등록하고 생성한다 - 캐시 무효화 적용
     *
     * @param request 상품 생성 요청 데이터
     * @return 생성이 완료된 상품 정보
     * @throws IllegalArgumentException 상품 정보가 유효하지 않은 경우
     * @throws RuntimeException 상품 생성 처리에 실패한 경우
     */
    @Transactional
    @Caching(evict = [
        CacheEvict(value = [CacheNames.PRODUCT_LIST], allEntries = true, cacheManager = "redisCacheManager"),
        CacheEvict(value = [CacheNames.PRODUCT_CATEGORY_LIST], key = "#request.categoryId", cacheManager = "redisCacheManager"),
        CacheEvict(value = [CacheNames.PRODUCT_POPULAR], allEntries = true, cacheManager = "redisCacheManager")
    ])
    fun createProduct(request: CreateProductRequest): Product {
        return productDomainService.createProduct(
            name = request.name,
            description = request.description,
            price = request.price,
            categoryId = request.categoryId
        )
    }

    /**
     * 지정된 상품의 정보를 수정하고 업데이트한다 - 캐시 무효화 적용
     *
     * @param productId 수정할 상품 ID
     * @param request 상품 수정 요청 데이터
     * @return 수정이 완료된 상품 정보
     * @throws IllegalArgumentException 상품을 찾을 수 없거나 수정 정보가 잘못된 경우
     * @throws RuntimeException 상품 수정 처리에 실패한 경우
     */
    @Transactional
    @Caching(evict = [
        CacheEvict(value = [CacheNames.PRODUCT_DETAIL], key = "#productId"),
        CacheEvict(value = [CacheNames.PRODUCT_LIST], allEntries = true, cacheManager = "redisCacheManager"),
        CacheEvict(value = [CacheNames.PRODUCT_CATEGORY_LIST], allEntries = true, cacheManager = "redisCacheManager"),
        CacheEvict(value = [CacheNames.PRODUCT_POPULAR], allEntries = true, cacheManager = "redisCacheManager")
    ])
    fun updateProduct(productId: Long, request: UpdateProductRequest): Product {
        return productDomainService.updateProductInfo(
            productId = productId,
            name = request.name,
            description = request.description,
            price = request.price
        )
    }

    /**
     * 상품 품절 처리 - 캐시 무효화 적용
     *
     * @param productId 품절 처리할 상품 ID
     * @return 품절 처리된 상품 정보
     */
    @Transactional
    @Caching(evict = [
        CacheEvict(value = [CacheNames.PRODUCT_DETAIL], key = "#productId"),
        CacheEvict(value = [CacheNames.PRODUCT_LIST], allEntries = true, cacheManager = "redisCacheManager"),
        CacheEvict(value = [CacheNames.PRODUCT_POPULAR], allEntries = true, cacheManager = "redisCacheManager")
    ])
    fun markProductOutOfStock(productId: Long): Product {
        return productDomainService.markProductOutOfStock(productId)
    }

    /**
     * 상품 단종 처리 - 캐시 무효화 적용
     *
     * @param productId 단종 처리할 상품 ID
     * @return 단종 처리된 상품 정보
     */
    @Transactional
    @Caching(evict = [
        CacheEvict(value = [CacheNames.PRODUCT_DETAIL], key = "#productId"),
        CacheEvict(value = [CacheNames.PRODUCT_LIST], allEntries = true, cacheManager = "redisCacheManager"),
        CacheEvict(value = [CacheNames.PRODUCT_POPULAR], allEntries = true, cacheManager = "redisCacheManager")
    ])
    fun discontinueProduct(productId: Long): Product {
        return productDomainService.discontinueProduct(productId)
    }

    /**
     * 상품 숨김 처리 - 캐시 무효화 적용
     *
     * @param productId 숨김 처리할 상품 ID
     * @return 숨김 처리된 상품 정보
     */
    @Transactional
    @Caching(evict = [
        CacheEvict(value = [CacheNames.PRODUCT_DETAIL], key = "#productId"),
        CacheEvict(value = [CacheNames.PRODUCT_LIST], allEntries = true, cacheManager = "redisCacheManager"),
        CacheEvict(value = [CacheNames.PRODUCT_POPULAR], allEntries = true, cacheManager = "redisCacheManager")
    ])
    fun hideProduct(productId: Long): Product {
        return productDomainService.hideProduct(productId)
    }

    /**
     * 상품 복구 처리 - 캐시 무효화 적용
     *
     * @param productId 복구할 상품 ID
     * @return 복구된 상품 정보
     */
    @Transactional
    @Caching(evict = [
        CacheEvict(value = [CacheNames.PRODUCT_DETAIL], key = "#productId"),
        CacheEvict(value = [CacheNames.PRODUCT_LIST], allEntries = true, cacheManager = "redisCacheManager"),
        CacheEvict(value = [CacheNames.PRODUCT_POPULAR], allEntries = true, cacheManager = "redisCacheManager")
    ])
    fun restoreProduct(productId: Long): Product {
        return productDomainService.restoreProduct(productId)
    }
}
