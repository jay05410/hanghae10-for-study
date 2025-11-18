package io.hhplus.ecommerce.integration.point

import io.hhplus.ecommerce.support.KotestIntegrationTestBase

import io.hhplus.ecommerce.point.exception.PointException
import io.hhplus.ecommerce.support.config.IntegrationTestFixtures
import io.hhplus.ecommerce.point.usecase.PointCommandUseCase
import io.hhplus.ecommerce.point.usecase.GetPointQueryUseCase
import io.hhplus.ecommerce.point.usecase.PointHistoryUseCase
import io.hhplus.ecommerce.point.domain.constant.PointTransactionType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * 포인트 사용 통합 테스트
 *
 * TestContainers MySQL을 사용하여 포인트 사용 전체 플로우를 검증합니다.
 * - 포인트 사용 성공
 * - 포인트 이력 자동 기록
 * - 잔액 부족 검증
 * - 최소 사용 단위 검증 (100원)
 */
class PointUseIntegrationTest(
    private val pointCommandUseCase: PointCommandUseCase,
    private val getPointQueryUseCase: GetPointQueryUseCase,
    private val pointHistoryUseCase: PointHistoryUseCase
) : KotestIntegrationTestBase({

    describe("포인트 사용") {
        context("정상적인 사용 요청일 때") {
            it("포인트를 정상적으로 사용할 수 있다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(1)
                val initialAmount = 10_000L
                val useAmount = 3_000L

                // When - 초기 적립 후 사용
                pointCommandUseCase.chargePoint(userId, initialAmount, "초기 적립")
                val updatedUserPoint = pointCommandUseCase.usePoint(
                    userId = userId,
                    amount = useAmount,
                    description = "주문 결제"
                )

                // Then
                updatedUserPoint shouldNotBe null
                updatedUserPoint.balance.value shouldBe 7_000L // 10,000 - 3,000

                // 데이터베이스에서 확인
                val savedUserPoint = getPointQueryUseCase.getUserPoint(userId)
                savedUserPoint.balance.value shouldBe 7_000L

                // 포인트 이력 확인
                val histories = pointHistoryUseCase.getPointHistory(userId)
                histories.size shouldBe 2  // 충전 1회 + 사용 1회
                val useHistory = histories.first { it.transactionType == PointTransactionType.USE }
                useHistory.amount shouldBe -3_000L // USE는 음수로 저장
                useHistory.balanceBefore shouldBe 10_000L
                useHistory.balanceAfter shouldBe 7_000L
            }
        }

        context("잔액이 부족할 때") {
            it("예외가 발생한다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(2)
                val initialAmount = 5_000L
                val useAmount = 6_000L // 잔액보다 많은 금액

                // 사용자 포인트 생성 및 초기 적립
                pointCommandUseCase.chargePoint(userId, initialAmount, "초기 적립")

                // When & Then
                shouldThrow<PointException.InsufficientBalance> {
                    pointCommandUseCase.usePoint(userId, useAmount, "테스트 사용")
                }

                // 잔액이 변경되지 않았는지 확인
                val userPoint = getPointQueryUseCase.getUserPoint(userId)
                userPoint.balance.value shouldBe 5_000L
            }
        }

        context("전액 사용 시") {
            it("포인트를 전액 사용할 수 있다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(3)
                val initialAmount = 8_000L

                // 사용자 포인트 생성 및 초기 적립
                pointCommandUseCase.chargePoint(userId, initialAmount, "초기 적립")

                // When - 전액 사용
                pointCommandUseCase.usePoint(userId, initialAmount)

                // Then
                val userPoint = getPointQueryUseCase.getUserPoint(userId)
                userPoint.balance.value shouldBe 0L
            }
        }

        context("연속으로 사용할 때") {
            it("여러 번 포인트를 사용할 수 있다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(4)
                val initialAmount = 10_000L
                val firstUse = 3_000L
                val secondUse = 2_000L

                // 사용자 포인트 생성 및 초기 적립
                pointCommandUseCase.chargePoint(userId, initialAmount, "초기 적립")

                // When - 첫 번째 사용
                pointCommandUseCase.usePoint(userId, firstUse, "첫 번째 사용")

                // When - 두 번째 사용
                pointCommandUseCase.usePoint(userId, secondUse, "두 번째 사용")

                // Then
                val finalUserPoint = getPointQueryUseCase.getUserPoint(userId)
                finalUserPoint.balance.value shouldBe 5_000L // 10,000 - 3,000 - 2,000

                // 포인트 이력 확인 (최신순)
                val histories = pointHistoryUseCase.getPointHistory(userId)
                histories.size shouldBe 3  // 충전 1회 + 사용 2회
                val useHistories = histories.filter { it.transactionType == PointTransactionType.USE }
                useHistories.size shouldBe 2
                useHistories[0].amount shouldBe -2_000L // USE는 음수로 저장 (최신)
                useHistories[1].amount shouldBe -3_000L // USE는 음수로 저장 (과거)
            }
        }

        context("0원 이하 금액으로 사용 시도 시") {
            it("예외가 발생한다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(5)
                val initialAmount = 10_000L

                // 사용자 포인트 생성 및 초기 적립
                pointCommandUseCase.chargePoint(userId, initialAmount, "초기 적립")

                // When & Then - 0원 사용 시도
                shouldThrow<PointException.InvalidAmount> {
                    pointCommandUseCase.usePoint(userId, 0)
                }

                // When & Then - 음수 사용 시도
                shouldThrow<IllegalArgumentException> {
                    pointCommandUseCase.usePoint(userId, -1000)
                }
            }
        }

        context("주문과 연결된 포인트 사용 시") {
            it("주문 정보가 포함된 사용 이력이 기록된다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(6)
                val initialAmount = 20_000L
                val useAmount = 5_000L
                val orderId = 12345L

                // 사용자 포인트 생성 및 초기 적립
                pointCommandUseCase.chargePoint(userId, initialAmount, "초기 적립")

                // When
                pointCommandUseCase.usePoint(
                    userId = userId,
                    amount = useAmount,
                    description = "주문 결제",
                    orderId = orderId
                )

                // Then
                val histories = pointHistoryUseCase.getPointHistory(userId)
                histories.size shouldBe 2  // 충전 1회 + 사용 1회
                val useHistory = histories.first { it.transactionType == PointTransactionType.USE }
                useHistory.orderId shouldBe orderId
                useHistory.description shouldBe "주문 결제"
                useHistory.transactionType shouldBe PointTransactionType.USE
            }
        }

        context("포인트 적립 후 사용할 때") {
            it("적립과 사용이 연속으로 정상 동작한다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(7)
                val firstEarn = 10_000L
                val firstUse = 3_000L
                val secondEarn = 5_000L
                val secondUse = 2_000L

                // When - 적립 → 사용 → 적립 → 사용
                pointCommandUseCase.chargePoint(userId, firstEarn, "첫 번째 적립")
                pointCommandUseCase.usePoint(userId, firstUse, "첫 번째 사용")
                pointCommandUseCase.chargePoint(userId, secondEarn, "두 번째 적립")
                pointCommandUseCase.usePoint(userId, secondUse, "두 번째 사용")

                // Then
                val userPoint = getPointQueryUseCase.getUserPoint(userId)
                userPoint.balance.value shouldBe 10_000L // 10,000 - 3,000 + 5,000 - 2,000
            }
        }
    }
})
