package io.hhplus.ecommerce.delivery.infra.persistence.entity

import io.hhplus.ecommerce.common.baseentity.BaseJpaEntity
import io.hhplus.ecommerce.delivery.domain.constant.DeliveryStatus
import io.hhplus.ecommerce.delivery.domain.converter.DeliveryAddressConverter
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 배송 JPA 엔티티 (영속성 모델)
 *
 * 역할:
 * - 데이터베이스 테이블과 매핑
 * - JPA 관련 어노테이션 및 설정 포함
 * - 도메인 모델과 분리하여 인프라 관심사 캡슐화
 *
 * 주의: 이 클래스는 영속성 전용이며 비즈니스 로직을 포함하지 않습니다.
 *       비즈니스 로직은 domain/entity/Delivery에 있습니다.
 *       createdAt, updatedAt, createdBy, updatedBy, isActive는 BaseJpaEntity에서 상속받습니다.
 */
@Entity
@Table(
    name = "delivery",
    indexes = [
        Index(name = "uk_delivery_order", columnList = "order_id", unique = true),
        Index(name = "idx_delivery_tracking", columnList = "tracking_number"),
        Index(name = "idx_delivery_status", columnList = "status, created_at")
    ]
)
class DeliveryJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, name = "order_id")
    val orderId: Long,

    @Convert(converter = DeliveryAddressConverter::class)
    @Column(nullable = false, columnDefinition = "JSON", name = "delivery_address")
    val deliveryAddress: String, // JSON 문자열로 저장

    @Column(length = 50, name = "tracking_number")
    var trackingNumber: String? = null,

    @Column(length = 50)
    var carrier: String? = null,

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var status: DeliveryStatus = DeliveryStatus.PENDING,

    @Column(name = "shipped_at")
    var shippedAt: LocalDateTime? = null,

    @Column(name = "delivered_at")
    var deliveredAt: LocalDateTime? = null,

    @Column(length = 500, name = "delivery_memo")
    val deliveryMemo: String? = null
) : BaseJpaEntity()
