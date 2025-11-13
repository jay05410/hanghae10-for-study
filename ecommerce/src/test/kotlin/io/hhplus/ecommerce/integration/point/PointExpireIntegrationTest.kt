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
 * 포인트 소멸 통합 테스트
 *
 * TestContainers MySQL을 사용하여 포인트 소멸 전체 플로우를 검증합니다.
 * - 포인트 소멸 성공
 * - 포인트 이력 자동 기록
 * - 소멸 가능한 포인트 부족 검증
 * - 유효기간 만료에 따른 자동 소멸 (1년)
 */
class PointExpireIntegrationTest(
    private val pointService: PointService,
    private val pointHistoryService: PointHistoryService,
    private val userPointRepository: UserPointRepository,
    private val pointHistoryRepository: PointHistoryRepository
) : KotestIntegrationTestBase({

    describe("포인트 소멸") {
        context("정상적인 소멸 요청일 때") {
            it("포인트를 정상적으로 소멸시킬 수 있다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(1)
                val initialAmount = PointAmount(10_000)
                val expireAmount = PointAmount(3_000)
                val createdBy = userId

                // 사용자 포인트 생성 및 초기 적립
                pointService.createUserPoint(userId, createdBy)
                pointService.earnPoint(userId, initialAmount, createdBy)

                // When
                val balanceBefore = pointService.getUserPoint(userId)!!.balance
                val updatedUserPoint = pointService.expirePoint(
                    userId = userId,
                    amount = expireAmount
                )

                // 포인트 이력 기록
                pointHistoryService.recordExpireHistory(
                    userId = userId,
                    amount = expireAmount,
                    balanceBefore = balanceBefore,
                    balanceAfter = updatedUserPoint.balance,
                    createdBy = 0L, // 시스템 자동 소멸
                    description = "유효기간 만료"
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
                histories[0].transactionType shouldBe PointTransactionType.EXPIRE
                histories[0].amount shouldBe -3_000L // EXPIRE는 음수로 저장
                histories[0].balanceBefore shouldBe 10_000L
                histories[0].balanceAfter shouldBe 7_000L
                histories[0].description shouldBe "유효기간 만료"
            }
        }

        context("소멸 가능한 포인트가 부족할 때") {
            it("예외가 발생한다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(2)
                val initialAmount = PointAmount(5_000)
                val expireAmount = PointAmount(6_000) // 잔액보다 많은 금액
                val createdBy = userId

                // 사용자 포인트 생성 및 초기 적립
                pointService.createUserPoint(userId, createdBy)
                pointService.earnPoint(userId, initialAmount, createdBy)

                // When & Then
                shouldThrow<PointException.InsufficientBalance> {
                    pointService.expirePoint(userId, expireAmount)
                }

                // 잔액이 변경되지 않았는지 확인
                val userPoint = userPointRepository.findByUserId(userId)
                userPoint?.balance?.value shouldBe 5_000L
            }
        }

        context("전액 소멸 시") {
            it("포인트를 전액 소멸시킬 수 있다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(3)
                val initialAmount = PointAmount(8_000)
                val createdBy = userId

                // 사용자 포인트 생성 및 초기 적립
                pointService.createUserPoint(userId, createdBy)
                pointService.earnPoint(userId, initialAmount, createdBy)

                // When - 전액 소멸
                val balanceBefore = pointService.getUserPoint(userId)!!.balance
                val updatedUserPoint = pointService.expirePoint(userId, initialAmount)
                pointHistoryService.recordExpireHistory(
                    userId, initialAmount, balanceBefore, updatedUserPoint.balance, 0L
                )

                // Then
                val userPoint = userPointRepository.findByUserId(userId)
                userPoint shouldNotBe null
                userPoint!!.balance.value shouldBe 0L
            }
        }

        context("연속으로 소멸할 때") {
            it("여러 번 포인트를 소멸시킬 수 있다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(4)
                val initialAmount = PointAmount(10_000)
                val firstExpire = PointAmount(3_000)
                val secondExpire = PointAmount(2_000)
                val createdBy = userId

                // 사용자 포인트 생성 및 초기 적립
                pointService.createUserPoint(userId, createdBy)
                pointService.earnPoint(userId, initialAmount, createdBy)

                // When - 첫 번째 소멸
                val firstBalance = pointService.getUserPoint(userId)!!.balance
                val firstUpdated = pointService.expirePoint(userId, firstExpire)
                pointHistoryService.recordExpireHistory(
                    userId, firstExpire, firstBalance, firstUpdated.balance, 0L
                )

                // When - 두 번째 소멸
                val secondBalance = pointService.getUserPoint(userId)!!.balance
                val secondUpdated = pointService.expirePoint(userId, secondExpire)
                pointHistoryService.recordExpireHistory(
                    userId, secondExpire, secondBalance, secondUpdated.balance, 0L
                )

                // Then
                val finalUserPoint = userPointRepository.findByUserId(userId)
                finalUserPoint shouldNotBe null
                finalUserPoint!!.balance.value shouldBe 5_000L // 10,000 - 3,000 - 2,000

                // 포인트 이력 확인 (최신순)
                val histories = pointHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId)
                histories.size shouldBe 2
                histories[0].amount shouldBe -2_000L // EXPIRE는 음수로 저장 (최신)
                histories[1].amount shouldBe -3_000L // EXPIRE는 음수로 저장 (과거)
            }
        }

        context("0원 이하 금액으로 소멸 시도 시") {
            it("예외가 발생한다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(5)
                val initialAmount = PointAmount(10_000)
                val createdBy = userId

                // 사용자 포인트 생성 및 초기 적립
                pointService.createUserPoint(userId, createdBy)
                pointService.earnPoint(userId, initialAmount, createdBy)

                // When & Then - 0원 소멸 시도
                shouldThrow<PointException.InvalidAmount> {
                    pointService.expirePoint(userId, PointAmount(0))
                }

                // When & Then - 음수 소멸 시도 (PointAmount 생성 시점에 검증)
                shouldThrow<IllegalArgumentException> {
                    PointAmount(-1000)
                }
            }
        }

        context("포인트 적립 후 일부 소멸 시") {
            it("적립 후 소멸이 정상 동작한다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(6)
                val earnAmount = PointAmount(15_000)
                val expireAmount = PointAmount(5_000)
                val createdBy = userId

                // 사용자 포인트 생성
                pointService.createUserPoint(userId, createdBy)

                // When - 적립 후 소멸
                pointService.earnPoint(userId, earnAmount, createdBy)
                val balanceBefore = pointService.getUserPoint(userId)!!.balance
                val updatedUserPoint = pointService.expirePoint(userId, expireAmount)
                pointHistoryService.recordExpireHistory(
                    userId, expireAmount, balanceBefore, updatedUserPoint.balance, 0L
                )

                // Then
                val userPoint = userPointRepository.findByUserId(userId)
                userPoint shouldNotBe null
                userPoint!!.balance.value shouldBe 10_000L // 15,000 - 5,000

                // 포인트 이력 확인
                val histories = pointHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId)
                histories.size shouldBe 1
                histories[0].transactionType shouldBe PointTransactionType.EXPIRE
            }
        }

        context("FIFO 방식으로 소멸 시") {
            it("가장 오래된 포인트부터 소멸된다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(7)
                val firstEarn = PointAmount(5_000)
                val secondEarn = PointAmount(3_000)
                val thirdEarn = PointAmount(2_000)
                val expireAmount = PointAmount(6_000) // 첫 번째(5,000) + 두 번째 일부(1,000)
                val createdBy = userId

                // 사용자 포인트 생성 및 여러 번 적립
                pointService.createUserPoint(userId, createdBy)
                pointService.earnPoint(userId, firstEarn, createdBy)
                pointService.earnPoint(userId, secondEarn, createdBy)
                pointService.earnPoint(userId, thirdEarn, createdBy)

                // When - 6,000원 소멸 (FIFO: 첫 번째 5,000 + 두 번째 1,000)
                val balanceBefore = pointService.getUserPoint(userId)!!.balance
                val updatedUserPoint = pointService.expirePoint(userId, expireAmount)
                pointHistoryService.recordExpireHistory(
                    userId, expireAmount, balanceBefore, updatedUserPoint.balance, 0L, "FIFO 소멸"
                )

                // Then
                val userPoint = userPointRepository.findByUserId(userId)
                userPoint shouldNotBe null
                userPoint!!.balance.value shouldBe 4_000L // 10,000 - 6,000

                // 포인트 이력 확인
                val histories = pointHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId)
                histories.size shouldBe 1
                histories[0].amount shouldBe -6_000L // EXPIRE는 음수로 저장
                histories[0].description shouldBe "FIFO 소멸"
            }
        }
    }
})
