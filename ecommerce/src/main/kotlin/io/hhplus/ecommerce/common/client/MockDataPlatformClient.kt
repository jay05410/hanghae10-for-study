package io.hhplus.ecommerce.common.client

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

/**
 * Mock 데이터 플랫폼 클라이언트
 *
 * mockAPI.io로 실제 HTTP 요청 전송
 *
 * 특징:
 * - 실제 외부 API 호출 (mockAPI.io)
 * - 전송 내용 상세 로깅
 * - 전송 통계 수집
 *
 * 설정:
 * - dataplatform.url: mockAPI.io 엔드포인트 URL
 * - dataplatform.enabled: false로 설정하면 로그만 출력
 */
@Component
class MockDataPlatformClient(
    restTemplateBuilder: RestTemplateBuilder
) : DataPlatformClient {

    private val restTemplate: RestTemplate = restTemplateBuilder
        .connectTimeout(Duration.ofSeconds(5))
        .readTimeout(Duration.ofSeconds(10))
        .build()

    private val logger = KotlinLogging.logger {}

    @Value("\${dataplatform.url:}")
    private lateinit var dataPlatformUrl: String

    @Value("\${dataplatform.enabled:true}")
    private var enabled: Boolean = true

    // 전송 통계
    private val totalSent = AtomicInteger(0)
    private val successCount = AtomicInteger(0)
    private val failureCount = AtomicInteger(0)

    override fun sendOrderInfo(orderInfo: OrderInfoPayload): DataPlatformResponse {
        totalSent.incrementAndGet()

        // URL이 설정되지 않았거나 비활성화된 경우 로그만 출력
        if (!enabled || dataPlatformUrl.isBlank()) {
            return logOnlyMode(orderInfo)
        }

        return try {
            // HTTP 요청 준비
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }
            val request = HttpEntity(orderInfo, headers)

            // mockAPI.io로 POST 요청
            val response = restTemplate.postForEntity(
                dataPlatformUrl,
                request,
                Map::class.java
            )

            if (response.statusCode.is2xxSuccessful) {
                successCount.incrementAndGet()
                logger.info {
                    "[DataPlatform] 전송 성공: orderId=${orderInfo.orderId}, orderNumber=${orderInfo.orderNumber}"
                }
                DataPlatformResponse(success = true, message = "전송 성공")
            } else {
                failureCount.incrementAndGet()
                logger.warn {
                    "[DataPlatform] 전송 실패: orderId=${orderInfo.orderId}, status=${response.statusCode}"
                }
                DataPlatformResponse(success = false, message = "HTTP 오류: ${response.statusCode}")
            }
        } catch (e: Exception) {
            failureCount.incrementAndGet()
            logger.error(e) {
                "[DataPlatform] 전송 오류: orderId=${orderInfo.orderId}, error=${e.message}"
            }
            DataPlatformResponse(success = false, message = "전송 오류: ${e.message}")
        }
    }

    private fun logOnlyMode(orderInfo: OrderInfoPayload): DataPlatformResponse {
        successCount.incrementAndGet()
        logger.info {
            "[DataPlatform] 로그 모드: orderId=${orderInfo.orderId}, orderNumber=${orderInfo.orderNumber}"
        }
        return DataPlatformResponse(success = true, message = "로그 모드")
    }
}
