package io.hhplus.ecommerce.order.domain.service

import io.hhplus.ecommerce.common.util.IdPrefix
import io.hhplus.ecommerce.common.util.SnowflakeGenerator
import io.hhplus.ecommerce.order.domain.entity.Order
import io.hhplus.ecommerce.order.domain.entity.OrderItem
import io.hhplus.ecommerce.order.domain.model.OrderItemData
import io.hhplus.ecommerce.order.domain.repository.OrderItemRepository
import io.hhplus.ecommerce.order.domain.repository.OrderRepository
import io.hhplus.ecommerce.order.exception.OrderException
import mu.KotlinLogging
import org.springframework.stereotype.Component

/**
 * 주문 도메인 서비스 - 순수 도메인 로직
 *
 * 역할:
 * - 주문 엔티티 생성 및 상태 관리
 * - 주문 아이템 관리
 * - 주문 조회 및 생명주기 관리
 *
 * 책임:
 * - 주문 도메인 불변식 보장
 * - Repository(Port)를 통한 영속성 위임
 *
 * 의존성:
 * - Product/Coupon 도메인에 직접 의존하지 않음
 * - 가격 계산은 PricingDomainService에서 수행 후 전달받음
 *
 * 주의:
 * - 트랜잭션 관리는 UseCase에서 담당
 * - 이벤트 발행 등 인프라 관련 로직 없음
 * - 오케스트레이션은 UseCase에서 담당
 */
@Component
class OrderDomainService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val snowflakeGenerator: SnowflakeGenerator
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 새로운 주문 생성
     *
     * @param userId 주문을 생성하는 사용자 ID
     * @param items 주문 아이템 목록
     * @param usedCouponIds 사용된 쿠폰 ID 목록
     * @param totalAmount 총 주문 금액
     * @param discountAmount 할인 금액
     * @return 생성된 주문 엔티티
     */
    fun createOrder(
        userId: Long,
        items: List<OrderItemData>,
        usedCouponIds: List<Long>,
        totalAmount: Long,
        discountAmount: Long
    ): Order {
        val orderNumber = snowflakeGenerator.generateNumberWithPrefix(IdPrefix.ORDER)

        val order = Order.create(
            orderNumber = orderNumber,
            userId = userId,
            totalAmount = totalAmount,
            discountAmount = discountAmount,
            usedCouponIds = usedCouponIds
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

        logger.info("주문 생성: orderId=${savedOrder.id}, orderNumber=$orderNumber, userId=$userId")
        return savedOrder
    }

    /**
     * 결제 대기 상태의 주문 생성 (체크아웃 시작 시)
     *
     * @param userId 주문을 생성하는 사용자 ID
     * @param items 주문 아이템 목록
     * @param usedCouponIds 사용된 쿠폰 ID 목록
     * @param totalAmount 총 주문 금액
     * @param discountAmount 할인 금액
     * @return 생성된 주문 엔티티 (PENDING_PAYMENT 상태)
     */
    fun createPendingPaymentOrder(
        userId: Long,
        items: List<OrderItemData>,
        usedCouponIds: List<Long>,
        totalAmount: Long,
        discountAmount: Long
    ): Order {
        val orderNumber = snowflakeGenerator.generateNumberWithPrefix(IdPrefix.ORDER)

        val order = Order.createPendingPayment(
            orderNumber = orderNumber,
            userId = userId,
            totalAmount = totalAmount,
            discountAmount = discountAmount,
            usedCouponIds = usedCouponIds
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

        logger.info("결제 대기 주문 생성: orderId=${savedOrder.id}, orderNumber=$orderNumber, userId=$userId")
        return savedOrder
    }

    /**
     * 주문 정보 업데이트
     *
     * @param order 업데이트할 주문 엔티티
     * @return 업데이트된 주문 엔티티
     */
    fun updateOrder(order: Order): Order {
        return orderRepository.save(order)
    }

    /**
     * 주문 ID로 주문 조회
     *
     * @param orderId 조회할 주문의 ID
     * @return 주문 엔티티 (존재하지 않을 경우 null)
     */
    fun getOrder(orderId: Long): Order? {
        return orderRepository.findById(orderId)
    }

    /**
     * 주문 ID로 주문 조회 (없으면 예외)
     *
     * @param orderId 조회할 주문의 ID
     * @return 주문 엔티티
     * @throws OrderException.OrderNotFound 주문을 찾을 수 없는 경우
     */
    fun getOrderOrThrow(orderId: Long): Order {
        return orderRepository.findById(orderId)
            ?: throw OrderException.OrderNotFound(orderId)
    }

    /**
     * 특정 사용자의 활성 주문 목록 조회
     *
     * @param userId 조회할 사용자의 ID
     * @return 사용자의 활성 주문 목록 (최신순 정렬)
     */
    fun getOrdersByUser(userId: Long): List<Order> {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
    }

    /**
     * 사용자에게 노출되는 주문만 조회 (PENDING_PAYMENT, EXPIRED 제외)
     *
     * @param userId 조회할 사용자의 ID
     * @return 사용자에게 보이는 주문 목록 (최신순 정렬)
     */
    fun getVisibleOrdersByUser(userId: Long): List<Order> {
        return orderRepository.findVisibleOrdersByUserId(userId)
    }

    /**
     * 주문 ID로 주문과 주문 아이템을 함께 조회
     *
     * @param orderId 조회할 주문 ID
     * @return 주문 정보와 주문 아이템 목록 Pair (존재하지 않으면 null)
     */
    fun getOrderWithItems(orderId: Long): Pair<Order, List<OrderItem>>? {
        val order = orderRepository.findById(orderId) ?: return null
        val orderItems = orderItemRepository.findByOrderId(orderId)
        return Pair(order, orderItems)
    }

    /**
     * 사용자가 진행한 모든 주문과 주문 아이템을 함께 조회
     *
     * @param userId 인증된 사용자 ID
     * @return 주문 정보와 주문 아이템 목록의 Map
     */
    fun getOrdersWithItemsByUser(userId: Long): Map<Order, List<OrderItem>> {
        val orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
        if (orders.isEmpty()) return emptyMap()

        val orderIds = orders.map { it.id }
        val allOrderItems = orderIds.flatMap { orderId ->
            orderItemRepository.findByOrderId(orderId)
        }
        val orderItemsMap = allOrderItems.groupBy { it.orderId }

        return orders.associateWith { order ->
            orderItemsMap[order.id] ?: emptyList()
        }
    }

    /**
     * 사용자에게 노출되는 주문과 주문 아이템을 함께 조회 (PENDING_PAYMENT, EXPIRED 제외)
     *
     * @param userId 인증된 사용자 ID
     * @return 주문 정보와 주문 아이템 목록의 Map
     */
    fun getVisibleOrdersWithItemsByUser(userId: Long): Map<Order, List<OrderItem>> {
        val orders = orderRepository.findVisibleOrdersByUserId(userId)
        if (orders.isEmpty()) return emptyMap()

        val orderIds = orders.map { it.id }
        val allOrderItems = orderIds.flatMap { orderId ->
            orderItemRepository.findByOrderId(orderId)
        }
        val orderItemsMap = allOrderItems.groupBy { it.orderId }

        return orders.associateWith { order ->
            orderItemsMap[order.id] ?: emptyList()
        }
    }

    /**
     * 주문 ID로 주문 아이템 목록 조회
     *
     * @param orderId 조회할 주문 ID
     * @return 주문 아이템 목록
     */
    fun getOrderItems(orderId: Long): List<OrderItem> {
        return orderItemRepository.findByOrderId(orderId)
    }

    /**
     * 주문 확정 처리
     *
     * @param orderId 확정할 주문의 ID
     * @return 확정된 주문 엔티티
     * @throws OrderException.OrderNotFound 주문을 찾을 수 없는 경우
     */
    fun confirmOrder(orderId: Long): Order {
        val order = getOrderOrThrow(orderId)
        order.confirm()
        val confirmedOrder = orderRepository.save(order)
        logger.info("주문 확정: orderId=$orderId, status=${confirmedOrder.status}")
        return confirmedOrder
    }

    /**
     * 주문 취소 처리
     *
     * @param orderId 취소할 주문의 ID
     * @param reason 취소 사유 (선택적)
     * @return 취소된 주문 엔티티
     * @throws OrderException.OrderNotFound 주문을 찾을 수 없는 경우
     */
    fun cancelOrder(orderId: Long, reason: String?): Order {
        val order = getOrderOrThrow(orderId)
        order.cancel()
        val cancelledOrder = orderRepository.save(order)
        logger.info("주문 취소: orderId=$orderId, reason=${reason ?: "사용자 요청"}")
        return cancelledOrder
    }

    /**
     * 결제 완료 처리 (PENDING_PAYMENT -> PENDING)
     *
     * @param orderId 결제 완료된 주문의 ID
     * @return 결제 완료된 주문 엔티티
     * @throws OrderException.OrderNotFound 주문을 찾을 수 없는 경우
     */
    fun confirmPayment(orderId: Long): Order {
        val order = getOrderOrThrow(orderId)
        order.confirmPayment()
        val confirmedOrder = orderRepository.save(order)
        logger.info("결제 완료: orderId=$orderId, status=${confirmedOrder.status}")
        return confirmedOrder
    }

    /**
     * 주문 만료 처리 (PENDING_PAYMENT -> EXPIRED)
     *
     * @param orderId 만료시킬 주문의 ID
     * @return 만료된 주문 엔티티
     * @throws OrderException.OrderNotFound 주문을 찾을 수 없는 경우
     */
    fun expireOrder(orderId: Long): Order {
        val order = getOrderOrThrow(orderId)
        order.expire()
        val expiredOrder = orderRepository.save(order)
        logger.info("주문 만료: orderId=$orderId, status=${expiredOrder.status}")
        return expiredOrder
    }
}
