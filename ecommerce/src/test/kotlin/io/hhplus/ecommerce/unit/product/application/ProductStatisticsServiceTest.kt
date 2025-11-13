package io.hhplus.ecommerce.unit.product.application

import io.hhplus.ecommerce.product.application.ProductStatisticsService
import io.hhplus.ecommerce.product.domain.entity.ProductStatistics
import io.hhplus.ecommerce.product.domain.repository.ProductStatisticsRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * ProductStatisticsService 단위 테스트
 *
 * 책임: 상품 통계 도메인 비즈니스 로직 처리 검증
 * - 상품 조회수 증가 로직 검증
 * - 상품 판매량 증가 로직 검증
 * - 인기 상품 조회 로직 검증
 * - ProductStatisticsRepository와의 올바른 상호작용 검증
 *
 * 검증 목표:
 * 1. 통계가 없는 상품의 경우 새로 생성하는가?
 * 2. 기존 통계가 있는 경우 올바르게 업데이트하는가?
 * 3. 락을 사용한 안전한 통계 업데이트가 이루어지는가?
 * 4. 인기 상품 조회 시 적절한 Repository 메서드를 호출하는가?
 * 5. 각 메서드의 반환값이 올바른가?
 */
class ProductStatisticsServiceTest : DescribeSpec({
    val mockProductStatisticsRepository = mockk<ProductStatisticsRepository>()
    val sut = ProductStatisticsService(mockProductStatisticsRepository)

    beforeEach {
        clearMocks(mockProductStatisticsRepository)
    }

    describe("incrementViewCount") {
        context("기존 통계가 존재하는 상품의 조회수 증가") {
            it("락을 사용해 통계를 조회하고 조회수를 증가시켜 저장") {
                val productId = 1L
                val userId = 1L
                val mockStatistics = mockk<ProductStatistics> {
                    every { incrementViewCount(userId) } returns 0L
                }

                every { mockProductStatisticsRepository.findByProductIdWithLock(productId) } returns mockStatistics
                every { mockProductStatisticsRepository.save(mockStatistics) } returns mockStatistics

                val result = sut.incrementViewCount(productId, userId)

                result shouldBe mockStatistics
                verify(exactly = 1) { mockProductStatisticsRepository.findByProductIdWithLock(productId) }
                verify(exactly = 1) { mockStatistics.incrementViewCount(userId) }
                verify(exactly = 1) { mockProductStatisticsRepository.save(mockStatistics) }
            }
        }

        context("통계가 없는 상품의 조회수 증가") {
            it("새로운 통계를 생성하고 조회수를 증가시켜 저장") {
                val productId = 2L
                val userId = 2L
                val mockNewStatistics = mockk<ProductStatistics> {
                    every { incrementViewCount(userId) } returns 0L
                }

                every { mockProductStatisticsRepository.findByProductIdWithLock(productId) } returns null
                every { mockProductStatisticsRepository.save(any()) } returns mockNewStatistics

                val result = sut.incrementViewCount(productId, userId)

                result shouldBe mockNewStatistics
                verify(exactly = 1) { mockProductStatisticsRepository.findByProductIdWithLock(productId) }
                verify(exactly = 2) { mockProductStatisticsRepository.save(any()) } // 생성 + 업데이트
                verify(exactly = 1) { mockNewStatistics.incrementViewCount(userId) }
            }
        }

        context("다양한 상품과 사용자의 조회수 증가") {
            it("각 파라미터가 정확히 전달되어 처리") {
                val testCases = listOf(
                    Pair(1L, 1L),
                    Pair(100L, 200L),
                    Pair(999L, 888L)
                )

                testCases.forEach { (productId, userId) ->
                    val mockStatistics = mockk<ProductStatistics> {
                        every { incrementViewCount(userId) } returns 0L
                    }

                    every { mockProductStatisticsRepository.findByProductIdWithLock(productId) } returns mockStatistics
                    every { mockProductStatisticsRepository.save(mockStatistics) } returns mockStatistics

                    val result = sut.incrementViewCount(productId, userId)

                    result shouldBe mockStatistics
                    verify(exactly = 1) { mockProductStatisticsRepository.findByProductIdWithLock(productId) }
                    verify(exactly = 1) { mockStatistics.incrementViewCount(userId) }
                    verify(exactly = 1) { mockProductStatisticsRepository.save(mockStatistics) }
                    clearMocks(mockProductStatisticsRepository, mockStatistics)
                }
            }
        }
    }

    describe("incrementSalesCount") {
        context("기존 통계가 존재하는 상품의 판매량 증가") {
            it("락을 사용해 통계를 조회하고 판매량을 증가시켜 저장") {
                val productId = 1L
                val quantity = 5
                val userId = 1L
                val mockStatistics = mockk<ProductStatistics> {
                    every { incrementSalesCount(quantity, userId) } returns 0L
                }

                every { mockProductStatisticsRepository.findByProductIdWithLock(productId) } returns mockStatistics
                every { mockProductStatisticsRepository.save(mockStatistics) } returns mockStatistics

                val result = sut.incrementSalesCount(productId, quantity, userId)

                result shouldBe mockStatistics
                verify(exactly = 1) { mockProductStatisticsRepository.findByProductIdWithLock(productId) }
                verify(exactly = 1) { mockStatistics.incrementSalesCount(quantity, userId) }
                verify(exactly = 1) { mockProductStatisticsRepository.save(mockStatistics) }
            }
        }

        context("통계가 없는 상품의 판매량 증가") {
            it("새로운 통계를 생성하고 판매량을 증가시켜 저장") {
                val productId = 3L
                val quantity = 2
                val userId = 3L
                val mockNewStatistics = mockk<ProductStatistics> {
                    every { incrementSalesCount(quantity, userId) } returns 0L
                }

                every { mockProductStatisticsRepository.findByProductIdWithLock(productId) } returns null
                every { mockProductStatisticsRepository.save(any()) } returns mockNewStatistics

                val result = sut.incrementSalesCount(productId, quantity, userId)

                result shouldBe mockNewStatistics
                verify(exactly = 1) { mockProductStatisticsRepository.findByProductIdWithLock(productId) }
                verify(exactly = 2) { mockProductStatisticsRepository.save(any()) } // 생성 + 업데이트
                verify(exactly = 1) { mockNewStatistics.incrementSalesCount(quantity, userId) }
            }
        }

        context("다양한 수량으로 판매량 증가") {
            it("각 수량이 정확히 통계 객체에 전달") {
                val productId = 4L
                val userId = 4L
                val quantities = listOf(1, 5, 10, 100)

                quantities.forEach { quantity ->
                    val mockStatistics = mockk<ProductStatistics> {
                        every { incrementSalesCount(quantity, userId) } returns 0L
                    }

                    every { mockProductStatisticsRepository.findByProductIdWithLock(productId) } returns mockStatistics
                    every { mockProductStatisticsRepository.save(mockStatistics) } returns mockStatistics

                    val result = sut.incrementSalesCount(productId, quantity, userId)

                    result shouldBe mockStatistics
                    verify(exactly = 1) { mockStatistics.incrementSalesCount(quantity, userId) }
                    clearMocks(mockProductStatisticsRepository, mockStatistics)
                }
            }
        }
    }

    describe("getPopularProducts") {
        context("인기 상품 목록 조회") {
            it("ProductStatisticsRepository에서 인기 상품을 조회하고 반환") {
                val limit = 10
                val mockStatistics = listOf(mockk<ProductStatistics>(), mockk<ProductStatistics>(), mockk<ProductStatistics>())

                every { mockProductStatisticsRepository.findTopPopularProducts(limit) } returns mockStatistics

                val result = sut.getPopularProducts(limit)

                result shouldBe mockStatistics
                verify(exactly = 1) { mockProductStatisticsRepository.findTopPopularProducts(limit) }
            }
        }

        context("다양한 limit으로 인기 상품 조회") {
            it("각 limit이 정확히 Repository에 전달") {
                val limits = listOf(5, 10, 20, 50)

                limits.forEach { limit ->
                    val mockStatistics = (1..limit).map { mockk<ProductStatistics>() }
                    every { mockProductStatisticsRepository.findTopPopularProducts(limit) } returns mockStatistics

                    val result = sut.getPopularProducts(limit)

                    result shouldBe mockStatistics
                    verify(exactly = 1) { mockProductStatisticsRepository.findTopPopularProducts(limit) }
                    clearMocks(mockProductStatisticsRepository)
                }
            }
        }

        context("인기 상품이 없는 경우") {
            it("빈 리스트를 반환") {
                val limit = 10

                every { mockProductStatisticsRepository.findTopPopularProducts(limit) } returns emptyList()

                val result = sut.getPopularProducts(limit)

                result shouldBe emptyList()
                verify(exactly = 1) { mockProductStatisticsRepository.findTopPopularProducts(limit) }
            }
        }
    }

    describe("getProductStatistics") {
        context("존재하는 상품의 통계 조회") {
            it("ProductStatisticsRepository에서 통계를 조회하고 반환") {
                val productId = 1L
                val mockStatistics = mockk<ProductStatistics>()

                every { mockProductStatisticsRepository.findByProductId(productId) } returns mockStatistics

                val result = sut.getProductStatistics(productId)

                result shouldBe mockStatistics
                verify(exactly = 1) { mockProductStatisticsRepository.findByProductId(productId) }
            }
        }

        context("통계가 없는 상품 조회") {
            it("null을 반환") {
                val productId = 999L

                every { mockProductStatisticsRepository.findByProductId(productId) } returns null

                val result = sut.getProductStatistics(productId)

                result shouldBe null
                verify(exactly = 1) { mockProductStatisticsRepository.findByProductId(productId) }
            }
        }

        context("다양한 상품 ID로 통계 조회") {
            it("각 productId가 정확히 Repository에 전달") {
                val productIds = listOf(1L, 100L, 999L)

                productIds.forEach { productId ->
                    val mockStatistics = mockk<ProductStatistics>()
                    every { mockProductStatisticsRepository.findByProductId(productId) } returns mockStatistics

                    val result = sut.getProductStatistics(productId)

                    result shouldBe mockStatistics
                    verify(exactly = 1) { mockProductStatisticsRepository.findByProductId(productId) }
                    clearMocks(mockProductStatisticsRepository)
                }
            }
        }
    }

    describe("createStatistics") {
        context("새로운 통계 생성 과정 검증") {
            it("incrementViewCount 호출 시 통계가 없으면 새로 생성") {
                val productId = 5L
                val userId = 5L
                val mockNewStatistics = mockk<ProductStatistics> {
                    every { incrementViewCount(userId) } returns 0L
                }

                every { mockProductStatisticsRepository.findByProductIdWithLock(productId) } returns null
                every { mockProductStatisticsRepository.save(any()) } returns mockNewStatistics

                sut.incrementViewCount(productId, userId)

                // createStatistics 내부 로직 검증: ProductStatistics.create -> save -> incrementViewCount -> save
                verify(exactly = 2) { mockProductStatisticsRepository.save(any()) }
                verify(exactly = 1) { mockNewStatistics.incrementViewCount(userId) }
            }
        }

        context("통계 생성 시 올바른 파라미터 전달") {
            it("incrementSalesCount 호출 시 productId와 userId가 올바르게 전달") {
                val productId = 6L
                val quantity = 3
                val userId = 6L
                val mockNewStatistics = mockk<ProductStatistics> {
                    every { incrementSalesCount(quantity, userId) } returns 0L
                }

                every { mockProductStatisticsRepository.findByProductIdWithLock(productId) } returns null
                every { mockProductStatisticsRepository.save(any()) } returns mockNewStatistics

                sut.incrementSalesCount(productId, quantity, userId)

                verify(exactly = 2) { mockProductStatisticsRepository.save(any()) }
                verify(exactly = 1) { mockNewStatistics.incrementSalesCount(quantity, userId) }
            }
        }
    }

    describe("동시성 처리 검증") {
        context("락 사용 확인") {
            it("모든 증가 메서드가 findByProductIdWithLock을 사용") {
                val productId = 7L
                val userId = 7L
                val mockStatistics = mockk<ProductStatistics> {
                    every { incrementViewCount(userId) } returns 0L
                    every { incrementSalesCount(any(), userId) } returns 0L
                }

                every { mockProductStatisticsRepository.findByProductIdWithLock(productId) } returns mockStatistics
                every { mockProductStatisticsRepository.save(mockStatistics) } returns mockStatistics

                // incrementViewCount 테스트
                sut.incrementViewCount(productId, userId)
                verify(exactly = 1) { mockProductStatisticsRepository.findByProductIdWithLock(productId) }

                clearMocks(mockProductStatisticsRepository)

                // incrementSalesCount 테스트
                every { mockProductStatisticsRepository.findByProductIdWithLock(productId) } returns mockStatistics
                every { mockProductStatisticsRepository.save(mockStatistics) } returns mockStatistics
                sut.incrementSalesCount(productId, 1, userId)
                verify(exactly = 1) { mockProductStatisticsRepository.findByProductIdWithLock(productId) }
            }
        }
    }
})