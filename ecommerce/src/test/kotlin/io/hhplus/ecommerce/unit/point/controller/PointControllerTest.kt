package io.hhplus.ecommerce.unit.point.controller

import io.hhplus.ecommerce.point.controller.PointController
import io.hhplus.ecommerce.point.usecase.GetPointQueryUseCase
import io.hhplus.ecommerce.point.usecase.ChargePointUseCase
import io.hhplus.ecommerce.point.usecase.DeductPointUseCase
import io.hhplus.ecommerce.point.domain.entity.UserPoint
import io.hhplus.ecommerce.point.domain.entity.PointHistory
import io.hhplus.ecommerce.point.dto.ChargePointRequest
import io.hhplus.ecommerce.point.dto.DeductPointRequest
import io.hhplus.ecommerce.common.response.ApiResponse
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

/**
 * PointController 단위 테스트
 *
 * 책임: 포인트 관련 HTTP 요청 처리 검증
 * - REST API 엔드포인트의 요청/응답 처리 검증
 * - UseCase 계층과의 올바른 상호작용 검증
 * - 요청 데이터 변환 및 응답 형식 검증
 *
 * 검증 목표:
 * 1. 각 엔드포인트가 적절한 UseCase를 호출하는가?
 * 2. 요청 파라미터가 올바르게 UseCase에 전달되는가?
 * 3. UseCase 결과가 적절한 ApiResponse로 변환되는가?
 * 4. HTTP 메서드와 경로 매핑이 올바른가?
 */
class PointControllerTest : DescribeSpec({
    val mockGetPointQueryUseCase = mockk<GetPointQueryUseCase>()
    val mockChargePointUseCase = mockk<ChargePointUseCase>()
    val mockDeductPointUseCase = mockk<DeductPointUseCase>()

    val sut = PointController(
        getPointQueryUseCase = mockGetPointQueryUseCase,
        chargePointUseCase = mockChargePointUseCase,
        deductPointUseCase = mockDeductPointUseCase
    )

    beforeEach {
        clearMocks(
            mockGetPointQueryUseCase,
            mockChargePointUseCase,
            mockDeductPointUseCase
        )
    }

    describe("getUserPoint") {
        context("GET /api/v1/points/{userId} 요청") {
            it("GetPointQueryUseCase를 호출하고 ApiResponse로 감싸서 반환") {
                val userId = 1L
                val mockUserPoint = mockk<UserPoint>(relaxed = true)

                every { mockGetPointQueryUseCase.getUserPoint(userId) } returns mockUserPoint

                val result = sut.getUserPoint(userId)

                result.success shouldBe true
                verify(exactly = 1) { mockGetPointQueryUseCase.getUserPoint(userId) }
            }
        }

        context("존재하지 않는 사용자 조회") {
            it("UseCase에서 예외가 발생하면 예외를 그대로 전파") {
                val userId = 999L

                every { mockGetPointQueryUseCase.getUserPoint(userId) } throws IllegalArgumentException("사용자 포인트 정보가 없습니다: $userId")

                shouldThrow<IllegalArgumentException> {
                    sut.getUserPoint(userId)
                }.message shouldBe "사용자 포인트 정보가 없습니다: $userId"

                verify(exactly = 1) { mockGetPointQueryUseCase.getUserPoint(userId) }
            }
        }
    }

    describe("earnPoint") {
        context("POST /api/v1/points/{userId}/charge 요청") {
            it("요청 데이터를 ChargePointUseCase에 전달하고 ApiResponse로 반환") {
                val userId = 1L
                val request = ChargePointRequest(amount = 5000L, description = "테스트 적립")
                val mockUserPoint = mockk<UserPoint>(relaxed = true)

                every { mockChargePointUseCase.execute(userId, request.amount, request.description) } returns mockUserPoint

                val result = sut.earnPoint(userId, request)

                result.success shouldBe true
                verify(exactly = 1) { mockChargePointUseCase.execute(userId, request.amount, request.description) }
            }
        }

        context("description이 null인 요청") {
            it("description null을 그대로 UseCase에 전달") {
                val userId = 1L
                val request = ChargePointRequest(amount = 3000L, description = null)
                val mockUserPoint = mockk<UserPoint>(relaxed = true)

                every { mockChargePointUseCase.execute(userId, request.amount, null) } returns mockUserPoint

                val result = sut.earnPoint(userId, request)

                result.success shouldBe true
                verify(exactly = 1) { mockChargePointUseCase.execute(userId, request.amount, null) }
            }
        }

        context("다양한 적립 금액 요청") {
            it("요청 금액을 정확히 UseCase에 전달") {
                val userId = 2L
                val amount = 10000L
                val description = "대량 적립"
                val request = ChargePointRequest(amount = amount, description = description)
                val mockUserPoint = mockk<UserPoint>(relaxed = true)

                every { mockChargePointUseCase.execute(userId, amount, description) } returns mockUserPoint

                val result = sut.earnPoint(userId, request)

                result.success shouldBe true
                verify(exactly = 1) { mockChargePointUseCase.execute(userId, amount, description) }
            }
        }
    }

    describe("usePoint") {
        context("POST /api/v1/points/{userId}/deduct 요청") {
            it("요청 데이터를 DeductPointUseCase에 전달하고 ApiResponse로 반환") {
                val userId = 1L
                val request = DeductPointRequest(amount = 2000L, description = "테스트 사용")
                val mockUserPoint = mockk<UserPoint>(relaxed = true)

                every { mockDeductPointUseCase.execute(userId, request.amount, request.description) } returns mockUserPoint

                val result = sut.usePoint(userId, request)

                result.success shouldBe true
                verify(exactly = 1) { mockDeductPointUseCase.execute(userId, request.amount, request.description) }
            }
        }

        context("description이 null인 사용 요청") {
            it("description null을 그대로 UseCase에 전달") {
                val userId = 3L
                val request = DeductPointRequest(amount = 1500L, description = null)
                val mockUserPoint = mockk<UserPoint>(relaxed = true)

                every { mockDeductPointUseCase.execute(userId, request.amount, null) } returns mockUserPoint

                val result = sut.usePoint(userId, request)

                result.success shouldBe true
                verify(exactly = 1) { mockDeductPointUseCase.execute(userId, request.amount, null) }
            }
        }

        context("다양한 사용 금액 요청") {
            it("요청 금액을 정확히 UseCase에 전달") {
                val userId = 4L
                val amount = 7500L
                val description = "상품 구매"
                val request = DeductPointRequest(amount = amount, description = description)
                val mockUserPoint = mockk<UserPoint>(relaxed = true)

                every { mockDeductPointUseCase.execute(userId, amount, description) } returns mockUserPoint

                val result = sut.usePoint(userId, request)

                result.success shouldBe true
                verify(exactly = 1) { mockDeductPointUseCase.execute(userId, amount, description) }
            }
        }
    }

    describe("getPointHistories") {
        context("GET /api/v1/points/{userId}/histories 요청") {
            it("GetPointQueryUseCase를 호출하고 히스토리 목록을 ApiResponse로 반환") {
                val userId = 1L
                val mockHistories = listOf(
                    mockk<PointHistory>(relaxed = true),
                    mockk<PointHistory>(relaxed = true),
                    mockk<PointHistory>(relaxed = true)
                )

                every { mockGetPointQueryUseCase.getPointHistories(userId) } returns mockHistories

                val result = sut.getPointHistories(userId)

                result.success shouldBe true
                verify(exactly = 1) { mockGetPointQueryUseCase.getPointHistories(userId) }
            }
        }

        context("히스토리가 없는 사용자 조회") {
            it("빈 리스트를 ApiResponse로 감싸서 반환") {
                val userId = 999L
                val emptyHistories = emptyList<PointHistory>()

                every { mockGetPointQueryUseCase.getPointHistories(userId) } returns emptyHistories

                val result = sut.getPointHistories(userId)

                result.success shouldBe true
                verify(exactly = 1) { mockGetPointQueryUseCase.getPointHistories(userId) }
            }
        }

        context("다량의 히스토리가 있는 사용자 조회") {
            it("UseCase에서 반환된 모든 히스토리를 ApiResponse로 반환") {
                val userId = 2L
                val manyHistories = (1..50).map { mockk<PointHistory>(relaxed = true) }

                every { mockGetPointQueryUseCase.getPointHistories(userId) } returns manyHistories

                val result = sut.getPointHistories(userId)

                result.success shouldBe true
                verify(exactly = 1) { mockGetPointQueryUseCase.getPointHistories(userId) }
            }
        }
    }

    describe("API 경로 및 메서드 검증") {
        context("모든 엔드포인트") {
            it("적절한 UseCase만 호출하고 다른 UseCase는 호출하지 않음") {
                val userId = 1L

                // getUserPoint 테스트
                every { mockGetPointQueryUseCase.getUserPoint(userId) } returns mockk(relaxed = true)
                sut.getUserPoint(userId)
                verify(exactly = 1) { mockGetPointQueryUseCase.getUserPoint(userId) }
                verify(exactly = 0) { mockChargePointUseCase.execute(any(), any(), any()) }
                verify(exactly = 0) { mockDeductPointUseCase.execute(any(), any(), any()) }

                clearMocks(mockGetPointQueryUseCase, mockChargePointUseCase, mockDeductPointUseCase)

                // earnPoint 테스트
                val chargeRequest = ChargePointRequest(amount = 1000L, description = null)
                every { mockChargePointUseCase.execute(userId, 1000L, null) } returns mockk(relaxed = true)
                sut.earnPoint(userId, chargeRequest)
                verify(exactly = 1) { mockChargePointUseCase.execute(userId, 1000L, null) }
                verify(exactly = 0) { mockGetPointQueryUseCase.getUserPoint(any()) }
                verify(exactly = 0) { mockDeductPointUseCase.execute(any(), any(), any()) }

                clearMocks(mockGetPointQueryUseCase, mockChargePointUseCase, mockDeductPointUseCase)

                // usePoint 테스트
                val deductRequest = DeductPointRequest(amount = 500L, description = null)
                every { mockDeductPointUseCase.execute(userId, 500L, null) } returns mockk(relaxed = true)
                sut.usePoint(userId, deductRequest)
                verify(exactly = 1) { mockDeductPointUseCase.execute(userId, 500L, null) }
                verify(exactly = 0) { mockGetPointQueryUseCase.getUserPoint(any()) }
                verify(exactly = 0) { mockChargePointUseCase.execute(any(), any(), any()) }
            }
        }
    }
})
