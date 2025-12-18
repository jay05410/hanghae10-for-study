package io.hhplus.ecommerce.product.infra.persistence.mapper

import io.hhplus.ecommerce.product.domain.entity.Category
import io.hhplus.ecommerce.product.infra.persistence.entity.CategoryJpaEntity
import org.springframework.stereotype.Component

/**
 * Category 도메인 모델 ↔ JPA 엔티티 변환 Mapper
 */
@Component
class CategoryMapper {

    fun toDomain(entity: CategoryJpaEntity): Category {
        return Category(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            displayOrder = entity.displayOrder
        )
    }

    fun toEntity(domain: Category): CategoryJpaEntity {
        return CategoryJpaEntity(
            id = domain.id,
            name = domain.name,
            description = domain.description,
            displayOrder = domain.displayOrder
        )
    }

    fun toDomainList(entities: List<CategoryJpaEntity>): List<Category> {
        return entities.map { toDomain(it) }
    }
}

fun CategoryJpaEntity?.toDomain(mapper: CategoryMapper): Category? =
    this?.let { mapper.toDomain(it) }

fun Category.toEntity(mapper: CategoryMapper): CategoryJpaEntity =
    mapper.toEntity(this)

fun List<CategoryJpaEntity>.toDomain(mapper: CategoryMapper): List<Category> =
    map { mapper.toDomain(it) }