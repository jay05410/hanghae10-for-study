package io.hhplus.ecommerce.common.client

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * 데이터 플랫폼 클라이언트 인터페이스
 *
 * 주문 정보 등 비즈니스 데이터를 외부 데이터 플랫폼에 전송
 * 실무에서는 HTTP Client, gRPC 등으로 구현
 */
interface DataPlatformClient {

    /**
     * 주문 정보 전송
     *
     * @param orderInfo 주문 정보
     * @param idempotencyKey 멱등성 키 (orderId:status 형태)
     * @return 전송 결과
     */
    fun sendOrderInfo(orderInfo: OrderInfoPayload, idempotencyKey: String): DataPlatformResponse
}

/**
 * 주문 정보 페이로드
 *
 * 데이터 플랫폼에 전송할 주문 정보
 */
@Serializable
data class OrderInfoPayload(
    val orderId: Long,
    val orderNumber: String,
    val userId: Long,
    val items: List<OrderItemPayload>,
    val totalAmount: Long,
    val discountAmount: Long,
    val finalAmount: Long,
    val status: String,
    val paymentId: Long? = null,
    val createdAt: String
)

@Serializable
data class OrderItemPayload(
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitPrice: Long,
    val totalPrice: Long
)

/**
 * 데이터 플랫폼 응답
 */
data class DataPlatformResponse(
    val success: Boolean,
    val message: String?,
    val timestamp: Instant = Instant.now()
)
