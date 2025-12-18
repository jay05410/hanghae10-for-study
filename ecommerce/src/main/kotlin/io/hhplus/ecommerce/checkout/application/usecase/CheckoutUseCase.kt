package io.hhplus.ecommerce.checkout.application.usecase

import io.hhplus.ecommerce.cart.domain.entity.CartItem
import io.hhplus.ecommerce.cart.domain.service.CartDomainService
import io.hhplus.ecommerce.checkout.domain.model.CheckoutSession
import io.hhplus.ecommerce.checkout.exception.CheckoutException
import io.hhplus.ecommerce.checkout.presentation.dto.DirectOrderItem
import io.hhplus.ecommerce.checkout.presentation.dto.InitiateCheckoutRequest
import io.hhplus.ecommerce.common.annotation.DistributedLock
import io.hhplus.ecommerce.common.annotation.DistributedTransaction
import io.hhplus.ecommerce.common.lock.DistributedLockKeys
import io.hhplus.ecommerce.inventory.domain.entity.StockReservation
import io.hhplus.ecommerce.inventory.domain.service.InventoryDomainService
import io.hhplus.ecommerce.inventory.domain.service.StockReservationDomainService
import io.hhplus.ecommerce.order.domain.constant.OrderStatus
import io.hhplus.ecommerce.order.domain.service.OrderDomainService
import io.hhplus.ecommerce.pricing.domain.model.PricingItemRequest
import io.hhplus.ecommerce.pricing.domain.model.toOrderItemDataList
import io.hhplus.ecommerce.pricing.domain.service.PricingDomainService
import io.hhplus.ecommerce.product.domain.repository.ProductRepository
import mu.KotlinLogging
import org.springframework.stereotype.Component

/**
 * 체크아웃 UseCase - 애플리케이션 계층
 *
 * 역할:
 * - 주문하기: 장바구니 또는 바로 주문으로 주문창 진입
 * - 재고 예약 + PENDING_PAYMENT 주문 생성
 * - 체크아웃 취소: 재고 예약 해제 + 주문 만료 처리
 *
 * 흐름:
 * 1. initiateCheckout: 장바구니에서 "주문하기" 또는 상품에서 "바로 주문" 클릭 시
 *    - 장바구니/바로주문 상품 정보 조회
 *    - 가격 계산 (PricingDomainService)
 *    - 재고 예약 (InventoryDomainService)
 *    - PENDING_PAYMENT 주문 생성 (OrderDomainService)
 *    - 주문창 정보 반환
 *
 * 2. cancelCheckout: 주문창 이탈 시
 *    - 주문 만료 처리 (OrderDomainService)
 *    - 재고 예약 해제 (InventoryDomainService)
 *
 * Note: 배송지, 쿠폰은 결제 시점에 처리
 */
@Component
class CheckoutUseCase(
    private val cartDomainService: CartDomainService,
    private val pricingDomainService: PricingDomainService,
    private val orderDomainService: OrderDomainService,
    private val inventoryDomainService: InventoryDomainService,
    private val stockReservationDomainService: StockReservationDomainService,
    private val productRepository: ProductRepository
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 주문하기 (체크아웃 시작)
     *
     * @param request 주문하기 요청 (장바구니 또는 바로 주문)
     * @return 체크아웃 세션 정보 (주문창에 표시할 정보)
     * @throws CheckoutException.InsufficientStock 재고 부족 시
     */
    @DistributedLock(key = DistributedLockKeys.Checkout.INITIATE, waitTime = 10L, leaseTime = 60L)
    @DistributedTransaction
    fun initiateCheckout(request: InitiateCheckoutRequest): CheckoutSession {
        request.validate()
        logger.info { "[CheckoutUseCase] 주문하기 시작: userId=${request.userId}, isFromCart=${request.isFromCart()}" }

        // 1. 주문할 상품 목록 조회
        val orderItems = if (request.isFromCart()) {
            getItemsFromCart(request.userId, request.cartItemIds!!)
        } else {
            request.directOrderItems!!.map { it.toOrderItemInfo() }
        }

        // 2. 가격 계산 (쿠폰 없이 기본 가격)
        val pricingRequests = orderItems.map { it.toPricingItemRequest() }
        val pricingResult = pricingDomainService.calculatePricing(pricingRequests)

        // 3. 재고 예약 (상품별)
        val reservations = mutableListOf<StockReservation>()
        try {
            orderItems.forEach { item ->
                val product = productRepository.findById(item.productId)
                    ?: throw CheckoutException.ProductNotFound(item.productId)

                // 재고 가용성 확인
                if (!inventoryDomainService.checkStockAvailability(item.productId, item.quantity)) {
                    val availableQuantity = inventoryDomainService.getAvailableQuantity(item.productId)
                    throw CheckoutException.InsufficientStock(
                        productId = item.productId,
                        availableQuantity = availableQuantity,
                        requestedQuantity = item.quantity
                    )
                }

                // 재고 예약
                inventoryDomainService.reserveStock(item.productId, item.quantity)

                // 예약 레코드 생성
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
            // 예외 발생 시 이미 예약된 재고 롤백
            rollbackReservations(reservations)
            throw e
        }

        // 4. PENDING_PAYMENT 주문 생성
        val savedOrder = orderDomainService.createPendingPaymentOrder(
            userId = request.userId,
            items = pricingResult.items.toOrderItemDataList(),
            usedCouponIds = emptyList(), // 쿠폰은 결제 시점에 적용
            totalAmount = pricingResult.totalAmount,
            discountAmount = pricingResult.discountAmount
        )

        // 5. 예약에 주문 ID 연결
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

        logger.info {
            "[CheckoutUseCase] 주문하기 완료: orderId=${savedOrder.id}, " +
                "expiresAt=${checkoutSession.expiresAt}, totalAmount=${pricingResult.totalAmount}"
        }

        return checkoutSession
    }

    /**
     * 체크아웃 취소 (주문창 이탈)
     *
     * @param orderId 주문 ID
     * @param userId 사용자 ID
     */
    @DistributedLock(key = DistributedLockKeys.Checkout.CANCEL, waitTime = 5L, leaseTime = 30L)
    @DistributedTransaction
    fun cancelCheckout(orderId: Long, userId: Long) {
        logger.info { "[CheckoutUseCase] 체크아웃 취소: orderId=$orderId, userId=$userId" }

        val order = orderDomainService.getOrderOrThrow(orderId)

        // 상태 검증
        if (order.status != OrderStatus.PENDING_PAYMENT) {
            throw CheckoutException.InvalidCheckoutState(orderId, order.status.name)
        }

        // 사용자 검증
        if (order.userId != userId) {
            throw CheckoutException.CheckoutNotFound(orderId)
        }

        // 1. 재고 예약 해제
        val reservations = stockReservationDomainService.findByOrderId(orderId)
        reservations.forEach { reservation ->
            inventoryDomainService.releaseReservation(
                productId = reservation.productId,
                quantity = reservation.quantity
            )
            stockReservationDomainService.cancelReservation(reservation)
        }

        // 2. 주문 만료 처리
        orderDomainService.expireOrder(orderId)

        logger.info { "[CheckoutUseCase] 체크아웃 취소 완료: orderId=$orderId" }
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

        // 요청한 아이템 중 찾지 못한 것이 있는지 확인
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
                logger.error(e) { "[CheckoutUseCase] 예약 롤백 실패: productId=${reservation.productId}" }
            }
        }
    }

    // ===== 내부 데이터 클래스 =====

    /**
     * 주문 상품 정보 (내부용)
     */
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

    private fun DirectOrderItem.toOrderItemInfo() = OrderItemInfo(
        productId = productId,
        quantity = quantity,
        giftWrap = giftWrap,
        giftMessage = giftMessage
    )
}

/**
 * 체크아웃 아이템 (응답용)
 */
data class CheckoutItem(
    val productId: Long,
    val quantity: Int,
    val giftWrap: Boolean,
    val giftMessage: String?
)
