package io.hhplus.ecommerce.coupon.infra

import io.hhplus.ecommerce.common.aop.SoftDeletedFilter
import io.hhplus.ecommerce.coupon.domain.entity.Coupon
import io.hhplus.ecommerce.coupon.domain.repository.CouponRepository
import io.hhplus.ecommerce.coupon.infra.mapper.CouponMapper
import io.hhplus.ecommerce.coupon.infra.mapper.toDomain
import io.hhplus.ecommerce.coupon.infra.mapper.toEntity
import io.hhplus.ecommerce.coupon.infra.persistence.repository.CouponJpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Coupon Repository JPA 구현체
 *
 * 역할:
 * - 도메인 Repository 인터페이스 구현
 * - JPA Repository와 Mapper를 사용하여 영속성 처리
 * - 도메인 모델과 JPA 엔티티 간 변환
 */
@Repository
class CouponRepositoryImpl(
    private val jpaRepository: CouponJpaRepository,
    private val mapper: CouponMapper
) : CouponRepository {

    override fun save(coupon: Coupon): Coupon =
        jpaRepository.save(coupon.toEntity(mapper)).toDomain(mapper)!!

    @SoftDeletedFilter
    override fun findById(id: Long): Coupon? =
        jpaRepository.findById(id).orElse(null).toDomain(mapper)

    @SoftDeletedFilter
    override fun findByName(name: String): Coupon? =
        jpaRepository.findByName(name).toDomain(mapper)

    @SoftDeletedFilter
    override fun findByCode(code: String): Coupon? =
        jpaRepository.findByCode(code).toDomain(mapper)

    @SoftDeletedFilter
    override fun findAll(): List<Coupon> =
        jpaRepository.findAll().toDomain(mapper)

    @SoftDeletedFilter
    override fun findByValidDateRange(startDate: LocalDateTime, endDate: LocalDateTime): List<Coupon> =
        jpaRepository.findByValidDateRange(startDate, endDate).toDomain(mapper)

    @SoftDeletedFilter
    override fun findExpiredCoupons(currentDate: LocalDateTime): List<Coupon> =
        jpaRepository.findExpiredCoupons(currentDate).toDomain(mapper)

    override fun delete(coupon: Coupon) {
        jpaRepository.delete(coupon.toEntity(mapper))
    }
}
