package io.hhplus.ecommerce.product.usecase

import io.hhplus.ecommerce.product.application.ProductService
import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.product.dto.UpdateProductRequest
import org.springframework.stereotype.Component

/**
 * 상품 수정 유스케이스 - 애플리케이션 계층
 *
 * 역할:
 * - 기존 상품 정보 수정 비즈니스 플로우 수행
 * - 상품 정보 검증 및 업데이트 처리
 * - CQRS Command 패턴 구현
 *
 * 책임:
 * - 상품 수정 권한 및 정보 유효성 검증
 * - 상품 정보 업데이트 트랜잭션 관리
 * - 수정 후 관련 데이터 일관성 보장
 */
@Component
class UpdateProductUseCase(
    private val productService: ProductService
) {

    /**
     * 지정된 상품의 정보를 수정하고 업데이트한다
     *
     * @param productId 수정할 상품 ID
     * @param request 상품 수정 요청 데이터
     * @return 수정이 완료된 상품 정보
     * @throws IllegalArgumentException 상품을 찾을 수 없거나 수정 정보가 잘못된 경우
     * @throws RuntimeException 상품 수정 처리에 실패한 경우
     */
    fun execute(productId: Long, request: UpdateProductRequest): Product {
        val product = productService.getProduct(productId)

        // 상품 정보 업데이트
        product.updateInfo(
            name = request.name,
            description = request.description,
            price = request.price,
            updatedBy = request.updatedBy
        )

        return productService.updateProduct(product)
    }
}