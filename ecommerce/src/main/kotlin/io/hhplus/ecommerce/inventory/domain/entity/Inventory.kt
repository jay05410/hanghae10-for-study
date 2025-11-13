package io.hhplus.ecommerce.inventory.domain.entity

import io.hhplus.ecommerce.common.exception.inventory.InventoryException
import java.time.LocalDateTime

/**
 * 재고 도메인 모델 (순수 비즈니스 로직)
 *
 * 역할:
 * - 상품 재고 정보 관리
 * - 재고 수량 변경 및 예약 관리
 * - 재고 가용성 검증
 *
 * 비즈니스 규칙:
 * - 가용 재고 = 전체 재고 - 예약된 재고
 * - 재고 차감/예약 시 가용 재고 검증 필수
 * - 모든 재고 변경 작업은 불변 객체 반환
 *
 * 주의: 이 클래스는 순수 도메인 모델이며 JPA 어노테이션이 없습니다.
 *       영속성은 infra/persistence/entity/InventoryJpaEntity에서 처리됩니다.
 */
data class Inventory(
    val id: Long = 0,
    val productId: Long,
    var quantity: Int = 0,
    var reservedQuantity: Int = 0,
    var version: Int = 0,
    var isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    val createdBy: Long = 0,
    var updatedBy: Long = 0
) {
    /**
     * 가용 재고 수량 조회
     *
     * @return 전체 재고 - 예약된 재고
     */
    fun getAvailableQuantity(): Int = quantity - reservedQuantity

    /**
     * 재고 가용성 확인
     *
     * @param requestedQuantity 요청 수량
     * @return 가용 재고가 요청 수량 이상이면 true
     */
    fun isStockAvailable(requestedQuantity: Int): Boolean = getAvailableQuantity() >= requestedQuantity

    /**
     * 재고 차감
     *
     * @param requestedQuantity 차감할 수량
     * @param deductedBy 차감 수행자 ID
     * @throws InventoryException.InsufficientStock 재고 부족 시
     */
    fun deduct(requestedQuantity: Int, deductedBy: Long) {
        if (!isStockAvailable(requestedQuantity)) {
            throw InventoryException.InsufficientStock(productId, getAvailableQuantity(), requestedQuantity)
        }

        this.quantity -= requestedQuantity
        this.updatedBy = deductedBy
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 재고 보충
     *
     * @param additionalQuantity 보충할 수량
     * @param restockedBy 보충 수행자 ID
     * @throws IllegalArgumentException 추가 수량이 0 이하일 때
     */
    fun restock(additionalQuantity: Int, restockedBy: Long) {
        require(additionalQuantity > 0) { "추가할 재고 수량은 0보다 커야 합니다" }

        this.quantity += additionalQuantity
        this.updatedBy = restockedBy
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 재고 예약
     *
     * @param requestedQuantity 예약할 수량
     * @param reservedBy 예약 수행자 ID
     * @throws InventoryException.InsufficientStock 재고 부족 시
     */
    fun reserve(requestedQuantity: Int, reservedBy: Long) {
        if (!isStockAvailable(requestedQuantity)) {
            throw InventoryException.InsufficientStock(productId, getAvailableQuantity(), requestedQuantity)
        }

        this.reservedQuantity += requestedQuantity
        this.updatedBy = reservedBy
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 예약 해제
     *
     * @param releaseQuantity 해제할 수량
     * @param releasedBy 해제 수행자 ID
     * @throws IllegalArgumentException 해제 수량이 잘못되었을 때
     */
    fun releaseReservation(releaseQuantity: Int, releasedBy: Long) {
        require(releaseQuantity > 0) { "해제할 예약 수량은 0보다 커야 합니다" }
        require(releaseQuantity <= reservedQuantity) { "해제할 수량이 예약된 수량보다 클 수 없습니다" }

        this.reservedQuantity -= releaseQuantity
        this.updatedBy = releasedBy
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * 예약 확정 (재고 차감 + 예약 해제)
     *
     * @param confirmQuantity 확정할 수량
     * @param confirmedBy 확정 수행자 ID
     * @throws IllegalArgumentException 확정 수량이 잘못되었을 때
     */
    fun confirmReservation(confirmQuantity: Int, confirmedBy: Long) {
        require(confirmQuantity > 0) { "확정할 예약 수량은 0보다 커야 합니다" }
        require(confirmQuantity <= reservedQuantity) { "확정할 수량이 예약된 수량보다 클 수 없습니다" }

        this.quantity -= confirmQuantity
        this.reservedQuantity -= confirmQuantity
        this.updatedBy = confirmedBy
        this.updatedAt = LocalDateTime.now()
    }

    companion object {
        /**
         * 재고 생성 팩토리 메서드
         *
         * @param productId 상품 ID
         * @param initialQuantity 초기 재고 수량
         * @param createdBy 생성자 ID
         * @return 생성된 Inventory 도메인 모델
         */
        fun create(
            productId: Long,
            initialQuantity: Int = 0,
            createdBy: Long
        ): Inventory {
            require(productId > 0) { "상품 ID는 유효해야 합니다" }
            require(initialQuantity >= 0) { "초기 재고는 0 이상이어야 합니다" }

            val now = LocalDateTime.now()
            return Inventory(
                productId = productId,
                quantity = initialQuantity,
                createdBy = createdBy,
                updatedBy = createdBy,
                createdAt = now,
                updatedAt = now
            )
        }
    }
}