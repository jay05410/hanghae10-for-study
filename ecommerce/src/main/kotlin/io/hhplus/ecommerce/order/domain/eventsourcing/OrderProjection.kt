package io.hhplus.ecommerce.order.domain.eventsourcing

import io.hhplus.ecommerce.common.messaging.DomainEvent
import io.hhplus.ecommerce.common.messaging.Topics
import io.hhplus.ecommerce.order.domain.constant.OrderStatus
import io.hhplus.ecommerce.order.domain.entity.Order
import io.hhplus.ecommerce.order.domain.entity.OrderItem
import io.hhplus.ecommerce.order.domain.repository.OrderItemRepository
import io.hhplus.ecommerce.order.domain.repository.OrderRepository
import mu.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Order Read Model Projection (Event Sourcing - CQRS)
 *
 * Event Sourcing에서 Write Model(Event Store)의 이벤트를 구독하여
 * Read Model(orders 테이블)을 업데이트하는 Projection
 *
 * 역할:
 * - 이벤트 수신 및 처리
 * - Read Model (orders, order_items 테이블) 업데이트
 * - 조회 최적화된 데이터 모델 유지
 *
 * 동작 방식:
 * 1. OrderEventStore에서 이벤트 발행 (MessagePublisher)
 * 2. InMemoryMessagePublisher가 Spring Event로 변환
 * 3. OrderProjection이 @EventListener로 수신
 * 4. Read Model 업데이트
 */
@Component
class OrderProjection(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 도메인 이벤트 수신 (InMemory)
     *
     * Kafka 전환 시 @KafkaListener로 교체
     */
    @EventListener
    @Transactional
    fun handle(domainEvent: DomainEvent) {
        // Order 토픽 이벤트만 처리
        if (domainEvent.topic != Topics.ORDER) return

        val payload = domainEvent.payload
        if (payload !is OrderEvent) {
            logger.warn { "알 수 없는 페이로드 타입: ${payload::class.simpleName}" }
            return
        }

        when (payload) {
            is OrderEvent.OrderCreated -> handleOrderCreated(payload)
            is OrderEvent.OrderConfirmed -> handleOrderConfirmed(payload)
            is OrderEvent.OrderCompleted -> handleOrderCompleted(payload)
            is OrderEvent.OrderCancelled -> handleOrderCancelled(payload)
            is OrderEvent.OrderFailed -> handleOrderFailed(payload)
        }
    }

    /**
     * 주문 생성 이벤트 처리
     */
    private fun handleOrderCreated(event: OrderEvent.OrderCreated) {
        logger.info { "Read Model 업데이트 - OrderCreated: orderId=${event.orderId}" }

        // Order 생성
        val order = Order(
            id = event.orderId,
            orderNumber = event.orderNumber,
            userId = event.userId,
            totalAmount = event.totalAmount,
            discountAmount = event.discountAmount,
            finalAmount = event.finalAmount,
            usedCouponIds = event.usedCouponIds,
            status = OrderStatus.PENDING
        )
        val savedOrder = orderRepository.save(order)

        // OrderItem 생성
        event.items.forEach { snapshot ->
            val orderItem = OrderItem(
                orderId = savedOrder.id,
                productId = snapshot.productId,
                productName = snapshot.productName,
                categoryName = snapshot.categoryName,
                quantity = snapshot.quantity,
                unitPrice = snapshot.unitPrice,
                giftWrap = snapshot.giftWrap,
                giftMessage = snapshot.giftMessage,
                giftWrapPrice = snapshot.giftWrapPrice,
                totalPrice = snapshot.totalPrice
            )
            orderItemRepository.save(orderItem)
        }

        logger.debug {
            "Read Model 생성 완료 - orderId=${savedOrder.id}, itemCount=${event.items.size}"
        }
    }

    /**
     * 주문 확정 이벤트 처리
     */
    private fun handleOrderConfirmed(event: OrderEvent.OrderConfirmed) {
        logger.info { "Read Model 업데이트 - OrderConfirmed: orderId=${event.orderId}" }

        val order = orderRepository.findById(event.orderId)
        if (order == null) {
            logger.warn { "Order를 찾을 수 없습니다: orderId=${event.orderId}" }
            return
        }

        order.confirm()
        orderRepository.save(order)

        logger.debug { "Read Model 업데이트 완료 - orderId=${event.orderId}, status=CONFIRMED" }
    }

    /**
     * 주문 완료 이벤트 처리
     */
    private fun handleOrderCompleted(event: OrderEvent.OrderCompleted) {
        logger.info { "Read Model 업데이트 - OrderCompleted: orderId=${event.orderId}" }

        val order = orderRepository.findById(event.orderId)
        if (order == null) {
            logger.warn { "Order를 찾을 수 없습니다: orderId=${event.orderId}" }
            return
        }

        order.complete()
        orderRepository.save(order)

        logger.debug { "Read Model 업데이트 완료 - orderId=${event.orderId}, status=COMPLETED" }
    }

    /**
     * 주문 취소 이벤트 처리
     */
    private fun handleOrderCancelled(event: OrderEvent.OrderCancelled) {
        logger.info { "Read Model 업데이트 - OrderCancelled: orderId=${event.orderId}" }

        val order = orderRepository.findById(event.orderId)
        if (order == null) {
            logger.warn { "Order를 찾을 수 없습니다: orderId=${event.orderId}" }
            return
        }

        order.cancel()
        orderRepository.save(order)

        logger.debug {
            "Read Model 업데이트 완료 - orderId=${event.orderId}, status=CANCELLED, reason=${event.reason}"
        }
    }

    /**
     * 주문 실패 이벤트 처리
     */
    private fun handleOrderFailed(event: OrderEvent.OrderFailed) {
        logger.info { "Read Model 업데이트 - OrderFailed: orderId=${event.orderId}" }

        val order = orderRepository.findById(event.orderId)
        if (order == null) {
            logger.warn { "Order를 찾을 수 없습니다: orderId=${event.orderId}" }
            return
        }

        order.fail()
        orderRepository.save(order)

        logger.debug {
            "Read Model 업데이트 완료 - orderId=${event.orderId}, status=FAILED, reason=${event.reason}"
        }
    }
}

/**
 * Projection 재구축 서비스
 *
 * Read Model이 손상되거나 스키마 변경 시 사용
 * Event Store의 모든 이벤트를 재생하여 Read Model 재구축
 */
@Component
class OrderProjectionRebuilder(
    private val orderEventRepository: OrderEventRepository,
    private val orderProjection: OrderProjection,
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 특정 주문의 Read Model 재구축
     */
    @Transactional
    fun rebuildOrder(orderId: Long) {
        logger.info { "Read Model 재구축 시작: orderId=$orderId" }

        // 기존 Read Model 삭제
        orderRepository.findById(orderId)?.let { order ->
            orderItemRepository.deleteByOrderId(orderId)
            // Note: OrderRepository doesn't have delete method
            // Read Model will be overwritten by replay
        }

        // 이벤트 재생
        val events = orderEventRepository.findByAggregateId(orderId)
        events.forEach { event ->
            val domainEvent = DomainEvent(
                topic = "ecommerce.order",
                key = orderId.toString(),
                payload = event
            )
            orderProjection.handle(domainEvent)
        }

        logger.info { "Read Model 재구축 완료: orderId=$orderId, eventCount=${events.size}" }
    }
}
