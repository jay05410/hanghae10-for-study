package io.hhplus.ecommerce.product.domain.repository

import io.hhplus.ecommerce.product.domain.entity.Category

interface CategoryRepository {
    fun save(category: Category): Category
    fun findById(id: Long): Category?
    fun findByName(name: String): Category?
    fun findAll(): List<Category>
    fun findActiveCategories(): List<Category>
    fun findByDisplayOrder(): List<Category>
    fun existsByName(name: String): Boolean
    fun deleteById(id: Long)
}