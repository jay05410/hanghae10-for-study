package io.hhplus.ecommerce.payment.infra.persistence.entity

import io.hhplus.ecommerce.common.baseentity.BaseJpaEntity
import jakarta.persistence.*

/**
 * 결제 이력 JPA 엔티티 (영속성 모델)
 *
 * 역할:
 * - 데이터베이스 테이블과 매핑
 * - JPA 관련 어노테이션 및 설정 포함
 * - 도메인 모델과 분리하여 인프라 관심사 캡슐화
 *
 * 주의: 이 클래스는 영속성 전용이며 비즈니스 로직을 포함하지 않습니다.
 *       비즈니스 로직은 domain/entity/PaymentHistory에 있습니다.
 *       createdAt, updatedAt, createdBy, updatedBy, deletedAt는 BaseJpaEntity에서 상속받습니다.
 */
@Entity
@Table(
    name = "payment_history",
    indexes = [
        Index(name = "idx_payment_history_payment_id", columnList = "payment_id"),
        Index(name = "idx_payment_history_status_after", columnList = "status_after"),
        Index(name = "idx_payment_history_created_at", columnList = "created_at")
    ]
)
class PaymentHistoryJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, name = "payment_id")
    val paymentId: Long,

    @Column(length = 20, name = "status_before")
    val statusBefore: String? = null,

    @Column(nullable = false, length = 20, name = "status_after")
    val statusAfter: String,

    @Column(length = 500)
    val reason: String? = null,

    @Column(columnDefinition = "TEXT", name = "pg_response")
    val pgResponse: String? = null,

    @Column(nullable = false)
    val amount: Long
) : BaseJpaEntity()
