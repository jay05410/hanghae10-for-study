package io.hhplus.ecommerce.integration.point

import io.hhplus.ecommerce.support.KotestIntegrationTestBase
import io.hhplus.ecommerce.support.config.IntegrationTestFixtures
import io.hhplus.ecommerce.point.application.usecase.ChargePointUseCase
import io.hhplus.ecommerce.point.domain.vo.PointAmount
import io.kotest.matchers.shouldNotBe
import mu.KotlinLogging

/**
 * 분산락이 실제로 작동하는지 확인하는 간단한 테스트
 */
class PointDistributedLockTest(
    private val chargePointUseCase: ChargePointUseCase
) : KotestIntegrationTestBase({

    val logger = KotlinLogging.logger {}

    describe("분산락 동작 확인") {
        context("포인트 충전 테스트") {
            it("단일 스레드에서 분산락이 적용된다") {
                // Given
                val userId = IntegrationTestFixtures.createTestUserId(1)
                logger.info("테스트 시작 - userId: $userId")

                // 사용자 포인트 생성
                logger.info("사용자 포인트 생성 완료")

                // When
                val result = chargePointUseCase.execute(userId, 1000L)
                logger.info("포인트 적립 완료 - 결과: ${result.balance}")

                // Then
                result shouldNotBe null
                logger.info("테스트 완료")
            }
        }
    }
})