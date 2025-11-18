package io.hhplus.ecommerce.integration.delivery

import io.hhplus.ecommerce.support.KotestIntegrationTestBase
import io.hhplus.ecommerce.delivery.exception.DeliveryException
import io.hhplus.ecommerce.delivery.usecase.DeliveryCommandUseCase
import io.hhplus.ecommerce.delivery.usecase.GetDeliveryQueryUseCase
import io.hhplus.ecommerce.delivery.domain.constant.DeliveryStatus
import io.hhplus.ecommerce.delivery.domain.repository.DeliveryRepository
import io.hhplus.ecommerce.delivery.domain.vo.DeliveryAddress
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * 배송 상태 통합 테스트
 *
 * TestContainers MySQL을 사용하여 배송 상태 변경 전체 플로우를 검증합니다.
 * - 배송 생성
 * - 배송 상태 변경 (PENDING → PREPARING → SHIPPED → DELIVERED)
 * - 배송 실패 처리
 * - 운송장 조회
 */
class DeliveryStatusIntegrationTest(
    private val deliveryCommandUseCase: DeliveryCommandUseCase,
    private val getDeliveryQueryUseCase: GetDeliveryQueryUseCase,
    private val deliveryRepository: DeliveryRepository
) : KotestIntegrationTestBase({

    describe("배송 생성") {
        context("정상적인 배송 생성 요청일 때") {
            it("배송을 정상적으로 생성할 수 있다") {
                // Given
                val orderId = 1000L
                val deliveryAddress = DeliveryAddress(
                    recipientName = "홍길동",
                    phone = "010-1234-5678",
                    zipCode = "12345",
                    address = "서울시 강남구 테헤란로 123",
                    addressDetail = "ABC빌딩 10층"
                )
                val createdBy = 1L

                // When
                val delivery = deliveryCommandUseCase.createDelivery(
                    orderId = orderId,
                    deliveryAddress = deliveryAddress,
                    deliveryMemo = "문 앞에 놓아주세요",
                    createdBy = createdBy
                )

                // Then
                delivery shouldNotBe null
                delivery.orderId shouldBe orderId
                delivery.deliveryAddress shouldBe deliveryAddress
                delivery.status shouldBe DeliveryStatus.PENDING
            }
        }

        context("동일 주문에 대해 중복 배송 생성 시도할 때") {
            it("예외가 발생한다") {
                // Given
                val orderId = 2000L
                val deliveryAddress = DeliveryAddress(
                    recipientName = "김철수",
                    phone = "010-9876-5432",
                    zipCode = "54321",
                    address = "부산시 해운대구",
                    addressDetail = "101호"
                )

                deliveryCommandUseCase.createDelivery(orderId, deliveryAddress, null, 1L)

                // When & Then
                shouldThrow<IllegalStateException> {
                    deliveryCommandUseCase.createDelivery(orderId, deliveryAddress, null, 1L)
                }
            }
        }
    }

    describe("배송 상태 변경") {
        context("배송 준비 시작 시") {
            it("상태가 PREPARING으로 변경된다") {
                // Given
                val orderId = 3000L
                val deliveryAddress = DeliveryAddress("수신자1", "010-1111-2222", "12345", "주소1", "상세주소1")
                val delivery = deliveryCommandUseCase.createDelivery(orderId, deliveryAddress, null, 1L)

                // When
                val updated = deliveryCommandUseCase.startPreparing(delivery.id, 1L)

                // Then
                updated.status shouldBe DeliveryStatus.PREPARING
            }
        }

        context("배송 발송 처리 시") {
            it("상태가 SHIPPED로 변경되고 운송장 번호가 기록된다") {
                // Given
                val orderId = 4000L
                val deliveryAddress = DeliveryAddress("수신자2", "010-2222-3333", "23456", "주소2", "상세주소2")
                val delivery = deliveryCommandUseCase.createDelivery(orderId, deliveryAddress, null, 1L)
                deliveryCommandUseCase.startPreparing(delivery.id, 1L)

                // When
                val trackingNumber = "1234567890"
                val carrier = "CJ대한통운"
                val updated = deliveryCommandUseCase.ship(delivery.id, trackingNumber, carrier, 1L)

                // Then
                updated.status shouldBe DeliveryStatus.SHIPPED
                updated.trackingNumber shouldBe trackingNumber
                updated.carrier shouldBe carrier
                updated.shippedAt shouldNotBe null
            }
        }

        context("배송 완료 처리 시") {
            it("상태가 DELIVERED로 변경된다") {
                // Given
                val orderId = 5000L
                val deliveryAddress = DeliveryAddress("수신자3", "010-3333-4444", "34567", "주소3", "상세주소3")
                val delivery = deliveryCommandUseCase.createDelivery(orderId, deliveryAddress, null, 1L)
                deliveryCommandUseCase.startPreparing(delivery.id, 1L)
                deliveryCommandUseCase.ship(delivery.id, "9876543210", "롯데택배", 1L)

                // When
                val updated = deliveryCommandUseCase.deliver(delivery.id, 1L)

                // Then
                updated.status shouldBe DeliveryStatus.DELIVERED
                updated.deliveredAt shouldNotBe null
            }
        }

        context("배송 실패 처리 시") {
            it("상태가 FAILED로 변경된다") {
                // Given
                val orderId = 6000L
                val deliveryAddress = DeliveryAddress("수신자4", "010-4444-5555", "45678", "주소4", "상세주소4")
                val delivery = deliveryCommandUseCase.createDelivery(orderId, deliveryAddress, null, 1L)
                deliveryCommandUseCase.startPreparing(delivery.id, 1L)

                // When
                val updated = deliveryCommandUseCase.fail(delivery.id, 1L)

                // Then
                updated.status shouldBe DeliveryStatus.FAILED
            }
        }
    }

    describe("배송 전체 플로우") {
        context("PENDING → PREPARING → SHIPPED → DELIVERED 순서로 진행 시") {
            it("정상적으로 배송이 완료된다") {
                // Given
                val orderId = 7000L
                val deliveryAddress = DeliveryAddress("수신자5", "010-5555-6666", "56789", "주소5", "상세주소5")

                // When - PENDING
                val delivery = deliveryCommandUseCase.createDelivery(orderId, deliveryAddress, "빠른 배송 부탁드립니다", 1L)
                delivery.status shouldBe DeliveryStatus.PENDING

                // When - PREPARING
                val preparing = deliveryCommandUseCase.startPreparing(delivery.id, 1L)
                preparing.status shouldBe DeliveryStatus.PREPARING

                // When - SHIPPED
                val shipped = deliveryCommandUseCase.ship(delivery.id, "TRACK123", "한진택배", 1L)
                shipped.status shouldBe DeliveryStatus.SHIPPED

                // When - DELIVERED
                val delivered = deliveryCommandUseCase.deliver(delivery.id, 1L)
                delivered.status shouldBe DeliveryStatus.DELIVERED

                // Then
                val final = getDeliveryQueryUseCase.getDelivery(delivery.id)
                final.status shouldBe DeliveryStatus.DELIVERED
                final.shippedAt shouldNotBe null
                final.deliveredAt shouldNotBe null
            }
        }
    }

    describe("배송 조회") {
        context("배송 ID로 조회할 때") {
            it("배송 정보를 조회할 수 있다") {
                // Given
                val orderId = 8000L
                val deliveryAddress = DeliveryAddress("수신자6", "010-6666-7777", "67890", "주소6", "상세주소6")
                val delivery = deliveryCommandUseCase.createDelivery(orderId, deliveryAddress, null, 1L)

                // When
                val found = getDeliveryQueryUseCase.getDelivery(delivery.id)

                // Then
                found shouldNotBe null
                found.id shouldBe delivery.id
                found.orderId shouldBe orderId
            }
        }

        context("주문 ID로 조회할 때") {
            it("해당 주문의 배송을 조회할 수 있다") {
                // Given
                val orderId = 9000L
                val deliveryAddress = DeliveryAddress("수신자7", "010-7777-8888", "78901", "주소7", "상세주소7")
                val delivery = deliveryCommandUseCase.createDelivery(orderId, deliveryAddress, null, 1L)

                // When
                val found = getDeliveryQueryUseCase.getDeliveryByOrderId(orderId)

                // Then
                found shouldNotBe null
                found.orderId shouldBe orderId
            }
        }

        context("존재하지 않는 주문 ID로 조회할 때") {
            it("예외가 발생한다") {
                // When & Then
                shouldThrow<DeliveryException.DeliveryNotFoundByOrder> {
                    getDeliveryQueryUseCase.getDeliveryByOrderId(99999L)
                }
            }
        }

        context("운송장 번호로 조회할 때") {
            it("배송 정보를 조회할 수 있다") {
                // Given
                val orderId = 10000L
                val deliveryAddress = DeliveryAddress("수신자8", "010-8888-9999", "89012", "주소8", "상세주소8")
                val delivery = deliveryCommandUseCase.createDelivery(orderId, deliveryAddress, null, 1L)
                deliveryCommandUseCase.startPreparing(delivery.id, 1L)
                val trackingNumber = "UNIQUE_TRACK_999"
                deliveryCommandUseCase.ship(delivery.id, trackingNumber, "우체국택배", 1L)

                // When
                val found = getDeliveryQueryUseCase.getDeliveryByTrackingNumber(trackingNumber)

                // Then
                found shouldNotBe null
                found.trackingNumber shouldBe trackingNumber
            }
        }

        context("배송 상태로 조회할 때") {
            it("해당 상태의 배송 목록을 조회할 수 있다") {
                // Given
                val deliveryAddress = DeliveryAddress("수신자9", "010-9999-0000", "90123", "주소9", "상세주소9")
                val delivery1 = deliveryCommandUseCase.createDelivery(11001L, deliveryAddress, null, 1L)
                val delivery2 = deliveryCommandUseCase.createDelivery(11002L, deliveryAddress, null, 1L)
                deliveryCommandUseCase.startPreparing(delivery1.id, 1L)

                // When
                val preparingDeliveries = getDeliveryQueryUseCase.getDeliveriesByStatus(DeliveryStatus.PREPARING)
                val pendingDeliveries = getDeliveryQueryUseCase.getDeliveriesByStatus(DeliveryStatus.PENDING)

                // Then
                preparingDeliveries.any { it.id == delivery1.id } shouldBe true
                pendingDeliveries.any { it.id == delivery2.id } shouldBe true
            }
        }
    }
})
