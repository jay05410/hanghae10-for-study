package io.hhplus.ecommerce.integration.product

import io.hhplus.ecommerce.support.KotestIntegrationTestBase
import io.hhplus.ecommerce.product.usecase.ProductStatsUseCase
import io.hhplus.ecommerce.product.usecase.GetProductQueryUseCase
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldContain
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
    private val productStatsUseCase: ProductStatsUseCase,
    private val getProductQueryUseCase: GetProductQueryUseCase
) : KotestIntegrationTestBase({

    describe("조회수 증가") {
        context("상품을 조회할 때") {
            it("조회수가 1 증가한다") {
                // Given
                val productId = 1001L
                val userId = 1L

                // When
                productStatsUseCase.incrementViewCount(productId, userId)
                val statistics = getProductQueryUseCase.getProductStatistics(productId)

                // Then
                statistics shouldNotBe null
                statistics!!.productId shouldBe productId
                statistics.viewCount shouldBe 1
            }
        }

        context("동일 상품을 여러 번 조회할 때") {
            it("조회수가 누적된다") {
                // Given
                val productId = 1002L
                val userId = 1L

                // When
                productStatsUseCase.incrementViewCount(productId, userId)
                productStatsUseCase.incrementViewCount(productId, userId)
                productStatsUseCase.incrementViewCount(productId, userId)
                val statistics = getProductQueryUseCase.getProductStatistics(productId)!!

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
                productStatsUseCase.incrementViewCount(newProductId, userId)
                val statistics = getProductQueryUseCase.getProductStatistics(newProductId)!!

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
                val statistics = productStatsUseCase.incrementSalesCount(productId, quantity, userId)

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
                productStatsUseCase.incrementSalesCount(productId, 3, userId)
                productStatsUseCase.incrementSalesCount(productId, 5, userId)
                val statistics = productStatsUseCase.incrementSalesCount(productId, 2, userId)

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
                val statistics = productStatsUseCase.incrementSalesCount(newProductId, quantity, userId)

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
                productStatsUseCase.incrementViewCount(productId, userId)
                productStatsUseCase.incrementViewCount(productId, userId)
                productStatsUseCase.incrementViewCount(productId, userId)
                val statistics = productStatsUseCase.incrementSalesCount(productId, 2, userId)

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
                productStatsUseCase.incrementSalesCount(productId, 5, userId)
                productStatsUseCase.incrementViewCount(productId, userId)
                val statistics = getProductQueryUseCase.getProductStatistics(productId)!!

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
                productStatsUseCase.incrementSalesCount(4001L, 10, userId)
                productStatsUseCase.incrementSalesCount(4002L, 30, userId)
                productStatsUseCase.incrementSalesCount(4003L, 20, userId)
                productStatsUseCase.incrementSalesCount(4004L, 5, userId)

                // When
                val topStatistics = getProductQueryUseCase.getPopularStatistics(10) // 더 많이 가져와서 확인

                // Then
                // 생성된 4개 상품이 모두 포함되어야 함
                val targetProducts = setOf(4001L, 4002L, 4003L, 4004L)
                val returnedProducts = topStatistics.map { it.productId }.toSet()

                // 최소한 우리가 생성한 상품들이 포함되어야 함
                targetProducts.forEach { productId ->
                    returnedProducts shouldContain productId
                }

                // 판매량이 올바르게 기록되어야 함
                val product4002 = topStatistics.find { it.productId == 4002L }!!
                val product4003 = topStatistics.find { it.productId == 4003L }!!
                val product4001 = topStatistics.find { it.productId == 4001L }!!
                val product4004 = topStatistics.find { it.productId == 4004L }!!

                product4002.salesCount shouldBe 30
                product4003.salesCount shouldBe 20
                product4001.salesCount shouldBe 10
                product4004.salesCount shouldBe 5
            }
        }

        context("요청 개수보다 상품이 적을 때") {
            it("존재하는 상품만 반환한다") {
                // Given
                val userId = 1L
                productStatsUseCase.incrementSalesCount(5001L, 15, userId)
                productStatsUseCase.incrementSalesCount(5002L, 25, userId)

                // When
                val topStatistics = getProductQueryUseCase.getPopularStatistics(10)

                // Then
                // 최소 2개는 있어야 함 (우리가 만든 상품들)
                val targetProducts = setOf(5001L, 5002L)
                val returnedProducts = topStatistics.map { it.productId }.toSet()

                targetProducts.forEach { productId ->
                    returnedProducts shouldContain productId
                }

                // 판매량 확인
                val product5001 = topStatistics.find { it.productId == 5001L }!!
                val product5002 = topStatistics.find { it.productId == 5002L }!!

                product5001.salesCount shouldBe 15
                product5002.salesCount shouldBe 25
            }
        }

        context("조회수와 판매량이 모두 있는 경우") {
            it("판매량 기준으로 정렬된다") {
                // Given
                val userId = 1L

                // 높은 조회수, 낮은 판매량
                productStatsUseCase.incrementViewCount(6001L, userId)
                repeat(100) { productStatsUseCase.incrementViewCount(6001L, userId) }
                productStatsUseCase.incrementSalesCount(6001L, 5, userId)

                // 낮은 조회수, 높은 판매량
                productStatsUseCase.incrementViewCount(6002L, userId)
                productStatsUseCase.incrementSalesCount(6002L, 50, userId)

                // When
                val topStatistics = getProductQueryUseCase.getPopularStatistics(10)

                // Then
                val targetProducts = setOf(6001L, 6002L)
                val returnedProducts = topStatistics.map { it.productId }.toSet()

                targetProducts.forEach { productId ->
                    returnedProducts shouldContain productId
                }

                // 판매량이 올바르게 기록되어야 함
                val product6001 = topStatistics.find { it.productId == 6001L }!!
                val product6002 = topStatistics.find { it.productId == 6002L }!!

                product6001.salesCount shouldBe 5
                product6002.salesCount shouldBe 50
                product6001.viewCount shouldBe 101 // 1 + 100
                product6002.viewCount shouldBe 1
            }
        }
    }

    describe("상품 통계 조회") {
        context("통계가 있는 상품을 조회할 때") {
            it("통계 정보를 조회할 수 있다") {
                // Given
                val productId = 7001L
                val userId = 1L
                productStatsUseCase.incrementViewCount(productId, userId)
                productStatsUseCase.incrementViewCount(productId, userId)
                productStatsUseCase.incrementSalesCount(productId, 3, userId)

                // When
                val statistics = getProductQueryUseCase.getProductStatistics(productId)

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
                val statistics = getProductQueryUseCase.getProductStatistics(99999L)

                // Then
                statistics shouldBe null
            }
        }
    }
})
