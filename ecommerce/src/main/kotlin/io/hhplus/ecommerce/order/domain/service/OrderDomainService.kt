package io.hhplus.ecommerce.order.domain.service

import io.hhplus.ecommerce.common.util.IdPrefix
import io.hhplus.ecommerce.common.util.SnowflakeGenerator
import io.hhplus.ecommerce.order.domain.entity.Order
import io.hhplus.ecommerce.order.domain.entity.OrderItem
import io.hhplus.ecommerce.order.domain.model.OrderItemData
import io.hhplus.ecommerce.order.domain.repository.OrderItemRepository
import io.hhplus.ecommerce.order.domain.repository.OrderRepository
import io.hhplus.ecommerce.order.exception.OrderException
import io.hhplus.ecommerce.order.presentation.dto.CreateOrderRequest
import io.hhplus.ecommerce.product.domain.service.ProductDomainService
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
 * 주의:
 * - 트랜잭션 관리는 UseCase에서 담당
 * - 이벤트 발행 등 인프라 관련 로직 없음
 * - 오케스트레이션은 UseCase에서 담당
 */
@Component
class OrderDomainService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val snowflakeGenerator: SnowflakeGenerator,
    private val productDomainService: ProductDomainService
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 새로운 주문 생성
     *
     * @param userId 주문을 생성하는 사용자 ID
     * @param items 주문 아이템 목록
     * @param usedCouponId 사용된 쿠폰 ID (선택적)
     * @param totalAmount 총 주문 금액
     * @param discountAmount 할인 금액
     * @return 생성된 주문 엔티티
     */
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

        logger.info("주문 생성: orderId=${savedOrder.id}, orderNumber=$orderNumber, userId=$userId")
        return savedOrder
    }

    /**
     * 요청 DTO로부터 주문 생성
     *
     * 상품 가격 조회 → 주문 아이템 데이터 생성 → 주문 생성
     *
     * @param request 주문 생성 요청 DTO
     * @return 생성된 주문 엔티티
     */
    fun createOrderFromRequest(request: CreateOrderRequest): Order {
        // 1. 상품 정보 조회 및 주문 아이템 데이터 생성
        val orderItems = request.items.map { item ->
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

        // 2. 총 금액 계산
        val totalAmount = orderItems.sumOf { it.totalPrice }.toLong()

        // 3. 주문 생성 (할인은 이벤트 핸들러에서 처리)
        return createOrder(
            userId = request.userId,
            items = orderItems,
            usedCouponId = request.usedCouponId,
            totalAmount = totalAmount,
            discountAmount = 0L  // 할인은 이벤트에서 처리
        )
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
}
