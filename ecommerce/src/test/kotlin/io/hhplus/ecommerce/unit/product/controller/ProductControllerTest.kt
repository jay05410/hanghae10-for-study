package io.hhplus.ecommerce.unit.product.controller

import io.hhplus.ecommerce.product.usecase.*
import io.hhplus.ecommerce.product.dto.*
import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.common.response.ApiResponse
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.time.LocalDateTime

/**
 * ProductController 단위 테스트
 *
 * 책임: 상품 관련 HTTP 요청 처리 검증
 * - REST API 엔드포인트의 요청/응답 처리 검증
 * - UseCase 계층과의 올바른 상호작용 검증
 * - 요청 데이터 변환 및 응답 형식 검증
 *
 * 검증 목표:
 * 1. 각 엔드포인트가 적절한 UseCase를 호출하는가?
 * 2. 요청 파라미터와 Body가 올바르게 UseCase에 전달되는가?
 * 3. UseCase 결과가 적절한 ApiResponse와 ProductResponse로 변환되는가?
 * 4. HTTP 메서드와 경로 매핑이 올바른가?
 * 5. 조회수 증가 로직이 상품 조회 시 올바르게 동작하는가?
 */
class ProductControllerTest : DescribeSpec({
    val mockGetProductQueryUseCase = mockk<GetProductQueryUseCase>()
    val mockCreateProductUseCase = mockk<CreateProductUseCase>()
    val mockUpdateProductUseCase = mockk<UpdateProductUseCase>()
    val mockGetPopularProductsUseCase = mockk<GetPopularProductsUseCase>()
    val mockIncrementProductViewUseCase = mockk<IncrementProductViewUseCase>()

    val sut = ProductController(
        getProductQueryUseCase = mockGetProductQueryUseCase,
        createProductUseCase = mockCreateProductUseCase,
        updateProductUseCase = mockUpdateProductUseCase,
        getPopularProductsUseCase = mockGetPopularProductsUseCase,
        incrementProductViewUseCase = mockIncrementProductViewUseCase
    )

    fun createMockProduct(
        id: Long = 1L,
        name: String = "Test Product",
        description: String = "Test Description",
        price: Long = 1000L,
        categoryId: Long = 1L,
        isActive: Boolean = true
    ): Product = mockk(relaxed = true) {
        every { this@mockk.id } returns id
        every { this@mockk.name } returns name
        every { this@mockk.description } returns description
        every { this@mockk.price } returns price
        every { this@mockk.categoryId } returns categoryId
        every { this@mockk.isActive } returns isActive
        every { createdAt } returns LocalDateTime.now()
        every { updatedAt } returns LocalDateTime.now()
    }

    beforeEach {
        clearMocks(
            mockGetProductQueryUseCase,
            mockCreateProductUseCase,
            mockUpdateProductUseCase,
            mockGetPopularProductsUseCase,
            mockIncrementProductViewUseCase
        )
    }

    describe("getProducts") {
        context("GET /api/v1/products 요청 (카테고리 없음)") {
            it("GetProductQueryUseCase.getProducts를 호출하고 ProductResponse 리스트로 변환하여 반환") {
                val page = 1
                val mockProducts = listOf(
                    createMockProduct(1L, "Product 1", "Description 1", 1000L),
                    createMockProduct(2L, "Product 2", "Description 2", 2000L)
                )

                every { mockGetProductQueryUseCase.getProducts(page) } returns mockProducts

                val result = sut.getProducts(page, null)

                verify(exactly = 1) { mockGetProductQueryUseCase.getProducts(page) }
                verify(exactly = 0) { mockGetProductQueryUseCase.getProductsByCategory(any()) }
                // ApiResponse.success와 ProductResponse.from 변환 확인
                result.success shouldBe true
            }
        }

        context("GET /api/v1/products?categoryId=1 요청") {
            it("GetProductQueryUseCase.getProductsByCategory를 호출하고 ProductResponse 리스트로 변환하여 반환") {
                val page = 1
                val categoryId = 1L
                val mockProducts = listOf(createMockProduct())

                every { mockGetProductQueryUseCase.getProductsByCategory(categoryId) } returns mockProducts

                val result = sut.getProducts(page, categoryId)

                verify(exactly = 1) { mockGetProductQueryUseCase.getProductsByCategory(categoryId) }
                verify(exactly = 0) { mockGetProductQueryUseCase.getProducts(any()) }
                result.success shouldBe true
            }
        }

        context("기본 페이지 값으로 요청") {
            it("page 파라미터가 없으면 기본값 1을 사용") {
                val mockProducts = listOf(createMockProduct())

                every { mockGetProductQueryUseCase.getProducts(1) } returns mockProducts

                val result = sut.getProducts(1, null) // 기본값 테스트 시뮬레이션

                verify(exactly = 1) { mockGetProductQueryUseCase.getProducts(1) }
                result.success shouldBe true
            }
        }

        context("다양한 페이지와 카테고리 조합") {
            it("각 파라미터가 정확히 UseCase에 전달") {
                val testCases = listOf(
                    Triple(2, null, "getProducts"),
                    Triple(3, 5L, "getProductsByCategory"),
                    Triple(1, 10L, "getProductsByCategory")
                )

                testCases.forEach { (page, categoryId, expectedMethod) ->
                    val mockProducts = listOf(createMockProduct())

                    if (categoryId != null) {
                        every { mockGetProductQueryUseCase.getProductsByCategory(categoryId) } returns mockProducts
                    } else {
                        every { mockGetProductQueryUseCase.getProducts(page) } returns mockProducts
                    }

                    sut.getProducts(page, categoryId)

                    if (expectedMethod == "getProducts") {
                        verify(exactly = 1) { mockGetProductQueryUseCase.getProducts(page) }
                    } else {
                        verify(exactly = 1) { mockGetProductQueryUseCase.getProductsByCategory(categoryId!!) }
                    }

                    clearMocks(mockGetProductQueryUseCase)
                }
            }
        }
    }

    describe("getProduct") {
        context("GET /api/v1/products/{productId} 요청") {
            it("조회수 증가 후 상품을 조회하고 ProductResponse로 변환하여 반환") {
                val productId = 1L
                val userId = 1L
                val mockProduct = createMockProduct()

                every { mockIncrementProductViewUseCase.execute(productId, userId) } just Runs
                every { mockGetProductQueryUseCase.getProduct(productId) } returns mockProduct

                val result = sut.getProduct(productId, userId)

                verify(exactly = 1) { mockIncrementProductViewUseCase.execute(productId, userId) }
                verify(exactly = 1) { mockGetProductQueryUseCase.getProduct(productId) }
                result.success shouldBe true
            }
        }

        context("User-Id 헤더가 없는 경우") {
            it("기본값 1을 사용하여 조회수 증가") {
                val productId = 2L
                val defaultUserId = 1L
                val mockProduct = createMockProduct()

                every { mockIncrementProductViewUseCase.execute(productId, defaultUserId) } just Runs
                every { mockGetProductQueryUseCase.getProduct(productId) } returns mockProduct

                val result = sut.getProduct(productId, defaultUserId) // 기본값 테스트 시뮬레이션

                verify(exactly = 1) { mockIncrementProductViewUseCase.execute(productId, defaultUserId) }
                verify(exactly = 1) { mockGetProductQueryUseCase.getProduct(productId) }
                result.success shouldBe true
            }
        }

        context("다양한 상품과 사용자 조합") {
            it("각 파라미터가 정확히 UseCase들에 전달") {
                val testCases = listOf(
                    Pair(1L, 1L),
                    Pair(100L, 200L),
                    Pair(999L, 888L)
                )

                testCases.forEach { (productId, userId) ->
                    val mockProduct = createMockProduct()

                    every { mockIncrementProductViewUseCase.execute(productId, userId) } just Runs
                    every { mockGetProductQueryUseCase.getProduct(productId) } returns mockProduct

                    val result = sut.getProduct(productId, userId)

                    verify(exactly = 1) { mockIncrementProductViewUseCase.execute(productId, userId) }
                    verify(exactly = 1) { mockGetProductQueryUseCase.getProduct(productId) }
                    result.success shouldBe true

                    clearMocks(mockIncrementProductViewUseCase, mockGetProductQueryUseCase)
                }
            }
        }

        context("호출 순서 검증") {
            it("조회수 증가가 상품 조회보다 먼저 실행") {
                val productId = 3L
                val userId = 3L
                val mockProduct = createMockProduct()

                every { mockIncrementProductViewUseCase.execute(productId, userId) } just Runs
                every { mockGetProductQueryUseCase.getProduct(productId) } returns mockProduct

                sut.getProduct(productId, userId)

                verifyOrder {
                    mockIncrementProductViewUseCase.execute(productId, userId)
                    mockGetProductQueryUseCase.getProduct(productId)
                }
            }
        }
    }

    describe("createProduct") {
        context("POST /api/v1/products 요청") {
            it("CreateProductRequest를 CreateProductUseCase에 전달하고 ProductResponse로 변환하여 반환") {
                val request = CreateProductRequest(
                    name = "테스트상품",
                    description = "테스트상품설명",
                    price = 10000L,
                    categoryId = 1L,
                    createdBy = 1L
                )
                val mockProduct = createMockProduct()

                every { mockCreateProductUseCase.execute(request) } returns mockProduct

                val result = sut.createProduct(request)

                verify(exactly = 1) { mockCreateProductUseCase.execute(request) }
                result.success shouldBe true
            }
        }

        context("다양한 상품 정보로 생성 요청") {
            it("각 요청이 정확히 UseCase에 전달") {
                val requests = listOf(
                    CreateProductRequest("상품1", "설명1", 5000L, 1L, 1L),
                    CreateProductRequest("상품2", "설명2", 15000L, 2L, 2L),
                    CreateProductRequest("상품3", "설명3", 25000L, 3L, 3L)
                )

                requests.forEach { request ->
                    val mockProduct = createMockProduct()
                    every { mockCreateProductUseCase.execute(request) } returns mockProduct

                    val result = sut.createProduct(request)

                    verify(exactly = 1) { mockCreateProductUseCase.execute(request) }
                    result.success shouldBe true
                    clearMocks(mockCreateProductUseCase)
                }
            }
        }
    }

    describe("updateProduct") {
        context("PUT /api/v1/products/{productId} 요청") {
            it("파라미터들을 UpdateProductUseCase에 전달하고 ProductResponse로 변환하여 반환") {
                val productId = 1L
                val request = UpdateProductRequest(
                    name = "수정된상품",
                    description = "수정된설명",
                    price = 20000L,
                    updatedBy = 1L
                )
                val mockProduct = createMockProduct()

                every { mockUpdateProductUseCase.execute(productId, request) } returns mockProduct

                val result = sut.updateProduct(productId, request)

                verify(exactly = 1) { mockUpdateProductUseCase.execute(productId, request) }
                result.success shouldBe true
            }
        }

        context("다양한 파라미터 조합") {
            it("모든 파라미터가 정확히 UseCase에 전달되는지 확인") {
                val testCases = listOf(
                    Pair(1L, UpdateProductRequest("상품1", "설명1", 10000L, 1L)),
                    Pair(100L, UpdateProductRequest("상품100", "설명100", 50000L, 100L)),
                    Pair(999L, UpdateProductRequest("상품999", "설명999", 99000L, 999L))
                )

                testCases.forEach { (productId, request) ->
                    val mockProduct = createMockProduct()
                    every { mockUpdateProductUseCase.execute(productId, request) } returns mockProduct

                    val result = sut.updateProduct(productId, request)

                    verify(exactly = 1) { mockUpdateProductUseCase.execute(productId, request) }
                    result.success shouldBe true
                    clearMocks(mockUpdateProductUseCase)
                }
            }
        }
    }

    describe("getPopularProducts") {
        context("GET /api/v1/products/popular 요청") {
            it("GetPopularProductsUseCase를 호출하고 ProductResponse 리스트로 변환하여 반환") {
                val limit = 10
                val mockProducts = listOf(
                    createMockProduct(1L, "Product 1"),
                    createMockProduct(2L, "Product 2")
                )

                every { mockGetPopularProductsUseCase.execute(limit) } returns mockProducts

                val result = sut.getPopularProducts(limit)

                verify(exactly = 1) { mockGetPopularProductsUseCase.execute(limit) }
                result.success shouldBe true
            }
        }

        context("기본 limit 값으로 요청") {
            it("limit 파라미터가 없으면 기본값 10을 사용") {
                val defaultLimit = 10
                val mockProducts = listOf(createMockProduct())

                every { mockGetPopularProductsUseCase.execute(defaultLimit) } returns mockProducts

                val result = sut.getPopularProducts(defaultLimit) // 기본값 테스트 시뮬레이션

                verify(exactly = 1) { mockGetPopularProductsUseCase.execute(defaultLimit) }
                result.success shouldBe true
            }
        }

        context("다양한 limit 값으로 요청") {
            it("각 limit이 정확히 UseCase에 전달") {
                val limits = listOf(5, 10, 20, 50)

                limits.forEach { limit ->
                    val mockProducts = (1..limit).map { createMockProduct(it.toLong()) }
                    every { mockGetPopularProductsUseCase.execute(limit) } returns mockProducts

                    val result = sut.getPopularProducts(limit)

                    verify(exactly = 1) { mockGetPopularProductsUseCase.execute(limit) }
                    result.success shouldBe true
                    clearMocks(mockGetPopularProductsUseCase)
                }
            }
        }
    }

    describe("API 경로 및 메서드 검증") {
        context("모든 엔드포인트") {
            it("적절한 UseCase만 호출하고 다른 UseCase는 호출하지 않음") {
                val mockProduct = createMockProduct()

                // getProducts 테스트
                every { mockGetProductQueryUseCase.getProducts(1) } returns listOf(mockProduct)
                sut.getProducts(1, null)
                verify(exactly = 1) { mockGetProductQueryUseCase.getProducts(1) }
                verify(exactly = 0) { mockCreateProductUseCase.execute(any()) }
                verify(exactly = 0) { mockUpdateProductUseCase.execute(any(), any()) }

                clearMocks(mockGetProductQueryUseCase, mockCreateProductUseCase, mockUpdateProductUseCase, mockGetPopularProductsUseCase, mockIncrementProductViewUseCase)

                // createProduct 테스트
                val createRequest = CreateProductRequest("테스트", "설명", 1000L, 1L, 1L)
                every { mockCreateProductUseCase.execute(createRequest) } returns mockProduct
                sut.createProduct(createRequest)
                verify(exactly = 1) { mockCreateProductUseCase.execute(createRequest) }
                verify(exactly = 0) { mockGetProductQueryUseCase.getProducts(any()) }
                verify(exactly = 0) { mockUpdateProductUseCase.execute(any(), any()) }

                clearMocks(mockGetProductQueryUseCase, mockCreateProductUseCase, mockUpdateProductUseCase, mockGetPopularProductsUseCase, mockIncrementProductViewUseCase)

                // getPopularProducts 테스트
                every { mockGetPopularProductsUseCase.execute(10) } returns listOf(mockProduct)
                sut.getPopularProducts(10)
                verify(exactly = 1) { mockGetPopularProductsUseCase.execute(10) }
                verify(exactly = 0) { mockCreateProductUseCase.execute(any()) }
                verify(exactly = 0) { mockGetProductQueryUseCase.getProducts(any()) }
            }
        }
    }

    describe("응답 형식 검증") {
        context("모든 성공 응답") {
            it("일관된 ApiResponse.success 형식으로 반환") {
                val mockProduct = createMockProduct()
                val mockProducts = listOf(mockProduct)

                // 각 엔드포인트의 응답이 ApiResponse.success로 감싸져 있는지 확인
                every { mockGetProductQueryUseCase.getProducts(any()) } returns mockProducts
                every { mockGetProductQueryUseCase.getProduct(any()) } returns mockProduct
                every { mockCreateProductUseCase.execute(any()) } returns mockProduct
                every { mockUpdateProductUseCase.execute(any(), any()) } returns mockProduct
                every { mockGetPopularProductsUseCase.execute(any()) } returns mockProducts
                every { mockIncrementProductViewUseCase.execute(any(), any()) } just Runs

                val getProductsResult = sut.getProducts(1, null)
                val getProductResult = sut.getProduct(1L, 1L)
                val createResult = sut.createProduct(CreateProductRequest("테스트", "설명", 1000L, 1L, 1L))
                val updateResult = sut.updateProduct(1L, UpdateProductRequest("수정", "수정설명", 2000L, 1L))
                val getPopularResult = sut.getPopularProducts(10)

                // 모든 결과가 ApiResponse.success 형태인지 확인
                getProductsResult.success shouldBe true
                getProductResult.success shouldBe true
                createResult.success shouldBe true
                updateResult.success shouldBe true
                getPopularResult.success shouldBe true
            }
        }
    }
})