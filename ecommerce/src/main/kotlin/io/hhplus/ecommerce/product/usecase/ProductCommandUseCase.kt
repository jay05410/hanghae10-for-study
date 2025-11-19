package io.hhplus.ecommerce.product.usecase

import io.hhplus.ecommerce.product.application.ProductService
import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.product.dto.CreateProductRequest
import io.hhplus.ecommerce.product.dto.UpdateProductRequest
import org.springframework.stereotype.Component

/**
 * 상품 명령 UseCase
 *
 * 역할:
 * - 모든 상품 변경 작업을 통합 관리
 * - 상품 생성, 수정 기능 제공
 *
 * 책임:
 * - 상품 생성/수정 요청 검증 및 실행
 * - 상품 데이터 무결성 보장
 */
@Component
class ProductCommandUseCase(
    private val productService: ProductService
) {

    /**
     * 새로운 상품을 등록하고 생성한다
     *
     * @param request 상품 생성 요청 데이터
     * @return 생성이 완료된 상품 정보
     * @throws IllegalArgumentException 상품 정보가 유효하지 않은 경우
     * @throws RuntimeException 상품 생성 처리에 실패한 경우
     */
    fun createProduct(request: CreateProductRequest): Product {
        return productService.createProduct(
            name = request.name,
            description = request.description,
            price = request.price,
            categoryId = request.categoryId
        )
    }

    /**
     * 지정된 상품의 정보를 수정하고 업데이트한다
     *
     * @param productId 수정할 상품 ID
     * @param request 상품 수정 요청 데이터
     * @return 수정이 완료된 상품 정보
     * @throws IllegalArgumentException 상품을 찾을 수 없거나 수정 정보가 잘못된 경우
     * @throws RuntimeException 상품 수정 처리에 실패한 경우
     */
    fun updateProduct(productId: Long, request: UpdateProductRequest): Product {
        val product = productService.getProduct(productId)

        // 상품 정보 업데이트
        product.updateInfo(
            name = request.name,
            description = request.description,
            price = request.price
        )

        return productService.updateProduct(product)
    }
}