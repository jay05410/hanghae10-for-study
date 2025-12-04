package io.hhplus.ecommerce.coupon.infra.persistence.mapper

import io.hhplus.ecommerce.coupon.domain.entity.UserCoupon
import io.hhplus.ecommerce.coupon.infra.persistence.entity.UserCouponJpaEntity
import org.springframework.stereotype.Component

/**
 * UserCoupon 도메인 모델 <-> JPA 엔티티 Mapper
 *
 * 역할:
 * - 도메인 모델과 영속성 모델 간의 양방향 변환
 * - 도메인 계층과 인프라 계층의 분리 유지
 */
@Component
class UserCouponMapper {

    /**
     * JPA 엔티티 -> 도메인 모델 변환
     */
    fun toDomain(entity: UserCouponJpaEntity): UserCoupon {
        return UserCoupon(
            id = entity.id,
            userId = entity.userId,
            couponId = entity.couponId,
            issuedAt = entity.issuedAt,
            usedAt = entity.usedAt,
            usedOrderId = entity.usedOrderId,
            status = entity.status,
            version = entity.version
        )
    }

    /**
     * 도메인 모델 -> JPA 엔티티 변환
     */
    fun toEntity(domain: UserCoupon): UserCouponJpaEntity {
        return UserCouponJpaEntity(
            id = domain.id,
            userId = domain.userId,
            couponId = domain.couponId,
            issuedAt = domain.issuedAt,
            usedAt = domain.usedAt,
            usedOrderId = domain.usedOrderId,
            status = domain.status,
            version = domain.version
        )
    }

    /**
     * JPA 엔티티 리스트 -> 도메인 모델 리스트 변환
     */
    fun toDomainList(entities: List<UserCouponJpaEntity>): List<UserCoupon> {
        return entities.map { toDomain(it) }
    }

    /**
     * 도메인 모델 리스트 -> JPA 엔티티 리스트 변환
     */
    fun toEntityList(domains: List<UserCoupon>): List<UserCouponJpaEntity> {
        return domains.map { toEntity(it) }
    }
}

/**
 * UserCoupon Mapper Extension Functions
 *
 * 역할:
 * - Mapper 호출을 간결하게 만들어 가독성 향상
 * - Nullable 처리를 자동화
 *
 * 사용법:
 * - entity.toDomain(mapper)  // JPA Entity → Domain
 * - domain.toEntity(mapper)   // Domain → JPA Entity
 * - entities.toDomain(mapper) // List 변환
 */
fun UserCouponJpaEntity?.toDomain(mapper: UserCouponMapper): UserCoupon? =
    this?.let { mapper.toDomain(it) }

fun UserCoupon.toEntity(mapper: UserCouponMapper): UserCouponJpaEntity =
    mapper.toEntity(this)

fun List<UserCouponJpaEntity>.toDomain(mapper: UserCouponMapper): List<UserCoupon> =
    map { mapper.toDomain(it) }
