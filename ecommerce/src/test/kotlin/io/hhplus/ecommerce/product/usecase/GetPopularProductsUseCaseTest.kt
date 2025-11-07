package io.hhplus.ecommerce.product.usecase

import io.hhplus.ecommerce.product.application.ProductService
import io.hhplus.ecommerce.product.application.ProductStatisticsService
import io.hhplus.ecommerce.product.domain.constant.ProductStatus
import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.product.domain.entity.ProductStatistics
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.time.LocalDateTime

/**
 * GetPopularProductsUseCase 단위 테스트
 *
 * 책임: 인기 상품 조회 요청 처리 및 서비스 계층 조율 검증
 * - ProductStatisticsService에서 인기 상품 통계 조회
 * - 각 통계의 productId로 실제 Product 엔티티 조회
 * - 통계와 상품 정보의 올바른 매핑 검증
 */
class GetPopularProductsUseCaseTest : DescribeSpec({
    val mockProductStatisticsService = mockk<ProductStatisticsService>()
    val mockProductService = mockk<ProductService>()
    val sut = GetPopularProductsUseCase(mockProductStatisticsService, mockProductService)

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
        every { caffeineType } returns "MEDIUM"
        every { tasteProfile } returns "MILD"
        every { aromaProfile } returns "FRESH"
        every { colorProfile } returns "GOLDEN"
        every { bagPerWeight } returns 3
        every { pricePer100g } returns price.toInt()
        every { ingredients } returns "차 잎 100%"
        every { origin } returns "한국"
        every { status } returns ProductStatus.ACTIVE
        every { createdAt } returns LocalDateTime.now()
        every { updatedAt } returns LocalDateTime.now()
        every { createdBy } returns 1L
        every { updatedBy } returns 1L
        every { deletedAt } returns null
        every { isAvailable() } returns true
    }

    beforeEach {
        clearMocks(mockProductStatisticsService, mockProductService)
    }

    describe("execute") {
        context("기본 limit으로 인기 상품 조회") {
            it("ProductStatisticsService에서 통계를 조회하고 각 productId로 상품을 조회하여 반환") {
                val limit = 10
                val mockStatistics = listOf(
                    mockk<ProductStatistics> { every { productId } returns 1L },
                    mockk<ProductStatistics> { every { productId } returns 2L },
                    mockk<ProductStatistics> { every { productId } returns 3L }
                )
                val mockProducts = listOf(
                    createMockProduct(1L),
                    createMockProduct(2L),
                    createMockProduct(3L)
                )

                every { mockProductStatisticsService.getPopularProducts(limit) } returns mockStatistics
                every { mockProductService.getProduct(1L) } returns mockProducts[0]
                every { mockProductService.getProduct(2L) } returns mockProducts[1]
                every { mockProductService.getProduct(3L) } returns mockProducts[2]

                val result = sut.execute(limit)

                result shouldBe mockProducts
                verify(exactly = 1) { mockProductStatisticsService.getPopularProducts(limit) }
                verify(exactly = 1) { mockProductService.getProduct(1L) }
                verify(exactly = 1) { mockProductService.getProduct(2L) }
                verify(exactly = 1) { mockProductService.getProduct(3L) }
            }
        }

        context("파라미터 없이 호출 (기본값 사용)") {
            it("기본값 10으로 ProductStatisticsService를 호출") {
                val defaultLimit = 10
                val mockStatistics = listOf(
                    mockk<ProductStatistics> { every { productId } returns 1L }
                )
                val mockProduct = createMockProduct()

                every { mockProductStatisticsService.getPopularProducts(defaultLimit) } returns mockStatistics
                every { mockProductService.getProduct(1L) } returns mockProduct

                val result = sut.execute() // 파라미터 없이 호출

                result shouldBe listOf(mockProduct)
                verify(exactly = 1) { mockProductStatisticsService.getPopularProducts(defaultLimit) }
                verify(exactly = 1) { mockProductService.getProduct(1L) }
            }
        }

        context("단일 인기 상품") {
            it("하나의 통계에 대해 하나의 상품을 반환") {
                val limit = 1
                val mockStatistics = listOf(
                    mockk<ProductStatistics> { every { productId } returns 100L }
                )
                val mockProduct = createMockProduct(100L)

                every { mockProductStatisticsService.getPopularProducts(limit) } returns mockStatistics
                every { mockProductService.getProduct(100L) } returns mockProduct

                val result = sut.execute(limit)

                result shouldBe listOf(mockProduct)
                verify(exactly = 1) { mockProductStatisticsService.getPopularProducts(limit) }
                verify(exactly = 1) { mockProductService.getProduct(100L) }
            }
        }

        context("인기 상품이 없는 경우") {
            it("빈 리스트를 반환") {
                val limit = 10

                every { mockProductStatisticsService.getPopularProducts(limit) } returns emptyList()

                val result = sut.execute(limit)

                result shouldBe emptyList()
                verify(exactly = 1) { mockProductStatisticsService.getPopularProducts(limit) }
                verify(exactly = 0) { mockProductService.getProduct(any()) }
            }
        }

        context("다양한 limit 값으로 조회") {
            it("limit 5로 ProductStatisticsService에 전달") {
                val limit = 5
                val mockStatistics = listOf(
                    mockk<ProductStatistics> { every { productId } returns 1L },
                    mockk<ProductStatistics> { every { productId } returns 2L }
                )
                val mockProducts = listOf(createMockProduct(1L), createMockProduct(2L))

                every { mockProductStatisticsService.getPopularProducts(limit) } returns mockStatistics
                every { mockProductService.getProduct(1L) } returns mockProducts[0]
                every { mockProductService.getProduct(2L) } returns mockProducts[1]

                val result = sut.execute(limit)

                result shouldBe mockProducts
                verify(exactly = 1) { mockProductStatisticsService.getPopularProducts(limit) }
                verify(exactly = 1) { mockProductService.getProduct(1L) }
                verify(exactly = 1) { mockProductService.getProduct(2L) }
            }
        }
    }

    describe("매핑 로직 검증") {
        context("통계와 상품의 매핑") {
            it("통계의 productId 순서대로 상품이 매핑") {
                val limit = 3
                val productIds = listOf(10L, 20L, 30L)
                val mockStatistics = productIds.map { productId ->
                    mockk<ProductStatistics> { every { this@mockk.productId } returns productId }
                }
                val mockProducts = listOf(
                    createMockProduct(10L),
                    createMockProduct(20L),
                    createMockProduct(30L)
                )

                every { mockProductStatisticsService.getPopularProducts(limit) } returns mockStatistics
                every { mockProductService.getProduct(10L) } returns mockProducts[0]
                every { mockProductService.getProduct(20L) } returns mockProducts[1]
                every { mockProductService.getProduct(30L) } returns mockProducts[2]

                val result = sut.execute(limit)

                result shouldBe mockProducts
                // 순서 확인: 통계의 순서대로 상품이 조회되었는지 확인
                verifyOrder {
                    mockProductService.getProduct(10L)
                    mockProductService.getProduct(20L)
                    mockProductService.getProduct(30L)
                }
            }
        }

        context("중복 productId 처리") {
            it("동일한 productId에 대해 각각 상품을 조회") {
                val limit = 3
                val mockStatistics = listOf(
                    mockk<ProductStatistics> { every { productId } returns 1L },
                    mockk<ProductStatistics> { every { productId } returns 1L }, // 중복
                    mockk<ProductStatistics> { every { productId } returns 2L }
                )
                val mockProduct1 = createMockProduct(1L)
                val mockProduct2 = createMockProduct(2L)

                every { mockProductStatisticsService.getPopularProducts(limit) } returns mockStatistics
                every { mockProductService.getProduct(1L) } returns mockProduct1
                every { mockProductService.getProduct(2L) } returns mockProduct2

                val result = sut.execute(limit)

                result shouldBe listOf(mockProduct1, mockProduct1, mockProduct2)
                verify(exactly = 2) { mockProductService.getProduct(1L) } // 2번 호출
                verify(exactly = 1) { mockProductService.getProduct(2L) }
            }
        }
    }

    describe("서비스 계층 조율 검증") {
        context("두 서비스의 협력") {
            it("ProductStatisticsService와 ProductService가 올바르게 협력") {
                val limit = 2
                val mockStatistics = listOf(
                    mockk<ProductStatistics> { every { productId } returns 5L },
                    mockk<ProductStatistics> { every { productId } returns 6L }
                )
                val mockProducts = listOf(createMockProduct(5L), createMockProduct(6L))

                every { mockProductStatisticsService.getPopularProducts(limit) } returns mockStatistics
                every { mockProductService.getProduct(5L) } returns mockProducts[0]
                every { mockProductService.getProduct(6L) } returns mockProducts[1]

                val result = sut.execute(limit)

                result shouldBe mockProducts

                // 먼저 통계를 조회하고, 그 다음 각 상품을 조회하는 순서 확인
                verifyOrder {
                    mockProductStatisticsService.getPopularProducts(limit)
                    mockProductService.getProduct(5L)
                    mockProductService.getProduct(6L)
                }
            }
        }
    }

    describe("비즈니스 로직 검증") {
        context("UseCase의 책임") {
            it("통계 조회와 상품 조회를 조율하여 최종 결과를 반환") {
                val limit = 3
                val mockStatistics = listOf(
                    mockk<ProductStatistics> { every { productId } returns 1L },
                    mockk<ProductStatistics> { every { productId } returns 2L },
                    mockk<ProductStatistics> { every { productId } returns 3L }
                )
                val mockProducts = listOf(
                    createMockProduct(1L),
                    createMockProduct(2L),
                    createMockProduct(3L)
                )

                every { mockProductStatisticsService.getPopularProducts(limit) } returns mockStatistics
                every { mockProductService.getProduct(1L) } returns mockProducts[0]
                every { mockProductService.getProduct(2L) } returns mockProducts[1]
                every { mockProductService.getProduct(3L) } returns mockProducts[2]

                val result = sut.execute(limit)

                // 결과가 올바르게 변환되었는지 확인
                result shouldBe mockProducts

                // 모든 서비스 호출이 정확히 이루어졌는지 확인
                verify(exactly = 1) { mockProductStatisticsService.getPopularProducts(limit) }
                verify(exactly = 1) { mockProductService.getProduct(1L) }
                verify(exactly = 1) { mockProductService.getProduct(2L) }
                verify(exactly = 1) { mockProductService.getProduct(3L) }
            }
        }
    }
})