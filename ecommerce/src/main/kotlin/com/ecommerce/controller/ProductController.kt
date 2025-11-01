package com.ecommerce.controller

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/products")
class ProductController {

    companion object {
        // 박스 상품 데이터
        private val products = mapOf(
            1 to mapOf(
                "id" to 1,
                "name" to "기본 7일 박스",
                "description" to "나를 위한 티 큐레이션 7일",
                "price" to 29000,
                "type" to "BASIC",
                "dailyProductionLimit" to 30,
                "isActive" to true
            ),
            2 to mapOf(
                "id" to 2,
                "name" to "프리미엄 7일 박스",
                "description" to "프리미엄 블렌딩 티로 구성된 7일 큐레이션",
                "price" to 49000,
                "type" to "PREMIUM",
                "dailyProductionLimit" to 20,
                "isActive" to true
            )
        )

        // 선택 옵션 데이터
        private val options = mapOf(
            "conditions" to listOf(
                mapOf("id" to 1, "name" to "FATIGUE", "displayName" to "피로", "description" to "몸이 무겁고 쉽게 지쳐요", "sortOrder" to 1),
                mapOf("id" to 2, "name" to "STRESS", "displayName" to "스트레스", "description" to "마음이 답답하고 긴장돼요", "sortOrder" to 2),
                mapOf("id" to 3, "name" to "DIGESTION", "displayName" to "소화불편", "description" to "속이 더부룩하고 소화가 안돼요", "sortOrder" to 3)
            ),
            "moods" to listOf(
                mapOf("id" to 1, "name" to "ENERGETIC", "displayName" to "활력", "description" to "에너지와 활기가 필요해요", "sortOrder" to 1),
                mapOf("id" to 2, "name" to "CALM", "displayName" to "평온", "description" to "편안하고 차분한 기분이 좋아요", "sortOrder" to 2),
                mapOf("id" to 3, "name" to "FOCUS", "displayName" to "집중", "description" to "집중력과 명료함이 필요해요", "sortOrder" to 3)
            ),
            "scents" to listOf(
                mapOf("id" to 1, "name" to "FLORAL", "displayName" to "플로럴", "description" to "꽃향기가 나는 부드러운 향", "sortOrder" to 1),
                mapOf("id" to 2, "name" to "CITRUS", "displayName" to "시트러스", "description" to "상큼하고 청량한 과일 향", "sortOrder" to 2),
                mapOf("id" to 3, "name" to "HERBAL", "displayName" to "허브", "description" to "허브의 깊고 진한 향", "sortOrder" to 3)
            )
        )

        // 조합별 재고 데이터
        private val combinations = mapOf(
            1 to mapOf(
                "combinationId" to 1,
                "combination" to mapOf(
                    "productId" to 1,
                    "productName" to "기본 7일 박스",
                    "condition" to "피로",
                    "mood" to "활력",
                    "scent" to "시트러스"
                ),
                "stock" to 15,
                "isAvailable" to true
            ),
            5 to mapOf(
                "combinationId" to 5,
                "combination" to mapOf(
                    "productId" to 1,
                    "productName" to "기본 7일 박스",
                    "condition" to "스트레스",
                    "mood" to "평온",
                    "scent" to "플로럴"
                ),
                "stock" to 8,
                "isAvailable" to true
            )
        )

        // 인기 조합 데이터
        private val popularCombinations = listOf(
            mapOf(
                "rank" to 1,
                "combination" to mapOf(
                    "id" to 1,
                    "productName" to "기본 7일 박스",
                    "condition" to "피로",
                    "mood" to "활력",
                    "scent" to "시트러스"
                ),
                "orderCount" to 143,
                "percentageChange" to "+15%",
                "tagline" to "에너지가 필요한 직장인들의 선택"
            ),
            mapOf(
                "rank" to 2,
                "combination" to mapOf(
                    "id" to 5,
                    "productName" to "기본 7일 박스",
                    "condition" to "스트레스",
                    "mood" to "평온",
                    "scent" to "플로럴"
                ),
                "orderCount" to 112,
                "percentageChange" to "+8%",
                "tagline" to "마음의 안정을 찾고 싶을 때"
            )
        )
    }

    // 3.1 박스 상품 목록 조회
    @GetMapping
    fun getProducts(): Map<String, Any> {
        return mapOf(
            "success" to true,
            "data" to products.values.toList(),
            "timestamp" to "2025-10-31T10:30:00Z"
        )
    }

    // 3.2 선택 옵션 조회
    @GetMapping("/options")
    fun getOptions(): Map<String, Any> {
        return mapOf(
            "success" to true,
            "data" to options,
            "timestamp" to "2025-10-31T10:30:00Z"
        )
    }

    // 3.3 조합별 재고 조회
    @GetMapping("/combinations/{combinationId}/inventory")
    fun getCombinationInventory(@PathVariable combinationId: Int): Map<String, Any> {
        val combination = combinations[combinationId]

        return if (combination == null) {
            mapOf(
                "success" to false,
                "error" to mapOf(
                    "code" to "PRODUCT003",
                    "message" to "존재하지 않는 조합입니다",
                    "details" to mapOf("combinationId" to combinationId)
                ),
                "timestamp" to "2025-10-31T10:30:00Z"
            )
        } else {
            val data = combination.toMutableMap()
            data["lastUpdated"] = "2025-10-31T10:30:00Z"

            mapOf(
                "success" to true,
                "data" to data,
                "timestamp" to "2025-10-31T10:30:00Z"
            )
        }
    }

    // 3.4 인기 조합 통계 조회
    @GetMapping("/popular-combinations")
    fun getPopularCombinations(@RequestParam(defaultValue = "3") days: Int): Map<String, Any> {
        return mapOf(
            "success" to true,
            "data" to mapOf(
                "period" to mapOf(
                    "days" to days,
                    "from" to "2025-10-28T00:00:00Z",
                    "to" to "2025-10-31T23:59:59Z"
                ),
                "combinations" to popularCombinations
            ),
            "timestamp" to "2025-10-31T10:30:00Z"
        )
    }

    // 3.5 박스 조합 미리보기
    @GetMapping("/combinations/{combinationId}/preview")
    fun getCombinationPreview(@PathVariable combinationId: Int): Map<String, Any> {
        val combination = combinations[combinationId]

        return if (combination == null) {
            mapOf(
                "success" to false,
                "error" to mapOf(
                    "code" to "PRODUCT003",
                    "message" to "존재하지 않는 조합입니다",
                    "details" to mapOf("combinationId" to combinationId)
                ),
                "timestamp" to "2025-10-31T10:30:00Z"
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
                ),
                mapOf(
                    "dayNumber" to 3,
                    "dayOfWeek" to "수요일",
                    "teaName" to "페퍼민트 그린티",
                    "mainIngredients" to "녹차, 페퍼민트",
                    "expectedEffects" to "정신 맑게, 상쾌함",
                    "brewingGuide" to "70도 물 200ml에 2-3분"
                ),
                mapOf(
                    "dayNumber" to 4,
                    "dayOfWeek" to "목요일",
                    "teaName" to "로즈마리 레몬티",
                    "mainIngredients" to "로즈마리, 레몬필",
                    "expectedEffects" to "집중력 향상, 활력",
                    "brewingGuide" to "90도 물 200ml에 5분"
                ),
                mapOf(
                    "dayNumber" to 5,
                    "dayOfWeek" to "금요일",
                    "teaName" to "유자 루이보스",
                    "mainIngredients" to "루이보스, 유자",
                    "expectedEffects" to "비타민 보충, 활력",
                    "brewingGuide" to "100도 물 200ml에 5분"
                ),
                mapOf(
                    "dayNumber" to 6,
                    "dayOfWeek" to "토요일",
                    "teaName" to "오렌지 카모마일",
                    "mainIngredients" to "카모마일, 오렌지필",
                    "expectedEffects" to "편안한 휴식, 기분 전환",
                    "brewingGuide" to "95도 물 200ml에 5분"
                ),
                mapOf(
                    "dayNumber" to 7,
                    "dayOfWeek" to "일요일",
                    "teaName" to "자스민 화이트티",
                    "mainIngredients" to "백차, 자스민",
                    "expectedEffects" to "마음 진정, 평온",
                    "brewingGuide" to "75도 물 200ml에 3분"
                )
            )

            mapOf(
                "success" to true,
                "data" to mapOf(
                    "combinationId" to combinationId,
                    "combination" to combination["combination"],
                    "weeklyTeas" to weeklyTeas
                ),
                "timestamp" to "2025-10-31T10:30:00Z"
            )
        }
    }
}