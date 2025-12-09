package io.hhplus.ecommerce.common.filter

import io.hhplus.ecommerce.common.util.SnowflakeGenerator
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * TraceId 필터
 *
 * HTTP 요청마다 TraceId를 생성/전파하여 분산 추적 지원
 *
 * 동작 방식:
 * 1. 요청 헤더에서 X-Trace-Id 확인
 * 2. 없으면 Snowflake ID로 새로 생성
 * 3. MDC에 traceId 설정 (로깅에 자동 포함)
 * 4. 응답 헤더에 X-Trace-Id 설정
 * 5. 요청 처리 후 MDC 정리
 *
 * Snowflake 기반 TraceId 장점:
 * - 시간순 정렬 가능 (타임스탬프 포함)
 * - 생성 시점 추출 가능 (extractTimestamp)
 * - UUID보다 짧음 (16진수 16자리)
 *
 * 로그 출력 예시:
 * [traceId=1A2B3C4D5E6F7890] 주문 생성 요청
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class TraceIdFilter(
    private val snowflakeGenerator: SnowflakeGenerator
) : OncePerRequestFilter() {

    companion object {
        const val TRACE_ID_HEADER = "X-Trace-Id"
        const val MDC_TRACE_ID_KEY = "traceId"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val traceId = extractOrGenerateTraceId(request)

        try {
            // MDC에 traceId 설정 (로깅 프레임워크에서 자동 포함)
            MDC.put(MDC_TRACE_ID_KEY, traceId)

            // 응답 헤더에 traceId 설정 (클라이언트에서 확인 가능)
            response.setHeader(TRACE_ID_HEADER, traceId)

            filterChain.doFilter(request, response)
        } finally {
            // 요청 처리 완료 후 MDC 정리 (스레드 풀 재사용 시 오염 방지)
            MDC.remove(MDC_TRACE_ID_KEY)
        }
    }

    /**
     * 요청 헤더에서 TraceId 추출하거나 새로 생성
     */
    private fun extractOrGenerateTraceId(request: HttpServletRequest): String {
        val headerTraceId = request.getHeader(TRACE_ID_HEADER)

        return if (!headerTraceId.isNullOrBlank()) {
            // 기존 traceId 사용 (서비스 간 전파)
            headerTraceId
        } else {
            // Snowflake ID를 16진수 대문자로 생성
            snowflakeGenerator.nextId().toString(16).uppercase()
        }
    }

    /**
     * Actuator, 정적 리소스 등 필터링 제외
     */
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return path.startsWith("/actuator") ||
            path.startsWith("/health") ||
            path.startsWith("/favicon.ico") ||
            path.endsWith(".css") ||
            path.endsWith(".js") ||
            path.endsWith(".png") ||
            path.endsWith(".jpg")
    }
}
