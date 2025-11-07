package io.hhplus.ecommerce.point.domain.repository

import io.hhplus.ecommerce.point.domain.entity.PointHistory

interface PointHistoryRepository {
    fun save(pointHistory: PointHistory): PointHistory
    fun findById(id: Long): PointHistory?
    fun findByUserId(userId: Long): List<PointHistory>
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<PointHistory>
    fun delete(pointHistory: PointHistory)
}