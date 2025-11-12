package io.hhplus.ecommerce.payment.domain.repository

import io.hhplus.ecommerce.payment.domain.entity.PaymentHistory

/**
 * PaymentHistory Repository Interface
 *
 * 역할:
 * - 결제 이력 도메인 모델의 영속성 추상화
 * - 인프라 계층의 구현 세부사항 숨김
 *
 * 책임:
 * - 결제 이력 저장 및 조회 메서드 정의
 * - 도메인 계층과 인프라 계층 간 경계 정의
 */
interface PaymentHistoryRepository {
    /**
     * 결제 이력 저장
     *
     * @param paymentHistory 저장할 결제 이력 도메인 모델
     * @return 저장된 결제 이력 도메인 모델
     */
    fun save(paymentHistory: PaymentHistory): PaymentHistory

    /**
     * ID로 결제 이력 조회
     *
     * @param id 결제 이력 ID
     * @return 결제 이력 도메인 모델 또는 null
     */
    fun findById(id: Long): PaymentHistory?

    /**
     * 결제 ID로 이력 목록 조회
     *
     * @param paymentId 결제 ID
     * @return 결제 이력 목록
     */
    fun findByPaymentId(paymentId: Long): List<PaymentHistory>

    /**
     * 결제 ID로 이력 목록 조회 (생성 시간 내림차순)
     *
     * @param paymentId 결제 ID
     * @return 결제 이력 목록
     */
    fun findByPaymentIdOrderByCreatedAtDesc(paymentId: Long): List<PaymentHistory>

    /**
     * 특정 상태로 변경된 이력 조회
     *
     * @param statusAfter 변경 후 상태
     * @return 결제 이력 목록
     */
    fun findByStatusAfter(statusAfter: String): List<PaymentHistory>

    /**
     * 결제 ID와 상태로 이력 조회
     *
     * @param paymentId 결제 ID
     * @param statusAfter 변경 후 상태
     * @return 결제 이력 목록
     */
    fun findByPaymentIdAndStatusAfter(paymentId: Long, statusAfter: String): List<PaymentHistory>
}
