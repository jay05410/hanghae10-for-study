package io.hhplus.ecommerce.product.infra.persistence.adapter

import io.hhplus.ecommerce.product.domain.entity.Category
import io.hhplus.ecommerce.product.domain.repository.CategoryRepository
import io.hhplus.ecommerce.product.infra.persistence.mapper.CategoryMapper
import io.hhplus.ecommerce.product.infra.persistence.mapper.toDomain
import io.hhplus.ecommerce.product.infra.persistence.mapper.toEntity
import io.hhplus.ecommerce.product.infra.persistence.repository.CategoryJpaRepository
import org.springframework.stereotype.Repository

/**
 * Category Repository JPA 구현체
 */
@Repository
class CategoryRepositoryImpl(
    private val jpaRepository: CategoryJpaRepository,
    private val mapper: CategoryMapper
) : CategoryRepository {

    override fun save(category: Category): Category {
        val entity = category.toEntity(mapper)
        return jpaRepository.save(entity).toDomain(mapper)!!
    }

    override fun findById(id: Long): Category? {
        return jpaRepository.findById(id).orElse(null).toDomain(mapper)
    }

    override fun findByName(name: String): Category? {
        return jpaRepository.findByName(name).toDomain(mapper)
    }

    override fun findAll(): List<Category> {
        return jpaRepository.findAll().toDomain(mapper)
    }

    override fun findActiveCategories(): List<Category> {
        return jpaRepository.findActiveCategories().toDomain(mapper)
    }

    override fun findByDisplayOrder(): List<Category> {
        return jpaRepository.findAllByOrderByDisplayOrderAsc().toDomain(mapper)
    }

    override fun existsByName(name: String): Boolean {
        return jpaRepository.existsByName(name)
    }

    override fun deleteById(id: Long) {
        jpaRepository.deleteById(id)
    }
}