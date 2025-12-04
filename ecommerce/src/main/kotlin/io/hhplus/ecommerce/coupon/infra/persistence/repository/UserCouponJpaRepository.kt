package io.hhplus.ecommerce.coupon.infra.persistence.repository

import io.hhplus.ecommerce.coupon.domain.constant.UserCouponStatus
import io.hhplus.ecommerce.coupon.infra.persistence.entity.UserCouponJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * UserCoupon Spring Data JPA Repository
 *
 * 역할:
 * - Spring Data JPA를 사용한 데이터베이스 접근
 * - 기본 CRUD 및 쿼리 메서드 제공
 */
@Repository
interface UserCouponJpaRepository : JpaRepository<UserCouponJpaEntity, Long> {

    fun findByUserId(userId: Long): List<UserCouponJpaEntity>

    fun findByUserIdAndStatus(userId: Long, status: UserCouponStatus): List<UserCouponJpaEntity>

    fun findByCouponId(couponId: Long): List<UserCouponJpaEntity>

    fun findByUserIdAndCouponId(userId: Long, couponId: Long): UserCouponJpaEntity?

    @Query(
        """
        SELECT uc FROM UserCouponJpaEntity uc
        JOIN CouponJpaEntity c ON uc.couponId = c.id
        WHERE uc.userId = :userId AND c.code = :couponCode
    """
    )
    fun findByUserIdAndCouponCode(
        @Param("userId") userId: Long,
        @Param("couponCode") couponCode: String
    ): UserCouponJpaEntity?

    fun countByUserIdAndCouponId(userId: Long, couponId: Long): Long
}
