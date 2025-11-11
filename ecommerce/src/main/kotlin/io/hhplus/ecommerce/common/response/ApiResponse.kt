package io.hhplus.ecommerce.common.response

import org.springframework.http.ResponseEntity

/**
 * API 응답 공통 구조
 *
 * 모든 API 응답은 이 sealed class를 통해 일관된 형식으로 반환
 * - Success: 정상 처리 시 데이터 반환
 * - Error: 오류 발생 시 에러 정보 반환
 *
 * @param T 응답 데이터 타입
 */
sealed class ApiResponse<T> {
    abstract val success: Boolean

    /**
     * 성공 응답
     *
     * @property data 응답 데이터
     */
    data class Success<T>(
        override val success: Boolean = true,
        val data: T,
    ) : ApiResponse<T>()

    /**
     * 에러 응답
     *
     * @property errorCode 에러 코드
     * @property errorMessage 에러 메시지
     * @property data 추가 에러 정보 (선택)
     */
    data class Error<T>(
        override val success: Boolean = false,
        val errorCode: String,
        val errorMessage: String,
        val data: Map<String, Any>? = null,
    ) : ApiResponse<T>()

    companion object {
        /**
         * 성공 응답 생성
         *
         * @param data 응답 데이터
         * @return Success 응답
         */
        fun <T> success(data: T): ApiResponse<T> = Success(data = data)

        /**
         * 에러 응답 생성
         *
         * @param errorCode 에러 코드
         * @param errorMessage 에러 메시지
         * @param data 추가 에러 정보 (선택)
         * @return Error 응답
         */
        fun <T> error(
            errorCode: String,
            errorMessage: String,
            data: Map<String, Any>? = null,
        ): ApiResponse<T> = Error(errorCode = errorCode, errorMessage = errorMessage, data = data)
    }
}

/**
 * 에러 응답 타입 별칭
 *
 * GlobalExceptionHandler에서 반환하는 에러 응답 타입을 간결하게 표현하기 위한 별칭
 * ResponseEntity<ApiResponse<Unit>> 대신 ErrorResponse로 사용 가능
 *
 * 예시:
 * ```
 * fun handleException(): ErrorResponse {
 *     return ResponseEntity.status(400).body(ApiResponse.error(...))
 * }
 * ```
 */
typealias ErrorResponse = ResponseEntity<ApiResponse<Unit>>