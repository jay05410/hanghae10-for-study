package io.hhplus.ecommerce.unit.inventory.application

import io.hhplus.ecommerce.inventory.domain.entity.StockReservation
import io.hhplus.ecommerce.inventory.domain.entity.Inventory
import io.hhplus.ecommerce.inventory.domain.repository.StockReservationRepository
import io.hhplus.ecommerce.inventory.domain.repository.InventoryRepository
import io.hhplus.ecommerce.inventory.domain.constant.ReservationStatus
import io.hhplus.ecommerce.common.exception.inventory.InventoryException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.time.LocalDateTime

/**
 * StockReservationService 단위 테스트
 *
 * 책임: 재고 예약 서비스의 핵심 비즈니스 로직 검증
 * - 재고 예약, 확정, 취소 기능 검증
 * - Repository와의 상호작용 검증
 */
class StockReservationServiceTest : DescribeSpec({
    val mockStockReservationRepository = mockk<StockReservationRepository>()
    val mockInventoryRepository = mockk<InventoryRepository>()
    val sut = StockReservationService(mockStockReservationRepository, mockInventoryRepository)

    fun createMockStockReservation(
        id: Long = 1L,
        productId: Long = 1L,
        userId: Long = 1L,
        quantity: Int = 5,
        status: ReservationStatus = ReservationStatus.RESERVED
    ): StockReservation = mockk(relaxed = true) {
        every { this@mockk.id } returns id
        every { this@mockk.productId } returns productId
        every { this@mockk.userId } returns userId
        every { this@mockk.quantity } returns quantity
        every { this@mockk.status } returns status
        every { isReservationActive() } returns (status == ReservationStatus.RESERVED)
        every { confirm(any()) } just Runs
        every { cancel(any()) } just Runs
        every { expire(any()) } just Runs
        every { createdAt } returns LocalDateTime.now()
        every { expiresAt } returns LocalDateTime.now().plusMinutes(20)
    }

    fun createMockInventory(
        id: Long = 1L,
        productId: Long = 1L,
        quantity: Int = 100,
        reservedQuantity: Int = 0
    ): Inventory {
        val mockInventory = mockk<Inventory>(relaxed = true)
        every { mockInventory.id } returns id
        every { mockInventory.productId } returns productId
        every { mockInventory.quantity } returns quantity
        every { mockInventory.reservedQuantity } returns reservedQuantity
        every { mockInventory.reserve(any(), any()) } just runs
        every { mockInventory.releaseReservation(any(), any()) } just runs
        every { mockInventory.confirmReservation(any(), any()) } just runs
        return mockInventory
    }

    beforeEach {
        clearMocks(mockStockReservationRepository, mockInventoryRepository)
    }

    describe("reserveStock") {
        context("정상적인 재고 예약") {
            it("재고를 예약하고 예약 기록을 생성") {
                val productId = 1L
                val userId = 1L
                val quantity = 5
                val mockInventory = createMockInventory(productId = productId)
                val mockReservation = createMockStockReservation(productId = productId, userId = userId, quantity = quantity)

                every { mockStockReservationRepository.findByUserIdAndProductIdAndStatus(userId, productId, ReservationStatus.RESERVED) } returns null
                every { mockInventoryRepository.findByProductIdWithLock(productId) } returns mockInventory
                every { mockInventoryRepository.save(mockInventory) } returns mockInventory
                every { mockStockReservationRepository.save(any()) } returns mockReservation

                mockkObject(StockReservation.Companion)
                every { StockReservation.create(productId, userId, quantity, 20, userId) } returns mockReservation

                val result = sut.reserveStock(productId, userId, quantity)

                result shouldBe mockReservation
                verify(exactly = 1) { mockInventoryRepository.findByProductIdWithLock(productId) }
                verify(exactly = 1) { mockInventory.reserve(quantity, userId) }
                verify(exactly = 1) { mockInventoryRepository.save(mockInventory) }
                verify(exactly = 1) { StockReservation.create(productId, userId, quantity, 20, userId) }
                verify(exactly = 1) { mockStockReservationRepository.save(any()) }
            }
        }

        context("이미 예약이 있는 경우") {
            it("StockAlreadyReserved 예외를 발생") {
                val productId = 1L
                val userId = 1L
                val quantity = 5
                val existingReservation = createMockStockReservation(productId = productId, userId = userId)

                every { mockStockReservationRepository.findByUserIdAndProductIdAndStatus(userId, productId, ReservationStatus.RESERVED) } returns existingReservation

                shouldThrow<InventoryException.StockAlreadyReserved> {
                    sut.reserveStock(productId, userId, quantity)
                }

                verify(exactly = 1) { mockStockReservationRepository.findByUserIdAndProductIdAndStatus(userId, productId, ReservationStatus.RESERVED) }
                verify(exactly = 0) { mockInventoryRepository.findByProductIdWithLock(any()) }
            }
        }

        context("만료된 예약이 존재하는 경우") {
            it("새로운 예약을 생성") {
                val productId = 1L
                val userId = 1L
                val quantity = 5
                val expiredReservation = createMockStockReservation(productId = productId, userId = userId).apply {
                    every { isReservationActive() } returns false
                }
                val mockInventory = createMockInventory(productId = productId)
                val newReservation = createMockStockReservation(productId = productId, userId = userId, quantity = quantity)

                every { mockStockReservationRepository.findByUserIdAndProductIdAndStatus(userId, productId, ReservationStatus.RESERVED) } returns expiredReservation
                every { mockInventoryRepository.findByProductIdWithLock(productId) } returns mockInventory
                every { mockInventoryRepository.save(mockInventory) } returns mockInventory
                every { mockStockReservationRepository.save(any()) } returns newReservation

                mockkObject(StockReservation.Companion)
                every { StockReservation.create(productId, userId, quantity, 20, userId) } returns newReservation

                val result = sut.reserveStock(productId, userId, quantity)

                result shouldBe newReservation
                verify(exactly = 1) { mockStockReservationRepository.findByUserIdAndProductIdAndStatus(userId, productId, ReservationStatus.RESERVED) }
                verify(exactly = 1) { expiredReservation.isReservationActive() }
                verify(exactly = 1) { mockInventoryRepository.findByProductIdWithLock(productId) }
            }
        }

        context("재고가 존재하지 않는 경우") {
            it("InventoryNotFound 예외를 발생") {
                val productId = 999L
                val userId = 1L
                val quantity = 5

                every { mockStockReservationRepository.findByUserIdAndProductIdAndStatus(userId, productId, ReservationStatus.RESERVED) } returns null
                every { mockInventoryRepository.findByProductIdWithLock(productId) } returns null

                shouldThrow<InventoryException.InventoryNotFound> {
                    sut.reserveStock(productId, userId, quantity)
                }

                verify(exactly = 1) { mockInventoryRepository.findByProductIdWithLock(productId) }
                verify(exactly = 0) { mockStockReservationRepository.save(any()) }
            }
        }
    }

    describe("confirmReservation") {
        context("정상적인 예약 확정") {
            it("예약을 확정하고 재고에서 실제 차감") {
                val reservationId = 1L
                val userId = 1L
                val productId = 1L
                val quantity = 5
                val mockReservation = createMockStockReservation(id = reservationId, userId = userId, productId = productId, quantity = quantity)
                val mockInventory = createMockInventory(productId = productId)

                every { mockStockReservationRepository.findById(reservationId) } returns mockReservation
                every { mockInventoryRepository.findByProductIdWithLock(productId) } returns mockInventory
                every { mockInventoryRepository.save(mockInventory) } returns mockInventory
                every { mockStockReservationRepository.save(mockReservation) } returns mockReservation

                val result = sut.confirmReservation(reservationId, userId)

                result shouldBe mockReservation
                verify(exactly = 1) { mockStockReservationRepository.findById(reservationId) }
                verify(exactly = 1) { mockInventory.confirmReservation(quantity, userId) }
                verify(exactly = 1) { mockReservation.confirm(userId) }
                verify(exactly = 1) { mockStockReservationRepository.save(mockReservation) }
            }
        }

        context("예약이 존재하지 않는 경우") {
            it("ReservationNotFound 예외를 발생") {
                val reservationId = 999L
                val userId = 1L

                every { mockStockReservationRepository.findById(reservationId) } returns null

                shouldThrow<InventoryException.ReservationNotFound> {
                    sut.confirmReservation(reservationId, userId)
                }

                verify(exactly = 1) { mockStockReservationRepository.findById(reservationId) }
                verify(exactly = 0) { mockInventoryRepository.findByProductIdWithLock(any()) }
            }
        }

        context("다른 사용자의 예약을 확정하려는 경우") {
            it("ReservationAccessDenied 예외를 발생") {
                val reservationId = 1L
                val userId = 1L
                val otherUserId = 2L
                val mockReservation = createMockStockReservation(id = reservationId, userId = otherUserId)

                every { mockStockReservationRepository.findById(reservationId) } returns mockReservation

                shouldThrow<InventoryException.ReservationAccessDenied> {
                    sut.confirmReservation(reservationId, userId)
                }

                verify(exactly = 1) { mockStockReservationRepository.findById(reservationId) }
                verify(exactly = 0) { mockInventoryRepository.findByProductIdWithLock(any()) }
            }
        }

        context("만료된 예약을 확정하려는 경우") {
            it("ReservationExpired 예외를 발생") {
                val reservationId = 1L
                val userId = 1L
                val mockReservation = createMockStockReservation(id = reservationId, userId = userId).apply {
                    every { isReservationActive() } returns false
                }

                every { mockStockReservationRepository.findById(reservationId) } returns mockReservation

                shouldThrow<InventoryException.ReservationExpired> {
                    sut.confirmReservation(reservationId, userId)
                }

                verify(exactly = 1) { mockStockReservationRepository.findById(reservationId) }
                verify(exactly = 1) { mockReservation.isReservationActive() }
                verify(exactly = 0) { mockInventoryRepository.findByProductIdWithLock(any()) }
            }
        }

        context("재고가 존재하지 않는 경우") {
            it("InventoryNotFound 예외를 발생") {
                val reservationId = 1L
                val userId = 1L
                val productId = 1L
                val mockReservation = createMockStockReservation(id = reservationId, userId = userId, productId = productId)

                every { mockStockReservationRepository.findById(reservationId) } returns mockReservation
                every { mockInventoryRepository.findByProductIdWithLock(productId) } returns null

                shouldThrow<InventoryException.InventoryNotFound> {
                    sut.confirmReservation(reservationId, userId)
                }

                verify(exactly = 1) { mockStockReservationRepository.findById(reservationId) }
                verify(exactly = 1) { mockInventoryRepository.findByProductIdWithLock(productId) }
                verify(exactly = 0) { mockStockReservationRepository.save(any()) }
            }
        }
    }

    describe("cancelReservation") {
        context("정상적인 예약 취소") {
            it("예약을 취소하고 재고 예약을 해제") {
                val reservationId = 1L
                val userId = 1L
                val productId = 1L
                val quantity = 5
                val mockReservation = createMockStockReservation(id = reservationId, userId = userId, productId = productId, quantity = quantity, status = ReservationStatus.RESERVED)
                val mockInventory = createMockInventory(productId = productId)

                every { mockStockReservationRepository.findById(reservationId) } returns mockReservation
                every { mockInventoryRepository.findByProductIdWithLock(productId) } returns mockInventory
                every { mockInventoryRepository.save(mockInventory) } returns mockInventory
                every { mockStockReservationRepository.save(mockReservation) } returns mockReservation

                val result = sut.cancelReservation(reservationId, userId)

                result shouldBe mockReservation
                verify(exactly = 1) { mockStockReservationRepository.findById(reservationId) }
                verify(exactly = 1) { mockInventory.releaseReservation(quantity, userId) }
                verify(exactly = 1) { mockReservation.cancel(userId) }
                verify(exactly = 1) { mockStockReservationRepository.save(mockReservation) }
            }
        }

        context("취소할 수 없는 상태의 예약") {
            it("ReservationCannotBeCancelled 예외를 발생") {
                val reservationId = 1L
                val userId = 1L
                val mockReservation = createMockStockReservation(id = reservationId, userId = userId, status = ReservationStatus.CONFIRMED)

                every { mockStockReservationRepository.findById(reservationId) } returns mockReservation

                shouldThrow<InventoryException.ReservationCannotBeCancelled> {
                    sut.cancelReservation(reservationId, userId)
                }

                verify(exactly = 1) { mockStockReservationRepository.findById(reservationId) }
                verify(exactly = 0) { mockInventoryRepository.findByProductIdWithLock(any()) }
            }
        }

        context("존재하지 않는 예약 취소") {
            it("ReservationNotFound 예외를 발생") {
                val reservationId = 999L
                val userId = 1L

                every { mockStockReservationRepository.findById(reservationId) } returns null

                shouldThrow<InventoryException.ReservationNotFound> {
                    sut.cancelReservation(reservationId, userId)
                }

                verify(exactly = 1) { mockStockReservationRepository.findById(reservationId) }
                verify(exactly = 0) { mockInventoryRepository.findByProductIdWithLock(any()) }
            }
        }

        context("다른 사용자의 예약을 취소하려는 경우") {
            it("ReservationAccessDenied 예외를 발생") {
                val reservationId = 1L
                val userId = 1L
                val otherUserId = 2L
                val mockReservation = createMockStockReservation(id = reservationId, userId = otherUserId, status = ReservationStatus.RESERVED)

                every { mockStockReservationRepository.findById(reservationId) } returns mockReservation

                shouldThrow<InventoryException.ReservationAccessDenied> {
                    sut.cancelReservation(reservationId, userId)
                }

                verify(exactly = 1) { mockStockReservationRepository.findById(reservationId) }
                verify(exactly = 0) { mockInventoryRepository.findByProductIdWithLock(any()) }
            }
        }

        context("재고가 존재하지 않는 경우") {
            it("InventoryNotFound 예외를 발생") {
                val reservationId = 1L
                val userId = 1L
                val productId = 1L
                val mockReservation = createMockStockReservation(id = reservationId, userId = userId, productId = productId, status = ReservationStatus.RESERVED)

                every { mockStockReservationRepository.findById(reservationId) } returns mockReservation
                every { mockInventoryRepository.findByProductIdWithLock(productId) } returns null

                shouldThrow<InventoryException.InventoryNotFound> {
                    sut.cancelReservation(reservationId, userId)
                }

                verify(exactly = 1) { mockStockReservationRepository.findById(reservationId) }
                verify(exactly = 1) { mockInventoryRepository.findByProductIdWithLock(productId) }
                verify(exactly = 0) { mockStockReservationRepository.save(any()) }
            }
        }
    }

    describe("expireReservations") {
        context("만료된 예약들이 있는 경우") {
            it("만료된 예약들을 처리하고 개수를 반환") {
                val now = LocalDateTime.now()
                val expiredReservation1 = createMockStockReservation(id = 1L, productId = 1L)
                val expiredReservation2 = createMockStockReservation(id = 2L, productId = 2L)
                val expiredReservations = listOf(expiredReservation1, expiredReservation2)

                val mockInventory1 = createMockInventory(productId = 1L)
                val mockInventory2 = createMockInventory(productId = 2L)

                every { mockStockReservationRepository.findExpiredReservations(any()) } returns expiredReservations
                every { mockInventoryRepository.findByProductIdWithLock(1L) } returns mockInventory1
                every { mockInventoryRepository.findByProductIdWithLock(2L) } returns mockInventory2
                every { mockInventoryRepository.save(any()) } returns mockInventory1 andThen mockInventory2
                every { mockStockReservationRepository.save(any()) } returns expiredReservation1 andThen expiredReservation2

                val result = sut.expireReservations()

                result shouldBe 2
                verify(exactly = 1) { mockStockReservationRepository.findExpiredReservations(any()) }
                verify(exactly = 1) { mockInventory1.releaseReservation(5, -1L) }
                verify(exactly = 1) { mockInventory2.releaseReservation(5, -1L) }
                verify(exactly = 1) { expiredReservation1.expire(-1L) }
                verify(exactly = 1) { expiredReservation2.expire(-1L) }
            }
        }

        context("재고가 존재하지 않는 만료된 예약") {
            it("재고 해제 없이 예약만 만료 처리") {
                val expiredReservation = createMockStockReservation(id = 1L, productId = 999L)
                val expiredReservations = listOf(expiredReservation)

                every { mockStockReservationRepository.findExpiredReservations(any()) } returns expiredReservations
                every { mockInventoryRepository.findByProductIdWithLock(999L) } returns null
                every { mockStockReservationRepository.save(expiredReservation) } returns expiredReservation

                val result = sut.expireReservations()

                result shouldBe 1
                verify(exactly = 1) { mockStockReservationRepository.findExpiredReservations(any()) }
                verify(exactly = 1) { mockInventoryRepository.findByProductIdWithLock(999L) }
                verify(exactly = 1) { expiredReservation.expire(-1L) }
                verify(exactly = 1) { mockStockReservationRepository.save(expiredReservation) }
            }
        }

        context("만료된 예약이 없는 경우") {
            it("0을 반환") {
                every { mockStockReservationRepository.findExpiredReservations(any()) } returns emptyList()

                val result = sut.expireReservations()

                result shouldBe 0
                verify(exactly = 1) { mockStockReservationRepository.findExpiredReservations(any()) }
                verify(exactly = 0) { mockInventoryRepository.findByProductIdWithLock(any()) }
            }
        }
    }

    describe("getUserReservations") {
        context("사용자의 활성 예약 조회") {
            it("활성 상태인 예약만 필터링하여 반환") {
                val userId = 1L
                val activeReservation = createMockStockReservation(id = 1L, userId = userId, status = ReservationStatus.RESERVED)
                val reservations = listOf(activeReservation)

                every { mockStockReservationRepository.findByUserIdAndStatus(userId, ReservationStatus.RESERVED) } returns reservations

                val result = sut.getUserReservations(userId)

                result shouldBe reservations
                verify(exactly = 1) { mockStockReservationRepository.findByUserIdAndStatus(userId, ReservationStatus.RESERVED) }
            }
        }

        context("사용자의 예약이 없는 경우") {
            it("빈 목록을 반환") {
                val userId = 999L

                every { mockStockReservationRepository.findByUserIdAndStatus(userId, ReservationStatus.RESERVED) } returns emptyList()

                val result = sut.getUserReservations(userId)

                result shouldBe emptyList()
                verify(exactly = 1) { mockStockReservationRepository.findByUserIdAndStatus(userId, ReservationStatus.RESERVED) }
            }
        }
    }
})