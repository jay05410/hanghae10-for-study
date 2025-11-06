package io.hhplus.ecommerce.common.exception

import io.hhplus.ecommerce.common.errorcode.CommonErrorCode
import io.hhplus.ecommerce.common.response.ApiResponse
import io.hhplus.ecommerce.common.response.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

/**
 * 전역 예외 처리 핸들러
 *
 * 모든 도메인 예외는 BusinessException을 통해 일관되게 처리
 * 요청 컨텍스트 정보와 함께 로깅
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }

    /**
     * 비즈니스 예외 처리
     *
     * 모든 BusinessException을 상속받은 예외를 처리
     * ErrorCode를 통해 일관된 에러 응답을 생성
     */
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(
        e: BusinessException,
        request: HttpServletRequest
    ): ErrorResponse {
        val context = request.toLogContext(e.data)
        e.logError(log, e.logLevel, e.message, context)

        val status = e.errorCode.httpStatus ?: 400

        return ResponseEntity
            .status(status)
            .body(ApiResponse.error(
                errorCode = e.errorCode.code,
                errorMessage = e.message,
                data = e.data.ifEmpty { null }
            ))
    }

    /**
     * 입력값 검증 실패 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        e: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ErrorResponse {
        val context = request.toLogContext()
        e.logError(log, Level.WARN, "입력값 검증 실패: ${e.message}", context)

        val firstError = e.bindingResult.fieldErrors.firstOrNull()
        val message = firstError?.defaultMessage ?: "유효하지 않은 입력값임"
        val field = firstError?.field

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(
                errorCode = CommonErrorCode.INVALID_INPUT.code,
                errorMessage = message,
                data = field?.let { mapOf("field" to it) }
            ))
    }

    /**
     * 예상하지 못한 런타임 오류 처리용
     */
    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(
        e: RuntimeException,
        request: HttpServletRequest
    ): ErrorResponse {
        val context = request.toLogContext()
        e.logError(log, Level.ERROR, "예상치 못한 오류 발생: ${e.message}", context)

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(
                errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR.code,
                errorMessage = e.message ?: "서버 내부 오류가 발생했음"
            ))
    }

    /**
     * 일반 Exception 처리
     */
    @ExceptionHandler(Exception::class)
    fun handleException(
        e: Exception,
        request: HttpServletRequest
    ): ErrorResponse {
        val context = request.toLogContext()
        e.logError(log, Level.ERROR, "예상치 못한 시스템 오류 발생: ${e.message}", context)

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(
                errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR.code,
                errorMessage = "서버 내부 오류 발생"
            ))
    }

    // --------------------- Helper Methods ---------------------

    /**
     * HttpServletRequest에서 주요 요청 정보를 추출해 로그 컨텍스트 맵으로 변환
     *
     * @param data 추가로 합치고 싶은 데이터(선택)
     * @return 로그용 컨텍스트 맵 (path, method, userAgent, timestamp 등 포함)
     */
    private fun HttpServletRequest.toLogContext(data: Map<String, Any>? = null): Map<String, Any> {
        return mutableMapOf<String, Any>(
            "path" to this.requestURI,
            "method" to this.method,
            "userAgent" to (this.getHeader("User-Agent") ?: "Unknown"),
            "timestamp" to LocalDateTime.now().toString()
        ).apply {
            data?.let { putAll(it) }
        }
    }

    /**
     * Exception을 로거와 함께 지정한 레벨/메시지/컨텍스트로 로깅
     *
     * @param logger 사용할 Logger 인스턴스
     * @param level 로그 레벨
     * @param message 로그 메시지(선택)
     * @param context 로그 컨텍스트(선택)
     */
    private fun Exception.logError(
        logger: Logger,
        level: Level,
        message: String? = null,
        context: Map<String, Any> = emptyMap()
    ) {
        val logMsg = "[$level] $message | Context: $context"
        when (level) {
            Level.ERROR -> logger.error(logMsg, this)
            Level.WARN -> logger.warn(logMsg)
            else -> logger.info(logMsg)
        }
    }
}