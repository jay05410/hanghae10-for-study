package io.hhplus.ecommerce.coupon.infra.persistence.entity

import io.hhplus.ecommerce.common.baseentity.BaseJpaEntity
import io.hhplus.ecommerce.coupon.domain.constant.DiscountType
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 쿠폰 JPA 엔티티 (영속성 모델)
 *
 * 역할:
 * - 데이터베이스 테이블과 매핑
 * - JPA 관련 어노테이션 및 설정 포함
 * - 도메인 모델과 분리하여 인프라 관심사 캡슐화
 *
 * 주의: 이 클래스는 영속성 전용이며 비즈니스 로직을 포함하지 않습니다.
 *       비즈니스 로직은 domain/entity/Coupon에 있습니다.
 *       createdAt, updatedAt, createdBy, updatedBy, isActive는 BaseJpaEntity에서 상속받습니다.
 */
@Entity
@Table(
    name = "coupons",
    indexes = [
        Index(name = "uk_coupon_name", columnList = "name", unique = true),
        Index(name = "uk_coupon_code", columnList = "code", unique = true),
        Index(name = "idx_coupon_valid_period", columnList = "valid_from, valid_to"),
        Index(name = "idx_coupon_active", columnList = "is_active")
    ]
)
class CouponJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 50)
    val name: String,

    @Column(nullable = false, unique = true, length = 20)
    val code: String,

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    val discountType: DiscountType,

    @Column(nullable = false)
    val discountValue: Long,

    @Column(nullable = false, name = "minimum_order_amount")
    val minimumOrderAmount: Long = 0,

    @Column(nullable = false, name = "total_quantity")
    val totalQuantity: Int,

    @Column(nullable = false, name = "issued_quantity")
    var issuedQuantity: Int = 0,

    @Version
    var version: Int = 0,

    @Column(nullable = false, name = "valid_from")
    val validFrom: LocalDateTime,

    @Column(nullable = false, name = "valid_to")
    val validTo: LocalDateTime
) : BaseJpaEntity()
