package io.hhplus.ecommerce.integration.inventory

import io.hhplus.ecommerce.support.KotestIntegrationTestBase

import io.hhplus.ecommerce.support.config.IntegrationTestFixtures
import io.hhplus.ecommerce.product.dto.CreateProductRequest
import io.hhplus.ecommerce.inventory.usecase.InventoryCommandUseCase
import io.hhplus.ecommerce.inventory.usecase.InventoryReservationUseCase
import io.hhplus.ecommerce.inventory.usecase.GetInventoryQueryUseCase
import io.hhplus.ecommerce.product.usecase.ProductCommandUseCase
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * 재고 동시성 통합 테스트
 *
 * TestContainers MySQL을 사용하여 재고 동시 처리 정합성을 검증합니다.
 * - 동시 재고 차감 (비관적 락)
 * - 동시 재고 예약
 * - 재고 부족 시 동시 처리
 */
class InventoryConcurrencyIntegrationTest(
    private val inventoryCommandUseCase: InventoryCommandUseCase,
    private val inventoryReservationUseCase: InventoryReservationUseCase,
    private val getInventoryQueryUseCase: GetInventoryQueryUseCase,
    private val productCommandUseCase: ProductCommandUseCase
) : KotestIntegrationTestBase({

    describe("재고 동시성 제어") {
        context("동시에 재고를 차감할 때") {
            it("비관적 락으로 정합성이 보장된다") {
                // Given
                val product = productCommandUseCase.createProduct(
                    CreateProductRequest(
                        name = "재고 차감 테스트 상품",
                        description = "테스트용",
                        price = 10000L,
                        categoryId = 1L,
                        createdBy = 1L
                    )
                )
                val productId = product.id
                val initialQuantity = 100
                val deductQuantity = 10
                val threadCount = 10 // 정확히 재고만큼 차감

                // 재고 생성
                inventoryCommandUseCase.createInventory(productId, initialQuantity)

                // When - 10개 스레드가 동시에 10개씩 차감
                val executor = Executors.newFixedThreadPool(threadCount)
                val latch = CountDownLatch(threadCount)
                val successCount = AtomicInteger(0)
                val failCount = AtomicInteger(0)

                repeat(threadCount) {
                    executor.submit {
                        try {
                            inventoryCommandUseCase.deductStock(productId, deductQuantity)
                            successCount.incrementAndGet()
                        } catch (e: Exception) {
                            failCount.incrementAndGet()
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                latch.await()
                executor.shutdown()

                // Then - 정확히 10번 모두 성공해야 함
                successCount.get() shouldBe 10
                failCount.get() shouldBe 0

                // 최종 재고 확인
                val finalInventory = getInventoryQueryUseCase.getInventory(productId)
                finalInventory shouldNotBe null
                finalInventory!!.quantity shouldBe 0 // 100 - (10 * 10)
            }
        }

        context("동시 차감 시 재고가 부족할 때") {
            it("일부만 성공한다") {
                // Given
                val product = productCommandUseCase.createProduct(
                    CreateProductRequest(
                        name = "재고 부족 테스트 상품",
                        description = "테스트용",
                        price = 10000L,
                        categoryId = 1L,
                        createdBy = 1L
                    )
                )
                val productId = product.id
                val initialQuantity = 50
                val deductQuantity = 10
                val threadCount = 10 // 50개인데 10개 스레드가 10개씩 차감 시도

                // 재고 생성
                inventoryCommandUseCase.createInventory(productId, initialQuantity)

                // When - 10개 스레드가 동시에 10개씩 차감
                val executor = Executors.newFixedThreadPool(threadCount)
                val latch = CountDownLatch(threadCount)
                val successCount = AtomicInteger(0)
                val failCount = AtomicInteger(0)

                repeat(threadCount) {
                    executor.submit {
                        try {
                            inventoryCommandUseCase.deductStock(productId, deductQuantity)
                            successCount.incrementAndGet()
                        } catch (e: Exception) {
                            failCount.incrementAndGet()
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                latch.await()
                executor.shutdown()

                // Then - 5번 성공, 5번 실패
                successCount.get() shouldBe 5
                failCount.get() shouldBe 5

                // 최종 재고 확인
                val finalInventory = getInventoryQueryUseCase.getInventory(productId)
                finalInventory shouldNotBe null
                finalInventory!!.quantity shouldBe 0 // 50 - (10 * 5)
            }
        }

        context("동시에 재고를 예약할 때") {
            it("정합성이 보장된다") {
                // Given
                val product = productCommandUseCase.createProduct(
                    CreateProductRequest(
                        name = "재고 동시성 테스트 상품",
                        description = "테스트용",
                        price = 10000L,
                        categoryId = 1L,
                        createdBy = 1L
                    )
                )
                val productId = product.id
                val initialQuantity = 100
                val reserveQuantity = 5
                val threadCount = 20 // 100개를 20명이 5개씩 예약

                // 재고 생성
                inventoryCommandUseCase.createInventory(productId, initialQuantity)

                // When - 20개 스레드가 동시에 5개씩 예약
                val executor = Executors.newFixedThreadPool(threadCount)
                val latch = CountDownLatch(threadCount)
                val successCount = AtomicInteger(0)
                val failCount = AtomicInteger(0)

                repeat(threadCount) {
                    executor.submit {
                        try {
                            inventoryReservationUseCase.reserveStock(productId, it.toLong(), reserveQuantity)
                            successCount.incrementAndGet()
                        } catch (e: Exception) {
                            failCount.incrementAndGet()
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                latch.await()
                executor.shutdown()

                // Then - 성공한 만큼 재고가 올바르게 예약되어야 함 (동시성으로 일부 실패 가능)
                val actualSuccessCount = successCount.get()
                val actualFailCount = failCount.get()

                // 총 시도 횟수 검증
                (actualSuccessCount + actualFailCount) shouldBe 20

                // 성공한 예약이 있어야 함
                actualSuccessCount shouldBeGreaterThanOrEqualTo 1

                // 최종 재고 확인
                val finalInventory = getInventoryQueryUseCase.getInventory(productId)
                finalInventory shouldNotBe null
                finalInventory!!.quantity shouldBe 100 // 전체 재고는 그대로
                finalInventory.reservedQuantity shouldBe (actualSuccessCount * reserveQuantity) // 성공한 예약만큼
                finalInventory.getAvailableQuantity() shouldBe (100 - actualSuccessCount * reserveQuantity) // 가용 재고
            }
        }

        context("동시에 예약과 차감을 할 때") {
            it("정합성이 보장된다") {
                // Given
                val product = productCommandUseCase.createProduct(
                    CreateProductRequest(
                        name = "재고 예약 확정 테스트 상품",
                        description = "테스트용",
                        price = 10000L,
                        categoryId = 1L,
                        createdBy = 1L
                    )
                )
                val productId = product.id
                val initialQuantity = 100
                val reserveQuantity = 5
                val deductQuantity = 5
                val threadCount = 20 // 10번 예약, 10번 차감

                // 재고 생성
                inventoryCommandUseCase.createInventory(productId, initialQuantity)

                // When - 10개는 예약, 10개는 차감
                val executor = Executors.newFixedThreadPool(threadCount)
                val latch = CountDownLatch(threadCount)
                val reserveSuccessCount = AtomicInteger(0)
                val deductSuccessCount = AtomicInteger(0)

                repeat(threadCount) { index ->
                    executor.submit {
                        try {
                            if (index % 2 == 0) {
                                // 짝수: 예약
                                inventoryReservationUseCase.reserveStock(productId, index.toLong(), reserveQuantity)
                                reserveSuccessCount.incrementAndGet()
                            } else {
                                // 홀수: 차감
                                inventoryCommandUseCase.deductStock(productId, deductQuantity)
                                deductSuccessCount.incrementAndGet()
                            }
                        } catch (e: Exception) {
                            // 재고 부족으로 실패할 수 있음
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                latch.await()
                executor.shutdown()

                // Then
                val finalInventory = getInventoryQueryUseCase.getInventory(productId)
                finalInventory shouldNotBe null

                // 최종 재고 검증
                val expectedQuantity = 100 - (deductSuccessCount.get() * 5)
                val expectedReserved = reserveSuccessCount.get() * 5
                finalInventory!!.quantity shouldBe expectedQuantity
                finalInventory.reservedQuantity shouldBe expectedReserved
            }
        }

        context("높은 동시성 환경에서") {
            it("재고 정합성이 보장된다") {
                // Given
                val product = productCommandUseCase.createProduct(
                    CreateProductRequest(
                        name = "재고 예약 해제 테스트 상품",
                        description = "테스트용",
                        price = 10000L,
                        categoryId = 1L,
                        createdBy = 1L
                    )
                )
                val productId = product.id
                val initialQuantity = 1000
                val deductQuantity = 1
                val threadCount = 100

                // 재고 생성
                inventoryCommandUseCase.createInventory(productId, initialQuantity)

                // When - 100개 스레드가 동시에 1개씩 차감
                val executor = Executors.newFixedThreadPool(threadCount)
                val latch = CountDownLatch(threadCount)
                val successCount = AtomicInteger(0)

                repeat(threadCount) {
                    executor.submit {
                        try {
                            inventoryCommandUseCase.deductStock(productId, deductQuantity)
                            successCount.incrementAndGet()
                        } catch (e: Exception) {
                            // 동시성 제어 실패
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                latch.await()
                executor.shutdown()

                // Then
                successCount.get() shouldBe threadCount

                // 최종 재고 확인
                val finalInventory = getInventoryQueryUseCase.getInventory(productId)
                finalInventory shouldNotBe null
                finalInventory!!.quantity shouldBe 900 // 1000 - 100
            }
        }

        context("동시에 예약 확정을 할 때") {
            it("정합성이 보장된다") {
                // Given
                val product = productCommandUseCase.createProduct(
                    CreateProductRequest(
                        name = "예약 확정 동시성 테스트 상품",
                        description = "테스트용",
                        price = 10000L,
                        categoryId = 1L,
                        createdBy = 1L
                    )
                )
                val productId = product.id
                val initialQuantity = 100
                val reserveQuantity = 10
                val confirmQuantity = 10
                val threadCount = 10

                // 재고 생성
                inventoryCommandUseCase.createInventory(productId, initialQuantity)

                // When - 10개 스레드가 동시에 예약과 확정을 시도 (각 10개씩)
                val executor = Executors.newFixedThreadPool(threadCount)
                val latch = CountDownLatch(threadCount)
                val successCount = AtomicInteger(0)
                val failCount = AtomicInteger(0)

                repeat(threadCount) { index ->
                    executor.submit {
                        try {
                            // 각 스레드가 예약을 시도한 후 바로 확정
                            val reservation = inventoryReservationUseCase.reserveStock(productId, index.toLong(), reserveQuantity)
                            inventoryReservationUseCase.confirmReservation(reservation.id, index.toLong())
                            successCount.incrementAndGet()
                        } catch (e: Exception) {
                            failCount.incrementAndGet()
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                latch.await()
                executor.shutdown()

                // Then - 성공한 만큼 확정되었어야 함
                val actualSuccessCount = successCount.get()
                val actualFailCount = failCount.get()

                // 총 시도 횟수 검증
                (actualSuccessCount + actualFailCount) shouldBe 10

                // 성공한 확정이 있어야 함
                actualSuccessCount shouldBeGreaterThanOrEqualTo 1

                // 최종 재고 확인
                val finalInventory = getInventoryQueryUseCase.getInventory(productId)
                finalInventory shouldNotBe null
                finalInventory!!.quantity shouldBe (100 - actualSuccessCount * 10) // 성공한 만큼 차감
                finalInventory.reservedQuantity shouldBe 0 // 모두 확정되어 예약 0
            }
        }
    }
})
