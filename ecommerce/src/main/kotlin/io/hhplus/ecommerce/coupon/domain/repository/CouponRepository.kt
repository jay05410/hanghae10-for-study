package io.hhplus.ecommerce.coupon.domain.repository

import io.hhplus.ecommerce.coupon.domain.entity.Coupon
import java.time.LocalDateTime

interface CouponRepository {
    fun save(coupon: Coupon): Coupon
    fun findById(id: Long): Coupon?
    fun findByIdWithLock(id: Long): Coupon?
    fun findByName(name: String): Coupon?
    fun findByCode(code: String): Coupon?
    fun findAvailableCoupons(): List<Coupon>
    fun findByIsActiveTrue(): List<Coupon>
    fun findByValidDateRange(startDate: LocalDateTime, endDate: LocalDateTime): List<Coupon>
    fun findExpiredCoupons(currentDate: LocalDateTime): List<Coupon>
    fun delete(coupon: Coupon)
}