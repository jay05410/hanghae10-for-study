package io.hhplus.tdd.common.error

/**
 * 포인트 도메인 에러 코드 정의
 * 포인트 관련 비즈니스 로직 처리 중 발생하는 오류에 대한 코드
 */
enum class PointErrorCode(
    override val code: String,
    override val message: String,
    override val httpStatus: Int? = 400,
) : ErrorCode {

    // ===== 잔고 관련 =====

    /**
     * 포인트 잔고 부족
     */
    INSUFFICIENT_BALANCE(
        code = "POINT001",
        message = "포인트 잔고가 부족합니다"
    ),

    // ===== 충전 관련 =====

    /**
     * 최소 충전 금액 미달
     */
    MINIMUM_CHARGE_AMOUNT(
        code = "POINT101",
        message = "최소 충전 금액은 1,000원임"
    ),

    /**
     * 최대 충전 금액 초과
     */
    MAXIMUM_CHARGE_AMOUNT(
        code = "POINT102",
        message = "최대 충전 금액은 1,000,000원임"
    ),

    /**
     * 충전 단위 불일치
     */
    INVALID_CHARGE_UNIT(
        code = "POINT103",
        message = "포인트 충전은 100원 단위로만 가능합니다"
    ),

    // ===== 사용 관련 =====

    /**
     * 최소 사용 금액 미달
     */
    MINIMUM_USE_AMOUNT(
        code = "POINT201",
        message = "최소 사용 금액은 100원임"
    ),

    /**
     * 사용 단위 불일치
     */
    INVALID_USE_UNIT(
        code = "POINT202",
        message = "포인트 사용은 100원 단위로만 가능합니다"
    ),

    /**
     * 일일 사용 한도 초과
     */
    DAILY_USE_LIMIT_EXCEEDED(
        code = "POINT203",
        message = "일일 사용 한도(100,000원)를 초과했음"
    );

    /**
     * 동적 파라미터를 포함한 메시지 생성
     *
     * @param params 메시지에 포함할 추가 정보
     * @return 파라미터가 포함된 에러 메시지
     */
    fun withParams(vararg params: Pair<String, Any>): String {
        val paramString = params.joinToString(", ") { "${it.first}: ${it.second}" }
        return if (params.isNotEmpty()) {
            "$message ($paramString)"
        } else {
            message
        }
    }
}
