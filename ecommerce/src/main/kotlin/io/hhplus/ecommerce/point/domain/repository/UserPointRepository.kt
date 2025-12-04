package io.hhplus.ecommerce.point.domain.repository

import io.hhplus.ecommerce.point.domain.entity.UserPoint

interface UserPointRepository {
    fun save(userPoint: UserPoint): UserPoint
    fun findById(id: Long): UserPoint?
    fun findByUserId(userId: Long): UserPoint?
    fun delete(userPoint: UserPoint)

    // FETCH JOIN 메서드들
    fun findUserPointWithHistoriesByUserId(userId: Long): UserPoint?
}