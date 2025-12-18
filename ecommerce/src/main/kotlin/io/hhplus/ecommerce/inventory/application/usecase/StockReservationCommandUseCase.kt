package io.hhplus.ecommerce.inventory.application.usecase

import io.hhplus.ecommerce.common.annotation.DistributedLock
import io.hhplus.ecommerce.common.annotation.DistributedTransaction
import io.hhplus.ecommerce.inventory.domain.entity.StockReservation
import io.hhplus.ecommerce.inventory.domain.service.InventoryDomainService
import io.hhplus.ecommerce.inventory.domain.service.StockReservationDomainService
import io.hhplus.ecommerce.inventory.exception.InventoryException
import io.hhplus.ecommerce.order.domain.service.OrderDomainService
import mu.KotlinLogging
import org.springframework.stereotype.Component

/**
 * 재고 예약 명령 UseCase - 애플리케이션 계층
 *
 * 역할:
 * - 트랜잭션 경계 관리
 * - 분산락을 통한 동시성 제어
 * - 재고 예약 작업 오케스트레이션
 *
 * 책임:
 * - 재고 예약, 확정, 취소 기능 제공
 * - 만료된 예약 처리
 * - InventoryDomainService, StockReservationDomainService 협력 조정
 */
@Component
class StockReservationCommandUseCase(
    private val inventoryDomainService: InventoryDomainService,
    private val stockReservationDomainService: StockReservationDomainService,
    private val orderDomainService: OrderDomainService
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 재고 예약 생성
     */
    @DistributedLock(key = "'inventory:reservation:' + #productId")
    @DistributedTransaction
    fun reserveStock(
        productId: Long,
        userId: Long,
        quantity: Int,
        reservationMinutes: Int = 20
    ): StockReservation {
        // 기존 예약 확인
        val existingReservation = stockReservationDomainService.findActiveReservation(userId, productId)
        if (existingReservation != null && existingReservation.isReservationActive()) {
            throw InventoryException.StockAlreadyReserved(productId, userId)
        }

        // 재고 예약
        inventoryDomainService.reserveStock(productId, quantity)

        // 예약 기록 생성
        return stockReservationDomainService.createReservation(
            productId = productId,
            userId = userId,
            quantity = quantity,
            reservationMinutes = reservationMinutes
        )
    }

    /**
     * 예약 확정
     */
    @DistributedLock(key = "'inventory:reservation:confirm:' + #reservationId")
    @DistributedTransaction
    fun confirmReservation(reservationId: Long, userId: Long): StockReservation {
        val reservation = stockReservationDomainService.getReservationOrThrow(reservationId)

        // 검증
        stockReservationDomainService.validateReservationAccess(reservation, userId)
        stockReservationDomainService.validateReservationActive(reservation)

        // 재고에서 예약 확정
        inventoryDomainService.confirmReservation(reservation.productId, reservation.quantity)

        // 예약 상태 변경
        return stockReservationDomainService.confirmReservation(reservation)
    }

    /**
     * 예약 취소
     */
    @DistributedLock(key = "'inventory:reservation:cancel:' + #reservationId")
    @DistributedTransaction
    fun cancelReservation(reservationId: Long, userId: Long): StockReservation {
        val reservation = stockReservationDomainService.getReservationOrThrow(reservationId)

        // 검증
        stockReservationDomainService.validateReservationAccess(reservation, userId)
        stockReservationDomainService.validateReservationCancellable(reservation)

        // 재고 예약 해제
        inventoryDomainService.releaseReservation(reservation.productId, reservation.quantity)

        // 예약 취소
        return stockReservationDomainService.cancelReservation(reservation)
    }

    /**
     * 만료된 예약 처리
     *
     * 1. 재고 예약 해제
     * 2. 예약 상태 만료 처리
     * 3. 연결된 주문 만료 처리 (PENDING_PAYMENT → EXPIRED)
     */
    @DistributedTransaction
    fun expireReservations(): Int {
        val expiredReservations = stockReservationDomainService.findExpiredReservations()
        var expiredCount = 0

        // orderId별로 그룹화하여 주문 만료는 한 번만 처리
        val processedOrderIds = mutableSetOf<Long>()

        expiredReservations.forEach { reservation ->
            try {
                // 1. 재고 예약 해제
                val inventory = inventoryDomainService.getInventory(reservation.productId)
                if (inventory != null) {
                    inventoryDomainService.releaseReservation(reservation.productId, reservation.quantity)
                }

                // 2. 예약 만료 처리
                stockReservationDomainService.expireReservation(reservation)

                // 3. 연결된 주문 만료 처리 (중복 방지)
                reservation.orderId?.let { orderId ->
                    if (!processedOrderIds.contains(orderId)) {
                        try {
                            orderDomainService.expireOrder(orderId)
                            processedOrderIds.add(orderId)
                            logger.info("[ReservationExpiry] 주문 만료 처리 완료: orderId=$orderId")
                        } catch (e: Exception) {
                            logger.warn("[ReservationExpiry] 주문 만료 처리 실패 (이미 처리되었거나 상태 불일치): orderId=$orderId, error=${e.message}")
                        }
                    }
                }

                expiredCount++
                logger.debug("[ReservationExpiry] 예약 만료 처리 완료: reservationId=${reservation.id}")
            } catch (e: Exception) {
                logger.warn("[ReservationExpiry] Failed to expire reservation ${reservation.id}: ${e.message}")
            }
        }

        return expiredCount
    }

    /**
     * 오래된 예약 데이터 정리
     */
    @DistributedTransaction
    fun cleanupOldReservations(daysOld: Long = 30): Int {
        return stockReservationDomainService.cleanupOldReservations(daysOld)
    }
}
