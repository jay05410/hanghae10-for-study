package io.hhplus.ecommerce.point.infra.persistence.entity

import io.hhplus.ecommerce.common.baseentity.BaseJpaEntity
import io.hhplus.ecommerce.point.domain.constant.PointTransactionType
import jakarta.persistence.*

/**
 * 포인트 거래 이력 JPA 엔티티 (영속성 모델)
 *
 * 역할:
 * - 데이터베이스 테이블과 매핑
 * - JPA 관련 어노테이션 및 설정 포함
 * - 도메인 모델과 분리하여 인프라 관심사 캡슐화
 *
 * 주의: 이 클래스는 영속성 전용이며 비즈니스 로직을 포함하지 않습니다.
 *       비즈니스 로직은 domain/entity/PointHistory에 있습니다.
 *       createdAt, updatedAt, createdBy, updatedBy, deletedAt는 BaseJpaEntity에서 상속받습니다.
 */
@Entity
@Table(
    name = "point_history",
    indexes = [
        Index(name = "idx_point_history_user", columnList = "user_id, created_at"),
        Index(name = "idx_point_history_order", columnList = "order_id"),
        Index(name = "idx_point_history_type", columnList = "transaction_type, created_at")
    ]
)
class PointHistoryJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val amount: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, name = "transaction_type")
    val transactionType: PointTransactionType,

    @Column(nullable = false, name = "balance_before")
    val balanceBefore: Long,

    @Column(nullable = false, name = "balance_after")
    val balanceAfter: Long,

    @Column(name = "order_id")
    val orderId: Long? = null,

    @Column(length = 500)
    val description: String? = null
) : BaseJpaEntity()
