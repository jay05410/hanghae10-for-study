package io.hhplus.ecommerce.product.infra

import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.product.domain.repository.ProductRepository
import io.hhplus.ecommerce.product.domain.constant.ProductStatus
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 제품 리포지토리 인메모리 구현체 - 인프라 계층
 *
 * 역할:
 * - 제품 데이터의 영속화 및 조회 기능 제공
 * - 테스트 환경에서 외부 의존성 제거
 * - 인메모리 저장소를 통한 빠른 데이터 접근
 *
 * 책임:
 * - ProductRepository 인터페이스 구현
 * - 동시성 안전한 데이터 저장 및 조회
 * - 테스트용 샘플 제품 데이터 초기화
 */
@Repository
class InMemoryProductRepository : ProductRepository {
    private val products = ConcurrentHashMap<Long, Product>()
    private val idGenerator = AtomicLong(1)

    init {
        initializeSampleData()
    }

    private fun initializeSampleData() {
        val product1 = Product.createDetailed(
            categoryId = 1L, // 녹차 카테고리
            name = "제주 유기농 녹차",
            description = "제주도에서 자란 유기농 녹차로 상쾌하고 깔끔한 맛이 특징입니다.",
            caffeineType = "LOW",
            tasteProfile = "FRESH",
            aromaProfile = "GRASSY",
            colorProfile = "GREEN",
            bagPerWeight = 3,
            pricePer100g = 15000,
            ingredients = "유기농 녹차 100%",
            origin = "제주도",
            createdBy = 1L
        )
        val product2 = Product.createDetailed(
            categoryId = 1L,
            name = "전통 우롱차",
            description = "반발효차의 깊은 맛과 향을 즐길 수 있는 전통 우롱차입니다.",
            caffeineType = "MEDIUM",
            tasteProfile = "COMPLEX",
            aromaProfile = "FLORAL",
            colorProfile = "AMBER",
            bagPerWeight = 4,
            pricePer100g = 25000,
            ingredients = "우롱차 100%",
            origin = "중국 푸젠성",
            createdBy = 1L
        )
        val product3 = Product.createDetailed(
            categoryId = 2L, // 허브차 카테고리
            name = "캐모마일 허브차",
            description = "편안한 휴식을 위한 캐모마일 허브차로 취침 전에 마시기 좋습니다.",
            caffeineType = "NONE",
            tasteProfile = "MILD",
            aromaProfile = "FLORAL",
            colorProfile = "GOLDEN",
            bagPerWeight = 2,
            pricePer100g = 12000,
            ingredients = "캐모마일 100%",
            origin = "독일",
            createdBy = 1L
        )

        products[1L] = Product(
            id = 1L,
            categoryId = product1.categoryId,
            name = product1.name,
            description = product1.description,
            caffeineType = product1.caffeineType,
            tasteProfile = product1.tasteProfile,
            aromaProfile = product1.aromaProfile,
            colorProfile = product1.colorProfile,
            bagPerWeight = product1.bagPerWeight,
            pricePer100g = product1.pricePer100g,
            ingredients = product1.ingredients,
            origin = product1.origin,
            status = ProductStatus.ACTIVE
        )
        products[2L] = Product(
            id = 2L,
            categoryId = product2.categoryId,
            name = product2.name,
            description = product2.description,
            caffeineType = product2.caffeineType,
            tasteProfile = product2.tasteProfile,
            aromaProfile = product2.aromaProfile,
            colorProfile = product2.colorProfile,
            bagPerWeight = product2.bagPerWeight,
            pricePer100g = product2.pricePer100g,
            ingredients = product2.ingredients,
            origin = product2.origin,
            status = ProductStatus.ACTIVE
        )
        products[3L] = Product(
            id = 3L,
            categoryId = product3.categoryId,
            name = product3.name,
            description = product3.description,
            caffeineType = product3.caffeineType,
            tasteProfile = product3.tasteProfile,
            aromaProfile = product3.aromaProfile,
            colorProfile = product3.colorProfile,
            bagPerWeight = product3.bagPerWeight,
            pricePer100g = product3.pricePer100g,
            ingredients = product3.ingredients,
            origin = product3.origin,
            status = ProductStatus.ACTIVE
        )

        idGenerator.set(4L)
    }

    /**
     * 제품 정보를 저장하거나 업데이트한다
     *
     * @param product 저장할 제품 엔티티
     * @return 저장된 제품 엔티티
     */
    override fun save(product: Product): Product {
        products[product.id] = product
        return product
    }

    /**
     * 제품 ID로 활성 상태의 제품을 조회한다
     *
     * @param id 조회할 제품의 ID
     * @return 제품 엔티티 (비활성이거나 존재하지 않을 경우 null)
     */
    override fun findById(id: Long): Product? {
        return products[id]?.takeIf { it.isActive }
    }

    /**
     * 제품 ID와 활성 상태로 제품을 조회한다
     *
     * @param id 조회할 제품의 ID
     * @param isActive 조회할 활성 상태
     * @return 제품 엔티티 (조건에 맞지 않을 경우 null)
     */
    override fun findByIdAndIsActive(id: Long, isActive: Boolean): Product? {
        return products[id]?.takeIf { it.isActive == isActive }
    }

    /**
     * 카테고리 ID와 활성 상태로 제품들을 조회한다
     *
     * @param categoryId 조회할 카테고리의 ID
     * @param isActive 조회할 활성 상태
     * @return 조건에 맞는 제품 목록
     */
    override fun findByCategoryIdAndIsActive(categoryId: Long, isActive: Boolean): List<Product> {
        return products.values.filter { it.categoryId == categoryId && it.isActive == isActive }
    }

    /**
     * 제품명에 키워드가 포함된 제품들을 조회한다
     *
     * @param keyword 검색할 키워드
     * @return 이름에 키워드가 포함된 제품 목록
     */
    override fun findByNameContaining(keyword: String): List<Product> {
        return products.values.filter {
            it.name.contains(keyword, ignoreCase = true)
        }
    }

    /**
     * 제품 상태로 제품들을 조회한다
     *
     * @param status 조회할 제품 상태
     * @return 상태에 맞는 제품 목록
     */
    override fun findByStatus(status: ProductStatus): List<Product> {
        return products.values.filter { it.status == status }
    }

    /**
     * 활성 상태로 모든 제품들을 조회한다
     *
     * @param isActive 조회할 활성 상태
     * @return 활성 상태에 맞는 모든 제품 목록
     */
    override fun findAllByIsActive(isActive: Boolean): List<Product> {
        return products.values.filter { it.isActive == isActive }
    }

    /**
     * 저장소를 초기화한다 (테스트 전용)
     */
    fun clear() {
        products.clear()
        idGenerator.set(1)
        initializeSampleData()
    }
}