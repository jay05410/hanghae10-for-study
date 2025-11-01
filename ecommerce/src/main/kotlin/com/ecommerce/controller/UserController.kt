package com.ecommerce.controller

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users")
class UserController {

    companion object {
        // 사용자 잔액 데이터
        private var currentBalance = 50000

        // 포인트 거래 내역 데이터
        private val balanceHistory = mutableListOf<Map<String, Any>>()
        private var nextTransactionId = 1

        init {
            // 기본 거래 내역 데이터
            balanceHistory.add(
                mapOf(
                    "transactionId" to 3,
                    "transactionType" to "USE",
                    "amount" to -29000,
                    "balanceAfter" to 21000,
                    "description" to "주문 결제 (ORD-20251031-001)",
                    "createdAt" to "2025-10-31T14:30:00Z"
                )
            )
            balanceHistory.add(
                mapOf(
                    "transactionId" to 2,
                    "transactionType" to "REFUND",
                    "amount" to 49000,
                    "balanceAfter" to 50000,
                    "description" to "주문 취소 환불 (ORD-20251030-045)",
                    "createdAt" to "2025-10-31T11:00:00Z"
                )
            )
            balanceHistory.add(
                mapOf(
                    "transactionId" to 1,
                    "transactionType" to "CHARGE",
                    "amount" to 50000,
                    "balanceAfter" to 50000,
                    "description" to "포인트 충전",
                    "createdAt" to "2025-10-31T10:00:00Z"
                )
            )
            nextTransactionId = 4
        }
    }

    // 7.1 포인트 충전
    @PostMapping("/balance/charge")
    fun chargeBalance(@RequestBody body: Map<String, Any>): Map<String, Any> {
        val amount = body["amount"] as? Int

        // 입력값 검증
        if (amount == null || amount <= 0) {
            return mapOf(
                "success" to false,
                "error" to mapOf(
                    "code" to "PAYMENT004",
                    "message" to "유효하지 않은 결제 금액입니다",
                    "details" to mapOf("amount" to amount)
                ),
                "timestamp" to "2025-10-31T10:00:00Z"
            )
        }

        if (amount % 100 != 0) {
            return mapOf(
                "success" to false,
                "error" to mapOf(
                    "code" to "PAYMENT004",
                    "message" to "충전 금액은 100원 단위여야 합니다",
                    "details" to mapOf("amount" to amount)
                ),
                "timestamp" to "2025-10-31T10:00:00Z"
            )
        }

        // 잔액 업데이트
        currentBalance += amount
        val transactionId = nextTransactionId++

        // 거래 내역 추가
        val transaction = mapOf(
            "transactionId" to transactionId,
            "transactionType" to "CHARGE",
            "amount" to amount,
            "balanceAfter" to currentBalance,
            "description" to "포인트 충전",
            "createdAt" to "2025-10-31T10:00:00Z"
        )
        balanceHistory.add(0, transaction) // 최신 내역을 앞에 추가

        return mapOf(
            "success" to true,
            "data" to mapOf(
                "transactionId" to transactionId,
                "amount" to amount,
                "balanceAfter" to currentBalance,
                "chargedAt" to "2025-10-31T10:00:00Z"
            ),
            "timestamp" to "2025-10-31T10:00:00Z"
        )
    }

    // 7.2 포인트 내역 조회
    @GetMapping("/balance/history")
    fun getBalanceHistory(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): Map<String, Any> {
        // 페이징 처리
        val startIndex = page * size
        val endIndex = minOf(startIndex + size, balanceHistory.size)

        val pagedHistory = if (startIndex < balanceHistory.size) {
            balanceHistory.subList(startIndex, endIndex)
        } else {
            emptyList()
        }

        val totalElements = balanceHistory.size
        val totalPages = (totalElements + size - 1) / size

        return mapOf(
            "success" to true,
            "data" to mapOf(
                "currentBalance" to currentBalance,
                "history" to pagedHistory,
                "pagination" to mapOf(
                    "page" to page,
                    "size" to size,
                    "totalElements" to totalElements,
                    "totalPages" to totalPages
                )
            ),
            "timestamp" to "2025-10-31T14:30:00Z"
        )
    }

    // 현재 잔액 조회 (추가 편의 메서드)
    @GetMapping("/balance")
    fun getCurrentBalance(): Map<String, Any> {
        return mapOf(
            "success" to true,
            "data" to mapOf(
                "currentBalance" to currentBalance
            ),
            "timestamp" to "2025-10-31T14:30:00Z"
        )
    }
}