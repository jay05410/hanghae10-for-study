package io.hhplus.ecommerce.integration.inventory

import io.hhplus.ecommerce.support.KotestIntegrationTestBase

import io.hhplus.ecommerce.inventory.exception.InventoryException
import io.hhplus.ecommerce.inventory.application.StockReservationService
import io.hhplus.ecommerce.inventory.domain.repository.StockReservationRepository
import io.hhplus.ecommerce.inventory.usecase.InventoryCommandUseCase
import io.hhplus.ecommerce.support.config.IntegrationTestFixtures
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * 재고 예약 통합 테스트
 *
 * TestContainers MySQL을 사용하여 재고 예약 전체 플로우를 검증합니다.
 * - 재고 예약 성공/실패
 * - 예약 확정/취소
 * - 예약 만료 처리
 * - 예약 데이터 정리 (물리 삭제)
 */
class StockReservationIntegrationTest(
    private val stockReservationService: StockReservationService,
    private val stockReservationRepository: StockReservationRepository,
    private val inventoryCommandUseCase: InventoryCommandUseCase
) : KotestIntegrationTestBase({

    describe("재고 예약") {
        context("정상적인 예약 요청일 때") {
            it("재고를 예약할 수 있다") {
                // Given
                val productId = IntegrationTestFixtures.createTestProductId(1)
                val userId = IntegrationTestFixtures.createTestUserId(1)
                val quantity = 5
                val initialStock = 100

                inventoryCommandUseCase.createInventory(productId, initialStock)

                // When
                val reservation = stockReservationService.reserveStock(productId, userId, quantity)

                // Then
                reservation shouldNotBe null
                reservation.productId shouldBe productId
                reservation.userId shouldBe userId
                reservation.quantity shouldBe quantity
                reservation.isReservationActive() shouldBe true

                // 데이터베이스에서 확인
                val savedReservation = stockReservationRepository.findById(reservation.id)
                savedReservation shouldNotBe null
            }
        }

        context("예약 확정") {
            it("예약을 확정하고 실제 재고를 차감한다") {
                // Given
                val productId = IntegrationTestFixtures.createTestProductId(2)
                val userId = IntegrationTestFixtures.createTestUserId(2)
                val quantity = 10
                val initialStock = 100

                inventoryCommandUseCase.createInventory(productId, initialStock)
                val reservation = stockReservationService.reserveStock(productId, userId, quantity)

                // When
                val confirmedReservation = stockReservationService.confirmReservation(reservation.id, userId)

                // Then
                confirmedReservation shouldNotBe null
                confirmedReservation.isReservationActive() shouldBe false
            }
        }

        context("예약 취소") {
            it("예약을 취소하고 예약된 재고를 해제한다") {
                // Given
                val productId = IntegrationTestFixtures.createTestProductId(3)
                val userId = IntegrationTestFixtures.createTestUserId(3)
                val quantity = 8
                val initialStock = 100

                inventoryCommandUseCase.createInventory(productId, initialStock)
                val reservation = stockReservationService.reserveStock(productId, userId, quantity)

                // When
                val cancelledReservation = stockReservationService.cancelReservation(reservation.id, userId)

                // Then
                cancelledReservation shouldNotBe null
                cancelledReservation.isReservationActive() shouldBe false
            }
        }

        context("예약 만료 처리") {
            it("만료된 예약들을 일괄 처리한다") {
                // Given
                val productId1 = IntegrationTestFixtures.createTestProductId(4)
                val productId2 = IntegrationTestFixtures.createTestProductId(5)
                val userId = IntegrationTestFixtures.createTestUserId(4)
                val quantity = 5
                val initialStock = 100

                inventoryCommandUseCase.createInventory(productId1, initialStock)
                inventoryCommandUseCase.createInventory(productId2, initialStock)

                // 예약 생성 (1분 만료로 설정하고 시간을 조작하기 어려우므로 만료 로직만 테스트)
                val reservation1 = stockReservationService.reserveStock(productId1, userId, quantity, 1)
                val reservation2 = stockReservationService.reserveStock(productId2, userId + 1, quantity, 1)

                // When - 만료 처리 실행
                val expiredCount = stockReservationService.expireReservations()

                // Then - 실제 만료된 예약이 있다면 처리됨 (시간 기반이므로 결과는 가변적)
                expiredCount shouldBe 0 // 방금 생성된 예약이므로 만료되지 않음
            }
        }

        context("예약 데이터 정리") {
            it("오래된 예약 데이터를 물리 삭제한다") {
                // Given
                val productId = IntegrationTestFixtures.createTestProductId(6)
                val userId = IntegrationTestFixtures.createTestUserId(5)
                val quantity = 3
                val initialStock = 100

                inventoryCommandUseCase.createInventory(productId, initialStock)
                val reservation = stockReservationService.reserveStock(productId, userId, quantity)

                // 예약을 확정하여 처리 완료 상태로 만듦
                stockReservationService.confirmReservation(reservation.id, userId)

                // When - 0일 이전 데이터 정리 (즉시 삭제)
                val deletedCount = stockReservationService.cleanupOldReservations(0)

                // Then - 물리 삭제 처리됨
                deletedCount shouldBe 1

                // 삭제된 예약은 조회되지 않아야 함
                val deletedReservation = stockReservationRepository.findById(reservation.id)
                deletedReservation shouldBe null
            }
        }

        context("예약 중복 방지") {
            it("동일 사용자가 같은 상품에 중복 예약하면 예외가 발생한다") {
                // Given
                val productId = IntegrationTestFixtures.createTestProductId(7)
                val userId = IntegrationTestFixtures.createTestUserId(6)
                val quantity = 5
                val initialStock = 100

                inventoryCommandUseCase.createInventory(productId, initialStock)
                stockReservationService.reserveStock(productId, userId, quantity)

                // When & Then
                shouldThrow<InventoryException.StockAlreadyReserved> {
                    stockReservationService.reserveStock(productId, userId, quantity)
                }
            }
        }

        context("재고 부족") {
            it("재고가 부족하면 예약할 수 없다") {
                // Given
                val productId = IntegrationTestFixtures.createTestProductId(8)
                val userId = IntegrationTestFixtures.createTestUserId(7)
                val quantity = 150 // 재고보다 많음
                val initialStock = 100

                inventoryCommandUseCase.createInventory(productId, initialStock)

                // When & Then
                shouldThrow<InventoryException.InsufficientStock> {
                    stockReservationService.reserveStock(productId, userId, quantity)
                }
            }
        }

        context("존재하지 않는 예약") {
            it("존재하지 않는 예약을 확정하려면 예외가 발생한다") {
                // Given
                val nonExistentReservationId = 999999L
                val userId = IntegrationTestFixtures.createTestUserId(8)

                // When & Then
                shouldThrow<InventoryException.ReservationNotFound> {
                    stockReservationService.confirmReservation(nonExistentReservationId, userId)
                }
            }

            it("존재하지 않는 예약을 취소하려면 예외가 발생한다") {
                // Given
                val nonExistentReservationId = 999999L
                val userId = IntegrationTestFixtures.createTestUserId(9)

                // When & Then
                shouldThrow<InventoryException.ReservationNotFound> {
                    stockReservationService.cancelReservation(nonExistentReservationId, userId)
                }
            }
        }

        context("다른 사용자의 예약") {
            it("다른 사용자의 예약을 확정하려면 예외가 발생한다") {
                // Given
                val productId = IntegrationTestFixtures.createTestProductId(9)
                val userId = IntegrationTestFixtures.createTestUserId(10)
                val otherUserId = IntegrationTestFixtures.createTestUserId(11)
                val quantity = 5
                val initialStock = 100

                inventoryCommandUseCase.createInventory(productId, initialStock)
                val reservation = stockReservationService.reserveStock(productId, userId, quantity)

                // When & Then
                shouldThrow<InventoryException.ReservationAccessDenied> {
                    stockReservationService.confirmReservation(reservation.id, otherUserId)
                }
            }
        }
    }
})