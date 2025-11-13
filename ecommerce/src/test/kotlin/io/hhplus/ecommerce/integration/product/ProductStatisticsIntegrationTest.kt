package io.hhplus.ecommerce.integration.product

import io.hhplus.ecommerce.support.KotestIntegrationTestBase
import io.hhplus.ecommerce.product.application.ProductStatisticsService
import io.hhplus.ecommerce.product.domain.repository.ProductStatisticsRepository
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * 상품 통계 통합 테스트
 *
 * TestContainers MySQL을 사용하여 상품 통계 전체 플로우를 검증합니다.
 * - 조회수 증가
 * - 판매량 증가
 * - 인기 상품 조회
 */
class ProductStatisticsIntegrationTest(
    private val productStatisticsService: ProductStatisticsService,
    private val productStatisticsRepository: ProductStatisticsRepository
) : KotestIntegrationTestBase({

    describe("조회수 증가") {
        context("상품을 조회할 때") {
            it("조회수가 1 증가한다") {
                // Given
                val productId = 1001L
                val userId = 1L

                // When
                val statistics = productStatisticsService.incrementViewCount(productId, userId)

                // Then
                statistics shouldNotBe null
                statistics.productId shouldBe productId
                statistics.viewCount shouldBe 1
            }
        }

        context("동일 상품을 여러 번 조회할 때") {
            it("조회수가 누적된다") {
                // Given
                val productId = 1002L
                val userId = 1L

                // When
                productStatisticsService.incrementViewCount(productId, userId)
                productStatisticsService.incrementViewCount(productId, userId)
                val statistics = productStatisticsService.incrementViewCount(productId, userId)

                // Then
                statistics.viewCount shouldBe 3
            }
        }

        context("통계가 없는 상품을 조회할 때") {
            it("통계가 새로 생성된다") {
                // Given
                val newProductId = 1003L
                val userId = 1L

                // When
                val statistics = productStatisticsService.incrementViewCount(newProductId, userId)

                // Then
                statistics shouldNotBe null
                statistics.productId shouldBe newProductId
                statistics.viewCount shouldBe 1
                statistics.salesCount shouldBe 0
            }
        }
    }

    describe("판매량 증가") {
        context("상품이 판매될 때") {
            it("판매량이 증가한다") {
                // Given
                val productId = 2001L
                val quantity = 5
                val userId = 1L

                // When
                val statistics = productStatisticsService.incrementSalesCount(productId, quantity, userId)

                // Then
                statistics shouldNotBe null
                statistics.productId shouldBe productId
                statistics.salesCount shouldBe 5
            }
        }

        context("여러 번 판매될 때") {
            it("판매량이 누적된다") {
                // Given
                val productId = 2002L
                val userId = 1L

                // When
                productStatisticsService.incrementSalesCount(productId, 3, userId)
                productStatisticsService.incrementSalesCount(productId, 5, userId)
                val statistics = productStatisticsService.incrementSalesCount(productId, 2, userId)

                // Then
                statistics.salesCount shouldBe 10 // 3 + 5 + 2
            }
        }

        context("통계가 없는 상품이 판매될 때") {
            it("통계가 새로 생성되고 판매량이 기록된다") {
                // Given
                val newProductId = 2003L
                val quantity = 7
                val userId = 1L

                // When
                val statistics = productStatisticsService.incrementSalesCount(newProductId, quantity, userId)

                // Then
                statistics shouldNotBe null
                statistics.productId shouldBe newProductId
                statistics.salesCount shouldBe 7
                statistics.viewCount shouldBe 0
            }
        }
    }

    describe("조회수와 판매량 통합") {
        context("상품 조회 후 판매될 때") {
            it("조회수와 판매량이 모두 기록된다") {
                // Given
                val productId = 3001L
                val userId = 1L

                // When
                productStatisticsService.incrementViewCount(productId, userId)
                productStatisticsService.incrementViewCount(productId, userId)
                productStatisticsService.incrementViewCount(productId, userId)
                val statistics = productStatisticsService.incrementSalesCount(productId, 2, userId)

                // Then
                statistics.viewCount shouldBe 3
                statistics.salesCount shouldBe 2
            }
        }

        context("판매 후 조회될 때") {
            it("판매량과 조회수가 모두 기록된다") {
                // Given
                val productId = 3002L
                val userId = 1L

                // When
                productStatisticsService.incrementSalesCount(productId, 5, userId)
                val statistics = productStatisticsService.incrementViewCount(productId, userId)

                // Then
                statistics.salesCount shouldBe 5
                statistics.viewCount shouldBe 1
            }
        }
    }

    describe("인기 상품 조회") {
        context("여러 상품이 있을 때") {
            it("판매량 순으로 인기 상품을 조회할 수 있다") {
                // Given
                val userId = 1L
                productStatisticsService.incrementSalesCount(4001L, 10, userId)
                productStatisticsService.incrementSalesCount(4002L, 30, userId)
                productStatisticsService.incrementSalesCount(4003L, 20, userId)
                productStatisticsService.incrementSalesCount(4004L, 5, userId)

                // When
                val topProducts = productStatisticsService.getPopularProducts(3)

                // Then
                topProducts shouldHaveSize 3
                topProducts[0].productId shouldBe 4002L // 30 판매
                topProducts[0].salesCount shouldBe 30
                topProducts[1].productId shouldBe 4003L // 20 판매
                topProducts[1].salesCount shouldBe 20
                topProducts[2].productId shouldBe 4001L // 10 판매
                topProducts[2].salesCount shouldBe 10
            }
        }

        context("요청 개수보다 상품이 적을 때") {
            it("존재하는 상품만 반환한다") {
                // Given
                val userId = 1L
                productStatisticsService.incrementSalesCount(5001L, 15, userId)
                productStatisticsService.incrementSalesCount(5002L, 25, userId)

                // When
                val topProducts = productStatisticsService.getPopularProducts(10)

                // Then
                topProducts.size shouldBe 2
                topProducts[0].productId shouldBe 5002L
                topProducts[1].productId shouldBe 5001L
            }
        }

        context("조회수와 판매량이 모두 있는 경우") {
            it("판매량 기준으로 정렬된다") {
                // Given
                val userId = 1L

                // 높은 조회수, 낮은 판매량
                productStatisticsService.incrementViewCount(6001L, userId)
                repeat(100) { productStatisticsService.incrementViewCount(6001L, userId) }
                productStatisticsService.incrementSalesCount(6001L, 5, userId)

                // 낮은 조회수, 높은 판매량
                productStatisticsService.incrementViewCount(6002L, userId)
                productStatisticsService.incrementSalesCount(6002L, 50, userId)

                // When
                val topProducts = productStatisticsService.getPopularProducts(2)

                // Then
                topProducts[0].productId shouldBe 6002L // 판매량 50
                topProducts[1].productId shouldBe 6001L // 판매량 5
            }
        }
    }

    describe("상품 통계 조회") {
        context("통계가 있는 상품을 조회할 때") {
            it("통계 정보를 조회할 수 있다") {
                // Given
                val productId = 7001L
                val userId = 1L
                productStatisticsService.incrementViewCount(productId, userId)
                productStatisticsService.incrementViewCount(productId, userId)
                productStatisticsService.incrementSalesCount(productId, 3, userId)

                // When
                val statistics = productStatisticsService.getProductStatistics(productId)

                // Then
                statistics shouldNotBe null
                statistics!!.productId shouldBe productId
                statistics.viewCount shouldBe 2
                statistics.salesCount shouldBe 3
            }
        }

        context("통계가 없는 상품을 조회할 때") {
            it("null을 반환한다") {
                // When
                val statistics = productStatisticsService.getProductStatistics(99999L)

                // Then
                statistics shouldBe null
            }
        }
    }
})
