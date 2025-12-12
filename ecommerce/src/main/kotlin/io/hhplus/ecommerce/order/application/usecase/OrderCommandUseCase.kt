package io.hhplus.ecommerce.order.application.usecase

import io.hhplus.ecommerce.common.annotation.DistributedLock
import io.hhplus.ecommerce.common.annotation.DistributedTransaction
import io.hhplus.ecommerce.common.lock.DistributedLockKeys
import io.hhplus.ecommerce.common.outbox.OutboxEventService
import io.hhplus.ecommerce.common.outbox.payload.OrderCancelledPayload
import io.hhplus.ecommerce.common.outbox.payload.OrderConfirmedPayload
import io.hhplus.ecommerce.common.outbox.payload.OrderCreatedItemPayload
import io.hhplus.ecommerce.common.outbox.payload.OrderCreatedPayload
import io.hhplus.ecommerce.common.outbox.payload.OrderItemPayloadSimple
import io.hhplus.ecommerce.config.event.EventRegistry
import io.hhplus.ecommerce.delivery.domain.service.DeliveryDomainService
import io.hhplus.ecommerce.delivery.domain.vo.DeliveryAddress
import io.hhplus.ecommerce.order.domain.entity.Order
import io.hhplus.ecommerce.order.domain.service.OrderDomainService
import io.hhplus.ecommerce.order.exception.OrderException
import io.hhplus.ecommerce.order.presentation.dto.CreateOrderRequest
import kotlinx.serialization.json.Json
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
 * - OrderCreated → 결제 처리 → 포인트 차감, 재고 차감, 쿠폰 사용, 배송 생성, 장바구니 정리
 * - OrderCancelled → 재고 복구, 포인트 환불
 * - OrderConfirmed → 통계 기록
 */
@Component
class OrderCommandUseCase(
    private val orderDomainService: OrderDomainService,
    private val deliveryDomainService: DeliveryDomainService,
    private val outboxEventService: OutboxEventService
) {
    private val logger = KotlinLogging.logger {}
    private val json = Json { ignoreUnknownKeys = true }

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
        val payload = OrderCreatedPayload(
            orderId = order.id,
            userId = order.userId,
            orderNumber = order.orderNumber,
            totalAmount = order.totalAmount,
            finalAmount = order.finalAmount,
            discountAmount = order.discountAmount,
            status = order.status.name,
            usedCouponId = request.usedCouponId,
            items = request.items.map { item ->
                OrderCreatedItemPayload(
                    productId = item.productId,
                    quantity = item.quantity,
                    giftWrap = item.giftWrap,
                    giftMessage = item.giftMessage
                )
            },
            deliveryAddress = DeliveryAddress(
                recipientName = request.deliveryAddress.recipientName,
                phone = request.deliveryAddress.phone,
                zipCode = request.deliveryAddress.zipCode,
                address = request.deliveryAddress.address,
                addressDetail = request.deliveryAddress.addressDetail,
                deliveryMessage = request.deliveryAddress.deliveryMessage
            )
        )

        outboxEventService.publishEvent(
            eventType = EventRegistry.EventTypes.ORDER_CREATED,
            aggregateType = EventRegistry.AggregateTypes.ORDER,
            aggregateId = order.id.toString(),
            payload = json.encodeToString(OrderCreatedPayload.serializer(), payload)
        )
    }

    /**
     * 주문 취소
     *
     * 취소 가능 조건:
     * - 주문 상태: PENDING 또는 CONFIRMED (Order 도메인에서 검증)
     * - 배송 상태: PENDING (Delivery 도메인에서 검증)
     */
    @DistributedLock(key = DistributedLockKeys.Order.CANCEL)
    @DistributedTransaction
    fun cancelOrder(orderId: Long, reason: String?): Order {
        // 배송 상태 확인 - 배송 도메인에서 취소 가능 여부 판단
        val delivery = try {
            deliveryDomainService.getDeliveryByOrderId(orderId)
        } catch (e: Exception) {
            null // 배송 정보가 없으면 취소 가능
        }

        if (delivery != null && !delivery.canBeCancelled()) {
            throw OrderException.OrderCancellationNotAllowedByDelivery(orderId, delivery.status)
        }

        // 주문 취소 - 주문 도메인에서 주문 상태 기반 취소 가능 여부 판단
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
        val payload = OrderCancelledPayload(
            orderId = order.id,
            userId = order.userId,
            orderNumber = order.orderNumber,
            finalAmount = order.finalAmount,
            reason = reason ?: "사용자 요청",
            status = order.status.name,
            items = orderItems.map { item ->
                OrderItemPayloadSimple(
                    productId = item.productId,
                    quantity = item.quantity
                )
            }
        )

        outboxEventService.publishEvent(
            eventType = EventRegistry.EventTypes.ORDER_CANCELLED,
            aggregateType = EventRegistry.AggregateTypes.ORDER,
            aggregateId = order.id.toString(),
            payload = json.encodeToString(OrderCancelledPayload.serializer(), payload)
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

        val payload = OrderConfirmedPayload(
            orderId = order.id,
            userId = order.userId,
            orderNumber = order.orderNumber,
            status = order.status.name,
            items = orderItems.map { item ->
                OrderItemPayloadSimple(
                    productId = item.productId,
                    quantity = item.quantity
                )
            }
        )

        outboxEventService.publishEvent(
            eventType = EventRegistry.EventTypes.ORDER_CONFIRMED,
            aggregateType = EventRegistry.AggregateTypes.ORDER,
            aggregateId = order.id.toString(),
            payload = json.encodeToString(OrderConfirmedPayload.serializer(), payload)
        )
    }
}
