package io.hhplus.tdd.common.errorcode

/**
 * 시스템에서 사용하는 모든 에러 코드의 인터페이스
 * 각 모듈/도메인별로 이를 구현한 enum 클래스를 정의하여 사용
 */
interface ErrorCode {
    /** 에러 코드 (예: "POINT001", "USER001" 등) */
    val code: String

    /** 에러 메시지 */
    val message: String

    /** HTTP 상태 코드 */
    val httpStatus: Int?
}
