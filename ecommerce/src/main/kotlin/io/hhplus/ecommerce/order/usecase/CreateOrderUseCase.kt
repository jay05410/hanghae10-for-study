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
 * 주문 생성 유스케이스 - 애플리케이션 계층
 *
 * 역할:
 * - 주문 생성의 비즈니스 플로우 조율
 * - 상품, 쿠폰, 결제 서비스 간 연계
 * - CQRS Command 패턴 구현
 *
 * 책임:
 * - 주문 생성 전 단계 비즈니스 검증
 * - 여러 도메인 서비스 조합 및 트랜잭션 관리
 * - 주문 생성 후 후처리 작업 수행
 */
@Component
class CreateOrderUseCase(
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
    fun execute(request: CreateOrderRequest): Order {
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
}