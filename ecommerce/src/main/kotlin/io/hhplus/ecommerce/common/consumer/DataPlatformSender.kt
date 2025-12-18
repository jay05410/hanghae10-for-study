package io.hhplus.ecommerce.common.consumer

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import io.hhplus.ecommerce.common.client.DataPlatformClient
import io.hhplus.ecommerce.common.client.DataPlatformResponse
import io.hhplus.ecommerce.common.client.OrderInfoPayload
import mu.KotlinLogging
import org.springframework.stereotype.Service

/**
 * 데이터 플랫폼 전송 서비스
 *
 * Resilience4j Retry + Circuit Breaker 적용
 * - Retry: 일시적 실패 시 지수 백오프로 3회 재시도
 * - Circuit Breaker: 연속 실패 시 빠른 실패 처리
 *
 * 실행 순서: Retry → Circuit Breaker
 * (Retry가 모두 실패해야 Circuit Breaker 실패로 카운트)
 */
@Service
class DataPlatformSender(
    private val dataPlatformClient: DataPlatformClient
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 데이터 플랫폼에 주문 정보 전송
     *
     * @Retry: 실패 시 3회 재시도 (1s → 2s → 4s 지수 백오프)
     * @CircuitBreaker: 50% 이상 실패 시 30초간 Open
     *
     * @throws DataPlatformException 전송 실패 시
     */
    @Retry(name = "dataPlatform", fallbackMethod = "sendFallback")
    @CircuitBreaker(name = "dataPlatform", fallbackMethod = "sendFallback")
    fun send(payload: OrderInfoPayload): DataPlatformResponse {
        logger.debug("[DataPlatformSender] 전송 시도: orderId={}", payload.orderId)

        val response = dataPlatformClient.sendOrderInfo(payload)

        if (!response.success) {
            throw DataPlatformException("전송 실패: ${response.message}")
        }

        logger.info("[DataPlatformSender] 전송 성공: orderId={}", payload.orderId)
        return response
    }

    /**
     * Fallback 메서드 (Retry 소진 또는 Circuit Open 시 호출)
     */
    @Suppress("unused")
    private fun sendFallback(payload: OrderInfoPayload, ex: Exception): DataPlatformResponse {
        logger.warn(
            "[DataPlatformSender] Fallback 호출: orderId={}, error={}",
            payload.orderId, ex.message
        )

        // Circuit Breaker Open 상태인 경우
        if (ex is io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
            logger.error("[DataPlatformSender] Circuit Breaker OPEN 상태: orderId={}", payload.orderId)
        }

        throw DataPlatformException("전송 실패 (Fallback): ${ex.message}", ex)
    }
}

/**
 * 데이터 플랫폼 전송 예외
 */
class DataPlatformException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
