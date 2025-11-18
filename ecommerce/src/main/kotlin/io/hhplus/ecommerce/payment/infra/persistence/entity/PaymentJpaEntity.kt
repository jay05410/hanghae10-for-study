package io.hhplus.ecommerce.payment.infra.persistence.entity

import io.hhplus.ecommerce.common.baseentity.BaseJpaEntity
import io.hhplus.ecommerce.payment.domain.constant.PaymentMethod
import io.hhplus.ecommerce.payment.domain.constant.PaymentStatus
import jakarta.persistence.*

/**
 * 결제 JPA 엔티티 (영속성 모델)
 *
 * 역할:
 * - 데이터베이스 테이블과 매핑
 * - JPA 관련 어노테이션 및 설정 포함
 * - 도메인 모델과 분리하여 인프라 관심사 캡슐화
 *
 * 주의: 이 클래스는 영속성 전용이며 비즈니스 로직을 포함하지 않습니다.
 *       비즈니스 로직은 domain/entity/Payment에 있습니다.
 *       createdAt, updatedAt, createdBy, updatedBy, deletedAt는 BaseJpaEntity에서 상속받습니다.
 */
@Entity
@Table(
    name = "payments",
    indexes = [
        Index(name = "uk_payments_payment_number", columnList = "payment_number", unique = true),
        Index(name = "idx_payments_order_id", columnList = "order_id"),
        Index(name = "idx_payments_user_id", columnList = "user_id"),
        Index(name = "idx_payments_status", columnList = "status"),
        Index(name = "idx_payments_external_tx_id", columnList = "external_transaction_id")
    ]
)
class PaymentJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 50, name = "payment_number")
    val paymentNumber: String,

    @Column(nullable = false, name = "order_id")
    val orderId: Long,

    @Column(nullable = false, name = "user_id")
    val userId: Long,

    @Column(nullable = false)
    val amount: Long,

    @Column(nullable = false, length = 20, name = "payment_method")
    @Enumerated(EnumType.STRING)
    val paymentMethod: PaymentMethod,

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    val status: PaymentStatus = PaymentStatus.PENDING,

    @Column(length = 100, name = "external_transaction_id")
    val externalTransactionId: String? = null,

    @Column(columnDefinition = "TEXT", name = "failure_reason")
    val failureReason: String? = null,

    @OneToMany(mappedBy = "payment", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val paymentHistories: List<PaymentHistoryJpaEntity> = mutableListOf()
) : BaseJpaEntity()
