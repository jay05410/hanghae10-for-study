package io.hhplus.ecommerce.order.infra.persistence.entity

import io.hhplus.ecommerce.common.baseentity.BaseJpaEntity
import jakarta.persistence.*

/**
 * OrderItem JPA 엔티티
 *
 * 역할:
 * - JPA를 통한 OrderItem 데이터 영속화
 * - 데이터베이스 테이블 매핑
 *
 * 주의: createdAt, updatedAt, createdBy, updatedBy, deletedAt는 BaseJpaEntity에서 상속받습니다.
 */
@Entity
@Table(name = "order_item")
class OrderItemJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    val order: OrderJpaEntity,

    @Column(nullable = false)
    val productId: Long,

    @Column(nullable = false, length = 100)
    val productName: String,

    @Column(nullable = false, length = 50)
    val categoryName: String,

    @Column(nullable = false)
    val quantity: Int,

    @Column(nullable = false)
    val unitPrice: Int,

    @Column(nullable = false)
    val giftWrap: Boolean = false,

    @Column(length = 500)
    val giftMessage: String? = null,

    @Column(nullable = false)
    val giftWrapPrice: Int = 0,

    @Column(nullable = false)
    val totalPrice: Int
) : BaseJpaEntity() {
    val orderId: Long get() = order.id
}
