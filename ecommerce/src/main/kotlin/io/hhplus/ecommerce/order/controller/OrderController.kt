package io.hhplus.ecommerce.order.controller

import io.hhplus.ecommerce.order.usecase.*
import io.hhplus.ecommerce.order.dto.*
import io.hhplus.ecommerce.delivery.usecase.GetDeliveryQueryUseCase
import io.hhplus.ecommerce.delivery.dto.DeliveryResponse
import io.hhplus.ecommerce.delivery.dto.toResponse
import io.hhplus.ecommerce.common.response.ApiResponse
import org.springframework.web.bind.annotation.*

/**
 * 주문 API 컨트롤러 - 프레젠테이션 계층
 *
 * 역할:
 * - 주문 관련 REST API 엔드포인트 제공
 * - HTTP 요청/응답 처리 및 데이터 변환
 * - 비즈니스 로직은 UseCase에 위임
 *
 * 책임:
 * - 요청 데이터 검증 및 응답 형식 통일
 * - 적절한 UseCase로 비즈니스 로직 위임
 * - HTTP 상태 코드 및 에러 처리
 */
@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val createOrderUseCase: CreateOrderUseCase,
    private val getOrderQueryUseCase: GetOrderQueryUseCase,
    private val confirmOrderUseCase: ConfirmOrderUseCase,
    private val cancelOrderUseCase: CancelOrderUseCase,
    private val getDeliveryQueryUseCase: GetDeliveryQueryUseCase
) {

    /**
     * 새로운 주문을 생성한다
     *
     * @param request 주문 생성 요청 데이터
     * @return 생성된 주문 정보를 포함한 API 응답
     */
    @PostMapping
    fun createOrder(@RequestBody request: CreateOrderRequest): ApiResponse<OrderResponse> {
        val order = createOrderUseCase.execute(request)
        return ApiResponse.success(order.toResponse())
    }

    /**
     * 주문 ID로 단일 주문을 조회한다
     *
     * @param orderId 조회할 주문의 ID
     * @return 주문 정보를 포함한 API 응답
     */
    @GetMapping("/{orderId}")
    fun getOrder(@PathVariable orderId: Long): ApiResponse<OrderResponse?> {
        val order = getOrderQueryUseCase.getOrder(orderId)
        return ApiResponse.success(order?.toResponse())
    }

    /**
     * 특정 사용자의 모든 주문을 조회한다
     *
     * @param userId 조회할 사용자의 ID
     * @return 사용자의 주문 목록을 포함한 API 응답
     */
    @GetMapping
    fun getOrders(@RequestParam userId: Long): ApiResponse<List<OrderResponse>> {
        val orders = getOrderQueryUseCase.getOrdersByUser(userId)
        return ApiResponse.success(orders.map { it.toResponse() })
    }

    /**
     * 주문을 확정 처리한다
     *
     * @param orderId 확정할 주문의 ID
     * @param request 주문 확정 요청 데이터
     * @return 확정된 주문 정보를 포함한 API 응답
     */
    @PostMapping("/{orderId}/confirm")
    fun confirmOrder(
        @PathVariable orderId: Long,
        @RequestBody request: OrderConfirmRequest
    ): ApiResponse<OrderResponse> {
        val order = confirmOrderUseCase.execute(orderId, request.confirmedBy)
        return ApiResponse.success(order.toResponse())
    }

    /**
     * 주문을 취소 처리한다
     *
     * @param orderId 취소할 주문의 ID
     * @param request 주문 취소 요청 데이터
     * @return 취소된 주문 정보를 포함한 API 응답
     */
    @PostMapping("/{orderId}/cancel")
    fun cancelOrder(
        @PathVariable orderId: Long,
        @RequestBody request: OrderCancelRequest
    ): ApiResponse<OrderResponse> {
        val order = cancelOrderUseCase.execute(orderId, request.cancelledBy, request.reason)
        return ApiResponse.success(order.toResponse())
    }

    /**
     * 주문의 배송 정보를 조회한다
     *
     * @param orderId 조회할 주문의 ID
     * @return 배송 정보를 포함한 API 응답
     */
    @GetMapping("/{orderId}/delivery")
    fun getDelivery(@PathVariable orderId: Long): ApiResponse<DeliveryResponse> {
        val delivery = getDeliveryQueryUseCase.getDeliveryByOrderId(orderId)
        return ApiResponse.success(delivery.toResponse())
    }

}