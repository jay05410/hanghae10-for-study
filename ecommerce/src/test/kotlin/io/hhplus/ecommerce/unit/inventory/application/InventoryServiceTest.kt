package io.hhplus.ecommerce.unit.inventory.application

import io.hhplus.ecommerce.inventory.application.InventoryService
import io.hhplus.ecommerce.inventory.domain.entity.Inventory
import io.hhplus.ecommerce.inventory.domain.repository.InventoryRepository
import io.hhplus.ecommerce.inventory.exception.InventoryException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.time.LocalDateTime

/**
 * InventoryService 단위 테스트
 *
 * 책임: 재고 서비스의 핵심 비즈니스 로직 검증
 * - 재고 생성, 차감, 충당, 예약 기능 검증
 * - Repository와의 상호작용 검증
 */
class InventoryServiceTest : DescribeSpec({
    val mockInventoryRepository = mockk<InventoryRepository>()
    val sut = InventoryService(mockInventoryRepository)

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
        every { mockInventory.deduct(any()) } just runs
        every { mockInventory.restock(any()) } just runs
        every { mockInventory.reserve(any()) } just runs
        every { mockInventory.releaseReservation(any()) } just runs
        every { mockInventory.confirmReservation(any()) } just runs
        every { mockInventory.isStockAvailable(any()) } returns true
        every { mockInventory.getAvailableQuantity() } returns (quantity - reservedQuantity)
        return mockInventory
    }

    beforeEach {
        clearMocks(mockInventoryRepository)
    }

    describe("getInventory") {
        context("존재하는 상품 재고 조회") {
            it("Repository에서 재고를 조회하여 반환") {
                val productId = 1L
                val mockInventory = createMockInventory(productId = productId)

                every { mockInventoryRepository.findByProductId(productId) } returns mockInventory

                val result = sut.getInventory(productId)

                result shouldBe mockInventory
                verify(exactly = 1) { mockInventoryRepository.findByProductId(productId) }
            }
        }

        context("존재하지 않는 상품 재고 조회") {
            it("null을 반환") {
                val productId = 999L

                every { mockInventoryRepository.findByProductId(productId) } returns null

                val result = sut.getInventory(productId)

                result shouldBe null
                verify(exactly = 1) { mockInventoryRepository.findByProductId(productId) }
            }
        }
    }

    describe("createInventory") {
        context("새로운 재고 생성") {
            it("재고를 생성하고 저장하여 반환") {
                val productId = 1L
                val initialQuantity = 100
                val mockInventory = createMockInventory(productId = productId, quantity = initialQuantity)

                every { mockInventoryRepository.findByProductId(productId) } returns null
                every { mockInventoryRepository.save(any()) } returns mockInventory

                mockkObject(Inventory.Companion)
                every { Inventory.create(productId, initialQuantity) } returns mockInventory

                val result = sut.createInventory(productId, initialQuantity)

                result shouldBe mockInventory
                verify(exactly = 1) { mockInventoryRepository.findByProductId(productId) }
                verify(exactly = 1) { Inventory.create(productId, initialQuantity) }
                verify(exactly = 1) { mockInventoryRepository.save(any()) }
            }
        }

        context("이미 존재하는 상품의 재고 생성") {
            it("InventoryAlreadyExists 예외를 발생") {
                val productId = 1L
                val initialQuantity = 100
                val existingInventory = createMockInventory(productId = productId)

                every { mockInventoryRepository.findByProductId(productId) } returns existingInventory

                shouldThrow<InventoryException.InventoryAlreadyExists> {
                    sut.createInventory(productId, initialQuantity)
                }

                verify(exactly = 1) { mockInventoryRepository.findByProductId(productId) }
                verify(exactly = 0) { mockInventoryRepository.save(any()) }
            }
        }
    }

    describe("deductStock") {
        context("정상적인 재고 차감") {
            it("락을 걸고 재고를 차감하여 저장") {
                val productId = 1L
                val quantity = 10
                val mockInventory = createMockInventory(productId = productId)

                every { mockInventoryRepository.findByProductIdWithLock(productId) } returns mockInventory
                every { mockInventoryRepository.save(mockInventory) } returns mockInventory

                val result = sut.deductStock(productId, quantity)

                result shouldBe mockInventory
                verify(exactly = 1) { mockInventoryRepository.findByProductIdWithLock(productId) }
                verify(exactly = 1) { mockInventory.deduct(quantity) }
                verify(exactly = 1) { mockInventoryRepository.save(mockInventory) }
            }
        }

        context("존재하지 않는 상품의 재고 차감") {
            it("InventoryNotFound 예외를 발생") {
                val productId = 999L
                val quantity = 10

                every { mockInventoryRepository.findByProductIdWithLock(productId) } returns null

                shouldThrow<InventoryException.InventoryNotFound> {
                    sut.deductStock(productId, quantity)
                }

                verify(exactly = 1) { mockInventoryRepository.findByProductIdWithLock(productId) }
                verify(exactly = 0) { mockInventoryRepository.save(any()) }
            }
        }
    }

    describe("restockInventory") {
        context("정상적인 재고 충당") {
            it("락을 걸고 재고를 충당하여 저장") {
                val productId = 1L
                val quantity = 50
                val mockInventory = createMockInventory(productId = productId)

                every { mockInventoryRepository.findByProductIdWithLock(productId) } returns mockInventory
                every { mockInventoryRepository.save(mockInventory) } returns mockInventory

                val result = sut.restockInventory(productId, quantity)

                result shouldBe mockInventory
                verify(exactly = 1) { mockInventoryRepository.findByProductIdWithLock(productId) }
                verify(exactly = 1) { mockInventory.restock(quantity) }
                verify(exactly = 1) { mockInventoryRepository.save(mockInventory) }
            }
        }

        context("존재하지 않는 상품의 재고 충당") {
            it("InventoryNotFound 예외를 발생") {
                val productId = 999L
                val quantity = 50

                every { mockInventoryRepository.findByProductIdWithLock(productId) } returns null

                shouldThrow<InventoryException.InventoryNotFound> {
                    sut.restockInventory(productId, quantity)
                }

                verify(exactly = 1) { mockInventoryRepository.findByProductIdWithLock(productId) }
                verify(exactly = 0) { mockInventoryRepository.save(any()) }
            }
        }
    }

    describe("reserveStock") {
        context("정상적인 재고 예약") {
            it("락을 걸고 재고를 예약하여 저장") {
                val productId = 1L
                val quantity = 5
                val mockInventory = createMockInventory(productId = productId)

                every { mockInventoryRepository.findByProductIdWithLock(productId) } returns mockInventory
                every { mockInventoryRepository.save(mockInventory) } returns mockInventory

                val result = sut.reserveStock(productId, quantity)

                result shouldBe mockInventory
                verify(exactly = 1) { mockInventoryRepository.findByProductIdWithLock(productId) }
                verify(exactly = 1) { mockInventory.reserve(quantity) }
                verify(exactly = 1) { mockInventoryRepository.save(mockInventory) }
            }
        }

        context("존재하지 않는 상품의 재고 예약") {
            it("InventoryNotFound 예외를 발생") {
                val productId = 999L
                val quantity = 5

                every { mockInventoryRepository.findByProductIdWithLock(productId) } returns null

                shouldThrow<InventoryException.InventoryNotFound> {
                    sut.reserveStock(productId, quantity)
                }

                verify(exactly = 1) { mockInventoryRepository.findByProductIdWithLock(productId) }
                verify(exactly = 0) { mockInventoryRepository.save(any()) }
            }
        }
    }

    describe("releaseReservation") {
        context("정상적인 예약 해제") {
            it("락을 걸고 예약을 해제하여 저장") {
                val productId = 1L
                val quantity = 5
                val mockInventory = createMockInventory(productId = productId, reservedQuantity = 5)

                every { mockInventoryRepository.findByProductIdWithLock(productId) } returns mockInventory
                every { mockInventoryRepository.save(mockInventory) } returns mockInventory

                val result = sut.releaseReservation(productId, quantity)

                result shouldBe mockInventory
                verify(exactly = 1) { mockInventoryRepository.findByProductIdWithLock(productId) }
                verify(exactly = 1) { mockInventory.releaseReservation(quantity) }
                verify(exactly = 1) { mockInventoryRepository.save(mockInventory) }
            }
        }

        context("존재하지 않는 상품의 예약 해제") {
            it("InventoryNotFound 예외를 발생") {
                val productId = 999L
                val quantity = 5

                every { mockInventoryRepository.findByProductIdWithLock(productId) } returns null

                shouldThrow<InventoryException.InventoryNotFound> {
                    sut.releaseReservation(productId, quantity)
                }

                verify(exactly = 1) { mockInventoryRepository.findByProductIdWithLock(productId) }
                verify(exactly = 0) { mockInventoryRepository.save(any()) }
            }
        }
    }

    describe("confirmReservation") {
        context("정상적인 예약 확정") {
            it("락을 걸고 예약을 확정하여 저장") {
                val productId = 1L
                val quantity = 5
                val mockInventory = createMockInventory(productId = productId, reservedQuantity = 5)

                every { mockInventoryRepository.findByProductIdWithLock(productId) } returns mockInventory
                every { mockInventoryRepository.save(mockInventory) } returns mockInventory

                val result = sut.confirmReservation(productId, quantity)

                result shouldBe mockInventory
                verify(exactly = 1) { mockInventoryRepository.findByProductIdWithLock(productId) }
                verify(exactly = 1) { mockInventory.confirmReservation(quantity) }
                verify(exactly = 1) { mockInventoryRepository.save(mockInventory) }
            }
        }

        context("존재하지 않는 상품의 예약 확정") {
            it("InventoryNotFound 예외를 발생") {
                val productId = 999L
                val quantity = 5

                every { mockInventoryRepository.findByProductIdWithLock(productId) } returns null

                shouldThrow<InventoryException.InventoryNotFound> {
                    sut.confirmReservation(productId, quantity)
                }

                verify(exactly = 1) { mockInventoryRepository.findByProductIdWithLock(productId) }
                verify(exactly = 0) { mockInventoryRepository.save(any()) }
            }
        }
    }

    describe("checkStockAvailability") {
        context("재고 가용성 확인") {
            it("충분한 재고가 있으면 true 반환") {
                val productId = 1L
                val requestedQuantity = 10
                val mockInventory = createMockInventory(productId = productId, quantity = 100, reservedQuantity = 5)

                every { mockInventoryRepository.findByProductId(productId) } returns mockInventory

                val result = sut.checkStockAvailability(productId, requestedQuantity)

                result shouldBe true
                verify(exactly = 1) { mockInventoryRepository.findByProductId(productId) }
                verify(exactly = 1) { mockInventory.isStockAvailable(requestedQuantity) }
            }
        }

        context("재고가 없는 상품") {
            it("false를 반환") {
                val productId = 999L
                val requestedQuantity = 10

                every { mockInventoryRepository.findByProductId(productId) } returns null

                val result = sut.checkStockAvailability(productId, requestedQuantity)

                result shouldBe false
                verify(exactly = 1) { mockInventoryRepository.findByProductId(productId) }
            }
        }
    }

    describe("getAvailableQuantity") {
        context("가용 재고량 조회") {
            it("예약된 수량을 제외한 가용 재고량 반환") {
                val productId = 1L
                val mockInventory = createMockInventory(productId = productId, quantity = 100, reservedQuantity = 10)

                every { mockInventoryRepository.findByProductId(productId) } returns mockInventory

                val result = sut.getAvailableQuantity(productId)

                result shouldBe 90
                verify(exactly = 1) { mockInventoryRepository.findByProductId(productId) }
                verify(exactly = 1) { mockInventory.getAvailableQuantity() }
            }
        }

        context("재고가 없는 상품") {
            it("0을 반환") {
                val productId = 999L

                every { mockInventoryRepository.findByProductId(productId) } returns null

                val result = sut.getAvailableQuantity(productId)

                result shouldBe 0
                verify(exactly = 1) { mockInventoryRepository.findByProductId(productId) }
            }
        }
    }
})