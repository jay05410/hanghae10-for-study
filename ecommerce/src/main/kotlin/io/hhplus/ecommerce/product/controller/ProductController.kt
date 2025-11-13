package io.hhplus.ecommerce.product.controller

import io.hhplus.ecommerce.product.usecase.*
import io.hhplus.ecommerce.product.dto.*
import io.hhplus.ecommerce.common.response.ApiResponse
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
@Tag(name = "상품 관리", description = "상품 조회, 생성, 수정, 인기 상품 API")
@RestController
@RequestMapping("/api/v1/products")
class ProductController(
    private val getProductQueryUseCase: GetProductQueryUseCase,
    private val productCommandUseCase: ProductCommandUseCase,
    private val productStatsUseCase: ProductStatsUseCase
) {

    @Operation(summary = "상품 목록 조회", description = "페이지네이션 또는 카테고리별 상품 목록을 조회합니다.")
    @GetMapping
    fun getProducts(
        @Parameter(description = "페이지 번호", example = "1")
        @RequestParam(defaultValue = "1") page: Int,
        @Parameter(description = "카테고리 ID (선택)")
        @RequestParam(required = false) categoryId: Long?
    ): ApiResponse<List<ProductResponse>> {
        val products = if (categoryId != null) {
            getProductQueryUseCase.getProductsByCategory(categoryId)
        } else {
            getProductQueryUseCase.getProducts(page)
        }
        return ApiResponse.success(products.map { it.toResponse() })
    }

    @Operation(summary = "상품 상세 조회", description = "상품 ID로 단일 상품을 조회하고 조회수를 증가시킵니다.")
    @GetMapping("/{productId}")
    fun getProduct(
        @Parameter(description = "조회할 상품 ID", required = true)
        @PathVariable productId: Long,
        @Parameter(description = "사용자 ID", example = "1")
        @RequestHeader("User-Id", defaultValue = "1") userId: Long
    ): ApiResponse<ProductResponse> {
        productStatsUseCase.incrementViewCount(productId, userId)
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


}