package io.hhplus.ecommerce.integration.product

import io.hhplus.ecommerce.support.KotestIntegrationTestBase
import io.hhplus.ecommerce.common.cache.RedisKeyNames
import io.hhplus.ecommerce.product.application.port.out.ProductRankingPort
import io.hhplus.ecommerce.product.application.usecase.ProductRankingQueryUseCase
import io.hhplus.ecommerce.product.domain.entity.Product
import io.hhplus.ecommerce.product.domain.repository.ProductRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldNotBe
import org.springframework.data.redis.core.RedisTemplate
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 상품 판매 랭킹 통합 테스트
 *
 * STEP 13: Redis Sorted Set 기반 판매 랭킹 시스템 검증
 *
 * 테스트 항목:
 * - ZINCRBY 원자적 판매량 증가
 * - ZREVRANGE 판매량 높은 순 조회
 * - 일별/주별/누적 랭킹 조회
 * - 특정 상품 순위 조회
 */
class ProductRankingIntegrationTest(
    private val productRankingPort: ProductRankingPort,
    private val productRankingQueryUseCase: ProductRankingQueryUseCase,
    private val productRepository: ProductRepository,
    private val redisTemplate: RedisTemplate<String, Any>
) : KotestIntegrationTestBase({

    val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    beforeEach {
        // Redis 데이터 초기화
        redisTemplate.execute { connection ->
            connection.serverCommands().flushAll()
            null
        }
    }

    describe("ProductRankingPort - Redis Sorted Set 기반 랭킹") {

        context("ZINCRBY 판매량 증가") {

            it("상품 판매량이 원자적으로 증가한다") {
                // Given
                val productId = 1L
                val quantity = 5

                // When - ZINCRBY로 판매량 증가
                val newScore = productRankingPort.incrementSalesCount(productId, quantity)

                // Then
                newScore shouldBe 5L

                // 실제 Redis에서 값 확인
                val today = LocalDate.now().format(dateFormatter)
                val dailySalesCount = productRankingPort.getDailySalesCount(productId, today)
                dailySalesCount shouldBe 5L
            }

            it("여러 번 판매 시 누적된다") {
                // Given
                val productId = 1L

                // When - 여러 번 ZINCRBY
                productRankingPort.incrementSalesCount(productId, 3)
                productRankingPort.incrementSalesCount(productId, 7)
                val finalScore = productRankingPort.incrementSalesCount(productId, 5)

                // Then
                finalScore shouldBe 15L

                val today = LocalDate.now().format(dateFormatter)
                productRankingPort.getDailySalesCount(productId, today) shouldBe 15L
            }

            it("여러 상품의 판매량이 독립적으로 관리된다") {
                // Given
                val product1 = 1L
                val product2 = 2L
                val product3 = 3L

                // When
                productRankingPort.incrementSalesCount(product1, 10)
                productRankingPort.incrementSalesCount(product2, 20)
                productRankingPort.incrementSalesCount(product3, 15)

                // Then
                val today = LocalDate.now().format(dateFormatter)
                productRankingPort.getDailySalesCount(product1, today) shouldBe 10L
                productRankingPort.getDailySalesCount(product2, today) shouldBe 20L
                productRankingPort.getDailySalesCount(product3, today) shouldBe 15L
            }
        }

        context("ZREVRANGE Top N 조회") {

            it("판매량이 높은 순으로 정렬된다") {
                // Given - 상품 3개에 판매량 부여
                productRankingPort.incrementSalesCount(1L, 100)  // 3위
                productRankingPort.incrementSalesCount(2L, 500)  // 1위
                productRankingPort.incrementSalesCount(3L, 300)  // 2위

                // When - Top 3 조회
                val today = LocalDate.now().format(dateFormatter)
                val topProducts = productRankingPort.getDailyTopProducts(today, 3)

                // Then - 판매량 높은 순으로 정렬
                topProducts shouldHaveSize 3
                topProducts[0].first shouldBe 2L  // 500개 - 1위
                topProducts[0].second shouldBe 500L
                topProducts[1].first shouldBe 3L  // 300개 - 2위
                topProducts[1].second shouldBe 300L
                topProducts[2].first shouldBe 1L  // 100개 - 3위
                topProducts[2].second shouldBe 100L
            }

            it("limit보다 상품이 적으면 있는 만큼만 반환한다") {
                // Given - 상품 2개만 등록
                productRankingPort.incrementSalesCount(1L, 100)
                productRankingPort.incrementSalesCount(2L, 200)

                // When - Top 10 요청
                val today = LocalDate.now().format(dateFormatter)
                val topProducts = productRankingPort.getDailyTopProducts(today, 10)

                // Then - 2개만 반환
                topProducts shouldHaveSize 2
            }

            it("데이터가 없으면 빈 리스트를 반환한다") {
                // Given - 데이터 없음

                // When
                val today = LocalDate.now().format(dateFormatter)
                val topProducts = productRankingPort.getDailyTopProducts(today, 10)

                // Then
                topProducts shouldHaveSize 0
            }
        }

        context("특정 상품 순위 조회 (ZREVRANK)") {

            it("상품의 순위를 정확히 반환한다") {
                // Given
                productRankingPort.incrementSalesCount(1L, 100)  // 3위 (0-based: 2)
                productRankingPort.incrementSalesCount(2L, 500)  // 1위 (0-based: 0)
                productRankingPort.incrementSalesCount(3L, 300)  // 2위 (0-based: 1)

                // When
                val today = LocalDate.now().format(dateFormatter)
                val rank1 = productRankingPort.getDailyRank(1L, today)
                val rank2 = productRankingPort.getDailyRank(2L, today)
                val rank3 = productRankingPort.getDailyRank(3L, today)

                // Then - 0-based rank
                rank1 shouldBe 2L  // 3위 (0-based: 2)
                rank2 shouldBe 0L  // 1위 (0-based: 0)
                rank3 shouldBe 1L  // 2위 (0-based: 1)
            }

            it("존재하지 않는 상품은 null을 반환한다") {
                // Given
                productRankingPort.incrementSalesCount(1L, 100)

                // When
                val today = LocalDate.now().format(dateFormatter)
                val rank = productRankingPort.getDailyRank(999L, today)

                // Then
                rank shouldBe null
            }
        }

        context("누적 랭킹 (Total)") {

            it("누적 판매량이 정확히 증가한다") {
                // Given
                val productId = 1L

                // When - 여러 번 판매
                productRankingPort.incrementSalesCount(productId, 10)
                productRankingPort.incrementSalesCount(productId, 20)
                productRankingPort.incrementSalesCount(productId, 30)

                // Then
                val totalSales = productRankingPort.getTotalSalesCount(productId)
                totalSales shouldBe 60L
            }

            it("누적 Top N 조회가 정확하다") {
                // Given
                productRankingPort.incrementSalesCount(1L, 1000)
                productRankingPort.incrementSalesCount(2L, 5000)
                productRankingPort.incrementSalesCount(3L, 3000)

                // When
                val topProducts = productRankingPort.getTotalTopProducts(3)

                // Then
                topProducts shouldHaveSize 3
                topProducts[0].first shouldBe 2L
                topProducts[0].second shouldBe 5000L
                topProducts[1].first shouldBe 3L
                topProducts[1].second shouldBe 3000L
                topProducts[2].first shouldBe 1L
                topProducts[2].second shouldBe 1000L
            }
        }

        context("getAllDailySales - 배치 동기화용") {

            it("일별 전체 판매 데이터를 조회한다") {
                // Given
                productRankingPort.incrementSalesCount(1L, 100)
                productRankingPort.incrementSalesCount(2L, 200)
                productRankingPort.incrementSalesCount(3L, 300)

                // When
                val today = LocalDate.now().format(dateFormatter)
                val allSales = productRankingPort.getAllDailySales(today)

                // Then
                allSales.size shouldBe 3
                allSales[1L] shouldBe 100L
                allSales[2L] shouldBe 200L
                allSales[3L] shouldBe 300L
            }
        }
    }

    describe("ProductRankingQueryUseCase - 랭킹 조회 API") {

        context("오늘의 판매 랭킹 조회") {

            it("상품 정보와 함께 랭킹을 반환한다") {
                // Given - 상품 생성 및 판매량 기록
                val product1 = productRepository.save(
                    Product.create("인기 상품 1", "설명1", 10000L, 1L)
                )
                val product2 = productRepository.save(
                    Product.create("인기 상품 2", "설명2", 20000L, 1L)
                )
                val product3 = productRepository.save(
                    Product.create("인기 상품 3", "설명3", 30000L, 1L)
                )

                // Redis에 판매량 기록
                productRankingPort.incrementSalesCount(product1.id, 100)
                productRankingPort.incrementSalesCount(product2.id, 500)
                productRankingPort.incrementSalesCount(product3.id, 300)

                // When
                val ranking = productRankingQueryUseCase.getTodayTopProducts(3)

                // Then
                ranking shouldHaveSize 3

                // 1위 검증
                ranking[0].rank shouldBe 1
                ranking[0].productId shouldBe product2.id
                ranking[0].productName shouldBe "인기 상품 2"
                ranking[0].salesCount shouldBe 500L

                // 2위 검증
                ranking[1].rank shouldBe 2
                ranking[1].productId shouldBe product3.id
                ranking[1].productName shouldBe "인기 상품 3"
                ranking[1].salesCount shouldBe 300L

                // 3위 검증
                ranking[2].rank shouldBe 3
                ranking[2].productId shouldBe product1.id
                ranking[2].productName shouldBe "인기 상품 1"
                ranking[2].salesCount shouldBe 100L
            }
        }

        context("특정 상품의 오늘 순위 조회") {

            it("상품의 순위와 판매량을 반환한다") {
                // Given
                val product = productRepository.save(
                    Product.create("테스트 상품", "설명", 10000L, 1L)
                )

                // 다른 상품들도 등록해서 순위가 생기도록
                productRankingPort.incrementSalesCount(999L, 1000)  // 1위
                productRankingPort.incrementSalesCount(product.id, 500)  // 2위
                productRankingPort.incrementSalesCount(998L, 100)  // 3위

                // When
                val rankingResponse = productRankingQueryUseCase.getProductTodayRanking(product.id)

                // Then
                rankingResponse.rank shouldBe 2  // 1-based rank
                rankingResponse.productId shouldBe product.id
                rankingResponse.productName shouldBe "테스트 상품"
                rankingResponse.salesCount shouldBe 500L
            }
        }

        context("누적 판매 랭킹 조회") {

            it("누적 판매량 기준 랭킹을 반환한다") {
                // Given
                val product1 = productRepository.save(Product.create("상품1", "설명1", 10000L, 1L))
                val product2 = productRepository.save(Product.create("상품2", "설명2", 20000L, 1L))

                productRankingPort.incrementSalesCount(product1.id, 1000)
                productRankingPort.incrementSalesCount(product2.id, 2000)

                // When
                val ranking = productRankingQueryUseCase.getTotalTopProducts(2)

                // Then
                ranking shouldHaveSize 2
                ranking[0].productId shouldBe product2.id
                ranking[0].salesCount shouldBe 2000L
                ranking[1].productId shouldBe product1.id
                ranking[1].salesCount shouldBe 1000L
            }
        }

        context("삭제된 상품 처리") {

            it("존재하지 않는 상품은 Unknown으로 표시된다") {
                // Given - Redis에만 데이터 존재 (DB에는 상품 없음)
                productRankingPort.incrementSalesCount(99999L, 100)

                // When
                val ranking = productRankingQueryUseCase.getTodayTopProducts(1)

                // Then
                ranking shouldHaveSize 1
                ranking[0].productId shouldBe 99999L
                ranking[0].productName shouldBe "Unknown"
                ranking[0].salesCount shouldBe 100L
            }
        }
    }

    describe("Redis 키 TTL 설정") {

        it("일별 키는 TTL이 설정된다") {
            // Given
            productRankingPort.incrementSalesCount(1L, 10)

            // When - TTL 확인
            val today = LocalDate.now().format(dateFormatter)
            val dailyKey = RedisKeyNames.Ranking.dailySalesKey(today)
            val ttl = redisTemplate.getExpire(dailyKey)

            // Then - TTL이 설정되어 있음 (0보다 큼)
            ttl shouldNotBe null
            (ttl!! > 0) shouldBe true
        }
    }
})
