package io.hhplus.ecommerce.product.presentation.controller

import io.hhplus.ecommerce.product.application.usecase.*
import io.hhplus.ecommerce.product.application.port.out.ProductStatisticsPort
import io.hhplus.ecommerce.product.presentation.dto.*
import io.hhplus.ecommerce.product.presentation.dto.response.ProductRankingResponse
import io.hhplus.ecommerce.common.response.ApiResponse
import io.hhplus.ecommerce.common.response.Cursor
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

/**
 * 상품 API 컨트롤러 - 프레젠테이션 계층
 *
 * 역할:
 * - 상품 관련 REST API 엔드포인트 제공
 * - HTTP 요청/응답 처리 및 데이터 변환
 * - 비즈니스 로직은 UseCase에 위임
 *
 * 책임:
 * - 요청 데이터 검증 및 응답 형식 통일
 * - 적절한 UseCase로 비즈니스 로직 위임
 * - HTTP 상태 코드 및 에러 처리
 */
@Tag(name = "상품 관리", description = "상품 조회, 생성, 수정, 인기 상품, 판매 랭킹 API")
@RestController
@RequestMapping("/api/v1/products")
class ProductController(
    private val getProductQueryUseCase: GetProductQueryUseCase,
    private val productCommandUseCase: ProductCommandUseCase,
    private val productStatisticsQueryUseCase: ProductStatisticsQueryUseCase,
    private val productStatisticsPort: ProductStatisticsPort,
    private val productRankingQueryUseCase: ProductRankingQueryUseCase
) {

    @Operation(summary = "상품 목록 조회", description = "커서 기반 페이징으로 상품 목록을 조회합니다.")
    @GetMapping
    fun getProducts(
        @Parameter(description = "마지막 상품 ID (커서)", example = "100")
        @RequestParam(required = false) lastId: Long?,
        @Parameter(description = "조회할 상품 수", example = "20")
        @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "카테고리 ID (선택)")
        @RequestParam(required = false) categoryId: Long?
    ): ApiResponse<Cursor<ProductResponse>> {
        val productsCursor = if (categoryId != null) {
            getProductQueryUseCase.getProductsByCategory(categoryId, lastId, size)
        } else {
            getProductQueryUseCase.getProducts(lastId, size)
        }

        val responsesCursor = Cursor.from(
            productsCursor.contents.map { it.toResponse() },
            productsCursor.lastId
        )

        return ApiResponse.success(responsesCursor)
    }

    @Operation(summary = "상품 상세 조회", description = "상품 ID로 단일 상품을 조회하고 조회수를 증가시킵니다.")
    @GetMapping("/{productId}")
    fun getProduct(
        @Parameter(description = "조회할 상품 ID", required = true)
        @PathVariable productId: Long,
        @Parameter(description = "사용자 ID", example = "1")
        @RequestHeader("User-Id", defaultValue = "1") userId: Long
    ): ApiResponse<ProductResponse> {
        productStatisticsPort.recordViewEvent(productId, userId)
        val product = getProductQueryUseCase.getProduct(productId)
        return ApiResponse.success(product.toResponse())
    }

    @Operation(summary = "상품 생성", description = "새로운 상품을 생성합니다.")
    @PostMapping
    fun createProduct(
        @Parameter(description = "상품 생성 정보", required = true)
        @RequestBody request: CreateProductRequest
    ): ApiResponse<ProductResponse> {
        val product = productCommandUseCase.createProduct(request)
        return ApiResponse.success(product.toResponse())
    }

    @Operation(summary = "상품 정보 수정", description = "기존 상품 정보를 업데이트합니다.")
    @PutMapping("/{productId}")
    fun updateProduct(
        @Parameter(description = "수정할 상품 ID", required = true)
        @PathVariable productId: Long,
        @Parameter(description = "상품 수정 정보", required = true)
        @RequestBody request: UpdateProductRequest
    ): ApiResponse<ProductResponse> {
        val product = productCommandUseCase.updateProduct(productId, request)
        return ApiResponse.success(product.toResponse())
    }

    @Operation(summary = "인기 상품 조회", description = "인기 상품 목록을 조회합니다.")
    @GetMapping("/popular")
    fun getPopularProducts(
        @Parameter(description = "조회할 상품 수", example = "10")
        @RequestParam(defaultValue = "10") limit: Int
    ): ApiResponse<List<ProductResponse>> {
        val products = getProductQueryUseCase.getPopularProducts(limit)
        return ApiResponse.success(products.map { it.toResponse() })
    }

    @Operation(
        summary = "정렬 기준별 상품 목록 조회",
        description = "다양한 정렬 기준(인기순, 조회순, 찜순, 판매순)에 따른 상품 목록을 조회합니다."
    )
    @GetMapping("/sorted")
    fun getProductsBySortCriteria(
        @Parameter(description = "정렬 기준", example = "POPULAR")
        @RequestParam sortBy: ProductSortCriteria,
        @Parameter(description = "조회할 상품 수", example = "20")
        @RequestParam(defaultValue = "20") limit: Int
    ): ApiResponse<List<ProductResponse>> {
        val products = productStatisticsQueryUseCase.getProductsBySortCriteria(sortBy, limit)
        return ApiResponse.success(products.map { it.toResponse() })
    }

    @Operation(
        summary = "카테고리별 정렬 상품 조회",
        description = "특정 카테고리에서 정렬 기준에 따른 상품 목록을 조회합니다."
    )
    @GetMapping("/categories/{categoryId}/sorted")
    fun getProductsByCategoryAndSortCriteria(
        @Parameter(description = "카테고리 ID", required = true)
        @PathVariable categoryId: Long,
        @Parameter(description = "정렬 기준", example = "POPULAR")
        @RequestParam sortBy: ProductSortCriteria,
        @Parameter(description = "조회할 상품 수", example = "20")
        @RequestParam(defaultValue = "20") limit: Int
    ): ApiResponse<List<ProductResponse>> {
        val products = productStatisticsQueryUseCase.getProductsByCategoryAndSortCriteria(categoryId, sortBy, limit)
        return ApiResponse.success(products.map { it.toResponse() })
    }

    // ===== 판매 랭킹 API (STEP 13) =====

    @Operation(
        summary = "오늘의 판매 랭킹 조회",
        description = "오늘 가장 많이 팔린 상품 랭킹을 조회합니다."
    )
    @GetMapping("/ranking/daily")
    fun getTodayRanking(
        @Parameter(description = "조회할 상품 수", example = "10")
        @RequestParam(defaultValue = "10") limit: Int
    ): ApiResponse<List<ProductRankingResponse>> {
        val ranking = productRankingQueryUseCase.getTodayTopProducts(limit)
        return ApiResponse.success(ranking)
    }

    @Operation(
        summary = "이번 주 판매 랭킹 조회",
        description = "이번 주 가장 많이 팔린 상품 랭킹을 조회합니다."
    )
    @GetMapping("/ranking/weekly")
    fun getThisWeekRanking(
        @Parameter(description = "조회할 상품 수", example = "10")
        @RequestParam(defaultValue = "10") limit: Int
    ): ApiResponse<List<ProductRankingResponse>> {
        val ranking = productRankingQueryUseCase.getThisWeekTopProducts(limit)
        return ApiResponse.success(ranking)
    }

    @Operation(
        summary = "누적 판매 랭킹 조회",
        description = "전체 기간 중 가장 많이 팔린 상품 랭킹을 조회합니다."
    )
    @GetMapping("/ranking/total")
    fun getTotalRanking(
        @Parameter(description = "조회할 상품 수", example = "10")
        @RequestParam(defaultValue = "10") limit: Int
    ): ApiResponse<List<ProductRankingResponse>> {
        val ranking = productRankingQueryUseCase.getTotalTopProducts(limit)
        return ApiResponse.success(ranking)
    }

    @Operation(
        summary = "특정 상품의 오늘 판매 순위 조회",
        description = "특정 상품의 오늘 판매 순위와 판매량을 조회합니다."
    )
    @GetMapping("/{productId}/ranking")
    fun getProductRanking(
        @Parameter(description = "상품 ID", required = true)
        @PathVariable productId: Long
    ): ApiResponse<ProductRankingResponse> {
        val ranking = productRankingQueryUseCase.getProductTodayRanking(productId)
        return ApiResponse.success(ranking)
    }
}
