package io.hhplus.ecommerce.order.usecase

import io.hhplus.ecommerce.order.domain.entity.Order
import io.hhplus.ecommerce.order.application.OrderService
import io.hhplus.ecommerce.order.dto.CreateOrderRequest
import io.hhplus.ecommerce.order.dto.OrderItemData
import io.hhplus.ecommerce.product.application.ProductService
import io.hhplus.ecommerce.coupon.application.CouponService
import io.hhplus.ecommerce.payment.application.PaymentService
import io.hhplus.ecommerce.delivery.application.DeliveryService
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
    private val deliveryService: DeliveryService
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
            val product = productService.getProduct(item.packageTypeId)
            OrderItemData(
                packageTypeId = item.packageTypeId,
                packageTypeName = item.packageTypeName,
                packageTypeDays = item.packageTypeDays,
                dailyServing = item.dailyServing,
                totalQuantity = item.totalQuantity,
                giftWrap = item.giftWrap,
                giftMessage = item.giftMessage,
                quantity = item.quantity,
                containerPrice = item.containerPrice,
                teaPrice = item.teaPrice,
                giftWrapPrice = item.giftWrapPrice,
                teaItems = item.teaItems
            )
        }

        val totalAmount = orderItems.sumOf { (it.containerPrice + it.teaPrice + it.giftWrapPrice) * it.quantity }.toLong()

        // 2. 쿠폰 적용 (선택적)
        val discountAmount = request.usedCouponId?.let { couponId ->
            couponService.validateCouponUsage(request.userId, couponId, totalAmount)
        } ?: 0L

        // 3. 주문 생성
        val order = orderService.createOrder(
            userId = request.userId,
            items = orderItems,
            usedCouponId = request.usedCouponId,
            totalAmount = totalAmount,
            discountAmount = discountAmount,
            createdBy = request.userId
        )

        // 4. 결제 처리
        paymentService.processPayment(
            userId = request.userId,
            orderId = order.id,
            amount = order.finalAmount
        )

        // 5. 쿠폰 사용 처리
        request.usedCouponId?.let { couponId ->
            couponService.applyCoupon(request.userId, couponId, order.id, totalAmount)
        }

        // 6. 배송 정보 생성
        deliveryService.createDelivery(
            orderId = order.id,
            deliveryAddress = request.deliveryAddress.toVo(),
            deliveryMemo = request.deliveryAddress.deliveryMessage,
            createdBy = request.userId
        )

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
        return orderService.cancelOrder(orderId, cancelledBy, reason)
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
        return orderService.confirmOrder(orderId, confirmedBy)
    }
}