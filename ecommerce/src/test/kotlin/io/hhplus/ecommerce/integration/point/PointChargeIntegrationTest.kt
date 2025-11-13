package io.hhplus.ecommerce.integration.point

import io.hhplus.ecommerce.common.exception.point.PointException
import io.hhplus.ecommerce.support.config.IntegrationTestFixtures
import io.hhplus.ecommerce.point.usecase.PointCommandUseCase
import io.hhplus.ecommerce.point.usecase.GetPointQueryUseCase
import io.hhplus.ecommerce.point.usecase.PointHistoryUseCase
import io.hhplus.ecommerce.point.domain.constant.PointTransactionType
import io.hhplus.ecommerce.point.domain.vo.Balance
import io.hhplus.ecommerce.point.domain.vo.PointAmount
import io.hhplus.ecommerce.support.KotestIntegrationTestBase
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * 포인트 충전(적립) 통합 테스트
 *
 * TestContainers MySQL을 사용하여 포인트 적립 전체 플로우를 검증합니다.
 */
class PointChargeIntegrationTest(
    private val pointCommandUseCase: PointCommandUseCase,
    private val getPointQueryUseCase: GetPointQueryUseCase,
    private val pointHistoryUseCase: PointHistoryUseCase
) : KotestIntegrationTestBase({

    describe("포인트 충전(적립)") {
        context("정상적인 적립 요청일 때") {
            it("포인트를 정상적으로 적립할 수 있다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(1)
                val earnAmount = PointAmount(5000)
                val createdBy = userId

                // 사용자 포인트 생성
                pointCommandUseCase.createUserPoint(userId, createdBy)

                // When
                val updatedUserPoint = pointCommandUseCase.chargePoint(
                    userId = userId,
                    amount = earnAmount.value,
                    description = "구매 적립"
                )

                // Then
                updatedUserPoint shouldNotBe null
                updatedUserPoint.balance.value shouldBe earnAmount.value

                // 데이터베이스에서 확인
                val savedUserPoint = getPointQueryUseCase.getUserPoint(userId)
                savedUserPoint shouldNotBe null
                savedUserPoint!!.balance.value shouldBe earnAmount.value

                // 포인트 이력 확인
                val histories = pointHistoryUseCase.getPointHistory(userId)
                histories.size shouldBe 1
                histories[0].transactionType shouldBe PointTransactionType.EARN
                histories[0].amount shouldBe earnAmount.value
                histories[0].balanceBefore shouldBe 0L
                histories[0].balanceAfter shouldBe earnAmount.value
            }
        }

        context("연속으로 포인트를 적립할 때") {
            it("누적 적립이 정상 동작한다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(2)
                val firstEarn = PointAmount(3000)
                val secondEarn = PointAmount(2000)
                val createdBy = userId

                // 사용자 포인트 생성
                pointCommandUseCase.createUserPoint(userId, createdBy)

                // When - 첫 번째 적립
                pointCommandUseCase.chargePoint(userId, firstEarn.value, "첫 번째 적립")

                // When - 두 번째 적립
                pointCommandUseCase.chargePoint(userId, secondEarn.value, "두 번째 적립")

                // Then
                val finalUserPoint = getPointQueryUseCase.getUserPoint(userId)
                finalUserPoint shouldNotBe null
                finalUserPoint!!.balance.value shouldBe 5000L // 3000 + 2000

                // 포인트 이력 확인 (최신순)
                val histories = pointHistoryUseCase.getPointHistory(userId)
                histories.size shouldBe 2
                histories[0].amount shouldBe 2000L // 최신
                histories[1].amount shouldBe 3000L // 과거
            }
        }

        context("최대 잔액(10,000,000원) 초과 시도 시") {
            it("예외가 발생한다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(3)
                val createdBy = userId

                // 사용자 포인트 생성
                pointCommandUseCase.createUserPoint(userId, createdBy)

                // 9,900,000원 적립
                pointCommandUseCase.chargePoint(userId, 9_900_000L, "초기 적립")

                // When & Then - 150,000원 추가 적립 시도 (10,000,000 초과)
                shouldThrow<PointException.MaxBalanceExceeded> {
                    pointCommandUseCase.chargePoint(userId, 150_000L, "초과 시도")
                }

                // 잔액이 변경되지 않았는지 확인
                val userPoint = getPointQueryUseCase.getUserPoint(userId)
                userPoint!!.balance.value shouldBe 9_900_000L
            }
        }

        context("정확히 최대 잔액까지 적립 시") {
            it("최대 잔액(10,000,000원)까지 적립할 수 있다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(4)
                val createdBy = userId

                // 사용자 포인트 생성
                pointCommandUseCase.createUserPoint(userId, createdBy)

                // When - 10,000,000원 적립
                pointCommandUseCase.chargePoint(userId, 10_000_000L, "최대 한도 적립")

                // Then
                val userPoint = getPointQueryUseCase.getUserPoint(userId)
                userPoint shouldNotBe null
                userPoint!!.balance.value shouldBe 10_000_000L
                (userPoint.balance.value == Balance.MAX_BALANCE) shouldBe true
            }
        }

        context("구매 시 5% 적립 정책일 때") {
            it("구매 금액의 5%가 정상 적립된다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(5)
                val purchaseAmount = 100_000L
                val earnRate = 0.05
                val earnAmount = PointAmount((purchaseAmount * earnRate).toLong()) // 5,000원
                val createdBy = userId

                // 사용자 포인트 생성
                pointCommandUseCase.createUserPoint(userId, createdBy)

                // When - 구매 적립
                pointCommandUseCase.chargePoint(
                    userId = userId,
                    amount = earnAmount.value,
                    description = "구매 적립 5%",
                    orderId = 12345L
                )

                // Then
                val userPoint = getPointQueryUseCase.getUserPoint(userId)
                userPoint shouldNotBe null
                userPoint!!.balance.value shouldBe 5_000L

                // 포인트 이력 확인
                val histories = pointHistoryUseCase.getPointHistory(userId)
                histories.size shouldBe 1
                histories[0].orderId shouldBe 12345L
                histories[0].description shouldBe "구매 적립 5%"
            }
        }
    }
})
