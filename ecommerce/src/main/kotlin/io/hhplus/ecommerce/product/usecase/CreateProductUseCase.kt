package io.hhplus.ecommerce.product.usecase

import io.hhplus.ecommerce.product.application.ProductService
import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.product.dto.CreateProductRequest
import org.springframework.stereotype.Component

/**
 * 상품 생성 유스케이스 - 애플리케이션 계층
 *
 * 역할:
 * - 새로운 상품 등록 비즈니스 플로우 수행
 * - 상품 정보 검증 및 생성 처리
 * - CQRS Command 패턴 구현
 *
 * 책임:
 * - 상품 정보 유효성 검증
 * - 상품 생성 트랜잭션 관리
 * - 상품 생성 후 초기 설정 냄 통계 처리
 */
@Component
class CreateProductUseCase(
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
    fun execute(request: CreateProductRequest): Product {
        return productService.createProduct(
            name = request.name,
            description = request.description,
            price = request.price,
            categoryId = request.categoryId,
            createdBy = request.createdBy
        )
    }
}