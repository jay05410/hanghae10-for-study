package io.hhplus.ecommerce.product.controller

import io.hhplus.ecommerce.product.usecase.*
import io.hhplus.ecommerce.product.dto.*
import io.hhplus.ecommerce.product.dto.ProductResponse.Companion.toResponse
import io.hhplus.ecommerce.common.response.ApiResponse
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
@RestController
@RequestMapping("/api/v1/products")
class ProductController(
    private val getProductQueryUseCase: GetProductQueryUseCase,
    private val createProductUseCase: CreateProductUseCase,
    private val updateProductUseCase: UpdateProductUseCase,
    private val getPopularProductsUseCase: GetPopularProductsUseCase,
    private val incrementProductViewUseCase: IncrementProductViewUseCase
) {

    /**
     * 상품 목록을 조회한다 (페이지네이션 또는 카테고리 필터링)
     *
     * @param page 조회할 페이지 번호 (기본값: 1)
     * @param categoryId 조회할 카테고리 ID (선택사항)
     * @return 상품 목록을 포함한 API 응답
     */
    @GetMapping
    fun getProducts(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(required = false) categoryId: Long?
    ): ApiResponse<List<ProductResponse>> {
        val products = if (categoryId != null) {
            getProductQueryUseCase.getProductsByCategory(categoryId)
        } else {
            getProductQueryUseCase.getProducts(page)
        }
        return ApiResponse.success(products.map { it.toResponse() })
    }

    /**
     * 상품 ID로 단일 상품을 조회하고 조회수를 증가시킨다
     *
     * @param productId 조회할 상품의 ID
     * @param userId 조회를 요청한 사용자 ID (기본값: 1)
     * @return 상품 정보를 포함한 API 응답
     */
    @GetMapping("/{productId}")
    fun getProduct(
        @PathVariable productId: Long,
        @RequestHeader("User-Id", defaultValue = "1") userId: Long
    ): ApiResponse<ProductResponse> {
        incrementProductViewUseCase.execute(productId, userId)
        val product = getProductQueryUseCase.getProduct(productId)
        return ApiResponse.success(product.toResponse())
    }

    /**
     * 새로운 상품을 생성한다
     *
     * @param request 상품 생성 요청 데이터
     * @return 생성된 상품 정보를 포함한 API 응답
     */
    @PostMapping
    fun createProduct(@RequestBody request: CreateProductRequest): ApiResponse<ProductResponse> {
        val product = createProductUseCase.execute(request)
        return ApiResponse.success(product.toResponse())
    }

    /**
     * 기존 상품 정보를 업데이트한다
     *
     * @param productId 업데이트할 상품의 ID
     * @param request 상품 업데이트 요청 데이터
     * @return 업데이트된 상품 정보를 포함한 API 응답
     */
    @PutMapping("/{productId}")
    fun updateProduct(
        @PathVariable productId: Long,
        @RequestBody request: UpdateProductRequest
    ): ApiResponse<ProductResponse> {
        val product = updateProductUseCase.execute(productId, request)
        return ApiResponse.success(product.toResponse())
    }

    /**
     * 인기 상품 목록을 조회한다
     *
     * @param limit 조회할 상품 수 (기본값: 10)
     * @return 인기 상품 목록을 포함한 API 응답
     */
    @GetMapping("/popular")
    fun getPopularProducts(@RequestParam(defaultValue = "10") limit: Int): ApiResponse<List<ProductResponse>> {
        val products = getPopularProductsUseCase.execute(limit)
        return ApiResponse.success(products.map { it.toResponse() })
    }


}