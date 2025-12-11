package io.hhplus.ecommerce.common.outbox.infra

import io.hhplus.ecommerce.common.outbox.EventHandler
import io.hhplus.ecommerce.config.event.EventRegistry
import mu.KotlinLogging
import org.springframework.stereotype.Component
import jakarta.annotation.PostConstruct

/**
 * 이벤트 핸들러 레지스트리
 *
 * 역할:
 * - 모든 EventHandler 빈 자동 수집
 * - 이벤트 타입별 핸들러 매핑 (다중 핸들러 지원)
 * - 핸들러 조회 API 제공
 */
@Component
class EventHandlerRegistry(
    private val handlers: List<EventHandler>
) {
    private val logger = KotlinLogging.logger {}
    private val handlerMap: MutableMap<String, MutableList<EventHandler>> = mutableMapOf()

    @PostConstruct
    fun init() {
        handlers.forEach { handler ->
            handler.supportedEventTypes().forEach { eventType ->
                handlerMap.getOrPut(eventType) { mutableListOf() }.add(handler)
                logger.info("[이벤트레지스트리] 핸들러 등록: eventType=$eventType, handler=${handler::class.simpleName}")
            }
        }

        val totalHandlers = handlerMap.values.sumOf { it.size }
        logger.info("[이벤트레지스트리] 초기화 완료: ${handlerMap.size}개 이벤트 타입, $totalHandlers 핸들러 등록")
        logEventFlowSummary()
    }

    fun getHandlers(eventType: String): List<EventHandler> {
        return handlerMap[eventType] ?: emptyList()
    }

    fun getHandler(eventType: String): EventHandler? {
        return handlerMap[eventType]?.firstOrNull()
    }

    fun getAllRegisteredEventTypes(): Set<String> {
        return handlerMap.keys.toSet()
    }

    private fun logEventFlowSummary() {
        logger.info("========================================")
        logger.info("[이벤트 흐름 요약]")
        logger.info("----------------------------------------")

        EventRegistry.catalog.forEach { (eventType, metadata) ->
            val registeredHandlers = handlerMap[eventType] ?: emptyList()
            val handlerStatus = if (registeredHandlers.isNotEmpty()) "O" else "X"
            val handlerNames = registeredHandlers.map { it::class.simpleName }.joinToString(", ")
            logger.info("[$handlerStatus] $eventType")
            logger.info("    발행: ${metadata.publisher}")
            logger.info("    등록된 핸들러: ${handlerNames.ifEmpty { "없음" }}")
            logger.info("    설명: ${metadata.description}")
        }

        val unregisteredTypes = handlerMap.keys.filter { !EventRegistry.catalog.containsKey(it) }
        if (unregisteredTypes.isNotEmpty()) {
            logger.warn("----------------------------------------")
            logger.warn("[경고] 카탈로그에 없는 이벤트 타입:")
            unregisteredTypes.forEach { logger.warn("  - $it") }
        }

        logger.info("========================================")
    }
}
