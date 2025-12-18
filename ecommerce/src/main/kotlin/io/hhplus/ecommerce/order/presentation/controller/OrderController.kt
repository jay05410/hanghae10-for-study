package io.hhplus.ecommerce.order.presentation.controller

import io.hhplus.ecommerce.order.application.usecase.OrderCommandUseCase
import io.hhplus.ecommerce.order.application.usecase.GetOrderQueryUseCase
import io.hhplus.ecommerce.order.presentation.dto.*
import io.hhplus.ecommerce.delivery.application.usecase.GetDeliveryQueryUseCase
import io.hhplus.ecommerce.delivery.presentation.dto.DeliveryResponse
import io.hhplus.ecommerce.delivery.presentation.dto.toResponse
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
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "주문", description = "주문 관리 API")
@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val orderCommandUseCase: OrderCommandUseCase,
    private val getOrderQueryUseCase: GetOrderQueryUseCase,
    private val getDeliveryQueryUseCase: GetDeliveryQueryUseCase
) {

    /**
     * 새로운 주문을 생성한다 (직접 처리)
     *
     * @param request 주문 생성 요청 데이터
     * @return 생성된 주문 정보를 포함한 API 응답
     */
    @Operation(summary = "주문 생성", description = "새로운 주문을 직접 생성하고 처리합니다.")
    @PostMapping
    fun createOrder(
        @Parameter(description = "주문 생성 요청 정보", required = true)
        @RequestBody request: CreateOrderRequest
    ): ApiResponse<OrderResponse> {
        val order = orderCommandUseCase.createOrder(request)
        val orderWithItems = getOrderQueryUseCase.getOrderWithItems(order.id)
        return ApiResponse.success(orderWithItems?.let { (_, items) ->
            order.toResponse(items)
        } ?: order.toResponse())
    }


    /**
     * 주문 ID로 단일 주문을 조회한다
     * Application-level에서 OrderItem과 조합하여 반환
     *
     * @param orderId 조회할 주문의 ID
     * @return 주문 정보를 포함한 API 응답
     */
    @Operation(summary = "주문 조회", description = "주문 ID로 주문 정보를 조회합니다.")
    @GetMapping("/{orderId}")
    fun getOrder(
        @Parameter(description = "주문 ID", required = true, example = "1")
        @PathVariable orderId: Long
    ): ApiResponse<OrderResponse?> {
        val orderWithItems = getOrderQueryUseCase.getOrderWithItems(orderId)
        return ApiResponse.success(orderWithItems?.let { (order, items) ->
            order.toResponse(items)
        })
    }

    /**
     * 특정 사용자의 주문 목록을 조회한다
     * 결제 진행 중(PENDING_PAYMENT) 및 만료(EXPIRED) 상태의 주문은 제외
     * Application-level에서 OrderItem과 조합하여 반환 (N+1 방지)
     *
     * @param userId 조회할 사용자의 ID
     * @return 사용자의 주문 목록을 포함한 API 응답
     */
    @Operation(summary = "사용자 주문 목록 조회", description = "특정 사용자의 주문 내역을 조회합니다. 결제 진행 중 및 만료된 주문은 제외됩니다.")
    @GetMapping
    fun getOrders(
        @Parameter(description = "사용자 ID", required = true, example = "1")
        @RequestParam userId: Long
    ): ApiResponse<List<OrderResponse>> {
        val ordersWithItems = getOrderQueryUseCase.getVisibleOrdersWithItemsByUser(userId)
        return ApiResponse.success(ordersWithItems.map { (order, items) ->
            order.toResponse(items)
        })
    }

    /**
     * 주문을 확정 처리한다
     *
     * @param orderId 확정할 주문의 ID
     * @param request 주문 확정 요청 데이터
     * @return 확정된 주문 정보를 포함한 API 응답
     */
    @Operation(summary = "주문 확정", description = "주문을 확정 처리합니다.")
    @PostMapping("/{orderId}/confirm")
    fun confirmOrder(
        @Parameter(description = "주문 ID", required = true, example = "1")
        @PathVariable orderId: Long,
        @Parameter(description = "주문 확정 요청 정보", required = true)
        @RequestBody request: OrderConfirmRequest
    ): ApiResponse<OrderResponse> {
        val order = orderCommandUseCase.confirmOrder(orderId)
        val orderWithItems = getOrderQueryUseCase.getOrderWithItems(orderId)
        return ApiResponse.success(orderWithItems?.let { (_, items) ->
            order.toResponse(items)
        } ?: order.toResponse())
    }

    /**
     * 주문을 취소 처리한다
     *
     * @param orderId 취소할 주문의 ID
     * @param request 주문 취소 요청 데이터
     * @return 취소된 주문 정보를 포함한 API 응답
     */
    @Operation(summary = "주문 취소", description = "주문을 취소 처리합니다.")
    @PostMapping("/{orderId}/cancel")
    fun cancelOrder(
        @Parameter(description = "주문 ID", required = true, example = "1")
        @PathVariable orderId: Long,
        @Parameter(description = "주문 취소 요청 정보", required = true)
        @RequestBody request: OrderCancelRequest
    ): ApiResponse<OrderResponse> {
        val order = orderCommandUseCase.cancelOrder(orderId, request.reason)
        val orderWithItems = getOrderQueryUseCase.getOrderWithItems(orderId)
        return ApiResponse.success(orderWithItems?.let { (_, items) ->
            order.toResponse(items)
        } ?: order.toResponse())
    }

    /**
     * 주문의 배송 정보를 조회한다
     *
     * @param orderId 조회할 주문의 ID
     * @return 배송 정보를 포함한 API 응답
     */
    @Operation(summary = "주문 배송 정보 조회", description = "특정 주문의 배송 정보를 조회합니다.")
    @GetMapping("/{orderId}/delivery")
    fun getDelivery(
        @Parameter(description = "주문 ID", required = true, example = "1")
        @PathVariable orderId: Long
    ): ApiResponse<DeliveryResponse> {
        val delivery = getDeliveryQueryUseCase.getDeliveryByOrderId(orderId)
        return ApiResponse.success(delivery.toResponse())
    }

}
