package io.hhplus.ecommerce.checkout.application.usecase

import io.hhplus.ecommerce.cart.domain.entity.CartItem
import io.hhplus.ecommerce.cart.domain.service.CartDomainService
import io.hhplus.ecommerce.checkout.domain.model.CheckoutSession
import io.hhplus.ecommerce.checkout.exception.CheckoutException
import io.hhplus.ecommerce.checkout.presentation.dto.CheckoutItem
import io.hhplus.ecommerce.checkout.presentation.dto.CheckoutRequest
import io.hhplus.ecommerce.common.annotation.DistributedLock
import io.hhplus.ecommerce.common.annotation.DistributedTransaction
import io.hhplus.ecommerce.common.lock.DistributedLockKeys
import io.hhplus.ecommerce.inventory.domain.entity.StockReservation
import io.hhplus.ecommerce.inventory.domain.service.InventoryDomainService
import io.hhplus.ecommerce.inventory.domain.service.StockReservationDomainService
import io.hhplus.ecommerce.order.domain.constant.OrderStatus
import io.hhplus.ecommerce.order.domain.service.OrderDomainService
import io.hhplus.ecommerce.point.domain.service.PointDomainService
import io.hhplus.ecommerce.pricing.domain.model.PricingItemRequest
import io.hhplus.ecommerce.pricing.domain.model.toOrderItemDataList
import io.hhplus.ecommerce.pricing.domain.service.PricingDomainService
import io.hhplus.ecommerce.product.domain.repository.ProductRepository
import mu.KotlinLogging
import org.springframework.stereotype.Component

/**
 * 체크아웃 UseCase
 *
 * 체크아웃 = 결제 버튼 클릭 시점, 재고 락 확보
 *
 * 흐름:
 * 1. processCheckout: Kafka 큐 기반 비동기 처리
 *    - Kafka 파티션이 순서 보장 → 락 경합 최소화
 *    - 낙관적 락(@Version)으로 동시성 제어
 *
 * 2. cancelCheckout: 체크아웃 취소/이탈 시
 *    - 재고 예약 해제
 *    - 주문 만료 처리
 *
 * 재고 예약 만료:
 * - TTL 기반 스케줄러가 주기적으로 만료 예약 해제
 * - 해제된 재고는 다른 사용자가 구매 가능
 */
@Component
class CheckoutUseCase(
    private val cartDomainService: CartDomainService,
    private val pricingDomainService: PricingDomainService,
    private val orderDomainService: OrderDomainService,
    private val inventoryDomainService: InventoryDomainService,
    private val stockReservationDomainService: StockReservationDomainService,
    private val productRepository: ProductRepository,
    private val pointDomainService: PointDomainService
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 체크아웃 처리 (Kafka 큐 기반)
     *
     * Kafka 파티션이 순서를 보장하므로 분산락 불필요.
     * JPA @Version 낙관적 락으로 동시성 제어.
     *
     * @param request 체크아웃 요청 (장바구니 또는 바로주문)
     * @return 체크아웃 세션 정보
     */
    @DistributedTransaction
    fun processCheckout(request: CheckoutRequest): CheckoutSession {
        logger.info { "[Checkout] 시작: userId=${request.userId}, isCart=${request.isFromCart()}" }

        // 1. 주문할 상품 목록 조회
        val orderItems = if (request.isFromCart()) {
            getItemsFromCart(request.userId, request.cartItemIds!!)
        } else {
            request.items!!.map { OrderItemInfo(it.productId, it.quantity, it.giftWrap, it.giftMessage) }
        }

        // 2. 가격 계산
        val pricingRequests = orderItems.map { it.toPricingItemRequest() }
        val pricingResult = pricingDomainService.calculatePricing(pricingRequests)

        // 3. 포인트 잔액 검증
        val userPoint = pointDomainService.getUserPoint(request.userId)
        if (userPoint == null || userPoint.balance.value < pricingResult.finalAmount) {
            val currentBalance = userPoint?.balance?.value ?: 0L
            throw CheckoutException.InsufficientBalance(request.userId, pricingResult.finalAmount, currentBalance)
        }

        // 4. 재고 예약 (낙관적 락 - @Version)
        val reservations = mutableListOf<StockReservation>()
        try {
            orderItems.forEach { item ->
                val product = productRepository.findById(item.productId)
                    ?: throw CheckoutException.ProductNotFound(item.productId)

                val inventory = inventoryDomainService.getInventoryOrThrow(item.productId)
                if (!inventory.isStockAvailable(item.quantity)) {
                    throw CheckoutException.InsufficientStock(item.productId, inventory.getAvailableQuantity(), item.quantity)
                }
                inventory.reserve(item.quantity)
                inventoryDomainService.saveInventory(inventory)

                val reservation = stockReservationDomainService.createReservation(
                    productId = item.productId,
                    userId = request.userId,
                    quantity = item.quantity,
                    reservationMinutes = if (product.requiresReservation)
                        StockReservation.RESERVATION_PRODUCT_MINUTES
                    else
                        StockReservation.DEFAULT_RESERVATION_MINUTES
                )
                reservations.add(reservation)
            }
        } catch (e: Exception) {
            rollbackReservations(reservations)
            throw e
        }

        // 5. PENDING_PAYMENT 주문 생성
        val savedOrder = orderDomainService.createPendingPaymentOrder(
            userId = request.userId,
            items = pricingResult.items.toOrderItemDataList(),
            usedCouponIds = emptyList(),
            totalAmount = pricingResult.totalAmount,
            discountAmount = pricingResult.discountAmount
        )

        // 6. 예약에 주문 ID 연결
        reservations.forEach { reservation ->
            reservation.linkToOrder(savedOrder.id)
            stockReservationDomainService.save(reservation)
        }

        val checkoutSession = CheckoutSession(
            orderId = savedOrder.id,
            orderNumber = savedOrder.orderNumber,
            userId = request.userId,
            expiresAt = reservations.minOf { it.expiresAt },
            totalAmount = pricingResult.totalAmount,
            discountAmount = pricingResult.discountAmount,
            finalAmount = pricingResult.finalAmount,
            reservationIds = reservations.map { it.id },
            items = orderItems.map { it.toCheckoutItem() }
        )

        logger.info { "[Checkout] 완료: orderId=${savedOrder.id}, expiresAt=${checkoutSession.expiresAt}" }

        return checkoutSession
    }

    /**
     * 체크아웃 취소 (이탈/뒤로가기)
     *
     * @param orderId 주문 ID
     * @param userId 사용자 ID
     */
    @DistributedLock(key = DistributedLockKeys.Checkout.CANCEL, waitTime = 5L, leaseTime = 30L)
    @DistributedTransaction
    fun cancelCheckout(orderId: Long, userId: Long) {
        logger.info { "[Checkout] 취소: orderId=$orderId, userId=$userId" }

        val order = orderDomainService.getOrderOrThrow(orderId)

        if (order.status != OrderStatus.PENDING_PAYMENT) {
            throw CheckoutException.InvalidCheckoutState(orderId, order.status.name)
        }

        if (order.userId != userId) {
            throw CheckoutException.CheckoutNotFound(orderId)
        }

        // 재고 예약 해제
        val reservations = stockReservationDomainService.findByOrderId(orderId)
        reservations.forEach { reservation ->
            inventoryDomainService.releaseReservation(
                productId = reservation.productId,
                quantity = reservation.quantity
            )
            stockReservationDomainService.cancelReservation(reservation)
        }

        // 주문 만료 처리
        orderDomainService.expireOrder(orderId)

        logger.info { "[Checkout] 취소 완료: orderId=$orderId" }
    }

    /**
     * 장바구니에서 선택한 상품 조회
     */
    private fun getItemsFromCart(userId: Long, cartItemIds: List<Long>): List<OrderItemInfo> {
        val cart = cartDomainService.getCartByUserOrThrow(userId)

        val selectedItems = cart.items.filter { it.id in cartItemIds }
        if (selectedItems.isEmpty()) {
            throw CheckoutException.CartItemsNotFound(cartItemIds)
        }

        val foundIds = selectedItems.map { it.id }.toSet()
        val notFoundIds = cartItemIds.filter { it !in foundIds }
        if (notFoundIds.isNotEmpty()) {
            throw CheckoutException.CartItemsNotFound(notFoundIds)
        }

        return selectedItems.map { it.toOrderItemInfo() }
    }

    /**
     * 예약 롤백 (예외 발생 시)
     */
    private fun rollbackReservations(reservations: List<StockReservation>) {
        reservations.forEach { reservation ->
            try {
                inventoryDomainService.releaseReservation(
                    productId = reservation.productId,
                    quantity = reservation.quantity
                )
                stockReservationDomainService.cancelReservation(reservation)
            } catch (e: Exception) {
                logger.error(e) { "[Checkout] 예약 롤백 실패: productId=${reservation.productId}" }
            }
        }
    }

    // ===== 내부 데이터 클래스 =====

    private data class OrderItemInfo(
        val productId: Long,
        val quantity: Int,
        val giftWrap: Boolean,
        val giftMessage: String?
    ) {
        fun toPricingItemRequest() = PricingItemRequest(
            productId = productId,
            quantity = quantity,
            giftWrap = giftWrap,
            giftMessage = giftMessage
        )

        fun toCheckoutItem() = CheckoutItem(
            productId = productId,
            quantity = quantity,
            giftWrap = giftWrap,
            giftMessage = giftMessage
        )
    }

    private fun CartItem.toOrderItemInfo() = OrderItemInfo(
        productId = productId,
        quantity = quantity,
        giftWrap = giftWrap,
        giftMessage = giftMessage
    )
}
