package io.hhplus.ecommerce.order.application

import io.hhplus.ecommerce.order.domain.entity.Order
import io.hhplus.ecommerce.order.domain.entity.OrderItem
import io.hhplus.ecommerce.order.domain.repository.OrderRepository
import io.hhplus.ecommerce.order.domain.repository.OrderItemRepository
import io.hhplus.ecommerce.order.dto.OrderItemData
import io.hhplus.ecommerce.common.util.SnowflakeGenerator
import io.hhplus.ecommerce.product.application.ProductStatisticsService
import io.hhplus.ecommerce.common.outbox.OutboxEventService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.fasterxml.jackson.databind.ObjectMapper
import io.hhplus.ecommerce.common.util.IdPrefix

/**
 * 주문 도메인 서비스 - 애플리케이션 계층
 *
 * 역할:
 * - 주문 도메인의 핵심 비즈니스 로직 처리
 * - 주문 생명주기 관리 및 상태 변경
 * - 관련 도메인 서비스와의 협력 조정
 *
 * 책임:
 * - 주문 생성, 수정, 조회, 확정, 취소 로직
 * - 주문 아이템 관리
 * - 주문 이벤트 발행 및 통계 연동
 */
@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val snowflakeGenerator: SnowflakeGenerator,
    private val productStatisticsService: ProductStatisticsService,
    private val outboxEventService: OutboxEventService,
    private val objectMapper: ObjectMapper
) {

    /**
     * 새로운 주문을 생성한다
     *
     * @param userId 주문을 생성하는 사용자 ID
     * @param items 주문 아이템 목록
     * @param usedCouponId 사용된 쿠폰 ID (선택적)
     * @param totalAmount 총 주문 금액
     * @param discountAmount 할인 금액
     * @return 생성된 주문 엔티티
     */
    @Transactional
    fun createOrder(
        userId: Long,
        items: List<OrderItemData>,
        usedCouponId: Long?,
        totalAmount: Long,
        discountAmount: Long
    ): Order {
        val orderNumber = snowflakeGenerator.generateNumberWithPrefix(IdPrefix.ORDER)

        val order = Order.create(
            orderNumber = orderNumber,
            userId = userId,
            totalAmount = totalAmount,
            discountAmount = discountAmount,
            usedCouponId = usedCouponId
        )

        val savedOrder = orderRepository.save(order)

        // 주문 아이템 생성 및 저장
        items.forEach { item ->
            val orderItem = OrderItem.create(
                orderId = savedOrder.id,
                productId = item.productId,
                productName = item.productName,
                categoryName = item.categoryName,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                giftWrap = item.giftWrap,
                giftMessage = item.giftMessage,
                giftWrapPrice = item.giftWrapPrice
            )
            orderItemRepository.save(orderItem)
        }

        // 주문 생성 이벤트 발행
        val orderEventPayload = mapOf(
            "orderId" to savedOrder.id,
            "userId" to userId,
            "orderNumber" to savedOrder.orderNumber,
            "totalAmount" to totalAmount,
            "finalAmount" to savedOrder.finalAmount,
            "status" to savedOrder.status.name
        )

        outboxEventService.publishEvent(
            eventType = "OrderCreated",
            aggregateType = "Order",
            aggregateId = savedOrder.id.toString(),
            payload = objectMapper.writeValueAsString(orderEventPayload)
        )

        return savedOrder
    }

    /**
     * 주문 정보를 업데이트한다
     *
     * @param order 업데이트할 주문 엔티티
     * @return 업데이트된 주문 엔티티
     */
    fun updateOrder(order: Order): Order {
        return orderRepository.save(order)
    }

    /**
     * 주문 ID로 주문을 조회한다
     *
     * @param orderId 조회할 주문의 ID
     * @return 주문 엔티티 (존재하지 않을 경우 null)
     */
    fun getOrder(orderId: Long): Order? {
        return orderRepository.findById(orderId)
    }

    /**
     * 특정 사용자의 활성 주문 목록을 조회한다
     *
     * @param userId 조회할 사용자의 ID
     * @return 사용자의 활성 주문 목록 (최신순 정렬)
     *
     * 참고: OrderItem은 별도로 조회해야 합니다 (OneToMany 관계 제거됨)
     */
    fun getOrdersByUser(userId: Long): List<Order> {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
    }

    /**
     * 주문을 확정 처리한다
     *
     * @param orderId 확정할 주문의 ID
     * @return 확정된 주문 엔티티
     * @throws IllegalArgumentException 주문을 찾을 수 없는 경우
     */
    @Transactional
    fun confirmOrder(orderId: Long): Order {
        val order = orderRepository.findById(orderId)
            ?: throw IllegalArgumentException("주문을 찾을 수 없습니다")

        order.confirm()

        // 주문 완료 시 판매량 증가
        val orderItems = orderItemRepository.findByOrderId(orderId)
        orderItems.forEach { orderItem ->
            productStatisticsService.incrementSalesCount(
                productId = orderItem.productId,
                quantity = orderItem.quantity
            )
        }

        return orderRepository.save(order)
    }

    /**
     * 주문을 취소 처리한다
     *
     * @param orderId 취소할 주문의 ID
     * @param reason 취소 사유 (선택적)
     * @return 취소된 주문 엔티티
     * @throws IllegalArgumentException 주문을 찾을 수 없는 경우
     */
    @Transactional
    fun cancelOrder(orderId: Long, reason: String?): Order {
        val order = orderRepository.findById(orderId)
            ?: throw IllegalArgumentException("주문을 찾을 수 없습니다")

        order.cancel()

        // 주문 취소 이벤트 발행
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

        return orderRepository.save(order)
    }
}