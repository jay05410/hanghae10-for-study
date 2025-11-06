package io.hhplus.ecommerce.common.errorcode

/**
 * 시스템에서 사용하는 모든 에러 코드의 인터페이스
 * 각 모듈/도메인별로 이를 구현한 enum 클래스를 정의하여 사용
 */
interface ErrorCode {
    /** 에러 코드 (예: "CART001", "PAYMENT001" 등) */
    val code: String

    /** 에러 메시지 */
    val message: String

    /** HTTP 상태 코드 */
    val httpStatus: Int?

    /**
     * 동적 파라미터를 메시지에 포함시키는 헬퍼 함수
     *
     * @param params 메시지에 삽입할 파라미터들 (키-값 쌍)
     * @return 파라미터가 포함된 메시지
     */
    fun withParams(vararg params: Pair<String, Any>): String {
        var result = message
        params.forEach { (key, value) ->
            result = result.replace("{$key}", value.toString())
        }
        return result
    }
}