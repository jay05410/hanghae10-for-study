package io.hhplus.ecommerce.common.errorcode

/**
 * 사용자 관련 에러 코드
 */
enum class UserErrorCode(
    override val code: String,
    override val message: String,
    override val httpStatus: Int? = null,
) : ErrorCode {

    USER_NOT_FOUND(
        code = "USER001",
        message = "사용자를 찾을 수 없습니다. ID: {userId}",
        httpStatus = 404
    ),

    DUPLICATE_EMAIL(
        code = "USER002",
        message = "이미 사용 중인 이메일입니다. 이메일: {email}",
        httpStatus = 409
    ),

    INVALID_PHONE_FORMAT(
        code = "USER003",
        message = "올바른 휴대폰 번호 형식이 아닙니다. 입력값: {phoneNumber}",
        httpStatus = 400
    );
}