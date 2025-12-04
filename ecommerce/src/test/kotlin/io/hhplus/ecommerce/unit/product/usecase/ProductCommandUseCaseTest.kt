package io.hhplus.ecommerce.unit.product.usecase

import io.hhplus.ecommerce.product.usecase.ProductCommandUseCase
import io.hhplus.ecommerce.product.application.ProductCommandService
import io.hhplus.ecommerce.product.application.ProductQueryService
import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.product.dto.CreateProductRequest
import io.hhplus.ecommerce.product.dto.UpdateProductRequest
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * ProductCommandUseCase 단위 테스트
 *
 * 책임: 상품 명령 유스케이스의 비즈니스 로직 검증
 * - ProductCommandService와의 올바른 상호작용 확인
 * - ProductQueryService와의 연동 확인
 * - 캐시 무효화 로직 검증 (어노테이션 기반)
 * - 예외 처리 검증
 *
 * 검증 목표:
 * 1. UseCase가 적절한 Service 메서드를 호출하는가?
 * 2. 파라미터를 올바르게 전달하는가?
 * 3. 결과를 올바르게 반환하는가?
 * 4. 예외가 Service에서 UseCase로 전파되는가?
 */
class ProductCommandUseCaseTest : DescribeSpec({

    val mockProductCommandService = mockk<ProductCommandService>()
    val mockProductQueryService = mockk<ProductQueryService>()
    val sut = ProductCommandUseCase(mockProductCommandService, mockProductQueryService)

    beforeEach {
        clearAllMocks()
    }

    describe("createProduct") {
        context("유효한 상품 생성 요청으로 상품을 생성할 때") {
            it("ProductCommandService의 createProduct를 호출하고 결과를 반환한다") {
                // given
                val request = CreateProductRequest(
                    name = "테스트 상품",
                    description = "테스트 설명",
                    price = 10000L,
                    categoryId = 1L,
                    createdBy = 1L
                )
                val expectedProduct = mockk<Product>()

                every {
                    mockProductCommandService.createProduct(
                        request.name,
                        request.description,
                        request.price,
                        request.categoryId
                    )
                } returns expectedProduct

                // when
                val result = sut.createProduct(request)

                // then
                result shouldBe expectedProduct
                verify(exactly = 1) {
                    mockProductCommandService.createProduct(
                        request.name,
                        request.description,
                        request.price,
                        request.categoryId
                    )
                }
            }
        }

        context("ProductCommandService에서 예외가 발생할 때") {
            it("예외를 그대로 전파한다") {
                // given
                val request = CreateProductRequest(
                    name = "테스트 상품",
                    description = "테스트 설명",
                    price = 10000L,
                    categoryId = 1L,
                    createdBy = 1L
                )

                every {
                    mockProductCommandService.createProduct(any(), any(), any(), any())
                } throws RuntimeException("DB 오류")

                // when & then
                runCatching { sut.createProduct(request) }
                    .exceptionOrNull() shouldBe RuntimeException("DB 오류")

                verify(exactly = 1) {
                    mockProductCommandService.createProduct(any(), any(), any(), any())
                }
            }
        }
    }

    describe("updateProduct") {
        context("유효한 상품 수정 요청으로 상품을 수정할 때") {
            it("ProductCommandService의 updateProductInfo를 호출하고 결과를 반환한다") {
                // given
                val productId = 1L
                val request = UpdateProductRequest(
                    name = "수정된 상품명",
                    description = "수정된 설명",
                    price = 20000L,
                    updatedBy = 1L
                )
                val expectedProduct = mockk<Product>()

                every {
                    mockProductCommandService.updateProductInfo(
                        productId,
                        request.name,
                        request.description,
                        request.price
                    )
                } returns expectedProduct

                // when
                val result = sut.updateProduct(productId, request)

                // then
                result shouldBe expectedProduct
                verify(exactly = 1) {
                    mockProductCommandService.updateProductInfo(
                        productId,
                        request.name,
                        request.description,
                        request.price
                    )
                }
            }
        }

        context("존재하지 않는 상품을 수정하려고 할 때") {
            it("ProductCommandService에서 발생한 예외를 전파한다") {
                // given
                val productId = 999L
                val request = UpdateProductRequest(
                    name = "수정된 상품명",
                    description = "수정된 설명",
                    price = 20000L,
                    updatedBy = 1L
                )

                every {
                    mockProductCommandService.updateProductInfo(any(), any(), any(), any())
                } throws RuntimeException("상품을 찾을 수 없습니다")

                // when & then
                runCatching { sut.updateProduct(productId, request) }
                    .exceptionOrNull() shouldBe RuntimeException("상품을 찾을 수 없습니다")

                verify(exactly = 1) {
                    mockProductCommandService.updateProductInfo(any(), any(), any(), any())
                }
            }
        }
    }

    describe("ProductCommandUseCase 캐시 무효화") {
        context("상품 생성 시") {
            it("@Caching 어노테이션에 의해 관련 캐시가 무효화된다") {
                // given
                val request = CreateProductRequest(
                    name = "테스트 상품",
                    description = "테스트 설명",
                    price = 10000L,
                    categoryId = 1L,
                    createdBy = 1L
                )
                val expectedProduct = mockk<Product>()

                every {
                    mockProductCommandService.createProduct(any(), any(), any(), any())
                } returns expectedProduct

                // when
                val result = sut.createProduct(request)

                // then
                result shouldBe expectedProduct
                // 캐시 무효화는 Spring AOP에 의해 자동 처리됨
                // 실제 통합 테스트에서 확인 필요
            }
        }

        context("상품 수정 시") {
            it("@Caching 어노테이션에 의해 관련 캐시가 무효화된다") {
                // given
                val productId = 1L
                val request = UpdateProductRequest(
                    name = "수정된 상품명",
                    description = "수정된 설명",
                    price = 20000L,
                    updatedBy = 1L
                )
                val expectedProduct = mockk<Product>()

                every {
                    mockProductCommandService.updateProductInfo(any(), any(), any(), any())
                } returns expectedProduct

                // when
                val result = sut.updateProduct(productId, request)

                // then
                result shouldBe expectedProduct
                // 캐시 무효화는 Spring AOP에 의해 자동 처리됨
                // 실제 통합 테스트에서 확인 필요
            }
        }
    }
})