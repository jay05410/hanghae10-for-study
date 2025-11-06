package com.ecommerce.controller

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/coupons")
class CouponController {

    companion object {
        // 쿠폰 데이터
        private val coupons = mapOf(
            1 to mapOf(
                "id" to 1,
                "name" to "첫구매 50% 할인",
                "code" to "FIRST50",
                "discountType" to "PERCENTAGE",
                "discountValue" to 50,
                "minOrderAmount" to 20000,
                "maxIssueCount" to 100,
                "issuedCount" to 78,
                "remainingCount" to 22,
                "issueStartAt" to "2025-10-25T00:00:00Z",
                "issueEndAt" to "2025-11-30T23:59:59Z",
                "validDays" to 7,
                "isActive" to true,
                "isIssuable" to true
            ),
            2 to mapOf(
                "id" to 2,
                "name" to "얼리버드 10,000원 할인",
                "code" to "EARLYBIRD",
                "discountType" to "FIXED",
                "discountValue" to 10000,
                "minOrderAmount" to 29000,
                "maxIssueCount" to 200,
                "issuedCount" to 200,
                "remainingCount" to 0,
                "issueStartAt" to "2025-10-20T00:00:00Z",
                "issueEndAt" to "2025-10-31T23:59:59Z",
                "validDays" to 14,
                "isActive" to true,
                "isIssuable" to false
            )
        )

        // 사용자 쿠폰 데이터
        private val userCoupons = mutableMapOf<Int, Map<String, Any?>>()
        private var nextUserCouponId = 1

        init {
            // 기본 사용자 쿠폰 데이터
            userCoupons[1] = mapOf(
                "userCouponId" to 1,
                "coupon" to mapOf(
                    "id" to 1,
                    "name" to "첫구매 50% 할인",
                    "discountType" to "PERCENTAGE",
                    "discountValue" to 50,
                    "minOrderAmount" to 20000
                ),
                "status" to "AVAILABLE",
                "issuedAt" to "2025-10-31T14:00:00Z",
                "expiredAt" to "2025-11-07T23:59:59Z",
                "usedAt" to null,
                "daysUntilExpiry" to 7
            )
            nextUserCouponId = 2
        }
    }

    // 6.1 선착순 쿠폰 발급
    @PostMapping("/{couponId}/issue")
    fun issueCoupon(@PathVariable couponId: Int): Map<String, Any> {
        val coupon = coupons[couponId]
        if (coupon == null) {
            return mapOf(
                "success" to false,
                "error" to mapOf(
                    "code" to "COUPON005",
                    "message" to "존재하지 않는 쿠폰입니다",
                    "details" to mapOf("couponId" to couponId)
                ),
                "timestamp" to "2025-10-31T14:00:00Z"
            )
        }

        // 발급 가능 여부 확인
        val isIssuable = coupon["isIssuable"] as Boolean
        if (!isIssuable) {
            val remainingCount = coupon["remainingCount"] as Int
            if (remainingCount == 0) {
                return mapOf(
                    "success" to false,
                    "error" to mapOf(
                        "code" to "COUPON001",
                        "message" to "쿠폰이 모두 소진되었습니다"
                    ),
                    "timestamp" to "2025-10-31T14:00:00Z"
                )
            }
        }

        // 중복 발급 확인 (간단히 구현)
        val alreadyIssued = userCoupons.values.any { uc ->
            val userCoupon = uc["coupon"] as Map<String, Any>
            couponId == userCoupon["id"]
        }

        if (alreadyIssued) {
            return mapOf(
                "success" to false,
                "error" to mapOf(
                    "code" to "COUPON002",
                    "message" to "이미 발급받은 쿠폰입니다"
                ),
                "timestamp" to "2025-10-31T14:00:00Z"
            )
        }

        // 쿠폰 발급
        val userCouponId = nextUserCouponId++
        return mapOf(
            "success" to true,
            "data" to mapOf(
                "userCouponId" to userCouponId,
                "coupon" to mapOf(
                    "id" to coupon["id"],
                    "name" to coupon["name"],
                    "discountType" to coupon["discountType"],
                    "discountValue" to coupon["discountValue"],
                    "minOrderAmount" to coupon["minOrderAmount"]
                ),
                "issuedAt" to "2025-10-31T14:00:00Z",
                "expiredAt" to "2025-11-07T23:59:59Z",
                "message" to "쿠폰이 발급되었습니다"
            ),
            "timestamp" to "2025-10-31T14:00:00Z"
        )
    }

    // 6.2 발급 가능 쿠폰 목록 조회
    @GetMapping
    fun getAvailableCoupons(): Map<String, Any> {
        return mapOf(
            "success" to true,
            "data" to coupons.values.toList(),
            "timestamp" to "2025-10-31T14:00:00Z"
        )
    }

    // 6.3 보유 쿠폰 조회
    @GetMapping("/my")
    fun getMyCoupons(@RequestParam(defaultValue = "ALL") status: String): Map<String, Any> {
        var userCouponList = userCoupons.values.toList()

        // 상태 필터링
        if (status != "ALL") {
            userCouponList = userCouponList.filter { it["status"] == status }
        }

        return mapOf(
            "success" to true,
            "data" to userCouponList,
            "timestamp" to "2025-10-31T14:00:00Z"
        )
    }
}