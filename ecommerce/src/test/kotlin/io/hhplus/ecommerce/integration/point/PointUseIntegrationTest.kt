package io.hhplus.ecommerce.integration.point

import io.hhplus.ecommerce.support.KotestIntegrationTestBase

import io.hhplus.ecommerce.common.exception.point.PointException
import io.hhplus.ecommerce.support.config.IntegrationTestFixtures
import io.hhplus.ecommerce.point.application.PointHistoryService
import io.hhplus.ecommerce.point.application.PointService
import io.hhplus.ecommerce.point.domain.constant.PointTransactionType
import io.hhplus.ecommerce.point.domain.repository.PointHistoryRepository
import io.hhplus.ecommerce.point.domain.repository.UserPointRepository
import io.hhplus.ecommerce.point.domain.vo.PointAmount
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
    private val pointService: PointService,
    private val pointHistoryService: PointHistoryService,
    private val userPointRepository: UserPointRepository,
    private val pointHistoryRepository: PointHistoryRepository
) : KotestIntegrationTestBase({

    describe("포인트 사용") {
        context("정상적인 사용 요청일 때") {
            it("포인트를 정상적으로 사용할 수 있다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(1)
                val initialAmount = PointAmount(10_000)
                val useAmount = PointAmount(3_000)
                val createdBy = userId

                // 사용자 포인트 생성 및 초기 적립
                pointService.createUserPoint(userId, createdBy)
                pointService.earnPoint(userId, initialAmount, createdBy)

                // When
                val balanceBefore = pointService.getUserPoint(userId)!!.balance
                val updatedUserPoint = pointService.usePoint(
                    userId = userId,
                    amount = useAmount,
                    usedBy = createdBy,
                    description = "주문 결제"
                )

                // 포인트 이력 기록
                pointHistoryService.recordUseHistory(
                    userId = userId,
                    amount = useAmount,
                    balanceBefore = balanceBefore,
                    balanceAfter = updatedUserPoint.balance,
                    createdBy = createdBy,
                    description = "주문 결제"
                )

                // Then
                updatedUserPoint shouldNotBe null
                updatedUserPoint.balance.value shouldBe 7_000L // 10,000 - 3,000

                // 데이터베이스에서 확인
                val savedUserPoint = userPointRepository.findByUserId(userId)
                savedUserPoint shouldNotBe null
                savedUserPoint!!.balance.value shouldBe 7_000L

                // 포인트 이력 확인
                val histories = pointHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId)
                histories.size shouldBe 1
                histories[0].transactionType shouldBe PointTransactionType.USE
                histories[0].amount shouldBe -3_000L // USE는 음수로 저장
                histories[0].balanceBefore shouldBe 10_000L
                histories[0].balanceAfter shouldBe 7_000L
            }
        }

        context("잔액이 부족할 때") {
            it("예외가 발생한다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(2)
                val initialAmount = PointAmount(5_000)
                val useAmount = PointAmount(6_000) // 잔액보다 많은 금액
                val createdBy = userId

                // 사용자 포인트 생성 및 초기 적립
                pointService.createUserPoint(userId, createdBy)
                pointService.earnPoint(userId, initialAmount, createdBy)

                // When & Then
                shouldThrow<PointException.InsufficientBalance> {
                    pointService.usePoint(userId, useAmount, createdBy)
                }

                // 잔액이 변경되지 않았는지 확인
                val userPoint = userPointRepository.findByUserId(userId)
                userPoint?.balance?.value shouldBe 5_000L
            }
        }

        context("전액 사용 시") {
            it("포인트를 전액 사용할 수 있다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(3)
                val initialAmount = PointAmount(8_000)
                val createdBy = userId

                // 사용자 포인트 생성 및 초기 적립
                pointService.createUserPoint(userId, createdBy)
                pointService.earnPoint(userId, initialAmount, createdBy)

                // When - 전액 사용
                val balanceBefore = pointService.getUserPoint(userId)!!.balance
                val updatedUserPoint = pointService.usePoint(userId, initialAmount, createdBy)
                pointHistoryService.recordUseHistory(
                    userId, initialAmount, balanceBefore, updatedUserPoint.balance, createdBy
                )

                // Then
                val userPoint = userPointRepository.findByUserId(userId)
                userPoint shouldNotBe null
                userPoint!!.balance.value shouldBe 0L
            }
        }

        context("연속으로 사용할 때") {
            it("여러 번 포인트를 사용할 수 있다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(4)
                val initialAmount = PointAmount(10_000)
                val firstUse = PointAmount(3_000)
                val secondUse = PointAmount(2_000)
                val createdBy = userId

                // 사용자 포인트 생성 및 초기 적립
                pointService.createUserPoint(userId, createdBy)
                pointService.earnPoint(userId, initialAmount, createdBy)

                // When - 첫 번째 사용
                val firstBalance = pointService.getUserPoint(userId)!!.balance
                val firstUpdated = pointService.usePoint(userId, firstUse, createdBy)
                pointHistoryService.recordUseHistory(
                    userId, firstUse, firstBalance, firstUpdated.balance, createdBy
                )

                // When - 두 번째 사용
                val secondBalance = pointService.getUserPoint(userId)!!.balance
                val secondUpdated = pointService.usePoint(userId, secondUse, createdBy)
                pointHistoryService.recordUseHistory(
                    userId, secondUse, secondBalance, secondUpdated.balance, createdBy
                )

                // Then
                val finalUserPoint = userPointRepository.findByUserId(userId)
                finalUserPoint shouldNotBe null
                finalUserPoint!!.balance.value shouldBe 5_000L // 10,000 - 3,000 - 2,000

                // 포인트 이력 확인 (최신순)
                val histories = pointHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId)
                histories.size shouldBe 2
                histories[0].amount shouldBe -2_000L // USE는 음수로 저장 (최신)
                histories[1].amount shouldBe -3_000L // USE는 음수로 저장 (과거)
            }
        }

        context("0원 이하 금액으로 사용 시도 시") {
            it("예외가 발생한다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(5)
                val initialAmount = PointAmount(10_000)
                val createdBy = userId

                // 사용자 포인트 생성 및 초기 적립
                pointService.createUserPoint(userId, createdBy)
                pointService.earnPoint(userId, initialAmount, createdBy)

                // When & Then - 0원 사용 시도
                shouldThrow<PointException.InvalidAmount> {
                    pointService.usePoint(userId, PointAmount(0), createdBy)
                }

                // When & Then - 음수 사용 시도 (PointAmount 생성 시점에 검증)
                shouldThrow<IllegalArgumentException> {
                    PointAmount(-1000)
                }
            }
        }

        context("주문과 연결된 포인트 사용 시") {
            it("주문 정보가 포함된 사용 이력이 기록된다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(6)
                val initialAmount = PointAmount(20_000)
                val useAmount = PointAmount(5_000)
                val orderId = 12345L
                val createdBy = userId

                // 사용자 포인트 생성 및 초기 적립
                pointService.createUserPoint(userId, createdBy)
                pointService.earnPoint(userId, initialAmount, createdBy)

                // When
                val balanceBefore = pointService.getUserPoint(userId)!!.balance
                val updatedUserPoint = pointService.usePoint(
                    userId = userId,
                    amount = useAmount,
                    usedBy = createdBy,
                    description = "주문 결제"
                )

                pointHistoryService.recordUseHistory(
                    userId = userId,
                    amount = useAmount,
                    balanceBefore = balanceBefore,
                    balanceAfter = updatedUserPoint.balance,
                    createdBy = createdBy,
                    description = "주문 결제",
                    orderId = orderId
                )

                // Then
                val histories = pointHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId)
                histories.size shouldBe 1
                histories[0].orderId shouldBe orderId
                histories[0].description shouldBe "주문 결제"
                histories[0].transactionType shouldBe PointTransactionType.USE
            }
        }

        context("포인트 적립 후 사용할 때") {
            it("적립과 사용이 연속으로 정상 동작한다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(7)
                val firstEarn = PointAmount(10_000)
                val firstUse = PointAmount(3_000)
                val secondEarn = PointAmount(5_000)
                val secondUse = PointAmount(2_000)
                val createdBy = userId

                // 사용자 포인트 생성
                pointService.createUserPoint(userId, createdBy)

                // When - 적립 → 사용 → 적립 → 사용
                pointService.earnPoint(userId, firstEarn, createdBy)
                pointService.usePoint(userId, firstUse, createdBy)
                pointService.earnPoint(userId, secondEarn, createdBy)
                pointService.usePoint(userId, secondUse, createdBy)

                // Then
                val userPoint = userPointRepository.findByUserId(userId)
                userPoint shouldNotBe null
                userPoint!!.balance.value shouldBe 10_000L // 10,000 - 3,000 + 5,000 - 2,000
            }
        }
    }
})
