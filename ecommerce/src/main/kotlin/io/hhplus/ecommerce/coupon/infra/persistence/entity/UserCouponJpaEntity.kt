package io.hhplus.ecommerce.coupon.infra.persistence.entity

import io.hhplus.ecommerce.common.baseentity.BaseJpaEntity
import io.hhplus.ecommerce.coupon.domain.constant.UserCouponStatus
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 사용자 쿠폰 JPA 엔티티 (영속성 모델)
 *
 * 역할:
 * - 데이터베이스 테이블과 매핑
 * - JPA 관련 어노테이션 및 설정 포함
 * - 도메인 모델과 분리하여 인프라 관심사 캡슐화
 *
 * 주의: 이 클래스는 영속성 전용이며 비즈니스 로직을 포함하지 않습니다.
 *       비즈니스 로직은 domain/entity/UserCoupon에 있습니다.
 *       createdAt, updatedAt, createdBy, updatedBy, deletedAt는 BaseJpaEntity에서 상속받습니다.
 */
@Entity
@Table(
    name = "user_coupons",
    indexes = [
        Index(name = "idx_user_coupon_user_id", columnList = "user_id"),
        Index(name = "idx_user_coupon_coupon_id", columnList = "coupon_id"),
        Index(name = "idx_user_coupon_status", columnList = "status"),
        Index(name = "uk_user_coupon", columnList = "user_id, coupon_id", unique = true)
    ]
)
class UserCouponJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, name = "user_id")
    val userId: Long,

    @Column(nullable = false, name = "coupon_id")
    val couponId: Long,

    @Column(nullable = false, name = "issued_at")
    val issuedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "used_at")
    var usedAt: LocalDateTime? = null,

    @Column(name = "used_order_id")
    val usedOrderId: Long? = null,

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var status: UserCouponStatus = UserCouponStatus.ISSUED,

    @Version
    @Column(nullable = false)
    var version: Int = 0
) : BaseJpaEntity()
