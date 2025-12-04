package io.hhplus.ecommerce.product.infra.persistence.mapper

import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.product.infra.persistence.entity.ProductJpaEntity
import org.springframework.stereotype.Component

/**
 * Product 도메인 모델 ↔ JPA 엔티티 변환 Mapper - 인프라 계층
 *
 * 역할:
 * - 도메인 모델과 영속성 모델 간의 변환 담당
 * - 도메인 계층과 인프라 계층 간의 격리 유지
 *
 * 책임:
 * - toDomain: JPA 엔티티 → 도메인 모델
 * - toEntity: 도메인 모델 → JPA 엔티티
 */
@Component
class ProductMapper {
    /**
     * JPA 엔티티를 도메인 모델로 변환
     *
     * @param entity JPA 엔티티
     * @return 도메인 모델
     */
    fun toDomain(entity: ProductJpaEntity): Product {
        return Product(
            id = entity.id,
            categoryId = entity.categoryId,
            name = entity.name,
            description = entity.description,
            caffeineType = entity.caffeineType,
            tasteProfile = entity.tasteProfile,
            aromaProfile = entity.aromaProfile,
            colorProfile = entity.colorProfile,
            bagPerWeight = entity.bagPerWeight,
            pricePer100g = entity.pricePer100g,
            ingredients = entity.ingredients,
            origin = entity.origin,
            status = entity.status,
            requiresReservation = entity.requiresReservation
        )
    }

    /**
     * 도메인 모델을 JPA 엔티티로 변환
     *
     * @param domain 도메인 모델
     * @return JPA 엔티티
     */
    fun toEntity(domain: Product): ProductJpaEntity {
        return ProductJpaEntity(
            id = domain.id,
            categoryId = domain.categoryId,
            name = domain.name,
            description = domain.description,
            caffeineType = domain.caffeineType,
            tasteProfile = domain.tasteProfile,
            aromaProfile = domain.aromaProfile,
            colorProfile = domain.colorProfile,
            bagPerWeight = domain.bagPerWeight,
            pricePer100g = domain.pricePer100g,
            ingredients = domain.ingredients,
            origin = domain.origin,
            status = domain.status,
            requiresReservation = domain.requiresReservation
        )
    }

    /**
     * JPA 엔티티 리스트를 도메인 모델 리스트로 변환
     */
    fun toDomainList(entities: List<ProductJpaEntity>): List<Product> {
        return entities.map { toDomain(it) }
    }

    /**
     * 도메인 모델 리스트를 JPA 엔티티 리스트로 변환
     */
    fun toEntityList(domains: List<Product>): List<ProductJpaEntity> {
        return domains.map { toEntity(it) }
    }
}

/**
 * Product Mapper Extension Functions
 *
 * 역할:
 * - Mapper 호출을 간결하게 만들어 가독성 향상
 * - Nullable 처리를 자동화
 *
 * 사용법:
 * - entity.toDomain(mapper)  // JPA Entity → Domain
 * - domain.toEntity(mapper)   // Domain → JPA Entity
 * - entities.toDomain(mapper) // List 변환
 */
fun ProductJpaEntity?.toDomain(mapper: ProductMapper): Product? =
    this?.let { mapper.toDomain(it) }

fun Product.toEntity(mapper: ProductMapper): ProductJpaEntity =
    mapper.toEntity(this)

fun List<ProductJpaEntity>.toDomain(mapper: ProductMapper): List<Product> =
    map { mapper.toDomain(it) }
