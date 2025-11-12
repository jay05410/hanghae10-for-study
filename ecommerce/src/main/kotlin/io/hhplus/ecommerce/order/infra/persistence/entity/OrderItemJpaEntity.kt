package io.hhplus.ecommerce.order.infra.persistence.entity

import io.hhplus.ecommerce.common.baseentity.ActiveJpaEntity
import jakarta.persistence.*

/**
 * OrderItem JPA 엔티티
 *
 * 역할:
 * - JPA를 통한 OrderItem 데이터 영속화
 * - 데이터베이스 테이블 매핑
 *
 * 주의: createdAt, updatedAt, createdBy, updatedBy, isActive, deletedAt는 ActiveJpaEntity에서 상속받습니다.
 */
@Entity
@Table(name = "order_item")
class OrderItemJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val orderId: Long,

    @Column(nullable = false)
    val packageTypeId: Long,

    @Column(nullable = false, length = 100)
    val packageTypeName: String,

    @Column(nullable = false)
    val packageTypeDays: Int,

    @Column(nullable = false)
    val dailyServing: Int,

    @Column(nullable = false)
    val totalQuantity: Double,

    @Column(nullable = false)
    val giftWrap: Boolean = false,

    @Column(length = 500)
    val giftMessage: String? = null,

    @Column(nullable = false)
    val quantity: Int,

    @Column(nullable = false)
    val containerPrice: Int,

    @Column(nullable = false)
    val teaPrice: Int,

    @Column(nullable = false)
    val giftWrapPrice: Int = 0,

    @Column(nullable = false)
    val totalPrice: Int
) : ActiveJpaEntity()
