package io.hhplus.ecommerce.common.sse

import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap

/**
 * SSE (Server-Sent Events) Emitter 관리 서비스
 *
 * 클라이언트별 SSE 연결을 관리하고 실시간 알림을 발송
 *
 * 사용 사례:
 * - 쿠폰 발급 완료 알림
 * - 주문 완료 알림
 * - 결제 완료 알림
 */
@Service
class SseEmitterService {
    private val logger = KotlinLogging.logger {}

    /**
     * 사용자별 SSE Emitter 저장소
     * Key: userId, Value: SseEmitter
     */
    private val emitters = ConcurrentHashMap<Long, SseEmitter>()

    companion object {
        private const val DEFAULT_TIMEOUT = 30 * 60 * 1000L // 30분
    }

    /**
     * 사용자 SSE 연결 생성
     *
     * @param userId 사용자 ID
     * @return SseEmitter 클라이언트에 전달할 SSE Emitter
     */
    fun createEmitter(userId: Long): SseEmitter {
        // 기존 연결이 있으면 종료
        emitters[userId]?.complete()

        val emitter = SseEmitter(DEFAULT_TIMEOUT)

        // 연결 종료 시 정리
        emitter.onCompletion {
            logger.debug("[SSE] 연결 종료: userId={}", userId)
            emitters.remove(userId)
        }

        emitter.onTimeout {
            logger.debug("[SSE] 연결 타임아웃: userId={}", userId)
            emitters.remove(userId)
        }

        emitter.onError { e ->
            logger.warn("[SSE] 연결 에러: userId={}, error={}", userId, e.message)
            emitters.remove(userId)
        }

        emitters[userId] = emitter
        logger.info("[SSE] 연결 생성: userId={}, 현재 연결 수={}", userId, emitters.size)

        // 초기 연결 확인 이벤트 전송
        sendEvent(userId, SseEventType.CONNECTED, "연결되었습니다")

        return emitter
    }

    /**
     * 사용자에게 SSE 이벤트 전송
     *
     * @param userId 사용자 ID
     * @param eventType 이벤트 타입
     * @param data 이벤트 데이터
     */
    fun sendEvent(userId: Long, eventType: SseEventType, data: Any) {
        val emitter = emitters[userId]

        if (emitter == null) {
            logger.debug("[SSE] 연결 없음: userId={}, eventType={}", userId, eventType)
            return
        }

        try {
            emitter.send(
                SseEmitter.event()
                    .name(eventType.value)
                    .data(data)
            )
            logger.info("[SSE] 이벤트 전송 완료: userId={}, eventType={}", userId, eventType)
        } catch (e: Exception) {
            logger.warn("[SSE] 이벤트 전송 실패: userId={}, eventType={}, error={}", userId, eventType, e.message)
            emitters.remove(userId)
        }
    }

    /**
     * 연결 종료
     */
    fun removeEmitter(userId: Long) {
        emitters.remove(userId)?.complete()
        logger.info("[SSE] 연결 제거: userId={}", userId)
    }

    /**
     * 현재 연결 수 조회
     */
    fun getConnectionCount(): Int = emitters.size
}

/**
 * SSE 이벤트 타입
 */
enum class SseEventType(val value: String) {
    CONNECTED("connected"),
    COUPON_ISSUED("coupon-issued"),
    ORDER_COMPLETED("order-completed"),
    PAYMENT_COMPLETED("payment-completed")
}
