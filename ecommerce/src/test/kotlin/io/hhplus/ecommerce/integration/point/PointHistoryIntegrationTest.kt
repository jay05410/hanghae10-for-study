package io.hhplus.ecommerce.integration.point

import io.hhplus.ecommerce.support.KotestIntegrationTestBase
import io.hhplus.ecommerce.point.usecase.PointCommandUseCase
import io.hhplus.ecommerce.point.usecase.GetPointQueryUseCase
import io.hhplus.ecommerce.point.domain.constant.PointTransactionType
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldBeSortedWith
import io.kotest.matchers.shouldBe

/**
 * 포인트 히스토리 조회 통합 테스트
 *
 * 목적:
 * - 포인트 히스토리 조회 검증
 * - 정렬 검증 (최신 거래가 먼저)
 * - 대량 데이터 조회 성능 검증
 *
 * 시나리오:
 * 1. 포인트 거래 여러 건 생성
 * 2. 히스토리 조회
 * 3. 최신순 정렬 확인
 * 4. 거래 타입별 필터링 (선택)
 */
class PointHistoryIntegrationTest(
    private val pointCommandUseCase: PointCommandUseCase,
    private val getPointQueryUseCase: GetPointQueryUseCase
) : KotestIntegrationTestBase({

    describe("포인트 히스토리 조회") {
        context("포인트 거래가 여러 번 발생했을 때") {
            it("모든 거래 내역을 최신순으로 조회할 수 있어야 한다") {
                // Given: 다양한 포인트 거래 생성
                val userId = 4000L

                // 충전 3회
                pointCommandUseCase.chargePoint(userId, 10000)
                Thread.sleep(10)
                pointCommandUseCase.chargePoint(userId, 20000)
                Thread.sleep(10)
                pointCommandUseCase.chargePoint(userId, 30000)
                Thread.sleep(10)

                // 사용 2회
                pointCommandUseCase.usePoint(userId, 5000)
                Thread.sleep(10)
                pointCommandUseCase.usePoint(userId, 10000)

                // When: 히스토리 조회
                val histories = getPointQueryUseCase.getPointHistories(userId)

                // Then: 총 5건 조회
                histories shouldHaveSize 5

                // 최신순 정렬 확인
                histories shouldBeSortedWith compareByDescending { it.createdAt }

                // 거래 타입 확인
                histories.count { it.transactionType == PointTransactionType.EARN } shouldBe 3
                histories.count { it.transactionType == PointTransactionType.USE } shouldBe 2

                // 금액 누적 확인 (balanceAfter)
                val latestHistory = histories.first()
                latestHistory.balanceAfter shouldBe (10000 + 20000 + 30000 - 5000 - 10000)
            }
        }

        context("대량의 포인트 거래가 있을 때") {
            it("성능 저하 없이 조회할 수 있어야 한다") {
                // Given: 대량 거래 생성
                val userId = 4001L
                val transactionCount = 100

                repeat(transactionCount / 2) {
                    pointCommandUseCase.chargePoint(userId, 1000)
                }

                repeat(transactionCount / 2) {
                    pointCommandUseCase.usePoint(userId, 500)
                }

                // When: 조회 시간 측정
                val startTime = System.currentTimeMillis()
                val histories = getPointQueryUseCase.getPointHistories(userId)
                val elapsedTime = System.currentTimeMillis() - startTime

                // Then: 조회 성공
                histories shouldHaveSize transactionCount

                // 성능 목표: 500ms 이내
                (elapsedTime < 500L) shouldBe true

                println("📊 대량 포인트 거래($transactionCount 건) 조회 시간: ${elapsedTime}ms")
            }
        }

        context("포인트 거래가 없는 사용자가 조회할 때") {
            it("빈 목록이 반환되어야 한다") {
                // Given: 거래 없는 사용자
                val userId = 4002L

                // When: 히스토리 조회
                val histories = getPointQueryUseCase.getPointHistories(userId)

                // Then: 빈 목록
                histories shouldHaveSize 0
            }
        }

        context("충전만 있는 사용자가 조회할 때") {
            it("충전 내역만 조회되어야 한다") {
                // Given: 충전만 5회
                val userId = 4003L

                repeat(5) {
                    pointCommandUseCase.chargePoint(userId, ((it + 1) * 1000).toLong())
                    Thread.sleep(10)
                }

                // When: 히스토리 조회
                val histories = getPointQueryUseCase.getPointHistories(userId)

                // Then: 충전 내역만 5건
                histories shouldHaveSize 5
                histories.all { it.transactionType == PointTransactionType.EARN } shouldBe true

                // 최신 거래가 먼저
                histories shouldBeSortedWith compareByDescending { it.createdAt }

                // 금액 확인
                histories.sumOf { it.amount.toLong() } shouldBe (1000L + 2000L + 3000L + 4000L + 5000L)
            }
        }

        context("사용만 있는 사용자가 조회할 때") {
            it("사용 내역만 조회되어야 한다") {
                // Given: 충전 후 사용
                val userId = 4004L

                pointCommandUseCase.chargePoint(userId, 50000)
                Thread.sleep(10)

                // 사용만 3회
                repeat(3) {
                    pointCommandUseCase.usePoint(userId, ((it + 1) * 1000).toLong())
                    Thread.sleep(10)
                }

                // When: 히스토리 조회
                val histories = getPointQueryUseCase.getPointHistories(userId)

                // Then: 총 4건 (충전 1 + 사용 3)
                histories shouldHaveSize 4

                // 사용 내역 필터링
                val useHistories = histories.filter { it.transactionType == PointTransactionType.USE }
                useHistories shouldHaveSize 3

                // 사용 금액 확인 (음수)
                useHistories.all { history -> history.amount < 0 } shouldBe true
            }
        }

        context("잔액 변화 추적") {
            it("각 거래마다 balanceBefore와 balanceAfter가 정확해야 한다") {
                // Given
                val userId = 4005L

                pointCommandUseCase.chargePoint(userId, 10000)
                Thread.sleep(10)
                pointCommandUseCase.usePoint(userId, 3000)
                Thread.sleep(10)
                pointCommandUseCase.chargePoint(userId, 5000)
                Thread.sleep(10)
                pointCommandUseCase.usePoint(userId, 2000)

                // When
                val histories = getPointQueryUseCase.getPointHistories(userId)

                // Then: 잔액 연속성 확인
                histories shouldHaveSize 4

                // 최신순이므로 역순으로 확인
                val chronologicalHistories = histories.reversed()

                // 첫 번째 거래: 0 → 10000
                chronologicalHistories[0].balanceBefore shouldBe 0
                chronologicalHistories[0].balanceAfter shouldBe 10000

                // 두 번째 거래: 10000 → 7000
                chronologicalHistories[1].balanceBefore shouldBe 10000
                chronologicalHistories[1].balanceAfter shouldBe 7000

                // 세 번째 거래: 7000 → 12000
                chronologicalHistories[2].balanceBefore shouldBe 7000
                chronologicalHistories[2].balanceAfter shouldBe 12000

                // 네 번째 거래: 12000 → 10000
                chronologicalHistories[3].balanceBefore shouldBe 12000
                chronologicalHistories[3].balanceAfter shouldBe 10000

                // 최종 잔액 확인
                val currentBalance = getPointQueryUseCase.getUserPoint(userId).balance.value
                currentBalance shouldBe 10000L
            }
        }
    }
})
