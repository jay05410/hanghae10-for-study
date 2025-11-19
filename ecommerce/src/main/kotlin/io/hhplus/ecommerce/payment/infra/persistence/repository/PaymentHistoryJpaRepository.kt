package io.hhplus.ecommerce.payment.infra.persistence.repository

import io.hhplus.ecommerce.payment.infra.persistence.entity.PaymentHistoryJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/**
 * PaymentHistory JPA Repository
 *
 * 역할:
 * - Spring Data JPA를 사용한 결제 이력 영속성 처리
 * - 기본 CRUD 및 사용자 정의 쿼리 제공
 *
 * 책임:
 * - 결제 이력 엔티티 저장/조회/수정/삭제
 * - 결제 ID 기반 이력 조회
 * - 상태 및 시간 기반 조회
 */
interface PaymentHistoryJpaRepository : JpaRepository<PaymentHistoryJpaEntity, Long> {

    /**
     * 결제 ID로 이력 목록 조회
     */
    @Query("SELECT ph FROM PaymentHistoryJpaEntity ph WHERE ph.paymentId = :paymentId")
    fun findByPaymentId(@Param("paymentId") paymentId: Long): List<PaymentHistoryJpaEntity>

    /**
     * 결제 ID로 이력 목록 조회 (생성 시간 내림차순)
     */
    @Query("SELECT ph FROM PaymentHistoryJpaEntity ph WHERE ph.paymentId = :paymentId ORDER BY ph.createdAt DESC")
    fun findByPaymentIdOrderByCreatedAtDesc(@Param("paymentId") paymentId: Long): List<PaymentHistoryJpaEntity>

    /**
     * 특정 상태로 변경된 이력 조회
     */
    @Query("SELECT ph FROM PaymentHistoryJpaEntity ph WHERE ph.statusAfter = :statusAfter")
    fun findByStatusAfter(@Param("statusAfter") statusAfter: String): List<PaymentHistoryJpaEntity>

    /**
     * 결제 ID와 상태로 이력 조회
     */
    @Query("SELECT ph FROM PaymentHistoryJpaEntity ph WHERE ph.paymentId = :paymentId AND ph.statusAfter = :statusAfter")
    fun findByPaymentIdAndStatusAfter(@Param("paymentId") paymentId: Long, @Param("statusAfter") statusAfter: String): List<PaymentHistoryJpaEntity>

    /**
     * 활성 결제 이력 전체 조회 (deletedAt이 null인 경우만)
     */
    @Query("SELECT ph FROM PaymentHistoryJpaEntity ph WHERE ph.deletedAt IS NULL")
    fun findActivePaymentHistories(): List<PaymentHistoryJpaEntity>

    /**
     * 결제 ID의 최근 이력 조회
     */
    @Query("SELECT ph FROM PaymentHistoryJpaEntity ph WHERE ph.paymentId = :paymentId AND ph.deletedAt IS NULL ORDER BY ph.createdAt DESC")
    fun findLatestByPaymentId(@Param("paymentId") paymentId: Long): List<PaymentHistoryJpaEntity>
}
