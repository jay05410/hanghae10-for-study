package io.hhplus.ecommerce.order.usecase

import io.hhplus.ecommerce.common.annotation.DistributedLock
import io.hhplus.ecommerce.common.annotation.DistributedTransaction
import io.hhplus.ecommerce.order.domain.entity.Order
import io.hhplus.ecommerce.order.application.OrderService
import io.hhplus.ecommerce.order.dto.CreateOrderRequest
import io.hhplus.ecommerce.order.dto.OrderItemData
import io.hhplus.ecommerce.product.application.ProductQueryService
import io.hhplus.ecommerce.coupon.application.CouponService
import io.hhplus.ecommerce.payment.application.usecase.ProcessPaymentUseCase
import io.hhplus.ecommerce.payment.presentation.dto.ProcessPaymentRequest
import io.hhplus.ecommerce.delivery.application.DeliveryService
import io.hhplus.ecommerce.inventory.application.InventoryService
import io.hhplus.ecommerce.point.application.PointService
import io.hhplus.ecommerce.point.domain.vo.PointAmount
import io.hhplus.ecommerce.order.exception.OrderException
import io.hhplus.ecommerce.delivery.domain.constant.DeliveryStatus
import io.hhplus.ecommerce.cart.application.CartService
import io.hhplus.ecommerce.common.lock.DistributedLockKeys
import mu.KotlinLogging
import org.springframework.stereotype.Component

/**
 * 주문 명령 UseCase
 *
 * 역할:
 * - 모든 주문 변경 작업을 통합 관리
 * - 주문 생성, 취소, 확정 기능 제공
 *
 * 책임:
 * - 주문 변경 요청 검증 및 실행
 * - 다른 도메인과의 연계 처리
 * - 주문 데이터 무결성 보장
 */
@Component
class OrderCommandUseCase(
    private val orderService: OrderService,
    private val productQueryService: ProductQueryService,
    private val couponService: CouponService,
    private val processPaymentUseCase: ProcessPaymentUseCase,
    private val deliveryService: DeliveryService,
    private val inventoryService: InventoryService,
    private val pointService: PointService,
    private val cartService: CartService
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 주문 생성 요청을 처리한다 (재시도 로직 포함)
     *
     * 동시성 제어:
     * - 분산락은 processOrderDirectly에서 적용 (재시도 시마다 락 재획득)
     * - 재시도 로직은 락 외부에서 처리하여 락 보유 시간 최소화
     *
     * @param request 주문 생성 요청 데이터
     * @return 생성되고 결제 처리가 완료된 주문
     * @throws IllegalArgumentException 상품 정보가 유효하지 않을 경우
     * @throws RuntimeException 최대 재시도 후에도 실패한 경우
     */
    fun createOrder(request: CreateOrderRequest): Order {
        return processOrderWithRetry(request)
    }

    /**
     * 재시도 로직이 포함된 주문 처리
     *
     * @param request 주문 생성 요청 데이터
     * @param maxRetries 최대 재시도 횟수
     * @param baseDelayMs 기본 지연 시간 (밀리초)
     * @return 처리 완료된 주문
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
                    val delayMs = baseDelayMs * (1L shl attempt) // 지수 백오프
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
     * 주문 요청을 직접 처리한다
     *
     * 동시성 제어:
     * - 사용자별 분산락으로 동시 주문 방지
     * - waitTime: 10초 (락 획득 대기)
     * - leaseTime: 60초 (주문 처리 최대 시간)
     *
     * @param request 주문 생성 요청 데이터
     * @return 생성되고 결제 처리가 완료된 주문
     * @throws IllegalArgumentException 상품 정보가 유효하지 않을 경우
     * @throws RuntimeException 결제 처리에 실패한 경우
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

        // 4. 부가 처리 (실패해도 주문 성공 유지)
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
        // 포인트 사용 (데드락 방지를 위해 사용자별 순서 통일)
        pointService.usePoint(
            userId = request.userId,
            amount = PointAmount.of(totalAmount - discountAmount),
            description = "주문 결제"
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
        val savedOrder = orderService.createOrder(
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
            deliveryService.createDelivery(
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
            cartService.removeOrderedItems(request.userId, orderedProductIds)
        } catch (e: Exception) {
            logger.warn("장바구니 정리 실패 (주문은 성공): ${e.message}")
        }
    }


    /**
     * 상품 정보를 검증하고 주문 아이템 데이터를 준비한다
     */
    private fun validateAndPrepareOrderItems(request: CreateOrderRequest): List<OrderItemData> {
        return request.items.map { item ->
            val product = productQueryService.getProduct(item.productId)
            OrderItemData(
                productId = item.productId,
                productName = product.name,
                categoryName = "기본카테고리", // TODO: 카테고리 서비스 연동 후 수정
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
     * 총 주문 금액을 계산한다
     */
    private fun calculateTotalAmount(orderItems: List<OrderItemData>): Long {
        return orderItems.sumOf { it.totalPrice }.toLong()
    }

    /**
     * 쿠폰 사용 가능 여부를 검증하고 할인 금액을 계산한다
     */
    private fun validateCoupon(request: CreateOrderRequest, totalAmount: Long): Long {
        return request.usedCouponId?.let { couponId ->
            couponService.validateCouponUsage(request.userId, couponId, totalAmount)
        } ?: 0L
    }

    /**
     * 지정된 주문을 취소하고 관련 후처리를 수행한다
     *
     * @param orderId 취소할 주문 ID
     * @param reason 주문 취소 사유 (선택적)
     * @return 취소 처리가 완료된 주문 정보
     * @throws IllegalArgumentException 주문을 찾을 수 없거나 취소 권한이 없는 경우
     * @throws RuntimeException 주문 취소 처리에 실패한 경우
     */
    @DistributedLock(key = DistributedLockKeys.Order.CANCEL)
    @DistributedTransaction
    fun cancelOrder(orderId: Long, reason: String?): Order {
        // 1. 배송 상태 확인 - 배송 준비 중 이후에는 취소 불가
        val delivery = deliveryService.getDeliveryByOrderId(orderId)
        delivery.let {
            if (it.status in listOf(
                DeliveryStatus.PREPARING,
                DeliveryStatus.SHIPPED,
                DeliveryStatus.DELIVERED
            )) {
                val order = orderService.getOrder(orderId)
                    ?: throw IllegalArgumentException("주문을 찾을 수 없습니다")
                throw OrderException.OrderCancellationNotAllowed(order.orderNumber, order.status)
            }
        }

        // 2. 주문 취소 처리 (상태 검증 포함)
        val cancelledOrder = orderService.cancelOrder(orderId, reason)

        // 3. 주문 아이템 조회
        val orderItems = orderService.getOrderItems(orderId)

        // 4. 재고 복구
        orderItems.forEach { orderItem ->
            inventoryService.restockInventory(
                productId = orderItem.productId,
                quantity = orderItem.quantity
            )
        }

        // 5. 포인트 환불
        pointService.earnPoint(
            userId = cancelledOrder.userId,
            amount = PointAmount.of(cancelledOrder.finalAmount),
            description = "주문 취소 환불"
        )

        return cancelledOrder
    }

    /**
     * 지정된 주문을 확정하고 최종 처리를 수행한다
     *
     * @param orderId 확정할 주문 ID
     * @return 확정 처리가 완료된 주문 정보
     * @throws IllegalArgumentException 주문을 찾을 수 없거나 확정 권한이 없는 경우
     * @throws RuntimeException 주문 확정 처리에 실패한 경우
     */
    @DistributedLock(key = DistributedLockKeys.Order.CONFIRM)
    @DistributedTransaction
    fun confirmOrder(orderId: Long): Order {
        return orderService.confirmOrder(orderId)
    }
}