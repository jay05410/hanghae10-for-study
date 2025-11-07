package io.hhplus.ecommerce.product.domain.repository

import io.hhplus.ecommerce.product.domain.entity.BoxType

interface BoxTypeRepository {
    fun save(boxType: BoxType): BoxType
    fun findById(id: Long): BoxType?
    fun findByCode(code: String): BoxType?
    fun findByName(name: String): BoxType?
    fun findAll(): List<BoxType>
    fun findActiveBoxTypes(): List<BoxType>
    fun findByDays(days: Int): List<BoxType>
    fun findByTeaCount(teaCount: Int): List<BoxType>
    fun existsByCode(code: String): Boolean
    fun existsByName(name: String): Boolean
    fun deleteById(id: Long)
}