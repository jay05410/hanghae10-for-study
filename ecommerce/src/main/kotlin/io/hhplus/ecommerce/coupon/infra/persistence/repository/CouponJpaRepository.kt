package io.hhplus.ecommerce.coupon.infra.persistence.repository

import io.hhplus.ecommerce.coupon.infra.persistence.entity.CouponJpaEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Coupon Spring Data JPA Repository
 *
 * 역할:
 * - Spring Data JPA를 사용한 데이터베이스 접근
 * - 소프트 딜리트 필터 자동 적용 (@Filter 사용)
 */
@Repository
interface CouponJpaRepository : JpaRepository<CouponJpaEntity, Long> {

    fun findByName(name: String): CouponJpaEntity?

    fun findByCode(code: String): CouponJpaEntity?

    @Query("SELECT c FROM CouponJpaEntity c WHERE c.validFrom >= :startDate AND c.validTo <= :endDate")
    fun findByValidDateRange(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<CouponJpaEntity>

    @Query("SELECT c FROM CouponJpaEntity c WHERE c.validTo < :currentDate")
    fun findExpiredCoupons(@Param("currentDate") currentDate: LocalDateTime): List<CouponJpaEntity>
}
