package io.hhplus.ecommerce.order.application.usecase

import com.fasterxml.jackson.databind.ObjectMapper
import io.hhplus.ecommerce.cart.domain.service.CartDomainService
import io.hhplus.ecommerce.common.annotation.DistributedLock
import io.hhplus.ecommerce.common.annotation.DistributedTransaction
import io.hhplus.ecommerce.common.lock.DistributedLockKeys
import io.hhplus.ecommerce.common.outbox.OutboxEventService
import io.hhplus.ecommerce.coupon.application.CouponService
import io.hhplus.ecommerce.delivery.domain.constant.DeliveryStatus
import io.hhplus.ecommerce.delivery.domain.service.DeliveryDomainService
import io.hhplus.ecommerce.inventory.application.InventoryService
import io.hhplus.ecommerce.order.domain.entity.Order
import io.hhplus.ecommerce.order.domain.model.OrderItemData
import io.hhplus.ecommerce.order.domain.service.OrderDomainService
import io.hhplus.ecommerce.order.exception.OrderException
import io.hhplus.ecommerce.order.presentation.dto.CreateOrderRequest
import io.hhplus.ecommerce.payment.application.usecase.ProcessPaymentUseCase
import io.hhplus.ecommerce.payment.presentation.dto.ProcessPaymentRequest
import io.hhplus.ecommerce.point.domain.service.PointDomainService
import io.hhplus.ecommerce.point.domain.vo.PointAmount
import io.hhplus.ecommerce.product.application.port.out.ProductStatisticsPort
import io.hhplus.ecommerce.product.domain.service.ProductDomainService
import mu.KotlinLogging
import org.springframework.stereotype.Component

/**
 * 주문 명령 UseCase - 애플리케이션 계층
 *
 * 역할:
 * - 트랜잭션 경계 관리
 * - 주문 변경 작업 오케스트레이션
 * - 외부 도메인 서비스 협력 조정
 * - 이벤트 발행 및 통계 기록
 *
 * 책임:
 * - 주문 생성, 취소, 확정 기능 제공
 * - 주문 변경 요청 검증 및 실행
 * - 다른 도메인과의 연계 처리
 */
@Component
class OrderCommandUseCase(
    private val orderDomainService: OrderDomainService,
    private val productDomainService: ProductDomainService,
    private val couponService: CouponService,
    private val processPaymentUseCase: ProcessPaymentUseCase,
    private val deliveryDomainService: DeliveryDomainService,
    private val inventoryService: InventoryService,
    private val pointDomainService: PointDomainService,
    private val cartDomainService: CartDomainService,
    private val productStatisticsPort: ProductStatisticsPort,
    private val outboxEventService: OutboxEventService,
    private val objectMapper: ObjectMapper
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 주문 생성 요청 처리 (재시도 로직 포함)
     */
    fun createOrder(request: CreateOrderRequest): Order {
        return processOrderWithRetry(request)
    }

    /**
     * 재시도 로직이 포함된 주문 처리
     */
    private fun processOrderWithRetry(
        request: CreateOrderRequest,
        maxRetries: Int = 3,
        baseDelayMs: Long = 100L
    ): Order {
        var lastException: Exception? = null

        repeat(maxRetries + 1) { attempt ->
            try {
                return processOrder(request)
            } catch (e: Exception) {
                lastException = e

                if (attempt < maxRetries) {
                    val delayMs = baseDelayMs * (1L shl attempt)
                    logger.warn("주문 처리 실패 (재시도 ${attempt + 1}/${maxRetries}): ${e.message}")

                    try {
                        Thread.sleep(delayMs)
                    } catch (ie: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw ie
                    }
                } else {
                    logger.error("주문 처리 최종 실패 (최대 재시도 초과): ${e.message}", e)
                }
            }
        }

        throw RuntimeException("주문 처리 실패: 최대 재시도 횟수($maxRetries)를 초과했습니다", lastException)
    }

    /**
     * 주문 요청 직접 처리
     */
    @DistributedLock(key = DistributedLockKeys.Order.PROCESS, waitTime = 10L, leaseTime = 60L)
    @DistributedTransaction
    fun processOrder(request: CreateOrderRequest): Order {
        // 1. 상품 정보 검증 및 가격 계산
        val orderItems = validateAndPrepareOrderItems(request)
        val totalAmount = calculateTotalAmount(orderItems)

        // 2. 쿠폰 검증 (선택적)
        val discountAmount = validateCoupon(request, totalAmount)

        // 3. 핵심 주문 처리 (분산락 + 트랜잭션 보장)
        val order = processOrderCore(request, orderItems, totalAmount, discountAmount)

        // 4. 이벤트 발행
        publishOrderCreatedEvent(order, request.userId, totalAmount)

        // 5. 부가 처리 (실패해도 주문 성공 유지)
        processOrderSideEffects(request, orderItems, order)

        return order
    }

    /**
     * 주문 핵심 처리 로직 (트랜잭션 보장)
     */
    private fun processOrderCore(
        request: CreateOrderRequest,
        orderItems: List<OrderItemData>,
        totalAmount: Long,
        discountAmount: Long
    ): Order {
        // 포인트 사용
        pointDomainService.usePoint(
            userId = request.userId,
            amount = PointAmount.of(totalAmount - discountAmount)
        )

        // 재고 처리 (productId 정렬로 데드락 방지)
        orderItems.sortedBy { it.productId }.forEach { item ->
            if (item.requiresReservation) {
                inventoryService.confirmReservation(item.productId, item.quantity)
            } else {
                inventoryService.deductStock(item.productId, item.quantity)
            }
        }

        // 주문 생성
        val savedOrder = orderDomainService.createOrder(
            userId = request.userId,
            items = orderItems,
            usedCouponId = request.usedCouponId,
            totalAmount = totalAmount,
            discountAmount = discountAmount
        )

        // 쿠폰 사용 처리
        request.usedCouponId?.let { couponId ->
            couponService.applyCoupon(request.userId, couponId, savedOrder.id, totalAmount)
        }

        return savedOrder
    }

    /**
     * 주문 생성 이벤트 발행
     */
    private fun publishOrderCreatedEvent(order: Order, userId: Long, totalAmount: Long) {
        val orderEventPayload = mapOf(
            "orderId" to order.id,
            "userId" to userId,
            "orderNumber" to order.orderNumber,
            "totalAmount" to totalAmount,
            "finalAmount" to order.finalAmount,
            "status" to order.status.name
        )

        outboxEventService.publishEvent(
            eventType = "OrderCreated",
            aggregateType = "Order",
            aggregateId = order.id.toString(),
            payload = objectMapper.writeValueAsString(orderEventPayload)
        )
    }

    /**
     * 주문 부가 처리 (실패해도 주문 성공 유지)
     */
    private fun processOrderSideEffects(
        request: CreateOrderRequest,
        orderItems: List<OrderItemData>,
        order: Order
    ) {
        // 결제 처리 (기록용)
        try {
            processPaymentUseCase.execute(
                ProcessPaymentRequest(
                    userId = request.userId,
                    orderId = order.id,
                    amount = order.finalAmount
                )
            )
        } catch (e: Exception) {
            logger.warn("결제 기록 실패 (주문은 성공): ${e.message}")
        }

        // 배송 정보 생성
        try {
            deliveryDomainService.validateNoDuplicateDelivery(order.id)
            deliveryDomainService.createDelivery(
                orderId = order.id,
                deliveryAddress = request.deliveryAddress.toVo(),
                deliveryMemo = request.deliveryAddress.deliveryMessage
            )
        } catch (e: Exception) {
            logger.warn("배송 정보 생성 실패 (주문은 성공): ${e.message}")
        }

        // 장바구니 정리
        try {
            val orderedProductIds = orderItems.map { it.productId }
            cartDomainService.removeOrderedItems(request.userId, orderedProductIds)
        } catch (e: Exception) {
            logger.warn("장바구니 정리 실패 (주문은 성공): ${e.message}")
        }
    }

    /**
     * 상품 정보 검증 및 주문 아이템 데이터 준비
     */
    private fun validateAndPrepareOrderItems(request: CreateOrderRequest): List<OrderItemData> {
        return request.items.map { item ->
            val product = productDomainService.getProduct(item.productId)
            OrderItemData(
                productId = item.productId,
                productName = product.name,
                categoryName = "기본카테고리",
                quantity = item.quantity,
                unitPrice = product.price.toInt(),
                giftWrap = item.giftWrap,
                giftMessage = item.giftMessage,
                giftWrapPrice = if (item.giftWrap) 2000 else 0,
                totalPrice = (product.price.toInt() * item.quantity) + if (item.giftWrap) 2000 else 0,
                requiresReservation = product.requiresStockReservation()
            )
        }
    }

    /**
     * 총 주문 금액 계산
     */
    private fun calculateTotalAmount(orderItems: List<OrderItemData>): Long {
        return orderItems.sumOf { it.totalPrice }.toLong()
    }

    /**
     * 쿠폰 사용 가능 여부 검증 및 할인 금액 계산
     */
    private fun validateCoupon(request: CreateOrderRequest, totalAmount: Long): Long {
        return request.usedCouponId?.let { couponId ->
            couponService.validateCouponUsage(request.userId, couponId, totalAmount)
        } ?: 0L
    }

    /**
     * 주문 취소
     */
    @DistributedLock(key = DistributedLockKeys.Order.CANCEL)
    @DistributedTransaction
    fun cancelOrder(orderId: Long, reason: String?): Order {
        // 1. 배송 상태 확인
        val delivery = deliveryDomainService.getDeliveryByOrderId(orderId)
        delivery.let {
            if (it.status in listOf(
                    DeliveryStatus.PREPARING,
                    DeliveryStatus.SHIPPED,
                    DeliveryStatus.DELIVERED
                )
            ) {
                val order = orderDomainService.getOrder(orderId)
                    ?: throw IllegalArgumentException("주문을 찾을 수 없습니다")
                throw OrderException.OrderCancellationNotAllowed(order.orderNumber, order.status)
            }
        }

        // 2. 주문 취소 처리
        val cancelledOrder = orderDomainService.cancelOrder(orderId, reason)

        // 3. 취소 이벤트 발행
        publishOrderCancelledEvent(cancelledOrder, reason)

        // 4. 주문 아이템 조회
        val orderItems = orderDomainService.getOrderItems(orderId)

        // 5. 재고 복구
        orderItems.forEach { orderItem ->
            inventoryService.restockInventory(
                productId = orderItem.productId,
                quantity = orderItem.quantity
            )
        }

        // 6. 포인트 환불
        pointDomainService.earnPoint(
            userId = cancelledOrder.userId,
            amount = PointAmount.of(cancelledOrder.finalAmount)
        )

        return cancelledOrder
    }

    /**
     * 주문 취소 이벤트 발행
     */
    private fun publishOrderCancelledEvent(order: Order, reason: String?) {
        val cancelEventPayload = mapOf(
            "orderId" to order.id,
            "userId" to order.userId,
            "orderNumber" to order.orderNumber,
            "reason" to (reason ?: "사용자 요청"),
            "status" to order.status.name
        )

        outboxEventService.publishEvent(
            eventType = "OrderCancelled",
            aggregateType = "Order",
            aggregateId = order.id.toString(),
            payload = objectMapper.writeValueAsString(cancelEventPayload)
        )
    }

    /**
     * 주문 확정
     */
    @DistributedLock(key = DistributedLockKeys.Order.CONFIRM)
    @DistributedTransaction
    fun confirmOrder(orderId: Long): Order {
        val confirmedOrder = orderDomainService.confirmOrder(orderId)

        // 주문 완료 시 판매 이벤트 발생
        val orderItems = orderDomainService.getOrderItems(orderId)
        orderItems.forEach { orderItem ->
            productStatisticsPort.recordSalesEvent(
                productId = orderItem.productId,
                quantity = orderItem.quantity,
                orderId = orderId
            )
        }

        return confirmedOrder
    }
}
