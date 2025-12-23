package io.hhplus.ecommerce.common.consumer

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import mu.KotlinLogging
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.stereotype.Component
import jakarta.annotation.PostConstruct

/**
 * Circuit Breaker 상태 전이 이벤트 리스너
 *
 * Circuit Breaker 상태에 따라 Kafka Consumer를 pause/resume
 * - OPEN/FORCED_OPEN: Consumer 일시 중지 (메시지 소비 중단)
 * - HALF_OPEN/CLOSED: Consumer 재개 (메시지 소비 시작)
 *
 * 이점:
 * - 외부 API 장애 시 불필요한 메시지 소비 방지
 * - DLQ 누적 방지
 * - 시스템 리소스 절약
 */
@Component
class CircuitBreakerEventListener(
    private val circuitBreakerRegistry: CircuitBreakerRegistry,
    private val kafkaListenerEndpointRegistry: KafkaListenerEndpointRegistry
) {
    private val logger = KotlinLogging.logger {}

    companion object {
        private const val CIRCUIT_BREAKER_NAME = "dataPlatform"
        private const val LISTENER_ID = "org.springframework.kafka.KafkaListenerEndpointContainer#0"
    }

    @PostConstruct
    fun registerEventListener() {
        val circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME)

        circuitBreaker.eventPublisher.onStateTransition { event ->
            val fromState = event.stateTransition.fromState
            val toState = event.stateTransition.toState

            logger.info(
                "[CircuitBreaker] 상태 전이: {} → {}",
                fromState, toState
            )

            when (toState) {
                CircuitBreaker.State.OPEN,
                CircuitBreaker.State.FORCED_OPEN -> {
                    pauseConsumer()
                }
                CircuitBreaker.State.HALF_OPEN,
                CircuitBreaker.State.CLOSED,
                CircuitBreaker.State.DISABLED -> {
                    resumeConsumer()
                }
                else -> {
                    logger.debug("[CircuitBreaker] 무시된 상태: {}", toState)
                }
            }
        }

        // 추가 이벤트 로깅
        circuitBreaker.eventPublisher.onFailureRateExceeded { event ->
            logger.warn(
                "[CircuitBreaker] 실패율 임계치 초과: failureRate={}%",
                event.failureRate
            )
        }

        circuitBreaker.eventPublisher.onSlowCallRateExceeded { event ->
            logger.warn(
                "[CircuitBreaker] 느린 호출 임계치 초과: slowCallRate={}%",
                event.slowCallRate
            )
        }

        logger.info("[CircuitBreaker] 이벤트 리스너 등록 완료: {}", CIRCUIT_BREAKER_NAME)
    }

    private fun pauseConsumer() {
        try {
            val container = kafkaListenerEndpointRegistry.getListenerContainer(LISTENER_ID)
            if (container != null && container.isRunning && !container.isPauseRequested) {
                container.pause()
                logger.warn("[CircuitBreaker] Kafka Consumer 일시 중지됨")
            }
        } catch (e: Exception) {
            logger.error("[CircuitBreaker] Consumer pause 실패: {}", e.message)
        }
    }

    private fun resumeConsumer() {
        try {
            val container = kafkaListenerEndpointRegistry.getListenerContainer(LISTENER_ID)
            if (container != null && container.isPauseRequested) {
                container.resume()
                logger.info("[CircuitBreaker] Kafka Consumer 재개됨")
            }
        } catch (e: Exception) {
            logger.error("[CircuitBreaker] Consumer resume 실패: {}", e.message)
        }
    }
}
