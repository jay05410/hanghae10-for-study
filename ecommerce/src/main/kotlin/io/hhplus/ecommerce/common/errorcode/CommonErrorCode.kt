package io.hhplus.ecommerce.common.errorcode

/**
 * 공통 에러 코드 정의
 * 시스템 전반에서 사용되는 공통 오류에 대한 코드
 */
enum class CommonErrorCode(
    override val code: String,
    override val message: String,
    override val httpStatus: Int? = null,
) : ErrorCode {

    /**
     * 입력값 검증 실패
     */
    INVALID_INPUT(
        code = "COMMON001",
        message = "유효하지 않은 입력값임",
        httpStatus = 400
    ),

    /**
     * 서버 내부 오류
     */
    INTERNAL_SERVER_ERROR(
        code = "COMMON500",
        message = "서버 내부 오류가 발생했음",
        httpStatus = 500
    ),

    /**
     * 리소스를 찾을 수 없음
     */
    NOT_FOUND(
        code = "COMMON404",
        message = "요청한 리소스를 찾을 수 없음",
        httpStatus = 404
    ),

    /**
     * 동시성 처리 오류
     */
    CONCURRENCY_ERROR(
        code = "COMMON429",
        message = "동시성 처리 중 오류가 발생했음. 잠시 후 다시 시도해주세요",
        httpStatus = 429
    );
}