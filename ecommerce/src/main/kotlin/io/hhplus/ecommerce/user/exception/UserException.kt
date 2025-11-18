package io.hhplus.ecommerce.user.exception

import io.hhplus.ecommerce.common.exception.BusinessException
import org.slf4j.event.Level

/**
 * 사용자 관련 예외 클래스
 *
 * 사용자 비즈니스 로직 처리 중 발생하는 예외들을 정의
 * 각 예외는 UserErrorCode를 통해 에러 코드와 메시지를 관리
 */
sealed class UserException(
    errorCode: UserErrorCode,
    message: String = errorCode.message,
    logLevel: Level = Level.WARN,
    data: Map<String, Any> = emptyMap(),
) : BusinessException(errorCode, message, logLevel, data) {

    /**
     * 사용자를 찾을 수 없음 예외
     */
    class UserNotFound(userId: Long) : UserException(
        errorCode = UserErrorCode.USER_NOT_FOUND,
        message = UserErrorCode.USER_NOT_FOUND.withParams("userId" to userId),
        data = mapOf("userId" to userId)
    )

    /**
     * 중복 이메일 예외
     */
    class DuplicateEmail(email: String) : UserException(
        errorCode = UserErrorCode.DUPLICATE_EMAIL,
        message = UserErrorCode.DUPLICATE_EMAIL.withParams("email" to email),
        data = mapOf("email" to email)
    )

    /**
     * 이메일 중복 예외 (별칭)
     */
    class EmailAlreadyExists(email: String) : UserException(
        errorCode = UserErrorCode.DUPLICATE_EMAIL,
        message = UserErrorCode.DUPLICATE_EMAIL.withParams("email" to email),
        data = mapOf("email" to email)
    )

    /**
     * 잘못된 휴대폰 번호 형식 예외
     */
    class InvalidPhoneFormat(phoneNumber: String) : UserException(
        errorCode = UserErrorCode.INVALID_PHONE_FORMAT,
        message = UserErrorCode.INVALID_PHONE_FORMAT.withParams("phoneNumber" to phoneNumber),
        data = mapOf("phoneNumber" to phoneNumber)
    )
}