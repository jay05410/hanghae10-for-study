package io.hhplus.ecommerce.common.sse

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

/**
 * SSE (Server-Sent Events) 컨트롤러
 *
 * 클라이언트가 실시간 알림을 수신하기 위한 SSE 연결 엔드포인트를 제공
 *
 */
@RestController
@RequestMapping("/api/sse")
@Tag(name = "SSE", description = "실시간 알림 구독 API")
class SseController(
    private val sseEmitterService: SseEmitterService
) {
    private val logger = KotlinLogging.logger {}

    /**
     * SSE 연결 구독
     *
     * 클라이언트가 이 엔드포인트에 연결하면 실시간 알림을 수신 가능
     *
     * @param userId 사용자 ID
     * @return SseEmitter SSE 연결
     */
    @GetMapping("/subscribe/{userId}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    @Operation(
        summary = "SSE 연결 구독",
        description = """
            사용자별 실시간 알림을 수신하기 위한 SSE 연결을 생성합니다.

            수신 가능한 이벤트:
            - connected: 연결 확인
            - coupon-issued: 쿠폰 발급 완료
            - order-completed: 주문 완료
            - payment-completed: 결제 완료
        """
    )
    fun subscribe(@PathVariable userId: Long): SseEmitter {
        logger.info("[SSE] 구독 요청: userId={}", userId)
        return sseEmitterService.createEmitter(userId)
    }

    /**
     * SSE 연결 해제
     *
     * @param userId 사용자 ID
     */
    @DeleteMapping("/unsubscribe/{userId}")
    @Operation(summary = "SSE 연결 해제", description = "SSE 연결을 명시적으로 종료합니다")
    fun unsubscribe(@PathVariable userId: Long) {
        logger.info("[SSE] 구독 해제 요청: userId={}", userId)
        sseEmitterService.removeEmitter(userId)
    }

    /**
     * 현재 SSE 연결 수 조회 (모니터링용)
     */
    @GetMapping("/connections/count")
    @Operation(summary = "SSE 연결 수 조회", description = "현재 활성화된 SSE 연결 수를 조회합니다")
    fun getConnectionCount(): Map<String, Int> {
        return mapOf("count" to sseEmitterService.getConnectionCount())
    }
}
