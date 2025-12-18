package io.hhplus.ecommerce.coupon.infra.persistence.mapper

import io.hhplus.ecommerce.coupon.domain.entity.Coupon
import io.hhplus.ecommerce.coupon.infra.persistence.entity.CouponJpaEntity
import org.springframework.stereotype.Component

/**
 * Coupon 도메인 모델 <-> JPA 엔티티 Mapper
 *
 * 역할:
 * - 도메인 모델과 영속성 모델 간의 양방향 변환
 * - 도메인 계층과 인프라 계층의 분리 유지
 *
 * 변환 규칙:
 * - targetCategoryIds, targetProductIds: List<Long> <-> 쉼표 구분 문자열
 */
@Component
class CouponMapper {

    /**
     * JPA 엔티티 -> 도메인 모델 변환
     */
    fun toDomain(entity: CouponJpaEntity): Coupon {
        return Coupon(
            id = entity.id,
            name = entity.name,
            code = entity.code,
            discountType = entity.discountType,
            discountValue = entity.discountValue,
            minimumOrderAmount = entity.minimumOrderAmount,
            totalQuantity = entity.totalQuantity,
            issuedQuantity = entity.issuedQuantity,
            version = entity.version,
            validFrom = entity.validFrom,
            validTo = entity.validTo,
            discountScope = entity.discountScope,
            targetCategoryIds = parseIds(entity.targetCategoryIds),
            targetProductIds = parseIds(entity.targetProductIds),
            maxDiscountAmount = entity.maxDiscountAmount
        )
    }

    /**
     * 도메인 모델 -> JPA 엔티티 변환
     */
    fun toEntity(domain: Coupon): CouponJpaEntity {
        return CouponJpaEntity(
            id = domain.id,
            name = domain.name,
            code = domain.code,
            discountType = domain.discountType,
            discountValue = domain.discountValue,
            minimumOrderAmount = domain.minimumOrderAmount,
            totalQuantity = domain.totalQuantity,
            issuedQuantity = domain.issuedQuantity,
            version = domain.version,
            validFrom = domain.validFrom,
            validTo = domain.validTo,
            discountScope = domain.discountScope,
            targetCategoryIds = joinIds(domain.targetCategoryIds),
            targetProductIds = joinIds(domain.targetProductIds),
            maxDiscountAmount = domain.maxDiscountAmount
        )
    }

    /**
     * 쉼표 구분 문자열 -> List<Long> 변환
     */
    private fun parseIds(idsString: String?): List<Long> {
        if (idsString.isNullOrBlank()) return emptyList()
        return idsString.split(",")
            .mapNotNull { it.trim().toLongOrNull() }
    }

    /**
     * List<Long> -> 쉼표 구분 문자열 변환 (빈 리스트는 null)
     */
    private fun joinIds(ids: List<Long>): String? {
        if (ids.isEmpty()) return null
        return ids.joinToString(",")
    }

    /**
     * JPA 엔티티 리스트 -> 도메인 모델 리스트 변환
     */
    fun toDomainList(entities: List<CouponJpaEntity>): List<Coupon> {
        return entities.map { toDomain(it) }
    }

    /**
     * 도메인 모델 리스트 -> JPA 엔티티 리스트 변환
     */
    fun toEntityList(domains: List<Coupon>): List<CouponJpaEntity> {
        return domains.map { toEntity(it) }
    }
}

/**
 * Coupon Mapper Extension Functions
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
fun CouponJpaEntity?.toDomain(mapper: CouponMapper): Coupon? =
    this?.let { mapper.toDomain(it) }

fun Coupon.toEntity(mapper: CouponMapper): CouponJpaEntity =
    mapper.toEntity(this)

fun List<CouponJpaEntity>.toDomain(mapper: CouponMapper): List<Coupon> =
    map { mapper.toDomain(it) }
