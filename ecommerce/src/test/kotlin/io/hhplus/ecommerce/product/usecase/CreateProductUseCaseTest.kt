package io.hhplus.ecommerce.product.usecase

import io.hhplus.ecommerce.product.application.ProductService
import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.product.dto.CreateProductRequest
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * CreateProductUseCase 단위 테스트
 *
 * 책임: 상품 생성 요청 처리 및 ProductService 호출 검증
 * - CreateProductRequest를 ProductService 파라미터로 변환
 * - 모든 요청 필드가 ProductService로 올바르게 전달되는지 검증
 * - ProductService와의 올바른 상호작용 검증
 *
 * 검증 목표:
 * 1. CreateProductRequest가 올바른 ProductService 파라미터로 변환되는가?
 * 2. 모든 필드(name, description, price, categoryId, createdBy)가 정확히 전달되는가?
 * 3. ProductService의 createProduct 메서드가 정확한 파라미터로 호출되는가?
 * 4. ProductService의 결과가 그대로 반환되는가?
 * 5. 다양한 요청 데이터에 대한 정확한 처리가 이루어지는가?
 */
class CreateProductUseCaseTest : DescribeSpec({
    val mockProductService = mockk<ProductService>()
    val sut = CreateProductUseCase(mockProductService)

    beforeEach {
        clearMocks(mockProductService)
    }

    describe("execute") {
        context("올바른 CreateProductRequest가 주어졌을 때") {
            it("ProductService.createProduct를 적절한 파라미터로 호출하고 결과를 반환") {
                val request = CreateProductRequest(
                    name = "테스트상품",
                    description = "테스트상품설명",
                    price = 10000L,
                    categoryId = 1L,
                    createdBy = 1L
                )
                val mockProduct = mockk<Product>()

                every {
                    mockProductService.createProduct(
                        name = "테스트상품",
                        description = "테스트상품설명",
                        price = 10000L,
                        categoryId = 1L,
                        createdBy = 1L
                    )
                } returns mockProduct

                val result = sut.execute(request)

                result shouldBe mockProduct
                verify(exactly = 1) {
                    mockProductService.createProduct(
                        name = "테스트상품",
                        description = "테스트상품설명",
                        price = 10000L,
                        categoryId = 1L,
                        createdBy = 1L
                    )
                }
            }
        }

        context("다양한 상품 정보로 요청") {
            it("각 요청의 모든 필드가 정확히 ProductService에 전달") {
                val testCases = listOf(
                    CreateProductRequest("상품1", "설명1", 5000L, 1L, 1L),
                    CreateProductRequest("상품2", "설명2", 15000L, 2L, 2L),
                    CreateProductRequest("상품3", "설명3", 25000L, 3L, 3L)
                )

                testCases.forEach { request ->
                    val mockProduct = mockk<Product>()
                    every {
                        mockProductService.createProduct(
                            name = request.name,
                            description = request.description,
                            price = request.price,
                            categoryId = request.categoryId,
                            createdBy = request.createdBy
                        )
                    } returns mockProduct

                    val result = sut.execute(request)

                    result shouldBe mockProduct
                    verify(exactly = 1) {
                        mockProductService.createProduct(
                            name = request.name,
                            description = request.description,
                            price = request.price,
                            categoryId = request.categoryId,
                            createdBy = request.createdBy
                        )
                    }
                    clearMocks(mockProductService)
                }
            }
        }

        context("경계값 데이터로 상품 생성") {
            it("최소값과 최대값으로 상품을 생성") {
                val testCases = listOf(
                    CreateProductRequest("", "", 0L, 1L, 1L),
                    CreateProductRequest("매우긴상품명".repeat(10), "매우긴설명".repeat(50), Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE)
                )

                testCases.forEach { request ->
                    val mockProduct = mockk<Product>()
                    every {
                        mockProductService.createProduct(
                            name = request.name,
                            description = request.description,
                            price = request.price,
                            categoryId = request.categoryId,
                            createdBy = request.createdBy
                        )
                    } returns mockProduct

                    val result = sut.execute(request)

                    result shouldBe mockProduct
                    verify(exactly = 1) {
                        mockProductService.createProduct(
                            name = request.name,
                            description = request.description,
                            price = request.price,
                            categoryId = request.categoryId,
                            createdBy = request.createdBy
                        )
                    }
                    clearMocks(mockProductService)
                }
            }
        }

        context("특수 문자가 포함된 상품 정보") {
            it("특수 문자가 포함된 이름과 설명이 정확히 전달") {
                val request = CreateProductRequest(
                    name = "상품!@#$%^&*()",
                    description = "설명<>?:{}[]|\\+=",
                    price = 12345L,
                    categoryId = 99L,
                    createdBy = 100L
                )
                val mockProduct = mockk<Product>()

                every {
                    mockProductService.createProduct(
                        name = "상품!@#$%^&*()",
                        description = "설명<>?:{}[]|\\+=",
                        price = 12345L,
                        categoryId = 99L,
                        createdBy = 100L
                    )
                } returns mockProduct

                val result = sut.execute(request)

                result shouldBe mockProduct
                verify(exactly = 1) {
                    mockProductService.createProduct(
                        name = "상품!@#$%^&*()",
                        description = "설명<>?:{}[]|\\+=",
                        price = 12345L,
                        categoryId = 99L,
                        createdBy = 100L
                    )
                }
            }
        }
    }

    describe("파라미터 변환 검증") {
        context("요청 데이터 변환") {
            it("CreateProductRequest의 모든 필드가 올바르게 ProductService 파라미터로 매핑") {
                val request = CreateProductRequest(
                    name = "완전한테스트상품",
                    description = "완전한테스트상품설명입니다",
                    price = 99999L,
                    categoryId = 999L,
                    createdBy = 999L
                )
                val mockProduct = mockk<Product>()

                every { mockProductService.createProduct(any(), any(), any(), any(), any()) } returns mockProduct

                sut.execute(request)

                verify(exactly = 1) {
                    mockProductService.createProduct(
                        name = "완전한테스트상품",                    // request.name
                        description = "완전한테스트상품설명입니다",      // request.description
                        price = 99999L,                           // request.price
                        categoryId = 999L,                        // request.categoryId
                        createdBy = 999L                          // request.createdBy
                    )
                }
            }
        }

        context("필드별 개별 검증") {
            it("각 필드가 정확한 위치에 매핑되는지 확인") {
                val request = CreateProductRequest(
                    name = "특정이름",
                    description = "특정설명",
                    price = 12345L,
                    categoryId = 67L,
                    createdBy = 89L
                )
                val mockProduct = mockk<Product>()

                every {
                    mockProductService.createProduct(
                        name = "특정이름",       // 첫 번째 파라미터
                        description = "특정설명", // 두 번째 파라미터
                        price = 12345L,        // 세 번째 파라미터
                        categoryId = 67L,      // 네 번째 파라미터
                        createdBy = 89L        // 다섯 번째 파라미터
                    )
                } returns mockProduct

                sut.execute(request)

                verify(exactly = 1) {
                    mockProductService.createProduct(
                        name = "특정이름",
                        description = "특정설명",
                        price = 12345L,
                        categoryId = 67L,
                        createdBy = 89L
                    )
                }
            }
        }
    }

    describe("비즈니스 로직 검증") {
        context("UseCase의 책임") {
            it("요청 변환 외에 추가적인 비즈니스 로직이 없음을 확인") {
                val request = CreateProductRequest("테스트", "설명", 1000L, 1L, 1L)
                val mockProduct = mockk<Product>()

                every { mockProductService.createProduct(any(), any(), any(), any(), any()) } returns mockProduct

                val result = sut.execute(request)

                // 결과가 ProductService에서 온 것과 동일한지 확인
                result shouldBe mockProduct

                // ProductService가 정확히 한 번만 호출되었는지 확인
                verify(exactly = 1) { mockProductService.createProduct(any(), any(), any(), any(), any()) }
            }
        }

        context("단순 위임 패턴 확인") {
            it("ProductService 결과를 변환 없이 그대로 반환") {
                val request = CreateProductRequest("상품", "설명", 5000L, 2L, 2L)
                val mockProduct = mockk<Product>()

                every { mockProductService.createProduct(any(), any(), any(), any(), any()) } returns mockProduct

                val result = sut.execute(request)

                // 참조가 동일한지 확인 (새로운 객체를 만들지 않았는지)
                result shouldBe mockProduct
                verify(exactly = 1) { mockProductService.createProduct(any(), any(), any(), any(), any()) }
            }
        }
    }

    describe("호출 패턴 검증") {
        context("ProductService 메서드 호출") {
            it("createProduct만 호출하고 다른 메서드는 호출하지 않음") {
                val request = CreateProductRequest("테스트", "설명", 1000L, 1L, 1L)
                val mockProduct = mockk<Product>()

                every { mockProductService.createProduct(any(), any(), any(), any(), any()) } returns mockProduct
                every { mockProductService.getProduct(any()) } returns mockProduct
                every { mockProductService.getProducts(any()) } returns listOf(mockProduct)
                every { mockProductService.updateProduct(any()) } returns mockProduct

                sut.execute(request)

                verify(exactly = 1) { mockProductService.createProduct(any(), any(), any(), any(), any()) }
                verify(exactly = 0) { mockProductService.getProduct(any()) }
                verify(exactly = 0) { mockProductService.getProducts(any()) }
                verify(exactly = 0) { mockProductService.updateProduct(any()) }
                verify(exactly = 0) { mockProductService.getProductsByCategory(any()) }
            }
        }
    }
})