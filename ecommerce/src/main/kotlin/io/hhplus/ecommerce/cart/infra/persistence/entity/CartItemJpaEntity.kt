package io.hhplus.ecommerce.cart.infra.persistence.entity

import io.hhplus.ecommerce.common.baseentity.BaseJpaEntity
import jakarta.persistence.*

/**
 * 장바구니 아이템 JPA 엔티티 (영속성 모델)
 *
 * 역할:
 * - 데이터베이스 테이블과 매핑
 * - JPA 관련 어노테이션 및 설정 포함
 * - 도메인 모델과 분리하여 인프라 관심사 캡슐화
 *
 * 주의: 이 클래스는 영속성 전용이며 비즈니스 로직을 포함하지 않습니다.
 *       비즈니스 로직은 domain/entity/CartItem에 있습니다.
 *       createdAt, updatedAt, createdBy, updatedBy는 BaseJpaEntity에서 상속받습니다.
 */
@Entity
@Table(
    name = "cart_items",
    indexes = [
        Index(name = "idx_cart_item_cart", columnList = "cart_id"),
        Index(name = "idx_cart_item_product", columnList = "product_id"),
        Index(name = "idx_cart_item_active", columnList = "is_active, created_at")
    ]
)
class CartItemJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, name = "cart_id")
    val cartId: Long,

    @Column(nullable = false, name = "product_id")
    val productId: Long,

    @Column(nullable = false, name = "quantity")
    val quantity: Int,

    @Column(nullable = false, name = "gift_wrap")
    val giftWrap: Boolean = false,

    @Column(length = 500, name = "gift_message")
    val giftMessage: String? = null
) : BaseJpaEntity()
