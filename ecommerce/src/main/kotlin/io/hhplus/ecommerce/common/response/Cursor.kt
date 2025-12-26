package io.hhplus.ecommerce.common.response

import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * 커서 기반 페이징 응답 객체
 *
 * Redis 캐싱 시 제네릭 타입 정보 보존을 위해 @JsonTypeInfo 적용
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
data class Cursor<T>(
    val contents: List<T>,
    val lastId: Long?
) {

    companion object {
        fun <T> from(content: List<T>, lastId: Long?): Cursor<T> = Cursor(content, lastId)
    }
}