package io.hhplus.ecommerce.product.usecase

import io.hhplus.ecommerce.product.application.ProductStatisticsService
import io.hhplus.ecommerce.product.domain.entity.ProductStatistics
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.*

/**
 * IncrementProductViewUseCase 단위 테스트
 *
 * 책임: 상품 조회수 증가 요청 처리 및 ProductStatisticsService 호출 검증
 * - productId와 userId를 ProductStatisticsService로 위임
 * - 조회수 증가 로직의 올바른 실행 검증
 * - 반환값이 없는 void 메서드의 호출 검증
 *
 * 검증 목표:
 * 1. productId와 userId가 올바르게 ProductStatisticsService에 전달되는가?
 * 2. ProductStatisticsService.incrementViewCount가 정확히 호출되는가?
 * 3. 다양한 입력값에 대한 정확한 처리가 이루어지는가?
 * 4. UseCase가 단순 위임 역할만 수행하는가?
 * 5. 반환값을 처리하지 않고 단순히 실행만 하는가?
 */
class IncrementProductViewUseCaseTest : DescribeSpec({
    val mockProductStatisticsService = mockk<ProductStatisticsService>()
    val sut = IncrementProductViewUseCase(mockProductStatisticsService)

    beforeEach {
        clearMocks(mockProductStatisticsService)
    }

    describe("execute") {
        context("상품 조회수 증가 요청") {
            it("ProductStatisticsService.incrementViewCount를 올바른 파라미터로 호출") {
                val productId = 1L
                val userId = 1L
                val mockStatistics = mockk<ProductStatistics>()

                every { mockProductStatisticsService.incrementViewCount(productId, userId) } returns mockStatistics

                sut.execute(productId, userId)

                verify(exactly = 1) { mockProductStatisticsService.incrementViewCount(productId, userId) }
            }
        }

        context("다양한 상품과 사용자 조합") {
            it("각 파라미터가 정확히 ProductStatisticsService에 전달") {
                val testCases = listOf(
                    Pair(1L, 1L),
                    Pair(100L, 200L),
                    Pair(999L, 888L),
                    Pair(Long.MAX_VALUE, Long.MAX_VALUE)
                )

                testCases.forEach { (productId, userId) ->
                    val mockStatistics = mockk<ProductStatistics>()
                    every { mockProductStatisticsService.incrementViewCount(productId, userId) } returns mockStatistics

                    sut.execute(productId, userId)

                    verify(exactly = 1) { mockProductStatisticsService.incrementViewCount(productId, userId) }
                    clearMocks(mockProductStatisticsService)
                }
            }
        }

        context("경계값 테스트") {
            it("최소값과 최대값 파라미터로 조회수 증가") {
                val boundaryTestCases = listOf(
                    Pair(1L, 1L),                             // 최소값
                    Pair(Long.MAX_VALUE, Long.MAX_VALUE)      // 최대값
                )

                boundaryTestCases.forEach { (productId, userId) ->
                    val mockStatistics = mockk<ProductStatistics>()
                    every { mockProductStatisticsService.incrementViewCount(productId, userId) } returns mockStatistics

                    sut.execute(productId, userId)

                    verify(exactly = 1) { mockProductStatisticsService.incrementViewCount(productId, userId) }
                    clearMocks(mockProductStatisticsService)
                }
            }
        }

        context("동일한 상품에 대한 다양한 사용자") {
            it("동일한 productId에 대해 다른 userId로 각각 호출") {
                val productId = 1L
                val userIds = listOf(1L, 2L, 3L, 4L, 5L)

                userIds.forEach { userId ->
                    val mockStatistics = mockk<ProductStatistics>()
                    every { mockProductStatisticsService.incrementViewCount(productId, userId) } returns mockStatistics

                    sut.execute(productId, userId)

                    verify(exactly = 1) { mockProductStatisticsService.incrementViewCount(productId, userId) }
                    clearMocks(mockProductStatisticsService)
                }
            }
        }

        context("동일한 사용자의 다양한 상품 조회") {
            it("동일한 userId에 대해 다른 productId로 각각 호출") {
                val userId = 1L
                val productIds = listOf(1L, 2L, 3L, 4L, 5L)

                productIds.forEach { productId ->
                    val mockStatistics = mockk<ProductStatistics>()
                    every { mockProductStatisticsService.incrementViewCount(productId, userId) } returns mockStatistics

                    sut.execute(productId, userId)

                    verify(exactly = 1) { mockProductStatisticsService.incrementViewCount(productId, userId) }
                    clearMocks(mockProductStatisticsService)
                }
            }
        }
    }

    describe("반환값 처리 검증") {
        context("ProductStatisticsService 결과 무시") {
            it("ProductStatisticsService의 반환값을 사용하지 않음") {
                val productId = 1L
                val userId = 1L
                val mockStatistics = mockk<ProductStatistics>()

                every { mockProductStatisticsService.incrementViewCount(productId, userId) } returns mockStatistics

                // execute 메서드는 Unit을 반환해야 함
                sut.execute(productId, userId)

                verify(exactly = 1) { mockProductStatisticsService.incrementViewCount(productId, userId) }
                // 반환값을 사용하지 않으므로 별도의 검증은 불필요
            }
        }

        context("void 메서드 동작 확인") {
            it("반환값 없이 실행만 수행") {
                val productId = 123L
                val userId = 456L
                val mockStatistics = mockk<ProductStatistics>()

                every { mockProductStatisticsService.incrementViewCount(productId, userId) } returns mockStatistics

                // 메서드 실행이 예외 없이 완료되는지 확인
                sut.execute(productId, userId)

                verify(exactly = 1) { mockProductStatisticsService.incrementViewCount(productId, userId) }
            }
        }
    }

    describe("위임 패턴 검증") {
        context("UseCase의 역할") {
            it("추가적인 비즈니스 로직 없이 ProductStatisticsService로 단순 위임") {
                val productId = 789L
                val userId = 321L
                val mockStatistics = mockk<ProductStatistics>()

                every { mockProductStatisticsService.incrementViewCount(productId, userId) } returns mockStatistics

                sut.execute(productId, userId)

                // ProductStatisticsService가 정확히 한 번만 호출되었는지 확인
                verify(exactly = 1) { mockProductStatisticsService.incrementViewCount(productId, userId) }
            }
        }

        context("파라미터 변환 없음") {
            it("입력 파라미터를 변환 없이 그대로 전달") {
                val productId = 555L
                val userId = 666L
                val mockStatistics = mockk<ProductStatistics>()

                every { mockProductStatisticsService.incrementViewCount(555L, 666L) } returns mockStatistics

                sut.execute(productId, userId)

                // 정확한 파라미터로 호출되었는지 확인
                verify(exactly = 1) { mockProductStatisticsService.incrementViewCount(555L, 666L) }
            }
        }
    }

    describe("메서드 호출 독립성") {
        context("다른 서비스 메서드 호출하지 않음") {
            it("incrementViewCount만 호출하고 다른 ProductStatisticsService 메서드는 호출하지 않음") {
                val productId = 111L
                val userId = 222L
                val mockStatistics = mockk<ProductStatistics>()

                every { mockProductStatisticsService.incrementViewCount(productId, userId) } returns mockStatistics
                every { mockProductStatisticsService.incrementSalesCount(any(), any(), any()) } returns mockStatistics
                every { mockProductStatisticsService.getPopularProducts(any()) } returns listOf(mockStatistics)
                every { mockProductStatisticsService.getProductStatistics(any()) } returns mockStatistics

                sut.execute(productId, userId)

                verify(exactly = 1) { mockProductStatisticsService.incrementViewCount(productId, userId) }
                verify(exactly = 0) { mockProductStatisticsService.incrementSalesCount(any(), any(), any()) }
                verify(exactly = 0) { mockProductStatisticsService.getPopularProducts(any()) }
                verify(exactly = 0) { mockProductStatisticsService.getProductStatistics(any()) }
            }
        }
    }

    describe("예외 상황 처리") {
        context("ProductStatisticsService 예외 전파") {
            it("ProductStatisticsService에서 발생한 예외를 그대로 전파") {
                val productId = 999L
                val userId = 999L
                val exception = RuntimeException("통계 업데이트 실패")

                every { mockProductStatisticsService.incrementViewCount(productId, userId) } throws exception

                try {
                    sut.execute(productId, userId)
                    throw AssertionError("예외가 발생해야 함")
                } catch (e: RuntimeException) {
                    // 예외가 정상적으로 전파되었는지 확인
                    verify(exactly = 1) { mockProductStatisticsService.incrementViewCount(productId, userId) }
                }
            }
        }
    }

    describe("성능 관련 검증") {
        context("단일 호출") {
            it("ProductStatisticsService를 정확히 한 번만 호출") {
                val productId = 777L
                val userId = 888L
                val mockStatistics = mockk<ProductStatistics>()

                every { mockProductStatisticsService.incrementViewCount(productId, userId) } returns mockStatistics

                sut.execute(productId, userId)

                // 정확히 한 번만 호출되었는지 확인
                verify(exactly = 1) { mockProductStatisticsService.incrementViewCount(productId, userId) }
            }
        }
    }
})