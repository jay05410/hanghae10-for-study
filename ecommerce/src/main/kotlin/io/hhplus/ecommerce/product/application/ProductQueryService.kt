package io.hhplus.ecommerce.product.application

import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.product.domain.repository.ProductRepository
import io.hhplus.ecommerce.product.exception.ProductException
import io.hhplus.ecommerce.common.cache.CacheNames
import io.hhplus.ecommerce.common.response.Cursor
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

/**
 * 상품 조회 서비스 - CQRS Query 담당
 *
 * 역할:
 * - 모든 상품 조회 로직 담당 (캐싱 포함)
 * - 읽기 전용 작업만 수행
 * - self-invocation 문제 해결을 위한 독립적 서비스
 *
 * 책임:
 * - 상품 단건/목록 조회
 * - 커서 기반 페이징 처리
 * - 카테고리별 상품 조회
 * - 캐시 적용 및 관리
 */
@Service
class ProductQueryService(
    private val productRepository: ProductRepository
) {

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

    /**
     * 특정 카테고리의 모든 활성 상품 조회 (통계 기반 정렬용)
     *
     * 통계 UseCase에서 카테고리별 정렬을 위해 사용
     * 캐시 없이 실시간 데이터 조회 (통계와 함께 정렬하므로)
     */
    fun getActiveProductsByCategory(categoryId: Long): List<Product> {
        return productRepository.findActiveProductsByCategory(categoryId)
    }

    /**
     * 여러 상품을 한번에 조회 (batch 처리용)
     *
     * @param productIds 조회할 상품 ID 목록
     * @return 조회된 상품 목록 (존재하지 않는 ID는 제외)
     */
    fun getProducts(productIds: List<Long>): List<Product> {
        return productIds.mapNotNull { productId ->
            try {
                getProduct(productId)
            } catch (e: ProductException.ProductNotFound) {
                null // 존재하지 않는 상품은 제외
            }
        }
    }
}