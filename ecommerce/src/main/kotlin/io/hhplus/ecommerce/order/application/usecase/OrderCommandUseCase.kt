package io.hhplus.ecommerce.order.application.usecase

import com.fasterxml.jackson.databind.ObjectMapper
import io.hhplus.ecommerce.common.annotation.DistributedLock
import io.hhplus.ecommerce.common.annotation.DistributedTransaction
import io.hhplus.ecommerce.common.lock.DistributedLockKeys
import io.hhplus.ecommerce.common.outbox.EventRegistry
import io.hhplus.ecommerce.common.outbox.OutboxEventService
import io.hhplus.ecommerce.order.domain.entity.Order
import io.hhplus.ecommerce.order.domain.service.OrderDomainService
import io.hhplus.ecommerce.order.presentation.dto.CreateOrderRequest
import mu.KotlinLogging
import org.springframework.stereotype.Component

/**
 * 주문 명령 UseCase - 애플리케이션 계층
 *
 * 역할:
 * - 주문 생성/취소/확정의 핵심 로직만 수행
 * - 이벤트 발행으로 다른 도메인과 느슨하게 연결
 *
 * 원칙:
 * - 주문은 주문만 담당
 * - 다른 도메인 로직은 이벤트 핸들러에서 처리
 *
 * 이벤트 흐름:
 * - OrderCreated → 포인트 차감, 재고 차감, 쿠폰 사용, 배송 생성, 장바구니 정리, 결제 기록
 * - OrderCancelled → 재고 복구, 포인트 환불
 * - OrderConfirmed → 통계 기록
 */
@Component
class OrderCommandUseCase(
    private val orderDomainService: OrderDomainService,
    private val outboxEventService: OutboxEventService,
    private val objectMapper: ObjectMapper
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 주문 생성
     *
     * 주문만 생성하고, 나머지 로직은 이벤트 핸들러에서 처리
     */
    @DistributedLock(key = DistributedLockKeys.Order.PROCESS, waitTime = 10L, leaseTime = 60L)
    @DistributedTransaction
    fun createOrder(request: CreateOrderRequest): Order {
        // 주문 생성
        val savedOrder = orderDomainService.createOrderFromRequest(request)

        // 이벤트 발행
        publishOrderCreatedEvent(savedOrder, request)

        logger.info("[OrderCommandUseCase] 주문 생성 완료: orderId=${savedOrder.id}")
        return savedOrder
    }

    private fun publishOrderCreatedEvent(order: Order, request: CreateOrderRequest) {
        val payload = mapOf(
            "orderId" to order.id,
            "userId" to order.userId,
            "orderNumber" to order.orderNumber,
            "totalAmount" to order.totalAmount,
            "finalAmount" to order.finalAmount,
            "discountAmount" to order.discountAmount,
            "status" to order.status.name,
            "usedCouponId" to request.usedCouponId,
            "items" to request.items.map { item ->
                mapOf(
                    "productId" to item.productId,
                    "quantity" to item.quantity,
                    "giftWrap" to item.giftWrap,
                    "giftMessage" to item.giftMessage
                )
            },
            "deliveryAddress" to mapOf(
                "recipientName" to request.deliveryAddress.recipientName,
                "phone" to request.deliveryAddress.phone,
                "zipCode" to request.deliveryAddress.zipCode,
                "address" to request.deliveryAddress.address,
                "addressDetail" to request.deliveryAddress.addressDetail,
                "deliveryMessage" to request.deliveryAddress.deliveryMessage
            )
        )

        outboxEventService.publishEvent(
            eventType = EventRegistry.EventTypes.ORDER_CREATED,
            aggregateType = EventRegistry.AggregateTypes.ORDER,
            aggregateId = order.id.toString(),
            payload = objectMapper.writeValueAsString(payload)
        )
    }

    /**
     * 주문 취소
     */
    @DistributedLock(key = DistributedLockKeys.Order.CANCEL)
    @DistributedTransaction
    fun cancelOrder(orderId: Long, reason: String?): Order {
        val cancelledOrder = orderDomainService.cancelOrder(orderId, reason)
        val orderItems = orderDomainService.getOrderItems(orderId)
        publishOrderCancelledEvent(cancelledOrder, reason, orderItems)
        logger.info("[OrderCommandUseCase] 주문 취소 완료: orderId=$orderId")
        return cancelledOrder
    }

    private fun publishOrderCancelledEvent(
        order: Order,
        reason: String?,
        orderItems: List<io.hhplus.ecommerce.order.domain.entity.OrderItem>
    ) {
        val payload = mapOf(
            "orderId" to order.id,
            "userId" to order.userId,
            "orderNumber" to order.orderNumber,
            "finalAmount" to order.finalAmount,
            "reason" to (reason ?: "사용자 요청"),
            "status" to order.status.name,
            "items" to orderItems.map { item ->
                mapOf("productId" to item.productId, "quantity" to item.quantity)
            }
        )

        outboxEventService.publishEvent(
            eventType = EventRegistry.EventTypes.ORDER_CANCELLED,
            aggregateType = EventRegistry.AggregateTypes.ORDER,
            aggregateId = order.id.toString(),
            payload = objectMapper.writeValueAsString(payload)
        )
    }

    /**
     * 주문 확정
     */
    @DistributedLock(key = DistributedLockKeys.Order.CONFIRM)
    @DistributedTransaction
    fun confirmOrder(orderId: Long): Order {
        val confirmedOrder = orderDomainService.confirmOrder(orderId)
        publishOrderConfirmedEvent(confirmedOrder)
        logger.info("[OrderCommandUseCase] 주문 확정 완료: orderId=$orderId")
        return confirmedOrder
    }

    private fun publishOrderConfirmedEvent(order: Order) {
        val orderItems = orderDomainService.getOrderItems(order.id)

        val payload = mapOf(
            "orderId" to order.id,
            "userId" to order.userId,
            "orderNumber" to order.orderNumber,
            "status" to order.status.name,
            "items" to orderItems.map { item ->
                mapOf("productId" to item.productId, "quantity" to item.quantity)
            }
        )

        outboxEventService.publishEvent(
            eventType = EventRegistry.EventTypes.ORDER_CONFIRMED,
            aggregateType = EventRegistry.AggregateTypes.ORDER,
            aggregateId = order.id.toString(),
            payload = objectMapper.writeValueAsString(payload)
        )
    }
}
