package com.ecommerce.controller

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/orders")
class OrderController {

    companion object {
        // 주문 데이터
        private val orders = mutableMapOf<Int, Map<String, Any>>()
        private var nextOrderId = 1

        init {
            // 기본 주문 데이터
            orders[1] = mapOf(
                "orderId" to 1,
                "orderNumber" to "ORD-20251031-001",
                "totalItems" to 2,
                "finalAmount" to 29000,
                "status" to "PAID",
                "orderedAt" to "2025-10-31T14:30:00Z"
            )
            orders[2] = mapOf(
                "orderId" to 2,
                "orderNumber" to "ORD-20251030-045",
                "totalItems" to 1,
                "finalAmount" to 49000,
                "status" to "SHIPPED",
                "orderedAt" to "2025-10-30T10:15:00Z"
            )
            nextOrderId = 3
        }
    }

    // 5.1 주문 생성
    @PostMapping
    fun createOrder(@RequestBody body: Map<String, Any>): Map<String, Any> {
        // 기본 검증
        val deliveryAddress = body["deliveryAddress"] as? Map<String, Any>
        if (deliveryAddress == null || deliveryAddress["recipient"] == null) {
            return mapOf(
                "success" to false,
                "error" to mapOf(
                    "code" to "ORDER004",
                    "message" to "배송지 정보가 필요합니다"
                ),
                "timestamp" to "2025-10-31T14:30:00Z"
            )
        }

        // 주문 생성
        val orderId = nextOrderId++
        val orderNumber = "ORD-20251031-${String.format("%03d", orderId)}"

        return mapOf(
            "success" to true,
            "data" to mapOf(
                "orderId" to orderId,
                "orderNumber" to orderNumber,
                "items" to listOf(
                    mapOf(
                        "product" to "기본 7일 박스",
                        "combination" to "피로 + 활력 + 시트러스",
                        "quantity" to 1,
                        "price" to 29000
                    )
                ),
                "payment" to mapOf(
                    "totalAmount" to 29000,
                    "discountAmount" to 14500,
                    "finalAmount" to 14500,
                    "method" to "BALANCE"
                ),
                "delivery" to mapOf(
                    "recipient" to deliveryAddress["recipient"],
                    "phone" to deliveryAddress["phone"],
                    "address" to "${deliveryAddress["address"]} ${deliveryAddress["addressDetail"]}",
                    "estimatedDeliveryDate" to "2025-11-03"
                ),
                "status" to "PAID",
                "orderedAt" to "2025-10-31T14:30:00Z"
            ),
            "timestamp" to "2025-10-31T14:30:00Z"
        )
    }

    // 5.2 주문 내역 조회
    @GetMapping
    fun getOrders(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "ALL") status: String
    ): Map<String, Any> {
        var content = orders.values.toList()

        // 상태 필터링
        if (status != "ALL") {
            content = content.filter { it["status"] == status }
        }

        return mapOf(
            "success" to true,
            "data" to mapOf(
                "content" to content,
                "pagination" to mapOf(
                    "page" to page,
                    "size" to size,
                    "totalElements" to content.size,
                    "totalPages" to (content.size + size - 1) / size
                )
            ),
            "timestamp" to "2025-10-31T14:30:00Z"
        )
    }

    // 5.3 주문 상세 조회
    @GetMapping("/{orderId}")
    fun getOrderDetail(@PathVariable orderId: Int): Map<String, Any> {
        return if (!orders.containsKey(orderId)) {
            mapOf(
                "success" to false,
                "error" to mapOf(
                    "code" to "ORDER001",
                    "message" to "존재하지 않는 주문입니다",
                    "details" to mapOf("orderId" to orderId)
                ),
                "timestamp" to "2025-10-31T14:30:00Z"
            )
        } else {
            val weeklyTeas = listOf(
                mapOf(
                    "dayNumber" to 1,
                    "dayOfWeek" to "월요일",
                    "teaName" to "레몬그라스 진저 티",
                    "mainIngredients" to "레몬그라스, 생강, 레몬밤",
                    "expectedEffects" to "활력 증진, 기분 전환",
                    "brewingGuide" to "80도 물 200ml에 3분"
                ),
                mapOf(
                    "dayNumber" to 2,
                    "dayOfWeek" to "화요일",
                    "teaName" to "얼그레이 베르가못",
                    "mainIngredients" to "홍차, 베르가못",
                    "expectedEffects" to "에너지 증진, 기분 상승",
                    "brewingGuide" to "95도 물 200ml에 3-4분"
                )
            )

            mapOf(
                "success" to true,
                "data" to mapOf(
                    "orderId" to orderId,
                    "orderNumber" to "ORD-20251031-001",
                    "status" to "PAID",
                    "items" to listOf(
                        mapOf(
                            "product" to mapOf(
                                "name" to "기본 7일 박스",
                                "price" to 29000
                            ),
                            "combination" to mapOf(
                                "condition" to "피로",
                                "mood" to "활력",
                                "scent" to "시트러스"
                            ),
                            "quantity" to 1,
                            "weeklyTeas" to weeklyTeas
                        )
                    ),
                    "payment" to mapOf(
                        "totalAmount" to 29000,
                        "discountAmount" to 14500,
                        "finalAmount" to 14500,
                        "paidAt" to "2025-10-31T14:30:15Z"
                    ),
                    "delivery" to mapOf<String, Any?>(
                        "recipient" to "김민지",
                        "phone" to "010-1234-5678",
                        "address" to "서울시 강남구 테헤란로 123 456호",
                        "estimatedDeliveryDate" to "2025-11-03",
                        "trackingNumber" to null
                    ),
                    "orderedAt" to "2025-10-31T14:30:00Z"
                ),
                "timestamp" to "2025-10-31T14:30:00Z"
            )
        }
    }

    // 5.4 주문 취소
    @PostMapping("/{orderId}/cancel")
    fun cancelOrder(@PathVariable orderId: Int, @RequestBody body: Map<String, Any>): Map<String, Any> {
        val order = orders[orderId]
        return if (order == null) {
            mapOf(
                "success" to false,
                "error" to mapOf(
                    "code" to "ORDER001",
                    "message" to "존재하지 않는 주문입니다",
                    "details" to mapOf("orderId" to orderId)
                ),
                "timestamp" to "2025-10-31T15:00:00Z"
            )
        } else {
            val status = order["status"] as String

            // 취소 가능 상태 확인
            if (status in listOf("PREPARING", "SHIPPED", "DELIVERED")) {
                mapOf(
                    "success" to false,
                    "error" to mapOf(
                        "code" to "ORDER003",
                        "message" to "취소할 수 없는 주문 상태입니다",
                        "details" to mapOf("currentStatus" to status)
                    ),
                    "timestamp" to "2025-10-31T15:00:00Z"
                )
            } else {
                // 주문 상태 업데이트
                val updatedOrder = order.toMutableMap()
                updatedOrder["status"] = "CANCELLED"
                orders[orderId] = updatedOrder

                mapOf(
                    "success" to true,
                    "data" to mapOf(
                        "orderId" to orderId,
                        "status" to "CANCELLED",
                        "refund" to mapOf(
                            "amount" to 14500,
                            "method" to "BALANCE",
                            "refundedAt" to "2025-10-31T15:00:00Z"
                        ),
                        "message" to "주문이 취소되었습니다"
                    ),
                    "timestamp" to "2025-10-31T15:00:00Z"
                )
            }
        }
    }
}