package io.hhplus.ecommerce.product.usecase

import io.hhplus.ecommerce.product.application.ProductService
import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.product.dto.UpdateProductRequest
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * UpdateProductUseCase 단위 테스트
 *
 * 책임: 상품 수정 요청 처리 및 ProductService 호출 검증
 * - 상품 조회 후 정보 업데이트 로직 검증
 * - UpdateProductRequest를 Product 도메인 메서드 파라미터로 변환
 * - ProductService와의 올바른 상호작용 검증
 *
 * 검증 목표:
 * 1. productId로 기존 상품을 조회하는가?
 * 2. UpdateProductRequest가 올바른 Product.updateInfo 파라미터로 변환되는가?
 * 3. 업데이트된 상품이 ProductService를 통해 저장되는가?
 * 4. ProductService의 getProduct와 updateProduct가 순서대로 호출되는가?
 * 5. 최종적으로 업데이트된 상품이 반환되는가?
 */
class UpdateProductUseCaseTest : DescribeSpec({
    val mockProductService = mockk<ProductService>()
    val sut = UpdateProductUseCase(mockProductService)

    beforeEach {
        clearMocks(mockProductService)
    }

    describe("execute") {
        context("존재하는 상품 수정 요청") {
            it("상품을 조회하고 정보를 업데이트한 후 저장") {
                val productId = 1L
                val request = UpdateProductRequest(
                    name = "수정된상품명",
                    description = "수정된설명",
                    price = 20000L,
                    updatedBy = 1L
                )
                val mockProduct = mockk<Product> {
                    every { updateInfo(any(), any(), any(), any()) } just Runs
                }
                val updatedMockProduct = mockk<Product>()

                every { mockProductService.getProduct(productId) } returns mockProduct
                every { mockProductService.updateProduct(mockProduct) } returns updatedMockProduct

                val result = sut.execute(productId, request)

                result shouldBe updatedMockProduct
                verify(exactly = 1) { mockProductService.getProduct(productId) }
                verify(exactly = 1) {
                    mockProduct.updateInfo(
                        name = "수정된상품명",
                        description = "수정된설명",
                        price = 20000L,
                        updatedBy = 1L
                    )
                }
                verify(exactly = 1) { mockProductService.updateProduct(mockProduct) }
            }
        }

        context("다양한 상품 정보로 수정 요청") {
            it("각 요청의 모든 필드가 정확히 Product.updateInfo에 전달") {
                val testCases = listOf(
                    Pair(1L, UpdateProductRequest("상품1", "설명1", 5000L, 1L)),
                    Pair(100L, UpdateProductRequest("상품100", "설명100", 50000L, 100L)),
                    Pair(999L, UpdateProductRequest("상품999", "설명999", 99000L, 999L))
                )

                testCases.forEach { (productId, request) ->
                    val mockProduct = mockk<Product> {
                        every { updateInfo(any(), any(), any(), any()) } just Runs
                    }
                    val updatedMockProduct = mockk<Product>()

                    every { mockProductService.getProduct(productId) } returns mockProduct
                    every { mockProductService.updateProduct(mockProduct) } returns updatedMockProduct

                    val result = sut.execute(productId, request)

                    result shouldBe updatedMockProduct
                    verify(exactly = 1) { mockProductService.getProduct(productId) }
                    verify(exactly = 1) {
                        mockProduct.updateInfo(
                            name = request.name,
                            description = request.description,
                            price = request.price,
                            updatedBy = request.updatedBy
                        )
                    }
                    verify(exactly = 1) { mockProductService.updateProduct(mockProduct) }
                    clearMocks(mockProductService, mockProduct)
                }
            }
        }

        context("경계값으로 상품 수정") {
            it("최소값과 최대값으로 상품을 수정") {
                val testCases = listOf(
                    Pair(1L, UpdateProductRequest("", "", 0L, 1L)),
                    Pair(Long.MAX_VALUE, UpdateProductRequest("매우긴상품명".repeat(10), "매우긴설명".repeat(50), Long.MAX_VALUE, Long.MAX_VALUE))
                )

                testCases.forEach { (productId, request) ->
                    val mockProduct = mockk<Product> {
                        every { updateInfo(any(), any(), any(), any()) } just Runs
                    }
                    val updatedMockProduct = mockk<Product>()

                    every { mockProductService.getProduct(productId) } returns mockProduct
                    every { mockProductService.updateProduct(mockProduct) } returns updatedMockProduct

                    val result = sut.execute(productId, request)

                    result shouldBe updatedMockProduct
                    verify(exactly = 1) { mockProductService.getProduct(productId) }
                    verify(exactly = 1) {
                        mockProduct.updateInfo(
                            name = request.name,
                            description = request.description,
                            price = request.price,
                            updatedBy = request.updatedBy
                        )
                    }
                    verify(exactly = 1) { mockProductService.updateProduct(mockProduct) }
                    clearMocks(mockProductService, mockProduct)
                }
            }
        }

        context("특수 문자가 포함된 상품 정보 수정") {
            it("특수 문자가 포함된 이름과 설명이 정확히 전달") {
                val productId = 5L
                val request = UpdateProductRequest(
                    name = "수정된상품!@#$%^&*()",
                    description = "수정된설명<>?:{}[]|\\+=",
                    price = 54321L,
                    updatedBy = 555L
                )
                val mockProduct = mockk<Product> {
                    every { updateInfo(any(), any(), any(), any()) } just Runs
                }
                val updatedMockProduct = mockk<Product>()

                every { mockProductService.getProduct(productId) } returns mockProduct
                every { mockProductService.updateProduct(mockProduct) } returns updatedMockProduct

                val result = sut.execute(productId, request)

                result shouldBe updatedMockProduct
                verify(exactly = 1) {
                    mockProduct.updateInfo(
                        name = "수정된상품!@#$%^&*()",
                        description = "수정된설명<>?:{}[]|\\+=",
                        price = 54321L,
                        updatedBy = 555L
                    )
                }
            }
        }
    }

    describe("실행 순서 검증") {
        context("ProductService 메서드 호출 순서") {
            it("getProduct -> updateInfo -> updateProduct 순서로 실행") {
                val productId = 1L
                val request = UpdateProductRequest("수정", "수정설명", 1000L, 1L)
                val mockProduct = mockk<Product> {
                    every { updateInfo(any(), any(), any(), any()) } just Runs
                }
                val updatedMockProduct = mockk<Product>()

                every { mockProductService.getProduct(productId) } returns mockProduct
                every { mockProductService.updateProduct(mockProduct) } returns updatedMockProduct

                sut.execute(productId, request)

                verifyOrder {
                    mockProductService.getProduct(productId)    // 1. 상품 조회
                    mockProduct.updateInfo(any(), any(), any(), any())  // 2. 상품 정보 업데이트
                    mockProductService.updateProduct(mockProduct)       // 3. 상품 저장
                }
            }
        }

        context("Product 도메인 메서드 호출 시점") {
            it("ProductService.getProduct 호출 후 Product.updateInfo가 호출") {
                val productId = 2L
                val request = UpdateProductRequest("테스트", "테스트설명", 2000L, 2L)
                val mockProduct = mockk<Product> {
                    every { updateInfo(any(), any(), any(), any()) } just Runs
                }
                val updatedMockProduct = mockk<Product>()

                every { mockProductService.getProduct(productId) } returns mockProduct
                every { mockProductService.updateProduct(mockProduct) } returns updatedMockProduct

                sut.execute(productId, request)

                // getProduct 호출 후 updateInfo가 호출되는지 확인
                verifyOrder {
                    mockProductService.getProduct(productId)
                    mockProduct.updateInfo("테스트", "테스트설명", 2000L, 2L)
                }
            }
        }
    }

    describe("파라미터 변환 검증") {
        context("UpdateProductRequest 필드 매핑") {
            it("UpdateProductRequest의 모든 필드가 올바르게 Product.updateInfo 파라미터로 매핑") {
                val productId = 123L
                val request = UpdateProductRequest(
                    name = "완전한수정테스트",
                    description = "완전한수정테스트설명",
                    price = 123456L,
                    updatedBy = 123L
                )
                val mockProduct = mockk<Product> {
                    every { updateInfo(any(), any(), any(), any()) } just Runs
                }
                val updatedMockProduct = mockk<Product>()

                every { mockProductService.getProduct(productId) } returns mockProduct
                every { mockProductService.updateProduct(mockProduct) } returns updatedMockProduct

                sut.execute(productId, request)

                verify(exactly = 1) {
                    mockProduct.updateInfo(
                        name = "완전한수정테스트",           // request.name
                        description = "완전한수정테스트설명",  // request.description
                        price = 123456L,                  // request.price
                        updatedBy = 123L                  // request.updatedBy
                    )
                }
            }
        }
    }

    describe("비즈니스 로직 검증") {
        context("UseCase의 책임") {
            it("상품 조회 -> 업데이트 -> 저장의 흐름을 조율") {
                val productId = 999L
                val request = UpdateProductRequest("흐름테스트", "흐름테스트설명", 999L, 999L)
                val mockProduct = mockk<Product> {
                    every { updateInfo(any(), any(), any(), any()) } just Runs
                }
                val updatedMockProduct = mockk<Product>()

                every { mockProductService.getProduct(productId) } returns mockProduct
                every { mockProductService.updateProduct(mockProduct) } returns updatedMockProduct

                val result = sut.execute(productId, request)

                // 결과가 ProductService.updateProduct에서 온 것과 동일한지 확인
                result shouldBe updatedMockProduct

                // 모든 단계가 정확히 한 번씩 호출되었는지 확인
                verify(exactly = 1) { mockProductService.getProduct(productId) }
                verify(exactly = 1) { mockProduct.updateInfo(any(), any(), any(), any()) }
                verify(exactly = 1) { mockProductService.updateProduct(mockProduct) }
            }
        }

        context("도메인 객체 상태 변경") {
            it("조회한 Product 객체에 updateInfo를 호출하여 상태를 변경") {
                val productId = 777L
                val request = UpdateProductRequest("상태변경", "상태변경설명", 7777L, 777L)
                val mockProduct = mockk<Product> {
                    every { updateInfo(any(), any(), any(), any()) } just Runs
                }
                val updatedMockProduct = mockk<Product>()

                every { mockProductService.getProduct(productId) } returns mockProduct
                every { mockProductService.updateProduct(mockProduct) } returns updatedMockProduct

                sut.execute(productId, request)

                // 조회한 동일한 객체에 updateInfo가 호출되었는지 확인
                verify(exactly = 1) { mockProduct.updateInfo(any(), any(), any(), any()) }
                // 동일한 객체가 updateProduct에 전달되었는지 확인
                verify(exactly = 1) { mockProductService.updateProduct(mockProduct) }
            }
        }
    }

    describe("호출 패턴 검증") {
        context("ProductService 메서드 사용") {
            it("getProduct와 updateProduct만 호출하고 다른 메서드는 호출하지 않음") {
                val productId = 888L
                val request = UpdateProductRequest("패턴테스트", "패턴테스트설명", 8888L, 888L)
                val mockProduct = mockk<Product> {
                    every { updateInfo(any(), any(), any(), any()) } just Runs
                }
                val updatedMockProduct = mockk<Product>()

                every { mockProductService.getProduct(productId) } returns mockProduct
                every { mockProductService.updateProduct(mockProduct) } returns updatedMockProduct
                every { mockProductService.getProducts(any()) } returns listOf(mockProduct)
                every { mockProductService.createProduct(any(), any(), any(), any(), any()) } returns mockProduct

                sut.execute(productId, request)

                verify(exactly = 1) { mockProductService.getProduct(productId) }
                verify(exactly = 1) { mockProductService.updateProduct(mockProduct) }
                verify(exactly = 0) { mockProductService.getProducts(any()) }
                verify(exactly = 0) { mockProductService.createProduct(any(), any(), any(), any(), any()) }
                verify(exactly = 0) { mockProductService.getProductsByCategory(any()) }
            }
        }
    }
})