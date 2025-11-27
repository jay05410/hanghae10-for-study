package io.hhplus.ecommerce.product.infra

import io.hhplus.ecommerce.product.domain.constant.ProductStatus
import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.product.domain.repository.ProductRepository
import io.hhplus.ecommerce.product.infra.mapper.ProductMapper
import io.hhplus.ecommerce.product.infra.mapper.toDomain
import io.hhplus.ecommerce.product.infra.mapper.toEntity
import io.hhplus.ecommerce.product.infra.persistence.repository.ProductJpaRepository
import org.springframework.stereotype.Repository

/**
 * Product Repository JPA 구현체
 *
 * 역할:
 * - 도메인 Repository 인터페이스 구현
 * - JPA Repository와 Mapper를 사용하여 영속성 처리
 * - 도메인 모델과 JPA 엔티티 간 변환
 *
 */
@Repository
class ProductRepositoryImpl(
    private val jpaRepository: ProductJpaRepository,
    private val mapper: ProductMapper
) : ProductRepository {

    override fun save(product: Product): Product =
        jpaRepository.save(product.toEntity(mapper)).toDomain(mapper)!!

    override fun findById(id: Long): Product? =
        jpaRepository.findById(id).orElse(null).toDomain(mapper)

    override fun findByIdAndIsActive(id: Long): Product? =
        jpaRepository.findByIdAndIsActive(id).toDomain(mapper)

    override fun findAllByIsActive(): List<Product> =
        jpaRepository.findAllByIsActive().toDomain(mapper)

    override fun findByStatus(status: ProductStatus): List<Product> =
        jpaRepository.findByStatus(status).toDomain(mapper)

    override fun findByCategoryIdAndIsActive(categoryId: Long): List<Product> =
        jpaRepository.findByCategoryIdAndIsActive(categoryId).toDomain(mapper)

    override fun findByNameContaining(keyword: String): List<Product> =
        jpaRepository.findByNameContaining(keyword).toDomain(mapper)

    override fun findActiveProductsWithCursor(lastId: Long?, size: Int): List<Product> =
        jpaRepository.findActiveProductsWithCursor(lastId, size).toDomain(mapper)

    override fun findCategoryProductsWithCursor(categoryId: Long, lastId: Long?, size: Int): List<Product> =
        jpaRepository.findCategoryProductsWithCursor(categoryId, lastId, size).toDomain(mapper)
}
