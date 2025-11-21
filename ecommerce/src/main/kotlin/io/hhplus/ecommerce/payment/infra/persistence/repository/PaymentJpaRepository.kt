package io.hhplus.ecommerce.payment.infra.persistence.repository

import io.hhplus.ecommerce.payment.domain.constant.PaymentStatus
import io.hhplus.ecommerce.payment.infra.persistence.entity.PaymentJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import jakarta.persistence.LockModeType

/**
 * Payment JPA Repository
 *
 * 역할:
 * - Spring Data JPA를 사용한 결제 영속성 처리
 * - 기본 CRUD 및 사용자 정의 쿼리 제공
 *
 * 책임:
 * - 결제 엔티티 저장/조회/수정/삭제
 * - 결제번호, 주문ID, 사용자ID, 상태 기반 조회
 * - 외부 거래 ID 기반 조회
 *
 * 주의:
 * - PaymentHistory는 별도 Repository를 통해 조회해야 합니다.
 */
interface PaymentJpaRepository : JpaRepository<PaymentJpaEntity, Long> {

    /**
     * 결제번호로 결제 조회
     */
    fun findByPaymentNumber(paymentNumber: String): PaymentJpaEntity?

    /**
     * 주문 ID로 결제 목록 조회
     */
    fun findByOrderId(orderId: Long): List<PaymentJpaEntity>

    /**
     * 사용자 ID로 결제 목록 조회
     */
    fun findByUserId(userId: Long): List<PaymentJpaEntity>

    /**
     * 결제 상태로 결제 목록 조회
     */
    fun findByStatus(status: PaymentStatus): List<PaymentJpaEntity>

    /**
     * 외부 거래 ID로 결제 조회
     */
    fun findByExternalTransactionId(externalTransactionId: String): PaymentJpaEntity?

    /**
     * 결제번호 존재 여부 확인
     */
    fun existsByPaymentNumber(paymentNumber: String): Boolean

    /**
     * 주문 ID로 결제 조회 (비관적 락 적용)
     * - 중복 결제 방지를 위해 사용
     * - 동시 결제 요청 시 순차 처리 보장
     */
    @Query("SELECT p FROM PaymentJpaEntity p WHERE p.orderId = :orderId AND p.deletedAt IS NULL")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByOrderIdWithLock(orderId: Long): PaymentJpaEntity?

    /**
     * 활성 결제 전체 조회 (deletedAt이 null인 경우만)
     */
    @Query("SELECT p FROM PaymentJpaEntity p WHERE p.deletedAt IS NULL")
    fun findActivePayments(): List<PaymentJpaEntity>

}
