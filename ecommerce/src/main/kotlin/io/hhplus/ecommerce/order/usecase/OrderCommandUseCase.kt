package io.hhplus.ecommerce.order.usecase

import io.hhplus.ecommerce.order.domain.entity.Order
import io.hhplus.ecommerce.order.application.OrderService
import io.hhplus.ecommerce.order.dto.CreateOrderRequest
import io.hhplus.ecommerce.order.dto.OrderItemData
import io.hhplus.ecommerce.product.application.ProductService
import io.hhplus.ecommerce.coupon.application.CouponService
import io.hhplus.ecommerce.payment.application.PaymentService
import io.hhplus.ecommerce.delivery.application.DeliveryService
import io.hhplus.ecommerce.inventory.application.InventoryService
import io.hhplus.ecommerce.order.domain.repository.OrderItemRepository
import io.hhplus.ecommerce.point.application.PointService
import io.hhplus.ecommerce.point.domain.vo.PointAmount
import io.hhplus.ecommerce.order.exception.OrderException
import io.hhplus.ecommerce.delivery.domain.constant.DeliveryStatus
import io.hhplus.ecommerce.cart.application.CartService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

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
    private val productService: ProductService,
    private val couponService: CouponService,
    private val paymentService: PaymentService,
    private val deliveryService: DeliveryService,
    private val inventoryService: InventoryService,
    private val pointService: PointService,
    private val orderItemRepository: OrderItemRepository,
    private val cartService: CartService
) {

    /**
     * 주문 생성 비즈니스 플로우를 실행한다
     *
     * @param request 주문 생성 요청 데이터
     * @return 생성되고 결제 처리가 완료된 주문
     * @throws IllegalArgumentException 상품 정보가 유효하지 않을 경우
     * @throws RuntimeException 결제 처리에 실패한 경우
     */
    @Transactional
    fun createOrder(request: CreateOrderRequest): Order {
        // 1. 상품 정보 검증 및 가격 계산
        val orderItems = request.items.map { item ->
            val product = productService.getProduct(item.productId)
            OrderItemData(
                productId = item.productId,
                productName = product.name,
                categoryName = "기본카테고리", // TODO: 카테고리 서비스 연동 후 수정
                quantity = item.quantity,
                unitPrice = product.price.toInt(),
                giftWrap = item.giftWrap,
                giftMessage = item.giftMessage,
                giftWrapPrice = if (item.giftWrap) 2000 else 0,
                totalPrice = (product.price.toInt() * item.quantity) + if (item.giftWrap) 2000 else 0
            )
        }

        val totalAmount = orderItems.sumOf { it.totalPrice }.toLong()

        // 2. 쿠폰 적용 (선택적)
        val discountAmount = request.usedCouponId?.let { couponId ->
            couponService.validateCouponUsage(request.userId, couponId, totalAmount)
        } ?: 0L

        // 3. 재고 차감
        orderItems.forEach { item ->
            inventoryService.deductStock(
                productId = item.productId,
                quantity = item.quantity
            )
        }

        // 4. 주문 생성
        val order = orderService.createOrder(
            userId = request.userId,
            items = orderItems,
            usedCouponId = request.usedCouponId,
            totalAmount = totalAmount,
            discountAmount = discountAmount
        )

        // 5. 포인트 사용 (결제)
        pointService.usePoint(
            userId = request.userId,
            amount = PointAmount.of(order.finalAmount),
            description = "주문 결제"
        )

        // 6. 결제 처리 (기록용)
        paymentService.processPayment(
            userId = request.userId,
            orderId = order.id,
            amount = order.finalAmount
        )

        // 7. 쿠폰 사용 처리
        request.usedCouponId?.let { couponId ->
            couponService.applyCoupon(request.userId, couponId, order.id, totalAmount)
        }

        // 8. 배송 정보 생성
        deliveryService.createDelivery(
            orderId = order.id,
            deliveryAddress = request.deliveryAddress.toVo(),
            deliveryMemo = request.deliveryAddress.deliveryMessage
        )

        // 9. 장바구니에서 주문된 상품 제거 (주문 완료 후 정리)
        val orderedProductIds = orderItems.map { it.productId }
        cartService.removeOrderedItems(request.userId, orderedProductIds)

        return order
    }

    /**
     * 지정된 주문을 취소하고 관련 후처리를 수행한다
     *
     * @param orderId 취소할 주문 ID
     * @param cancelledBy 취소를 요청하는 사용자 ID
     * @param reason 주문 취소 사유 (선택적)
     * @return 취소 처리가 완료된 주문 정보
     * @throws IllegalArgumentException 주문을 찾을 수 없거나 취소 권한이 없는 경우
     * @throws RuntimeException 주문 취소 처리에 실패한 경우
     */
    @Transactional
    fun cancelOrder(orderId: Long, cancelledBy: Long, reason: String?): Order {
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
        val orderItems = orderItemRepository.findByOrderId(orderId)

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
     * @param confirmedBy 확정을 수행하는 사용자 ID
     * @return 확정 처리가 완료된 주문 정보
     * @throws IllegalArgumentException 주문을 찾을 수 없거나 확정 권한이 없는 경우
     * @throws RuntimeException 주문 확정 처리에 실패한 경우
     */
    @Transactional
    fun confirmOrder(orderId: Long, confirmedBy: Long): Order {
        return orderService.confirmOrder(orderId)
    }
}