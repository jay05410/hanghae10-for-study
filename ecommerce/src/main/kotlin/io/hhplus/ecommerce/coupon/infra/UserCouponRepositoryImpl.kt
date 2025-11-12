package io.hhplus.ecommerce.coupon.infra

import io.hhplus.ecommerce.coupon.domain.constant.UserCouponStatus
import io.hhplus.ecommerce.coupon.domain.entity.UserCoupon
import io.hhplus.ecommerce.coupon.domain.repository.UserCouponRepository
import io.hhplus.ecommerce.coupon.infra.mapper.UserCouponMapper
import io.hhplus.ecommerce.coupon.infra.mapper.toDomain
import io.hhplus.ecommerce.coupon.infra.mapper.toEntity
import io.hhplus.ecommerce.coupon.infra.persistence.repository.UserCouponJpaRepository
import org.springframework.stereotype.Repository

/**
 * UserCoupon Repository JPA 구현체
 *
 * 역할:
 * - 도메인 Repository 인터페이스 구현
 * - JPA Repository와 Mapper를 사용하여 영속성 처리
 * - 도메인 모델과 JPA 엔티티 간 변환
 *
 */
@Repository
class UserCouponRepositoryImpl(
    private val jpaRepository: UserCouponJpaRepository,
    private val mapper: UserCouponMapper
) : UserCouponRepository {

    override fun save(userCoupon: UserCoupon): UserCoupon =
        jpaRepository.save(userCoupon.toEntity(mapper)).toDomain(mapper)!!

    override fun findById(id: Long): UserCoupon? =
        jpaRepository.findById(id).orElse(null).toDomain(mapper)

    override fun findByUserId(userId: Long): List<UserCoupon> =
        jpaRepository.findByUserId(userId).toDomain(mapper)

    override fun findByUserIdAndStatus(userId: Long, status: UserCouponStatus): List<UserCoupon> =
        jpaRepository.findByUserIdAndStatus(userId, status).toDomain(mapper)

    override fun findByCouponId(couponId: Long): List<UserCoupon> =
        jpaRepository.findByCouponId(couponId).toDomain(mapper)

    override fun findByUserIdAndCouponId(userId: Long, couponId: Long): UserCoupon? =
        jpaRepository.findByUserIdAndCouponId(userId, couponId).toDomain(mapper)

    override fun findByUserIdAndCouponCode(userId: Long, couponCode: String): UserCoupon? =
        jpaRepository.findByUserIdAndCouponCode(userId, couponCode).toDomain(mapper)

    override fun countByUserIdAndCouponId(userId: Long, couponId: Long): Long =
        jpaRepository.countByUserIdAndCouponId(userId, couponId)
}
