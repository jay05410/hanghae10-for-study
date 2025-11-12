package io.hhplus.ecommerce.order.infra.persistence.entity

import io.hhplus.ecommerce.common.baseentity.ActiveJpaEntity
import io.hhplus.ecommerce.order.domain.constant.OrderStatus
import jakarta.persistence.*

/**
 * Order JPA 엔티티
 *
 * 역할:
 * - JPA를 통한 Order 데이터 영속화
 * - 데이터베이스 테이블 매핑
 *
 * 주의: createdAt, updatedAt, createdBy, updatedBy, isActive, deletedAt는 ActiveJpaEntity에서 상속받습니다.
 */
@Entity
@Table(name = "orders")
class OrderJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 50)
    val orderNumber: String,

    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val totalAmount: Long,

    @Column(nullable = false)
    val discountAmount: Long = 0,

    @Column(nullable = false)
    val finalAmount: Long,

    @Column(nullable = true)
    val usedCouponId: Long? = null,

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    val status: OrderStatus = OrderStatus.PENDING
) : ActiveJpaEntity()
